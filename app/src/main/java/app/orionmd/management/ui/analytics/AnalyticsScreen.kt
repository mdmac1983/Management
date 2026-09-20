package app.orionmd.management.ui.analytics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.orionmd.management.ui.common.SectionCard
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.CustomerEntity
import app.orionmd.management.data.entity.GoodsEntity
import app.orionmd.management.data.entity.GoodsTransactionEntity
import app.orionmd.management.data.entity.GoodsTransactionType
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import app.orionmd.management.util.PdfExporter

private enum class AnalyticsView { CLIENTS, BREAKDOWNS, INVENTORY }
private enum class BreakdownDimension { PAYMENT_METHOD, SERVICE, CLIENT }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val repository = remember { AppSession.repository!! }
    val bookings by repository.observeAllBookings().collectAsState(initial = emptyList())
    val settings by repository.observeSettings().collectAsState(initial = null)
    val bookToDollarRate = settings?.bookToDollarRate ?: 8.0
    val mackerelToBookRate = settings?.mackerelToBookRate ?: 4.0
    var pdfMessage by remember { mutableStateOf<String?>(null) }

    val goods by repository.observeGoods().collectAsState(initial = emptyList())
    val goodsTransactions by repository.observeGoodsTransactions().collectAsState(initial = emptyList())

    val customers by repository.observeCustomers().collectAsState(initial = emptyList())
    val customersById = remember(customers) { customers.associateBy { it.id } }
    var contractUsedMinutes by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(customers, bookings) {
        contractUsedMinutes = customers
            .filter { it.hasContract && it.contractStartEpochDay != null }
            .associate { c -> c.id to repository.getPhoneTimeMinutes(c.id, c.contractStartEpochDay) }
    }

    var view by remember { mutableStateOf(AnalyticsView.CLIENTS) }
    var dimension by remember { mutableStateOf(BreakdownDimension.SERVICE) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val entries: List<PdfExporter.Entry> = if (view == AnalyticsView.CLIENTS) {
                bookings.groupBy { it.customerName }.entries.sortedByDescending { (_, list) ->
                    list.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
                }.map { (name, clientBookings) ->
                    val totalSpent = clientBookings.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
                    val preferredMethod = clientBookings.groupingBy { it.paymentMethod }.eachCount().maxByOrNull { it.value }?.key ?: "—"
                    PdfExporter.Entry(
                        listOf(
                            "$name — ${Money.formatDollars(totalSpent)}",
                            "${strings.preferredPaymentPrefix}$preferredMethod",
                            strings.bookingsSummary(clientBookings.size, clientBookings.count { it.paymentStatus != PaymentStatus.PAID })
                        )
                    )
                }
            } else {
                val grouped: Map<String, Double> = when (dimension) {
                    BreakdownDimension.PAYMENT_METHOD -> bookings.groupBy { it.paymentMethod }
                    BreakdownDimension.SERVICE -> bookings.groupBy { it.serviceName }
                    BreakdownDimension.CLIENT -> bookings.groupBy { it.customerName }
                }.mapValues { (_, list) -> list.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) } }
                grouped.entries.sortedByDescending { it.value }.map { (label, amount) ->
                    PdfExporter.Entry(listOf("$label — ${Money.formatDollars(amount)}"))
                }
            }
            val result = PdfExporter.export(
                context = context,
                uri = uri,
                title = strings.analyticsScreenTitle,
                subtitle = if (view == AnalyticsView.CLIENTS) strings.clientsLabel else strings.breakdownsLabel,
                entries = entries,
                emptyMessage = strings.noAnalyticsData
            )
            pdfMessage = if (result.isSuccess) strings.pdfSavedMessage else "${strings.pdfFailedPrefix}${result.exceptionOrNull()?.message}"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.analyticsScreenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = view == AnalyticsView.CLIENTS, onClick = { view = AnalyticsView.CLIENTS }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(strings.clientsLabel) }
                SegmentedButton(selected = view == AnalyticsView.BREAKDOWNS, onClick = { view = AnalyticsView.BREAKDOWNS }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(strings.breakdownsLabel) }
                SegmentedButton(selected = view == AnalyticsView.INVENTORY, onClick = { view = AnalyticsView.INVENTORY }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(strings.inventoryLabel) }
            }
            Spacer(Modifier.height(12.dp))

            if (bookings.isEmpty() && view != AnalyticsView.INVENTORY) {
                Text(
                    strings.noAnalyticsData,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 24.dp)
                )
                return@Scaffold
            }

            Column(modifier = Modifier.weight(1f)) {
                if (view == AnalyticsView.INVENTORY) {
                    InventoryAnalytics(goods = goods, transactions = goodsTransactions)
                } else if (view == AnalyticsView.CLIENTS) {
                    var selectedClient by remember { mutableStateOf<Pair<Long, String>?>(null) }
                    ClientProfiles(
                        bookings = bookings,
                        bookToDollarRate = bookToDollarRate,
                        mackerelToBookRate = mackerelToBookRate,
                        customersById = customersById,
                        contractUsedMinutes = contractUsedMinutes,
                        onClientSelected = { id, name -> selectedClient = id to name }
                    )
                    selectedClient?.let { (id, name) ->
                        ClientDetailDialog(
                            customerId = id,
                            fallbackName = name,
                            clientBookings = bookings.filter { it.customerId == id },
                            bookToDollarRate = bookToDollarRate,
                            mackerelToBookRate = mackerelToBookRate,
                            onDismiss = { selectedClient = null }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            BreakdownDimension.SERVICE to strings.dimensionService,
                            BreakdownDimension.PAYMENT_METHOD to strings.dimensionPaymentMethod,
                            BreakdownDimension.CLIENT to strings.dimensionClient
                        ).forEach { (dim, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = dimension == dim,
                                onClick = { dimension = dim },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BreakdownList(bookings = bookings, dimension = dimension, bookToDollarRate = bookToDollarRate, mackerelToBookRate = mackerelToBookRate)
                }
            }

            if (view != AnalyticsView.INVENTORY) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pdfLauncher.launch("analytics.pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.saveAsPdf)
                }
                pdfMessage?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ClientProfiles(
    bookings: List<BookingEntity>,
    bookToDollarRate: Double,
    mackerelToBookRate: Double,
    customersById: Map<Long, CustomerEntity>,
    contractUsedMinutes: Map<Long, Long>,
    onClientSelected: (customerId: Long, name: String) -> Unit
) {
    val strings = LocalStrings.current
    val byClient = bookings.groupBy { it.customerId to it.customerName }.toList()
        .sortedBy { (idAndName, _) -> idAndName.second.lowercase() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(byClient) { (idAndName, clientBookings) ->
            val (customerId, name) = idAndName
            val totalSpent = clientBookings.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
            val preferredMethod = clientBookings.groupingBy { it.paymentMethod }.eachCount().maxByOrNull { it.value }?.key ?: strings.noneDash
            val services = clientBookings.groupingBy { it.serviceName }.eachCount().entries.sortedByDescending { it.value }
            val unpaidCount = clientBookings.count { it.paymentStatus != PaymentStatus.PAID }
            val customer = customersById[customerId]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClientSelected(customerId, name) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(Money.formatDollars(totalSpent), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${strings.preferredPaymentPrefix}$preferredMethod", style = MaterialTheme.typography.bodyMedium)
                    Text(strings.bookingsSummary(clientBookings.size, unpaidCount), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.servicesPrefix + services.joinToString(", ") { "${it.key} (${it.value})" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    if (customer != null && customer.hasContract) {
                        val grantedMinutes = (customer.contractHours * 60).toLong()
                        val usedMinutes = contractUsedMinutes[customerId] ?: 0L
                        val remainingMinutes = grantedMinutes - usedMinutes
                        val remainingOrOver = if (remainingMinutes >= 0)
                            strings.remainingSuffix(DateTimeUtils.formatDuration(remainingMinutes))
                        else
                            strings.overSuffix(DateTimeUtils.formatDuration(-remainingMinutes))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.contractStatusPrefix + strings.contractCardLine(DateTimeUtils.formatDuration(usedMinutes), remainingOrOver),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingMinutes >= 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inventory-focused analytics: low-stock alerts, top-selling goods, current inventory valuation,
 * and overall profit margin on goods sold — the "anything I forgot" suggestions folded into
 * Analytics rather than Revenue, since these are forward-looking/operational rather than a ledger.
 */
@Composable
private fun InventoryAnalytics(goods: List<GoodsEntity>, transactions: List<GoodsTransactionEntity>) {
    val strings = LocalStrings.current
    val lowStock = goods.filter { it.quantityOnHand <= it.lowStockThreshold }
    val outTransactions = transactions.filter { it.type == GoodsTransactionType.OUT }
    val topSelling = outTransactions.groupBy { it.goodsName }
        .mapValues { (_, list) -> list.sumOf { it.quantity } }
        .entries.sortedByDescending { it.value }
        .take(5)
    val totalUnitsInStock = goods.sumOf { it.quantityOnHand }
    val valueAtCost = goods.sumOf { it.quantityOnHand * it.costPrice }
    val valueAtSale = goods.sumOf { it.quantityOnHand * it.salePrice }
    val potentialProfit = valueAtSale - valueAtCost
    val totalRevenue = outTransactions.sumOf { it.quantity * it.unitSalePrice }
    val totalCost = outTransactions.sumOf { it.quantity * it.unitCost }
    val margin = if (totalRevenue > 0) ((totalRevenue - totalCost) / totalRevenue) * 100 else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(title = strings.lowStockAlertsTitle) {
                if (lowStock.isEmpty()) {
                    Text(
                        strings.noLowStockItems,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lowStock.forEach { item ->
                            Text(
                                strings.lowStockRow(item.name, item.quantityOnHand, item.lowStockThreshold),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = strings.topSellingGoodsTitle) {
                if (topSelling.isEmpty()) {
                    Text(
                        strings.noGoodsSalesYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topSelling.forEach { (name, qty) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(strings.unitsSoldSuffix(qty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = strings.inventoryValuationTitle) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledValueRow(strings.totalUnitsInStockLabel, totalUnitsInStock.toString())
                    LabeledValueRow(strings.valueAtCostLabel, Money.formatDollars(valueAtCost))
                    LabeledValueRow(strings.valueAtSaleLabel, Money.formatDollars(valueAtSale))
                    LabeledValueRow(strings.potentialProfitLabel, Money.formatDollars(potentialProfit))
                }
            }
        }
        item {
            SectionCard(title = strings.profitMarginTitle) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledValueRow(strings.goodsRevenueLabel, Money.formatDollars(totalRevenue))
                    LabeledValueRow(strings.goodsCostLabel, Money.formatDollars(totalCost))
                    LabeledValueRow(strings.goodsProfitLabel, Money.formatDollars(totalRevenue - totalCost))
                    LabeledValueRow(strings.overallMarginLabel, "${"%.1f".format(margin)}%")
                }
            }
        }
    }
}

@Composable
private fun LabeledValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BreakdownList(bookings: List<BookingEntity>, dimension: BreakdownDimension, bookToDollarRate: Double, mackerelToBookRate: Double) {
    val grouped: Map<String, Double> = when (dimension) {
        BreakdownDimension.PAYMENT_METHOD -> bookings.groupBy { it.paymentMethod }
        BreakdownDimension.SERVICE -> bookings.groupBy { it.serviceName }
        BreakdownDimension.CLIENT -> bookings.groupBy { it.customerName }
    }.mapValues { (_, list) -> list.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) } }

    val total = grouped.values.sum().takeIf { it > 0 } ?: 1.0
    val sorted = grouped.entries.sortedByDescending { it.value }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sorted) { (label, amount) ->
            val fraction = (amount / total).toFloat().coerceIn(0f, 1f)
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(Money.formatDollars(amount), style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
