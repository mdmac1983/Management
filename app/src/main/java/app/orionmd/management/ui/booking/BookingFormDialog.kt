package app.orionmd.management.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.orionmd.management.data.RentalsRepository
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.CustomerEntity
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.data.entity.PaymentUnit
import app.orionmd.management.data.entity.ServiceEntity
import app.orionmd.management.ui.common.ClearContractConfirmDialog
import app.orionmd.management.ui.common.ContractSetupDialog
import app.orionmd.management.ui.common.EditableDropdown
import app.orionmd.management.ui.common.TimePickerField
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormDialog(
    dateEpochDay: Long,
    existing: BookingEntity?,
    customerNames: List<String>,
    services: List<ServiceEntity>,
    rateTypeNames: List<String>,
    paymentMethodNames: List<String>,
    repository: RentalsRepository,
    onDismiss: () -> Unit,
    onAddCustomer: (String) -> Unit,
    onAddService: (String) -> Unit,
    onAddRateType: (String) -> Unit,
    onAddPaymentMethod: (String) -> Unit,
    onSave: (BookingEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var customerName by remember { mutableStateOf(existing?.customerName ?: "") }
    var serviceName by remember { mutableStateOf(existing?.serviceName ?: services.firstOrNull { it.isDefault }?.name ?: "") }
    var startMinute by remember { mutableStateOf(existing?.startMinute) }
    var endMinute by remember { mutableStateOf(existing?.endMinute) }
    var notApplicable by remember {
        mutableStateOf(existing?.timeNotApplicable ?: services.firstOrNull { it.name == serviceName }?.requiresTimeSlot?.not() ?: false)
    }
    var rateType by remember { mutableStateOf(existing?.rateType ?: rateTypeNames.firstOrNull().orEmpty()) }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: paymentMethodNames.firstOrNull().orEmpty()) }
    var paymentUnit by remember { mutableStateOf(existing?.paymentUnit ?: PaymentUnit.DOLLARS) }
    var paymentAmountText by remember { mutableStateOf(existing?.paymentAmount?.let { formatAmount(it) } ?: "") }
    var paymentStatus by remember { mutableStateOf(existing?.paymentStatus ?: PaymentStatus.UNPAID) }
    var amountPaidText by remember { mutableStateOf(existing?.amountPaid?.let { formatAmount(it) } ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    var resolvedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showContractSetup by remember { mutableStateOf(false) }
    var showClearContractConfirm by remember { mutableStateOf(false) }

    var contractMinutesUsed by remember { mutableStateOf(0L) }

    LaunchedEffect(customerName) {
        val trimmed = customerName.trim()
        resolvedCustomer = if (trimmed.isNotBlank()) repository.getOrCreateCustomer(trimmed) else null
    }

    LaunchedEffect(resolvedCustomer?.id, resolvedCustomer?.contractStartEpochDay) {
        val c = resolvedCustomer
        val start = c?.contractStartEpochDay
        contractMinutesUsed = if (c != null && c.hasContract && start != null) {
            repository.getPhoneTimeMinutes(c.id, start)
        } else 0L
    }

    suspend fun refreshResolvedCustomer() {
        resolvedCustomer?.id?.let { id -> resolvedCustomer = repository.getCustomerById(id) }
    }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    if (existing == null) strings.newBookingTitle else strings.editBookingTitle,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))

                EditableDropdown(
                    label = strings.customerLabel,
                    options = customerNames,
                    selected = customerName,
                    onSelected = { customerName = it },
                    onAddNew = onAddCustomer
                )
                Spacer(Modifier.height(10.dp))

                resolvedCustomer?.let { customer ->
                    Text(strings.contractSectionTitle, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    if (customer.hasContract) {
                        val grantedMinutes = (customer.contractHours * 60).toLong()
                        val remainingMinutes = grantedMinutes - contractMinutesUsed
                        val remainingOrOver = if (remainingMinutes >= 0)
                            strings.remainingSuffix(DateTimeUtils.formatDuration(remainingMinutes))
                        else
                            strings.overSuffix(DateTimeUtils.formatDuration(-remainingMinutes))
                        Text(
                            strings.contractStatusPrefix + strings.contractGivenSummary(
                                Money.formatDollars(customer.contractPrice),
                                DateTimeUtils.formatDuration(grantedMinutes)
                            ) + " · " + remainingOrOver,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingMinutes >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showContractSetup = true }) { Text(strings.editContractButton) }
                            OutlinedButton(onClick = { showClearContractConfirm = true }) { Text(strings.clearContractButton) }
                        }
                    } else {
                        Text(
                            strings.noContractSetUp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { showContractSetup = true }) { Text(strings.setUpContractButton) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                EditableDropdown(
                    label = strings.serviceLabel,
                    options = services.map { it.name },
                    selected = serviceName,
                    onSelected = { name ->
                        serviceName = name
                        services.firstOrNull { it.name == name }?.let { svc ->
                            if (!svc.requiresTimeSlot) notApplicable = true
                        }
                    },
                    onAddNew = onAddService
                )
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notApplicable, onCheckedChange = { notApplicable = it })
                    Text(strings.notApplicableCheckboxLabel)
                }

                if (!notApplicable) {
                    Spacer(Modifier.height(4.dp))
                    TimePickerField(label = strings.timeStartLabel, minuteOfDay = startMinute, onTimeSelected = { startMinute = it })
                    Spacer(Modifier.height(10.dp))
                    TimePickerField(label = strings.timeEndLabel, minuteOfDay = endMinute, onTimeSelected = { endMinute = it })
                }

                Spacer(Modifier.height(10.dp))
                EditableDropdown(
                    label = strings.rateLabel,
                    options = rateTypeNames,
                    selected = rateType,
                    onSelected = { rateType = it },
                    onAddNew = onAddRateType
                )
                Spacer(Modifier.height(10.dp))
                EditableDropdown(
                    label = strings.paymentMethodLabel,
                    options = paymentMethodNames,
                    selected = paymentMethod,
                    onSelected = { paymentMethod = it },
                    onAddNew = onAddPaymentMethod
                )

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text(strings.paymentAmountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    FilterChip(
                        selected = paymentUnit == PaymentUnit.DOLLARS,
                        onClick = { paymentUnit = PaymentUnit.DOLLARS },
                        label = { Text(strings.dollarsChipLabel) }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = paymentUnit == PaymentUnit.BOOKS,
                        onClick = { paymentUnit = PaymentUnit.BOOKS },
                        label = { Text(strings.booksChipLabel) }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = paymentUnit == PaymentUnit.MACKERELS,
                        onClick = { paymentUnit = PaymentUnit.MACKERELS },
                        label = { Text(strings.mackerelsChipLabel) }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(strings.paymentStatusLabel, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentStatus.entries.forEach { status ->
                        FilterChip(
                            selected = paymentStatus == status,
                            onClick = { paymentStatus = status },
                            label = { Text(strings.statusLabel(status)) }
                        )
                    }
                }
                if (paymentStatus == PaymentStatus.PARTIAL) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = it },
                        label = { Text(strings.amountPaidLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (existing != null && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text(strings.delete, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text(strings.cancel) }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val amount = paymentAmountText.toDoubleOrNull()
                            val amountPaid = amountPaidText.toDoubleOrNull() ?: 0.0
                            errorText = when {
                                customerName.isBlank() -> strings.errorPickCustomer
                                serviceName.isBlank() -> strings.errorPickService
                                rateType.isBlank() -> strings.errorPickRate
                                paymentMethod.isBlank() -> strings.errorPickPaymentMethod
                                amount == null || amount < 0 -> strings.errorInvalidAmount
                                !notApplicable && (startMinute == null || endMinute == null) -> strings.errorSetStartEnd
                                !notApplicable && startMinute != null && endMinute != null && endMinute!! <= startMinute!! -> strings.errorEndAfterStart
                                else -> null
                            }
                            if (errorText == null) {
                                onSave(
                                    BookingEntity(
                                        id = existing?.id ?: 0,
                                        dateEpochDay = dateEpochDay,
                                        customerId = resolvedCustomer?.id ?: existing?.customerId ?: 0,
                                        customerName = customerName.trim(),
                                        serviceId = services.firstOrNull { it.name == serviceName.trim() }?.id ?: existing?.serviceId ?: 0,
                                        serviceName = serviceName.trim(),
                                        startMinute = if (notApplicable) null else startMinute,
                                        endMinute = if (notApplicable) null else endMinute,
                                        timeNotApplicable = notApplicable,
                                        rateType = rateType,
                                        paymentMethod = paymentMethod,
                                        paymentAmount = amount ?: 0.0,
                                        paymentUnit = paymentUnit,
                                        paymentStatus = paymentStatus,
                                        amountPaid = if (paymentStatus == PaymentStatus.PARTIAL) amountPaid else 0.0,
                                        notes = notes.trim()
                                    )
                                )
                            }
                        }) { Text(strings.save) }
                    }
                }
            }
        }
    }

    if (showContractSetup) {
        val c = resolvedCustomer
        ContractSetupDialog(
            initialPrice = c?.contractPrice?.takeIf { c.hasContract },
            initialHours = c?.contractHours?.takeIf { c.hasContract },
            initialStart = c?.contractStartEpochDay?.let { java.time.LocalDate.ofEpochDay(it) } ?: java.time.LocalDate.now(),
            onDismiss = { showContractSetup = false },
            onSave = { price, hours, startDate ->
                c?.let { customerEntity ->
                    scope.launch {
                        repository.setContract(customerEntity, price, hours, startDate.toEpochDay())
                        refreshResolvedCustomer()
                    }
                }
                showContractSetup = false
            }
        )
    }

    if (showClearContractConfirm) {
        resolvedCustomer?.let { customer ->
            ClearContractConfirmDialog(
                name = customer.name,
                onConfirm = {
                    scope.launch {
                        repository.clearContract(customer)
                        refreshResolvedCustomer()
                    }
                    showClearContractConfirm = false
                },
                onDismiss = { showClearContractConfirm = false }
            )
        }
    }
}

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
