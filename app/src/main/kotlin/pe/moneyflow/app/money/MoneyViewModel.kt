package pe.moneyflow.app.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetNetWorthUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.ObserveSavingsGoalsUseCase
import javax.inject.Inject

data class MoneyUiState(
    val isLoading: Boolean = true,
    /** Budgets at or past the warning threshold (≥80% of the limit). */
    val budgetsAtRisk: Int = 0,
    val upcomingTotalMinor: Long = 0,
    val overdueCount: Int = 0,
    val accountsBalanceMinor: Long = 0,
    val savingsBalanceMinor: Long = 0,
    val methodsCount: Int = 0,
    val currencyCode: String = "PEN",
)

/**
 * Combines budgets, upcoming payments, accounts, savings and payment methods for the live
 * figure each row of "Tu dinero" shows — half the visits to those screens are just to read
 * one number, and the row answers that without entering.
 */
@HiltViewModel
class MoneyViewModel @Inject constructor(
    getBudgetsProgress: GetBudgetsProgressUseCase,
    getUpcomingPayments: GetUpcomingPaymentsUseCase,
    getNetWorth: GetNetWorthUseCase,
    observeSavingsGoals: ObserveSavingsGoalsUseCase,
    paymentMethodRepository: PaymentMethodRepository,
) : ViewModel() {

    val uiState: StateFlow<MoneyUiState> = combine(
        getBudgetsProgress(),
        getUpcomingPayments(),
        getNetWorth(),
        observeSavingsGoals(),
        paymentMethodRepository.observeAll(),
    ) { budgets, upcoming, netWorth, goals, methods ->
        MoneyUiState(
            isLoading = false,
            budgetsAtRisk = budgets.count { it.isOverBudget || it.isNearLimit },
            upcomingTotalMinor = upcoming.sumOf { it.transaction.amountMinor },
            overdueCount = upcoming.count { it.isOverdue },
            accountsBalanceMinor = netWorth.totalMinor,
            savingsBalanceMinor = goals.sumOf { it.currentAmountMinor },
            methodsCount = methods.size,
            currencyCode = netWorth.currencyCode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoneyUiState(),
    )
}
