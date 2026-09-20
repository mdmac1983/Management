package app.orionmd.management.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.orionmd.management.BuildConfig
import app.orionmd.management.Changelog
import app.orionmd.management.R
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.entity.ThemeMode
import app.orionmd.management.security.CredentialType
import app.orionmd.management.security.LockManager
import app.orionmd.management.ui.common.SectionCard
import app.orionmd.management.ui.lock.ChooseTypeButtons
import app.orionmd.management.ui.lock.CredentialInput
import app.orionmd.management.util.BackupManager
import app.orionmd.management.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val repository = remember { AppSession.repository!! }
    val scope = rememberCoroutineScope()

    val settings by repository.observeSettings().collectAsState(initial = null)

    var bookRateText by remember(settings?.bookToDollarRate) {
        mutableStateOf(settings?.bookToDollarRate?.let { formatRate(it) } ?: "8.00")
    }
    var mackerelRateText by remember(settings?.mackerelToBookRate) {
        mutableStateOf(settings?.mackerelToBookRate?.let { formatRate(it) } ?: "4.00")
    }
    var showChangeLock by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var changelogExpanded by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager.export(context, uri)
                backupMessage = if (result.isSuccess) strings.backupSavedMessage else "${strings.backupFailedPrefix}${result.exceptionOrNull()?.message}"
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager.import(context, uri)
                backupMessage = if (result.isSuccess) strings.backupRestoredMessage else "${strings.restoreFailedPrefix}${result.exceptionOrNull()?.message}"
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsScreenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                SectionCard(title = strings.booksConversionTitle) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.oneBookEquals)
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(
                                value = bookRateText,
                                onValueChange = { bookRateText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            TextButton(onClick = {
                                bookRateText.toDoubleOrNull()?.let { rate ->
                                    scope.launch { repository.updateBookToDollarRate(rate) }
                                }
                            }) { Text(strings.save) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.mackerelsEqualOneBook)
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(
                                value = mackerelRateText,
                                onValueChange = { mackerelRateText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            TextButton(onClick = {
                                mackerelRateText.toDoubleOrNull()?.let { rate ->
                                    scope.launch { repository.updateMackerelToBookRate(rate) }
                                }
                            }) { Text(strings.save) }
                        }
                    }
                }
            }

            item {
                SectionCard(title = strings.themeSection) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = (settings?.themeMode ?: ThemeMode.DAY) == ThemeMode.DAY,
                            onClick = { scope.launch { repository.updateThemeMode(ThemeMode.DAY) } },
                            label = { Text(strings.themeDay) }
                        )
                        FilterChip(
                            selected = settings?.themeMode == ThemeMode.NIGHT,
                            onClick = { scope.launch { repository.updateThemeMode(ThemeMode.NIGHT) } },
                            label = { Text(strings.themeNight) }
                        )
                        FilterChip(
                            selected = settings?.themeMode == ThemeMode.GRAY,
                            onClick = { scope.launch { repository.updateThemeMode(ThemeMode.GRAY) } },
                            label = { Text(strings.themeMaterialGray) }
                        )
                    }
                }
            }

            item {
                SectionCard(title = strings.languageSection) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = (settings?.language ?: "en") == "en",
                            onClick = { scope.launch { repository.updateLanguage("en") } },
                            label = { Text(strings.languageEnglish) }
                        )
                        FilterChip(
                            selected = settings?.language == "es",
                            onClick = { scope.launch { repository.updateLanguage("es") } },
                            label = { Text(strings.languageSpanish) }
                        )
                    }
                }
            }

            item {
                SectionCard(title = strings.appLockSection) {
                    Column {
                        Text(strings.changeLockDescription)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showChangeLock = true }) {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.changeLockButton)
                        }
                    }
                }
            }

            item {
                SectionCard(title = strings.backupSection) {
                    Column {
                        Text(strings.backupDescription)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { exportLauncher.launch("rentals_backup.zip") }) { Text(strings.exportLabel) }
                            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip")) }) { Text(strings.importLabel) }
                        }
                        backupMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "${strings.changelogTitlePrefix}${BuildConfig.VERSION_NAME}") {
                    Column {
                        val releases = Changelog.entries
                        val latest = releases.firstOrNull()
                        // Collapsed by default, showing only the current version's notes; expanding
                        // reveals the full version history.
                        latest?.let { entry ->
                            Text("v${entry.version} — ${entry.date}", style = MaterialTheme.typography.labelLarge)
                            Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (releases.size > 1) {
                            AnimatedVisibility(visible = changelogExpanded) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    releases.drop(1).forEachIndexed { index, entry ->
                                        Text("v${entry.version} — ${entry.date}", style = MaterialTheme.typography.labelLarge)
                                        Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
                                        if (index != releases.drop(1).lastIndex) Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { changelogExpanded = !changelogExpanded }) {
                                Icon(if (changelogExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (changelogExpanded) strings.showLess else strings.showFullHistory)
                            }
                        }
                    }
                }
            }

            item { AboutFooter() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showChangeLock) {
        ChangeLockDialog(onDismiss = { showChangeLock = false })
    }
}

private enum class ChangeLockStep { VERIFY_CURRENT, CHOOSE_NEW_TYPE, ENTER_NEW, CONFIRM_NEW }

@Composable
private fun ChangeLockDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lockManager = remember { LockManager(context) }
    val currentType = remember { lockManager.credentialType() ?: CredentialType.PIN }

    var step by remember { mutableStateOf(ChangeLockStep.VERIFY_CURRENT) }
    var newType by remember { mutableStateOf<CredentialType?>(null) }
    var firstEntry by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(
            when (step) {
                ChangeLockStep.VERIFY_CURRENT -> "${strings.confirmCurrentPrefix}${strings.credentialTypeLabel(currentType.name)}"
                ChangeLockStep.CHOOSE_NEW_TYPE -> strings.chooseNewLockType
                ChangeLockStep.ENTER_NEW -> "${strings.enterNewPrefix}${strings.credentialTypeLabel(newType?.name ?: "")}"
                ChangeLockStep.CONFIRM_NEW -> "${strings.confirmNewPrefix}${strings.credentialTypeLabel(newType?.name ?: "")}"
            }
        ) },
        text = {
            when (step) {
                ChangeLockStep.VERIFY_CURRENT -> CredentialInput(
                    type = currentType,
                    errorText = errorText,
                    onSubmit = { attempt ->
                        if (lockManager.verify(attempt) != null) {
                            errorText = null
                            step = ChangeLockStep.CHOOSE_NEW_TYPE
                        } else {
                            errorText = strings.incorrectTryAgain
                        }
                    }
                )
                ChangeLockStep.CHOOSE_NEW_TYPE -> ChooseTypeButtons { type ->
                    newType = type
                    step = ChangeLockStep.ENTER_NEW
                }
                ChangeLockStep.ENTER_NEW -> CredentialInput(
                    type = newType!!,
                    errorText = errorText,
                    onSubmit = { value ->
                        firstEntry = value
                        errorText = null
                        step = ChangeLockStep.CONFIRM_NEW
                    }
                )
                ChangeLockStep.CONFIRM_NEW -> CredentialInput(
                    type = newType!!,
                    errorText = errorText,
                    confirmMode = true,
                    onSubmit = { value ->
                        if (value == firstEntry) {
                            errorText = null
                            // The new credential is only PERSISTED after the database has actually
                            // been re-keyed to it - if the re-key fails for any reason, the old
                            // credential keeps working instead of the app being locked out.
                            val pending = lockManager.prepareCredential(newType!!, value)
                            scope.launch {
                                try {
                                    AppSession.rekey(context, pending.derivedKey)
                                    lockManager.commitCredential(pending)
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorText = "${strings.changeLockFailedPrefix}${e.message ?: e.javaClass.simpleName}"
                                    step = ChangeLockStep.CONFIRM_NEW
                                }
                            }
                        } else {
                            errorText = strings.didntMatchTryAgain
                            step = ChangeLockStep.ENTER_NEW
                            firstEntry = ""
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun AboutFooter() {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.buy_me_a_coffee_url)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.rentals_brand),
            contentDescription = strings.appDisplayName,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(8.dp))
        Text("${strings.appDisplayName} v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { uriHandler.openUri(url) }) {
            Icon(Icons.Filled.LocalCafe, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(strings.buyMeACoffee)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${strings.copyrightPrefix}${java.time.LocalDate.now().year} OrionMD",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun formatRate(value: Double): String =
    if (value == value.toLong().toDouble()) "${value.toLong()}.00" else "%.2f".format(value)
