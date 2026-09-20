package app.orionmd.management.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.ui.common.ClearContractConfirmDialog
import app.orionmd.management.ui.common.ContractSetupDialog
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class PhoneTimeView { ALL_TIME, CUSTOM, CONTRACT }

/**
 * Full client profile: existing spend/payment/service stats plus the "Phone Time" tracker
 * (All Time / User Defined / Contract). Opened by tapping a client card in Analytics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailDialog(
    customerId: Long,
    fallbackName: String,
    clientBookings: List<BookingEntity>,
    bookToDollarRate: Double,
    mackerelToBookRate: Double,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val repository = remember { AppSession.repository!! }
    val scope = rememberCoroutineScope()
    val customer by repository.observeCustomerById(customerId).collectAsState(initial = null)

    var view by remember { mutableStateOf(PhoneTimeView.ALL_TIME) }
    var allTimeMinutes by remember { mutableStateOf(0L) }
    var customMinutes by remember { mutableStateOf(0L) }
    var contractMinutes by remember { mutableStateOf(0L) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearContractConfirm by remember { mutableStateOf(false) }
    var showContractSetup by remember { mutableStateOf(false) }

    LaunchedEffect(customerId, customer?.customCounterResetEpochDay, customer?.contractStartEpochDay) {
        allTimeMinutes = repository.getPhoneTimeMinutes(customerId)
        customMinutes = repository.getPhoneTimeMinutes(customerId, customer?.customCounterResetEpochDay)
        val start = customer?.contractStartEpochDay
        contractMinutes = if (start != null) repository.getPhoneTimeMinutes(customerId, start) else 0L
    }

    val totalSpent = clientBookings.sumOf { Money.toDollars(it.paymentAmount, it.paymentUnit, bookToDollarRate, mackerelToBookRate) }
    val preferredMethod = clientBookings.groupingBy { it.paymentMethod }.eachCount().maxByOrNull { it.value }?.key ?: strings.noneDash
    val services = clientBookings.groupingBy { it.serviceName }.eachCount().entries.sortedByDescending { it.value }
    val unpaidCount = clientBookings.count { it.paymentStatus != PaymentStatus.PAID }
    val name = customer?.name ?: fallbackName

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                Spacer(Modifier.height(4.dp))
                Text(Money.formatDollars(totalSpent), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("${strings.preferredPaymentPrefix}$preferredMethod", style = MaterialTheme.typography.bodyMedium)
                Text(
                    strings.bookingsSummary(clientBookings.size, unpaidCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (services.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.servicesPrefix + services.joinToString(", ") { "${it.key} (${it.value})" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(strings.phoneTimeTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = view == PhoneTimeView.ALL_TIME, onClick = { view = PhoneTimeView.ALL_TIME }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(strings.allTimeLabel) }
                    SegmentedButton(selected = view == PhoneTimeView.CUSTOM, onClick = { view = PhoneTimeView.CUSTOM }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(strings.customLabel) }
                    SegmentedButton(selected = view == PhoneTimeView.CONTRACT, onClick = { view = PhoneTimeView.CONTRACT }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(strings.contractLabel) }
                }
                Spacer(Modifier.height(14.dp))

                when (view) {
                    PhoneTimeView.ALL_TIME -> {
                        Text(strings.lifetimePhoneTime, style = MaterialTheme.typography.bodyMedium)
                        Text(DateTimeUtils.formatDuration(allTimeMinutes), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    PhoneTimeView.CUSTOM -> {
                        val since = customer?.customCounterResetEpochDay
                        Text(
                            if (since != null) "${strings.sincePrefix}${DateTimeUtils.mdy(LocalDate.ofEpochDay(since), strings.locale)}" else strings.sinceClientAdded,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(DateTimeUtils.formatDuration(customMinutes), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { showResetConfirm = true }) { Text(strings.resetCounterButton) }
                    }
                    PhoneTimeView.CONTRACT -> {
                        val c = customer
                        if (c != null && c.hasContract) {
                            val grantedMinutes = (c.contractHours * 60).toLong()
                            val remainingMinutes = grantedMinutes - contractMinutes
                            ContractStatRow(strings.contractPriceLabel, Money.formatDollars(c.contractPrice))
                            ContractStatRow(strings.hoursGivenLabel, DateTimeUtils.formatDuration(grantedMinutes))
                            ContractStatRow(strings.timeUsedLabel, DateTimeUtils.formatDuration(contractMinutes))
                            ContractStatRow(
                                strings.timeRemainingLabel,
                                if (remainingMinutes >= 0) DateTimeUtils.formatDuration(remainingMinutes) else "${strings.overByPrefix}${DateTimeUtils.formatDuration(-remainingMinutes)}",
                                valueColor = if (remainingMinutes >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                            c.contractStartEpochDay?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${strings.contractStartedPrefix}${DateTimeUtils.mdy(LocalDate.ofEpochDay(it), strings.locale)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
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
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { showContractSetup = true }) { Text(strings.setUpContractButton) }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(strings.resetCounterConfirmTitle) },
            text = { Text(strings.resetCounterConfirmText(name)) },
            confirmButton = {
                TextButton(onClick = {
                    customer?.let { c -> scope.launch { repository.resetPhoneTimeCounter(c, LocalDate.now().toEpochDay()) } }
                    showResetConfirm = false
                }) { Text(strings.resetCounterButton) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(strings.cancel) } }
        )
    }

    if (showClearContractConfirm) {
        ClearContractConfirmDialog(
            name = name,
            onConfirm = {
                customer?.let { c -> scope.launch { repository.clearContract(c) } }
                showClearContractConfirm = false
            },
            onDismiss = { showClearContractConfirm = false }
        )
    }

    if (showContractSetup) {
        val c = customer
        ContractSetupDialog(
            initialPrice = c?.contractPrice?.takeIf { c.hasContract },
            initialHours = c?.contractHours?.takeIf { c.hasContract },
            initialStart = c?.contractStartEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now(),
            onDismiss = { showContractSetup = false },
            onSave = { price, hours, startDate ->
                c?.let { customerEntity ->
                    scope.launch { repository.setContract(customerEntity, price, hours, startDate.toEpochDay()) }
                }
                showContractSetup = false
            }
        )
    }
}

@Composable
private fun ContractStatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}
