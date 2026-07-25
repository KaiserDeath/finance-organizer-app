package pe.moneyflow.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import pe.moneyflow.app.notification.MoneyFlowNotifier
import pe.moneyflow.app.work.RecurringGenerationWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MoneyFlowApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifier: MoneyFlowNotifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notifier.ensureChannel()
        scheduleRecurringGeneration()
    }

    /** Enqueues the once-a-day recurring generation + reminder pass, keeping any existing schedule. */
    private fun scheduleRecurringGeneration() {
        val request = PeriodicWorkRequestBuilder<RecurringGenerationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecurringGenerationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
