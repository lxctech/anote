package com.anote.data

import android.content.Context
import com.anote.data.local.AppDatabase
import com.anote.data.lock.LockStore
import com.anote.data.lock.SessionLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val imageStore = ImageStore(appContext)
    private val database = AppDatabase.create(appContext)
    val repository = DiaryRepository(database.diaryDao(), imageStore)
    val lockStore = LockStore(appContext)
    val sessionLock = SessionLock(lockStore, appScope)

    init {
        imageStore.clearPending()
    }
}
