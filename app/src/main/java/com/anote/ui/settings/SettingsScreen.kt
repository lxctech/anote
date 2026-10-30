package com.anote.ui.settings

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anote.R
import com.anote.ui.anoteViewModel
import com.anote.ui.components.PinEntry
import com.anote.ui.theme.anoteTopBarColors

private enum class PinMode {
    Create,
    Change,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel = anoteViewModel { SettingsViewModel(it.lockStore, it.sessionLock) }
    val config by viewModel.config.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val biometricReady = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }
    var confirmDisable by remember { mutableStateOf(false) }
    val pinChanged = stringResource(R.string.pin_changed)

    LaunchedEffect(message) {
        if (message == SettingsMessage.PinChanged) {
            snackbar.showSnackbar(pinChanged)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                colors = anoteTopBarColors(),
                windowInsets = WindowInsets(0),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_section_security),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    SettingSwitch(
                        title = stringResource(R.string.settings_lock),
                        summary = stringResource(R.string.settings_lock_summary),
                        checked = config.enabled && config.hasPin,
                        onCheckedChange = { enabled ->
                            if (enabled) pinMode = PinMode.Create else confirmDisable = true
                        },
                    )
                    if (config.enabled && config.hasPin) {
                        TextButton(
                            onClick = { pinMode = PinMode.Change },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Text(stringResource(R.string.settings_change_pin))
                        }
                        SettingSwitch(
                            title = stringResource(R.string.settings_biometric),
                            summary = stringResource(
                                if (biometricReady) {
                                    R.string.settings_biometric_summary
                                } else {
                                    R.string.settings_biometric_unavailable
                                },
                            ),
                            checked = config.biometricEnabled && biometricReady,
                            enabled = biometricReady,
                            onCheckedChange = viewModel::setBiometric,
                        )
                    }
                }
            }
        }
    }

    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text(stringResource(R.string.settings_disable_title)) },
            text = { Text(stringResource(R.string.settings_disable_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisable = false
                    viewModel.disable()
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisable = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    pinMode?.let { mode ->
        PinFlowDialog(
            mode = mode,
            onDismiss = { pinMode = null },
            onCreate = { pin, done -> viewModel.enableWithPin(pin, done) },
            onVerify = viewModel::verifyCurrent,
            onChange = { pin, done -> viewModel.changePin(pin, done) },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PinFlowDialog(
    mode: PinMode,
    onDismiss: () -> Unit,
    onCreate: (String, () -> Unit) -> Unit,
    onVerify: (String, (Boolean) -> Unit) -> Unit,
    onChange: (String, () -> Unit) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val lengthError = stringResource(R.string.pin_length_hint)
    val mismatch = stringResource(R.string.pin_mismatch)
    val wrongCurrent = stringResource(R.string.pin_wrong_current)
    val title = when (mode) {
        PinMode.Create -> if (step == 0) R.string.pin_set_title else R.string.pin_set_confirm_title
        PinMode.Change -> when (step) {
            0 -> R.string.pin_current_title
            1 -> R.string.pin_new_title
            else -> R.string.pin_set_confirm_title
        }
    }

    Dialog(onDismissRequest = { if (!working) onDismiss() }) {
        Card {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PinEntry(
                    pin = pin,
                    onPinChange = {
                        pin = it
                        error = null
                    },
                    onConfirm = {
                        if (pin.length !in 4..6) {
                            error = lengthError
                            return@PinEntry
                        }
                        when (mode) {
                            PinMode.Create -> {
                                if (step == 0) {
                                    first = pin
                                    pin = ""
                                    step = 1
                                } else if (pin != first) {
                                    error = mismatch
                                    pin = ""
                                } else {
                                    working = true
                                    onCreate(pin) { onDismiss() }
                                }
                            }
                            PinMode.Change -> when (step) {
                                0 -> {
                                    working = true
                                    onVerify(pin) { ok ->
                                        working = false
                                        if (ok) {
                                            pin = ""
                                            step = 1
                                        } else {
                                            error = wrongCurrent
                                            pin = ""
                                        }
                                    }
                                }
                                1 -> {
                                    first = pin
                                    pin = ""
                                    step = 2
                                }
                                else -> {
                                    if (pin != first) {
                                        error = mismatch
                                        pin = ""
                                    } else {
                                        working = true
                                        onChange(pin) { onDismiss() }
                                    }
                                }
                            }
                        }
                    },
                    confirmText = stringResource(if (working) R.string.lock_checking else R.string.action_ok),
                    confirmEnabled = !working,
                    error = error,
                )
                TextButton(
                    onClick = onDismiss,
                    enabled = !working,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}
