package com.anote.ui.lock

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anote.R
import com.anote.ui.anoteViewModel
import com.anote.ui.components.PinEntry

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val viewModel = anoteViewModel { LockViewModel(it.lockStore) }
    val failed by viewModel.error.collectAsStateWithLifecycle()
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val onUnlockedState = rememberUpdatedState(onUnlocked)
    val canBiometric = remember(biometricEnabled) {
        biometricEnabled &&
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    val promptTitle = stringResource(R.string.lock_title)
    val promptSubtitle = stringResource(R.string.lock_subtitle)
    val promptNegative = stringResource(R.string.lock_use_pin)
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val prompt = remember(activity, executor) {
        BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockedState.value()
                }
            },
        )
    }
    val authenticate = {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(promptNegative)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { keyboard?.hide() }
    LaunchedEffect(canBiometric) {
        if (canBiometric) authenticate()
    }
    DisposableEffect(prompt) {
        onDispose { prompt.cancelAuthentication() }
    }
    BackHandler { activity.finish() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .windowInsetsPadding(WindowInsets(0)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.lock_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            PinEntry(
                pin = pin,
                onPinChange = {
                    pin = it
                    viewModel.clearError()
                },
                onConfirm = { viewModel.submit(pin) { onUnlocked() } },
                confirmText = stringResource(
                    if (checking) R.string.lock_checking else R.string.lock_confirm,
                ),
                confirmEnabled = pin.length in 4..6 && !checking,
                error = if (failed) stringResource(R.string.error_pin) else null,
            )
            if (canBiometric) {
                TextButton(onClick = authenticate, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = stringResource(R.string.cd_fingerprint))
                    Text(
                        text = stringResource(R.string.settings_biometric),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
