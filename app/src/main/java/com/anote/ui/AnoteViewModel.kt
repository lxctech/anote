package com.anote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anote.AnoteApp
import com.anote.data.AppContainer

@Composable
inline fun <reified T : ViewModel> anoteViewModel(
    key: String? = null,
    crossinline factory: (AppContainer) -> T,
): T {
    val app = LocalContext.current.applicationContext as AnoteApp
    val viewModelFactory = remember(app, key) {
        val container = app.container
        object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                @Suppress("UNCHECKED_CAST")
                return factory(container) as VM
            }
        }
    }
    return viewModel(key = key, factory = viewModelFactory)
}
