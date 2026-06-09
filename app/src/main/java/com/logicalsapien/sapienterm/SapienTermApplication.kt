package com.logicalsapien.sapienterm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.logicalsapien.sapienterm.data.export.AutoBackupManager
import com.logicalsapien.sapienterm.logging.TimberInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SapienTermApplication : Application() {

    @Inject
    lateinit var timberInitializer: TimberInitializer

    @Inject
    lateinit var autoBackupManager: AutoBackupManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        timberInitializer.initialize()
        appScope.launch { autoBackupManager.runIfDue() }
    }
}
