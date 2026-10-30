package com.anote

import android.app.Application
import com.anote.data.AppContainer

class AnoteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
