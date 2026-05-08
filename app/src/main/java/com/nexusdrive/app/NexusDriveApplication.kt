package com.nexusdrive.app

import android.app.Application

class NexusDriveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        lateinit var instance: NexusDriveApplication
            private set
    }
}
