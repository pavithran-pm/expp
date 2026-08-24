package com.pavithran.paisa

import android.app.Application
import com.pavithran.paisa.backup.ExportReminderWorker

class PaisaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { ExportReminderWorker.schedule(this) }
    }
}
