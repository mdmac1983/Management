package app.orionmd.management.ui.database

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.RentalsRepository
import app.orionmd.management.data.SellGoodsResult
import app.orionmd.management.data.entity.GoodsEntity
import app.orionmd.management.ui.common.EditableListSection
import app.orionmd.management.ui.common.SectionCard
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class DatabaseSection { CUSTOMERS, SERVICES, GOODS, RATES, PAYMENT_METHODS }

/**
 * The Database tab: Customers, Services, Goods, Rates, and Payment Methods each get their own
 * add/rename/delete list here (Services/Rates/Payment Methods used to live under Settings; moving
 * them here keeps every list this app "schedules from" in one place). Every list here is the same
 * data Schedule's booking form reads from, so adding an item here makes it immediately available
 * when scheduling an appointment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseScreen() {
    val strings = LocalStrings.current
    val repository = remember { AppSession.repository!! }
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf(DatabaseSection.CUSTOMERS) }

    val customers by repository.observeCustomers().collectAsState(initial = emptyList())
    val services by repository.observeServices().collectAsState(initial = emptyList())
    val rateTypes by repository.observeRateTypes().collectAsState(initial = emptyList())
    val paymentMethods by repository.observePaymentMethods().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.tabDatabase) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = section == DatabaseSection.CUSTOMERS, onClick = { section = DatabaseSection.CUSTOMERS }, label = { Text(strings.customersSection) })
                FilterChip(selected = section == DatabaseSection.SERVICES, onClick = { section = DatabaseSection.SERVICES }, label = { Text(strings.servicesSection) })
                FilterChip(selected = section == DatabaseSection.GOODS, onClick = { section = DatabaseSection.GOODS }, label = { Text(strings.goodsSection) })
                FilterChip(selected = section == DatabaseSection.RATES, onClick = { section = DatabaseSection.RATES }, label = { Text(strings.ratesSection) })
                FilterChip(selected = section == DatabaseSection.PAYMENT_METHODS, onClick = { section = DatabaseSection.PAYMENT_METHODS }, label = { Text(strings.paymentMethodsSection) })
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                when (section) {
                    DatabaseSection.CUSTOMERS -> item {
                        SectionCard(title = strings.customersSection) {
                            EditableListSection(
                                items = customers.map { it.id to it.name },
                                addLabel = strings.addCustomerLabel,
                                onAdd = { name -> scope.launch { repository.getOrCreateCustomer(name) } },
                                onRename = { id, newName ->
                                    customers.firstOrNull { it.id == id }?.let { scope.launch { repository.updateCustomer(it.copy(name = newName)) } }
                                },
                                onDelete = { id ->
                                    customers.firstOrNull { it.id == id }?.let { scope.launch { repository.deleteCustomer(it) } }
                                }
                            )
                        }
                    }
                    DatabaseSection.SERVICES -> item {
                        SectionCard(title = strings.servicesSection) {
                            EditableListSection(
                                items = services.map { it.id to it.name },
                                addLabel = strings.addServiceLabel,
                                onAdd = { name -> scope.launch { repository.addService(name, requiresTimeSlot = true) } },
                                onRename = { id, newName ->
                                    services.firstOrNull { it.id == id }?.let { scope.launch { repository.updateService(it.copy(name = newName)) } }
                                },
                                onDelete = { id ->
                                    services.firstOrNull { it.id == id }?.let { scope.launch { repository.deleteService(it) } }
                                }
                            )
                        }
                    }
                    DatabaseSection.RATES -> item {
                        SectionCard(title = strings.ratesSection) {
                            EditableListSection(
                                items = rateTypes.map { it.id to it.name },
                                addLabel = strings.addRateTypeLabel,
                                onAdd = { name -> scope.launch { repository.addRateType(name) } },
                                onRename = { id, newName ->
                                    rateTypes.firstOrNull { it.id == id }?.let { scope.launch { repository.updateRateType(it.copy(name = newName)) } }
                                },
                                onDelete = { id ->
                                    rateTypes.firstOrNull { it.id == id }?.let { scope.launch { repository.deleteRateType(it) } }
                                }
                            )
                        }
                    }
                    DatabaseSection.PAYMENT_METHODS -> item {
                        SectionCard(title = strings.paymentMethodsSection) {
                            EditableListSection(
                                items = paymentMethods.map { it.id to it.name },
                                addLabel = strings.addPaymentMethodLabel,
                                onAdd = { name -> scope.launch { repository.addPaymentMethod(name) } },
                                onRename = { id, newName ->
                                    paymentMethods.firstOrNull { it.id == id }?.let { scope.launch { repository.updatePaymentMethod(it.copy(name = newName)) } }
                                },
                                onDelete = { id ->
                                    paymentMethods.firstOrNull { it.id == id }?.let { scope.launch { repository.deletePaymentMethod(it) } }
                                }
                            )
                        }
                    }
                    DatabaseSection.GOODS -> item {
                        GoodsSection(repository = repository)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoodsSection(repository: RentalsRepository) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val goods by repository.observeGoods().collectAsState(initial = emptyList())

    var showAddGoods by remember { mutableStateOf(false) }
    var selectedGoods by remember { mutableStateOf<GoodsEntity?>(null) }

    SectionCard(title = strings.goodsSection) {
        Column {
            if (goods.isEmpty()) {
                Text(
                    strings.noGoodsYet,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                goods.forEach { item ->
                    GoodsRow(item = item, onClick = { selectedGoods = item })
                    Divider()
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showAddGoods = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(strings.addGoodsLabel)
            }
        }
    }

    if (showAddGoods) {
        AddGoodsDialog(
            onDismiss = { showAddGoods = false },
            onSave = { name, cost, price, qty ->
                scope.launch { repository.addGoods(name, cost, price, qty) }
                showAddGoods = false
            }
        )
    }

    selectedGoods?.let { item ->
        GoodsDetailDialog(
            goods = item,
            repository = repository,
            onDismiss = { selectedGoods = null }
        )
    }
}

@Composable
private fun GoodsRow(item: GoodsEntity, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val lowStock = item.quantityOnHand <= item.lowStockThreshold
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${strings.quantityAbbrev}${item.quantityOnHand} • ${Money.formatDollars(item.salePrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            if (lowStock) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = strings.lowStockLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.lowStockLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AddGoodsDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Int) -> Unit) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addGoodsLabel) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.goodsNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost, onValueChange = { cost = it }, label = { Text(strings.goodsCostPriceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it }, label = { Text(strings.goodsSalePriceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it }, label = { Text(strings.goodsInitialQuantityLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val costVal = cost.toDoubleOrNull()
                val priceVal = price.toDoubleOrNull()
                val qtyVal = qty.toIntOrNull()
                error = when {
                    name.isBlank() -> strings.errorEnterGoodsName
                    costVal == null || costVal < 0 -> strings.errorInvalidPrice
                    priceVal == null || priceVal < 0 -> strings.errorInvalidPrice
                    qtyVal == null || qtyVal < 0 -> strings.errorInvalidQuantity
                    else -> null
                }
                if (error == null) onSave(name.trim(), costVal!!, priceVal!!, qtyVal!!)
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun GoodsDetailDialog(goods: GoodsEntity, repository: RentalsRepository, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var costText by remember { mutableStateOf(formatAmount(goods.costPrice)) }
    var priceText by remember { mutableStateOf(formatAmount(goods.salePrice)) }
    var restockQtyText by remember { mutableStateOf("") }
    var sellQtyText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goods.name) },
        text = {
            Column {
                Text("${strings.quantityAbbrev}${goods.quantityOnHand}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = costText, onValueChange = { costText = it }, label = { Text(strings.goodsCostPriceLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = priceText, onValueChange = { priceText = it }, label = { Text(strings.goodsSalePriceLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    val c = costText.toDoubleOrNull(); val p = priceText.toDoubleOrNull()
                    if (c != null && p != null) scope.launch { repository.updateGoods(goods.copy(costPrice = c, salePrice = p)) }
                }) { Text(strings.updatePricesLabel) }

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                Text(strings.restockLabel, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = restockQtyText, onValueChange = { restockQtyText = it }, label = { Text(strings.quantityLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val n = restockQtyText.toIntOrNull()
                        if (n != null && n > 0) {
                            scope.launch {
                                repository.restockGoods(goods, n, costText.toDoubleOrNull() ?: goods.costPrice, LocalDate.now().toEpochDay())
                                restockQtyText = ""
                                message = strings.restockedMessage
                            }
                        }
                    }) { Text(strings.add) }
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.sellLabel, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sellQtyText, onValueChange = { sellQtyText = it }, label = { Text(strings.quantityLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val n = sellQtyText.toIntOrNull()
                        if (n != null && n > 0) {
                            scope.launch {
                                when (repository.sellGoods(goods, n, priceText.toDoubleOrNull() ?: goods.salePrice, LocalDate.now().toEpochDay())) {
                                    is SellGoodsResult.Success -> { sellQtyText = ""; message = strings.soldMessage }
                                    SellGoodsResult.InsufficientStock -> message = strings.insufficientStockMessage
                                }
                            }
                        }
                    }) { Text(strings.sellLabel) }
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    scope.launch { repository.deleteGoods(goods) }
                    onDismiss()
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
