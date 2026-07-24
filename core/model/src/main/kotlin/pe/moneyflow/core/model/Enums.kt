package pe.moneyflow.core.model

/** Whether money leaves (EXPENSE), arrives (INCOME) or moves between accounts (TRANSFER). */
enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/** Lifecycle of a payment. A logged past expense is PAID; a future bill is PENDING. */
enum class TransactionStatus { PENDING, PAID, OVERDUE, CANCELLED }

enum class Priority { LOW, NORMAL, HIGH }

enum class AccountType { CASH, BANK, CREDIT_CARD, EWALLET, SAVINGS }

enum class CategoryType { EXPENSE, INCOME }

enum class PaymentMethodType { CASH, CARD, EWALLET, BANK }

enum class BudgetPeriod { WEEKLY, MONTHLY, YEARLY }

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/** App-wide theme preference. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }
