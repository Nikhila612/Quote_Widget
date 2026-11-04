package io.github.nikhila612.quotewidget

import android.app.Application
import androidx.work.ExistingWorkPolicy
import io.github.nikhila612.quotewidget.widget.MidnightScheduler

class QuoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MidnightScheduler.schedule(this, ExistingWorkPolicy.KEEP)
    }
}
