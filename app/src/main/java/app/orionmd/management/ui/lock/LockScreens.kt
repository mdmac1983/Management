package app.orionmd.management.ui.lock

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.orionmd.management.data.AppSession
import app.orionmd.management.security.CredentialType
import app.orionmd.management.security.LockManager
import app.orionmd.management.util.LocalStrings
import kotlinx.coroutines.launch

private enum class SetupStep { CHOOSE_TYPE, ENTER, CONFIRM }

@Composable
fun LockGateway(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lockManager = remember { LockManager(context) }
    var credentialExists by remember { mutableStateOf(lockManager.isCredentialSet()) }

    when {
        AppSession.isUnlocked -> content()
        credentialExists -> UnlockScreen(lockManager = lockManager)
        else -> LockSetupScreen(lockManager = lockManager, onCredentialCreated = { credentialExists = true })
    }
}

@Composable
private fun LockSetupScreen(lockManager: LockManager, onCredentialCreated: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(SetupStep.CHOOSE_TYPE) }
    var chosenType by remember { mutableStateOf<CredentialType?>(null) }
    var firstEntry by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun finish(type: CredentialType, credential: String) {
        val derivedKey = lockManager.setCredential(type, credential)
        scope.launch {
            AppSession.unlock(context, derivedKey)
            onCredentialCreated()
        }
    }

    LockScaffold(title = strings.setUpYourLockTitle, subtitle = subtitleFor(strings, step, chosenType)) {
        when (step) {
            SetupStep.CHOOSE_TYPE -> ChooseTypeButtons { type ->
                chosenType = type
                step = SetupStep.ENTER
                errorText = null
            }
            SetupStep.ENTER -> CredentialInput(
                type = chosenType!!,
                errorText = errorText,
                onSubmit = { value ->
                    firstEntry = value
                    errorText = null
                    step = SetupStep.CONFIRM
                }
            )
            SetupStep.CONFIRM -> CredentialInput(
                type = chosenType!!,
                errorText = errorText,
                confirmMode = true,
                onSubmit = { value ->
                    if (value == firstEntry) {
                        finish(chosenType!!, value)
                    } else {
                        errorText = strings.didntMatchTryAgain
                        step = SetupStep.ENTER
                        firstEntry = ""
                    }
                }
            )
        }
        if (step != SetupStep.CHOOSE_TYPE) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                step = SetupStep.CHOOSE_TYPE
                errorText = null
                firstEntry = ""
            }) { Text(strings.chooseDifferentLockType) }
        }
    }
}

private fun subtitleFor(strings: app.orionmd.management.util.AppStrings, step: SetupStep, type: CredentialType?): String = when (step) {
    SetupStep.CHOOSE_TYPE -> strings.chooseHowToUnlockSubtitle
    SetupStep.ENTER -> "${strings.enterYourNewPrefix}${strings.credentialTypeLabel(type?.name ?: "")}"
    SetupStep.CONFIRM -> "${strings.confirmYourNewPrefix}${strings.credentialTypeLabel(type?.name ?: "")}"
}

@Composable
private fun UnlockScreen(lockManager: LockManager) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val type = remember { lockManager.credentialType() ?: CredentialType.PIN }
    var errorText by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableStateOf(0) }

    fun attempt(value: String) {
        val derivedKey = lockManager.verify(value)
        if (derivedKey != null) {
            errorText = null
            scope.launch { AppSession.unlock(context, derivedKey) }
        } else {
            errorText = strings.incorrectTryAgain
            errorTick++
        }
    }

    LockScaffold(title = strings.appDisplayName, subtitle = strings.enterToUnlockSubtitle(strings.credentialTypeLabel(type.name))) {
        CredentialInput(
            type = type,
            errorText = errorText,
            errorSignal = errorTick,
            onSubmit = { attempt(it) }
        )
    }
}

@Composable
private fun LockScaffold(
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun ChooseTypeButtons(onChosen: (CredentialType) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = { onChosen(CredentialType.PIN) }, modifier = Modifier.fillMaxWidth()) {
            Text(strings.usePin)
        }
        Button(onClick = { onChosen(CredentialType.PATTERN) }, modifier = Modifier.fillMaxWidth()) {
            Text(strings.usePattern)
        }
        Button(onClick = { onChosen(CredentialType.PASSWORD) }, modifier = Modifier.fillMaxWidth()) {
            Text(strings.usePassword)
        }
    }
}

/**
 * Shared credential-entry UI. In setup mode the caller drives confirm/re-enter via [onSubmit];
 * in unlock mode there's just one attempt per submit.
 */
@Composable
fun CredentialInput(
    type: CredentialType,
    errorText: String?,
    confirmMode: Boolean = false,
    errorSignal: Int = 0,
    onSubmit: (String) -> Unit
) {
    when (type) {
        CredentialType.PIN -> PinEntry(errorText = errorText, onSubmit = onSubmit)
        CredentialType.PATTERN -> PatternEntry(errorText = errorText, errorSignal = errorSignal, onSubmit = onSubmit)
        CredentialType.PASSWORD -> PasswordEntry(errorText = errorText, onSubmit = onSubmit)
    }
}

@Composable
private fun PinEntry(errorText: String?, onSubmit: (String) -> Unit) {
    val strings = LocalStrings.current
    var pin by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(8) { index ->
                val filled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(if (filled) 16.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                )
            }
        }
        errorText?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
        NumericKeypad(
            onDigit = { d -> if (pin.length < 8) pin += d },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(pin); pin = "" },
            enabled = pin.length in 4..8,
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.continueLabel) }
    }
}

@Composable
private fun NumericKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "back")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (key.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == "back") onBackspace() else if (key.isNotEmpty()) onDigit(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "back" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                            "" -> {}
                            else -> Text(key, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternEntry(errorText: String?, errorSignal: Int, onSubmit: (String) -> Unit) {
    val strings = LocalStrings.current
    var showError by remember(errorSignal) { mutableStateOf(errorSignal > 0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        PatternLockView(
            showError = showError,
            onPatternComplete = { dots ->
                onSubmit(dots.joinToString(","))
            }
        )
        Text(
            strings.connectAtLeastFourDots,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PasswordEntry(errorText: String?, onSubmit: (String) -> Unit) {
    val strings = LocalStrings.current
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(strings.passwordLabel) },
            singleLine = true,
            isError = errorText != null,
            supportingText = { errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = strings.toggleVisibilityDesc)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(password); password = "" },
            enabled = password.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.continueLabel) }
    }
}
