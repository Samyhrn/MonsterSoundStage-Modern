package com.monster.soundstage

import android.app.Application
import android.util.Log

class SoundStageApplication : Application() {
    companion object {
        const val TAG = "SoundStageApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Catch all uncaught crashes to prevent force-close
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT CRASH on thread ${thread.name}", throwable)
            // Don't re-throw - this prevents the system from showing "App keeps stopping"
            // The app will just show the error UI on next launch
        }
    }
}
