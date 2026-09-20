package app.orionmd.management.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Shared Phone Time Contract UI, used by both Analytics' client detail dialog and the booking
 * form's inline "Phone Time Contract" section - extracted here (rather than left private to
 * Analytics) so neither screen package has to depend on the other.
 */

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractSetupDialog(
    initialPrice: Double?,
    initialHours: Double?,
    initialStart: LocalDate,
    onDismiss: () -> Unit,
    onSave: (price: Double, hours: Double, start: LocalDate) -> Unit
) {
    val strings = LocalStrings.current
    var priceText by remember { mutableStateOf(initialPrice?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var hoursText by remember { mutableStateOf(initialHours?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var startDate by remember { mutableStateOf(initialStart) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.contractSetupTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; error = null },
                    label = { Text(strings.contractPriceFieldLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it; error = null },
                    label = { Text(strings.contractHoursFieldLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${strings.contractStartDatePrefix}${DateTimeUtils.mdy(startDate, strings.locale)}")
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceText.toDoubleOrNull()
                val hours = hoursText.toDoubleOrNull()
                when {
                    price == null || price < 0 -> error = strings.errorInvalidPrice
                    hours == null || hours <= 0 -> error = strings.errorInvalidHours
                    else -> onSave(price, hours, startDate)
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDate = millisToLocalDate(it) }
                    showDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun ClearContractConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.clearContractConfirmTitle) },
        text = { Text(strings.clearContractConfirmText(name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(strings.clearContractButton) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
