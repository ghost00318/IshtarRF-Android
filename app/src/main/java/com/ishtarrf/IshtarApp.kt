package com.ishtarrf

import android.app.Application
import android.content.Context
import com.ishtarrf.data.prefs.SettingsRepository
import com.ishtarrf.data.serial.UsbSerialManager
import com.ishtarrf.data.sub.SubLibraryRepository

/** Process-wide singletons. Kept tiny — no DI framework needed for this app. */
class AppContainer(context: Context) {
    val serialManager = UsbSerialManager(context.applicationContext)
    val settings = SettingsRepository(context.applicationContext)
    val library = SubLibraryRepository(context.applicationContext)
}

class IshtarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
