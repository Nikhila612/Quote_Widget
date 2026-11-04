package io.github.nikhila612.quotewidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

/** Refreshes the widget just after local midnight, then schedules the next night. */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        QuoteGlanceWidget().updateAll(applicationContext)
        MidnightScheduler.schedule(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}

object MidnightScheduler {
    private const val WORK_NAME = "midnight-refresh"

    fun schedule(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val now = ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusSeconds(5)
        val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
            .setInitialDelay(Duration.between(now, nextMidnight))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }
}

/** Time zone or clock changed: today's quote may differ, and midnight moved. */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MidnightScheduler.schedule(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                QuoteGlanceWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
