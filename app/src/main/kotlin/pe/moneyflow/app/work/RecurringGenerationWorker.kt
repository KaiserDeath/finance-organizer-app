package pe.moneyflow.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pe.moneyflow.app.notification.MoneyFlowNotifier
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.usecase.GenerateDueRecurringUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase

/**
 * Daily background pass: materializes due recurring templates into pending transactions, then
 * posts a reminder summarizing anything overdue or due within the next day. Runs under WorkManager
 * with Hilt-injected dependencies via [pe.moneyflow.app.MoneyFlowApplication]'s worker factory.
 */
@HiltWorker
class RecurringGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val generateDueRecurring: GenerateDueRecurringUseCase,
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val notifier: MoneyFlowNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        generateDueRecurring()
        notifyImminent()
        Result.success()
    }.getOrElse { Result.retry() }

    private suspend fun notifyImminent() {
        val payments = getUpcomingPayments().first()
        // Only real rows: a projected occurrence is a forecast, not something to nag about.
        val imminent = payments.filter { !it.isProjected && it.bucket == UpcomingBucket.DUE_NOW }
        if (imminent.isEmpty()) return

        val totalMinor = imminent.sumOf { it.transaction.amountMinor }
        val currency = imminent.first().transaction.currencyCode
        val title = if (imminent.size == 1) {
            "Pago próximo: ${imminent.first().transaction.title}"
        } else {
            "${imminent.size} pagos próximos"
        }
        val body = buildString {
            append("Total ${Money.format(totalMinor, currency)}. ")
            append(imminent.take(3).joinToString(", ") { it.transaction.title })
            if (imminent.size > 3) append("…")
        }
        notifier.showUpcomingReminder(title, body)
    }

    companion object {
        const val WORK_NAME = "recurring-generation"

        /** One-shot catch-up enqueued on cold start, so an idle stretch can't leave data stale. */
        const val CATCH_UP_WORK_NAME = "recurring-generation-catch-up"
    }
}
