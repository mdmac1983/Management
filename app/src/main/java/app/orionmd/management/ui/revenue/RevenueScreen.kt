package app.orionmd.management.ui.revenue

import androidx.compose.foundation.layout.Arrangement
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.GoodsTransactionEntity
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import app.orionmd.management.util.PdfExporter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class RevenuePeriod { DATE, WEEK, RANGE }

/** One row in the merged Entries list - either a rental booking or a goods sale. */
private sealed class RevenueEntry {
    abstract val dateEpochDay: Long
    data class FromBooking(val booking: BookingEntity) : RevenueEntry() {
        override val dateEpochDay get() = booking.dateEpochDay
    }
    data class FromGoodsSale(val sale: GoodsTransactionEntity) : RevenueEntry() {
        override val dateEpochDay get() = sale.dateEpochDay
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueScreen() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val repository = remember { AppSession.repository!! }
    val settings by repository.observeSettings().collectAsState(initial = null)
    val bookToDollarRate = settings?.bookToDollarRate ?: 8.0
    val mackerelToBookRate = settings?.mackerelToBookRate ?: 4.0
    var pdfMessage by remember { mutableStateOf<String?>(null) }

    var period by remember { mutableStateOf(RevenuePeriod.WEEK) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var rangeStartCustom by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var rangeEndCustom by remember { mutableStateOf(LocalDate.now()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val rangeStart: LocalDate
    val rangeEnd: LocalDate
    val periodLabel: String
    when (period) {
        RevenuePeriod.DATE -> {
            rangeStart = anchorDate; rangeEnd = anchorDate
            periodLabel = DateTimeUtils.fullDate(anchorDate, strings.locale)
        }
        RevenuePeriod.WEEK -> {
            rangeStart = DateTimeUtils.weekStart(anchorDate); rangeEnd = DateTimeUtils.weekEnd(anchorDate)
            periodLabel = DateTimeUtils.weekRangeLabel(rangeStart, strings.locale)
        }
        RevenuePeriod.RANGE -> {
            rangeStart = rangeStartCustom; rangeEnd = rangeEndCustom
            periodLabel = "${DateTimeUtils.mdy(rangeStart, strings.locale)} – ${DateTimeUtils.mdy(rangeEnd, strings.locale)}"
        }
    }

    val bookings by repository
        .observeBookingsForRange(rangeStart.toEpochDay(), rangeEnd.toEpochDay())
        .collectAsState(initial = emptyList())
    val goodsSales by repository
        .observeGoodsTransactionsForRange(rangeStart.toEpochDay(), rangeEnd.toEpochDay())
        .collectAsState(initial = emptyList())
    val goodsSalesOnly = goodsSales.filter { it.type == app.orionmd.management.data.entity.GoodsTransactionType.OUT }

    val bookingRevenue = bookings.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
    val goodsRevenue = goodsSalesOnly.sumOf { it.quantity * it.unitSalePrice }
    val goodsCost = goodsSalesOnly.sumOf { it.quantity * it.unitCost }
    val goodsProfit = goodsRevenue - goodsCost
    val totalRevenue = bookingRevenue + goodsRevenue
    val paidTotal = bookings.filter { it.paymentStatus == PaymentStatus.PAID }
        .sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
    val partialPaidTotal = bookings.filter { it.paymentStatus == PaymentStatus.PARTIAL }
        .sumOf { Money.toDollars(it.amountPaid, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
    // Goods sales are recorded as already-completed transactions (no partial/unpaid concept), so
    // their full revenue counts as "paid" the moment they're logged.
    val totalPaidSoFar = paidTotal + partialPaidTotal + goodsRevenue
    val totalUnpaid = totalRevenue - totalPaidSoFar

    val mergedEntries: List<RevenueEntry> =
        (bookings.map { RevenueEntry.FromBooking(it) } + goodsSalesOnly.map { RevenueEntry.FromGoodsSale(it) })
            .sortedByDescending { it.dateEpochDay }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val entries = mergedEntries.map { entry ->
                when (entry) {
                    is RevenueEntry.FromBooking -> {
                        val booking = entry.booking
                        PdfExporter.Entry(
                            listOf(
                                "${DateTimeUtils.mdy(LocalDate.ofEpochDay(booking.dateEpochDay), strings.locale)} — ${booking.customerName} (${booking.serviceName})",
                                "${Money.formatDollars(Money.toDollars(booking.paymentAmount, booking.paymentUnit, bookToDollarRate, mackerelToBookRate))} — ${strings.statusLabel(booking.paymentStatus)}"
                            )
                        )
                    }
                    is RevenueEntry.FromGoodsSale -> {
                        val sale = entry.sale
                        PdfExporter.Entry(
                            listOf(
                                "${DateTimeUtils.mdy(LocalDate.ofEpochDay(sale.dateEpochDay), strings.locale)} — ${strings.goodsSoldPrefix}${sale.goodsName} × ${sale.quantity}",
                                Money.formatDollars(sale.quantity * sale.unitSalePrice)
                            )
                        )
                    }
                }
            }
            val result = PdfExporter.export(
                context = context,
                uri = uri,
                title = strings.revenueScreenTitle,
                subtitle = periodLabel,
                summaryLines = listOf(
                    "${strings.bottomLineTitle}: ${Money.formatDollars(totalRevenue)}",
                    "${strings.paidLabel}: ${Money.formatDollars(totalPaidSoFar)}",
                    "${strings.unpaidLabel}: ${Money.formatDollars(totalUnpaid.coerceAtLeast(0.0))}",
                    "${strings.goodsProfitLabel}: ${Money.formatDollars(goodsProfit)}"
                ),
                sectionLabel = strings.entriesLabel,
                entries = entries,
                emptyMessage = strings.noRevenueEntries
            )
            pdfMessage = if (result.isSuccess) strings.pdfSavedMessage else "${strings.pdfFailedPrefix}${result.exceptionOrNull()?.message}"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.revenueScreenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = period == RevenuePeriod.DATE, onClick = { period = RevenuePeriod.DATE }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(strings.dateLabel) }
                SegmentedButton(selected = period == RevenuePeriod.WEEK, onClick = { period = RevenuePeriod.WEEK }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(strings.weekLabel) }
                SegmentedButton(selected = period == RevenuePeriod.RANGE, onClick = { period = RevenuePeriod.RANGE }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(strings.rangeLabel) }
            }
            Spacer(Modifier.height(12.dp))

            when (period) {
                RevenuePeriod.DATE -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { anchorDate = anchorDate.minusDays(1) }) { Icon(Icons.Filled.ChevronLeft, null) }
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(periodLabel)
                    }
                    IconButton(onClick = { anchorDate = anchorDate.plusDays(1) }) { Icon(Icons.Filled.ChevronRight, null) }
                }
                RevenuePeriod.WEEK -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { anchorDate = anchorDate.minusWeeks(1) }) { Icon(Icons.Filled.ChevronLeft, null) }
                    Text(periodLabel, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { anchorDate = anchorDate.plusWeeks(1) }) { Icon(Icons.Filled.ChevronRight, null) }
                }
                RevenuePeriod.RANGE -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showStartPicker = true }) { Text("${strings.fromLabel}: ${DateTimeUtils.mdy(rangeStartCustom, strings.locale)}") }
                    Text("→")
                    TextButton(onClick = { showEndPicker = true }) { Text("${strings.toLabel}: ${DateTimeUtils.mdy(rangeEndCustom, strings.locale)}") }
                }
            }

            Spacer(Modifier.height(16.dp))
            BottomLineCard(totalRevenue = totalRevenue, paid = totalPaidSoFar, unpaid = totalUnpaid)
            if (goodsSalesOnly.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                GoodsProfitCard(goodsRevenue = goodsRevenue, goodsCost = goodsCost, goodsProfit = goodsProfit)
            }
            Spacer(Modifier.height(16.dp))
            Text(strings.entriesLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (mergedEntries.isEmpty()) {
                    Text(
                        strings.noRevenueEntries,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(mergedEntries) { entry ->
                            when (entry) {
                                is RevenueEntry.FromBooking -> RevenueRow(entry.booking, bookToDollarRate, mackerelToBookRate)
                                is RevenueEntry.FromGoodsSale -> GoodsSaleRow(entry.sale)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { pdfLauncher.launch("revenue_${rangeStart}_${rangeEnd}.pdf") },
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

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = anchorDate.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { anchorDate = millisToLocalDate(it) }
                    showDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }
        ) { DatePicker(state = state) }
    }
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = rangeStartCustom.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { rangeStartCustom = millisToLocalDate(it) }
                    showStartPicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(strings.cancel) } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = rangeEndCustom.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { rangeEndCustom = millisToLocalDate(it) }
                    showEndPicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(strings.cancel) } }
        ) { DatePicker(state = state) }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun BottomLineCard(totalRevenue: Double, paid: Double, unpaid: Double) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.bottomLineTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(Money.formatDollars(totalRevenue), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(strings.paidLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(Money.formatDollars(paid))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.unpaidLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    Text(Money.formatDollars(unpaid.coerceAtLeast(0.0)))
                }
            }
        }
    }
}

@Composable
private fun GoodsProfitCard(goodsRevenue: Double, goodsCost: Double, goodsProfit: Double) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.goodsSection, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(strings.goodsRevenueLabel, style = MaterialTheme.typography.labelLarge)
                    Text(Money.formatDollars(goodsRevenue))
                }
                Column {
                    Text(strings.goodsCostLabel, style = MaterialTheme.typography.labelLarge)
                    Text(Money.formatDollars(goodsCost))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.goodsProfitLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(Money.formatDollars(goodsProfit), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun RevenueRow(booking: BookingEntity, bookToDollarRate: Double, mackerelToBookRate: Double) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(booking.customerName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${DateTimeUtils.shortDate(LocalDate.ofEpochDay(booking.dateEpochDay), strings.locale)} • ${booking.serviceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.formatDollars(Money.toDollars(booking.paymentAmount, booking.paymentUnit, bookToDollarRate, mackerelToBookRate)))
            Text(
                strings.statusLabel(booking.paymentStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = when (booking.paymentStatus) {
                    PaymentStatus.PAID -> MaterialTheme.colorScheme.secondary
                    PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun GoodsSaleRow(sale: GoodsTransactionEntity) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("${strings.goodsSoldPrefix}${sale.goodsName}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${DateTimeUtils.shortDate(LocalDate.ofEpochDay(sale.dateEpochDay), strings.locale)} • ${strings.quantityAbbrev}${sale.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.formatDollars(sale.quantity * sale.unitSalePrice))
            Text(strings.goodsLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}
