package com.anote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anote.AnoteApp
import com.anote.ui.lock.LockScreen
import com.anote.ui.navigation.AnoteNavHost

@Composable
fun AnoteRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as AnoteApp
    val activity = context as FragmentActivity
    val gate by app.container.sessionLock.state.collectAsStateWithLifecycle()

    DisposableEffect(activity, app) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                app.container.sessionLock.onHostStop(activity.isChangingConfigurations)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    if (!gate.ready) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        AnoteNavHost()
        if (gate.locked) {
            LockScreen(
                biometricEnabled = gate.biometricEnabled,
                onUnlocked = { app.container.sessionLock.unlock() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
