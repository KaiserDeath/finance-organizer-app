package pe.moneyflow.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
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

    /**
     * Enqueues the once-a-day recurring generation + reminder pass, keeping any existing schedule,
     * plus a one-shot catch-up run.
     *
     * The catch-up matters because the periodic pass alone leaves the ledger stale after an idle
     * stretch — opening the app would show missing due items until Work next fires. Generation is
     * idempotent per day, so running it on every cold start is safe.
     */
    private fun scheduleRecurringGeneration() {
        val periodic = PeriodicWorkRequestBuilder<RecurringGenerationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).build()

        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            RecurringGenerationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
        workManager.enqueueUniqueWork(
            RecurringGenerationWorker.CATCH_UP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RecurringGenerationWorker>().build(),
        )
    }
}
