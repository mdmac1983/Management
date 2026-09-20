package app.orionmd.management.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings

/** A titled card used throughout Settings and the Database tab. */
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * A simple named-item list with inline rename (tap the name) and delete, plus an "add" text
 * button that opens a small entry dialog. Used for Services/Rates/Payment Methods/Customers in
 * the Database tab (this used to live only in Settings before those moved to their own tab).
 */
@Composable
fun EditableListSection(
    items: List<Pair<Long, String>>,
    addLabel: String,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var showAdd by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column {
        items.forEach { (id, name) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editingId == id) {
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        if (editingText.isNotBlank()) onRename(id, editingText.trim())
                        editingId = null
                    }) { Text(strings.save) }
                } else {
                    Text(
                        name,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { editingId = id; editingText = name }
                    )
                    IconButton(onClick = { onDelete(id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete $name")
                    }
                }
            }
            HorizontalDivider()
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showAdd = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(addLabel)
        }
    }

    if (showAdd) {
        var newValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(addLabel) },
            text = {
                OutlinedTextField(value = newValue, onValueChange = { newValue = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newValue.isNotBlank()) onAdd(newValue.trim())
                    showAdd = false
                }) { Text(strings.add) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(strings.cancel) } }
        )
    }
}

/**
 * A dropdown of [options] with an inline "Add new..." entry that opens a small text-entry dialog.
 * Used for Customer, Service, Rate, and Payment Method pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    onAddNew: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
            DropdownMenuItem(
                text = { Text(strings.addNewPrefix) },
                onClick = {
                    expanded = false
                    showAddDialog = true
                },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
            )
        }
    }

    if (showAddDialog) {
        var newValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("${strings.addNewTitlePrefix}$label") },
            text = {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newValue.isNotBlank()) {
                            onAddNew(newValue.trim())
                            onSelected(newValue.trim())
                        }
                        showAddDialog = false
                    }
                ) { Text(strings.add) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) } }
        )
    }
}

/** A field that opens a Material3 time picker dialog and displays the chosen time as h:mm a. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    minuteOfDay: Int?,
    onTimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = minuteOfDay?.let { DateTimeUtils.formatMinute(it, strings.locale) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = "Pick time") },
            placeholder = { Text(strings.tapToSelect) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so the whole (disabled, non-focusable) field is one tap target.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        val initialHour = (minuteOfDay ?: 9 * 60) / 60
        val initialMinute = (minuteOfDay ?: 9 * 60) % 60
        val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(state.hour * 60 + state.minute)
                    showPicker = false
                }) { Text(strings.setLabel) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(strings.cancel) } }
        )
    }
}
