package com.logicalsapien.sapienterm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.logicalsapien.sapienterm.logging.TimberInitializer
import javax.inject.Inject

@HiltAndroidApp
class ConnectBotApplication : Application() {

    @Inject
    lateinit var timberInitializer: TimberInitializer

    override fun onCreate() {
        super.onCreate()
        timberInitializer.initialize()
    }
}
