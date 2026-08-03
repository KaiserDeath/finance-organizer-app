package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.amount
import pe.moneyflow.core.domain.model.msg
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

/**
 * Pure, rule-based insight generator. Given the raw transactions and categories it derives a small
 * set of actionable suggestions (cash-flow, spending spikes, top category, upcoming/overdue bills).
 * Kept free of Android and IO so it is fully unit-testable; the LLM-backed variant can later
 * implement the same [SmartInsights] seam.
 */
object InsightEngine {

    /** A category must grow by at least this fraction over last month to count as a spike. */
    private const val SPIKE_RATIO = 1.3
    /** ...and by at least this absolute amount (S/ 20.00), to ignore trivial jumps. */
    private const val SPIKE_MIN_DELTA_MINOR = 2_000L

    fun generate(
        transactions: List<Transaction>,
        categories: List<Category>,
        today: LocalDate,
        currencyCode: String,
    ): List<Insight> {
        if (transactions.isEmpty()) {
            return listOf(
                Insight(
                    id = "getting-started",
                    kind = InsightKind.GETTING_STARTED,
                    severity = InsightSeverity.INFO,
                    title = "Empieza a registrar",
                    message = msg(
                        "Agrega tus gastos e ingresos para recibir sugerencias personalizadas.",
                    ),
                ),
            )
        }

        val categoriesById = categories.associateBy { it.id }
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val prevEnd = monthStart.minusDays(1)
        val prevStart = prevEnd.withDayOfMonth(1)

        val insights = buildList {
            cashflow(transactions, monthStart, monthEnd, currencyCode)?.let(::add)

            val spikeCategoryIds = mutableSetOf<String>()
            addAll(
                spendingSpikes(
                    transactions, categoriesById, currencyCode,
                    monthStart, monthEnd, prevStart, prevEnd, spikeCategoryIds,
                ),
            )

            topCategory(transactions, categoriesById, currencyCode, monthStart, monthEnd, spikeCategoryIds)
                ?.let(::add)

            overdueBills(transactions, today, currencyCode)?.let(::add)
            upcomingBills(transactions, today, currencyCode)?.let(::add)
        }

        // WARNING first, then INFO, then POSITIVE.
        return insights.sortedBy { it.severity.ordinal }
    }

    private fun cashflow(
        transactions: List<Transaction>,
        start: LocalDate,
        end: LocalDate,
        currencyCode: String,
    ): Insight? {
        val income = sumInWindow(transactions, TransactionType.INCOME, start, end)
        val expense = sumInWindow(transactions, TransactionType.EXPENSE, start, end)
        return when {
            income <= 0 -> null
            expense > income -> Insight(
                id = "cashflow-negative",
                kind = InsightKind.CASHFLOW,
                severity = InsightSeverity.WARNING,
                title = "Gastas más de lo que ingresas",
                message = msg(
                    "Este mes llevas ", amount(expense, currencyCode), " en gastos frente a ",
                    amount(income, currencyCode), " de ingresos.",
                ),
            )
            expense < income -> Insight(
                id = "cashflow-positive",
                kind = InsightKind.CASHFLOW,
                severity = InsightSeverity.POSITIVE,
                title = "Vas ahorrando",
                message = msg(
                    "Este mes has ahorrado ", amount(income - expense, currencyCode), ".",
                ),
            )
            else -> null
        }
    }

    private fun spendingSpikes(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
        currencyCode: String,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        prevStart: LocalDate,
        prevEnd: LocalDate,
        flagged: MutableSet<String>,
    ): List<Insight> {
        val thisByCat = expenseByCategory(transactions, monthStart, monthEnd)
        val prevByCat = expenseByCategory(transactions, prevStart, prevEnd)

        return thisByCat.mapNotNull { (categoryId, thisAmount) ->
            val prevAmount = prevByCat[categoryId] ?: return@mapNotNull null
            val delta = thisAmount - prevAmount
            if (prevAmount <= 0 || thisAmount < prevAmount * SPIKE_RATIO || delta < SPIKE_MIN_DELTA_MINOR) {
                return@mapNotNull null
            }
            val name = categoriesById[categoryId]?.name ?: return@mapNotNull null
            val pct = (delta * 100 / prevAmount).toInt()
            categoryId to Insight(
                id = "spike-$categoryId",
                kind = InsightKind.SPENDING_SPIKE,
                severity = InsightSeverity.WARNING,
                title = "Más gasto en $name",
                message = msg(
                    "Gastaste un $pct% más en $name que el mes pasado (",
                    amount(thisAmount, currencyCode), ").",
                ),
            )
        }
            .sortedByDescending { (categoryId, _) -> thisByCat[categoryId] ?: 0 }
            .take(2)
            .onEach { (categoryId, _) -> flagged += categoryId }
            .map { it.second }
    }

    private fun topCategory(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
        currencyCode: String,
        start: LocalDate,
        end: LocalDate,
        exclude: Set<String>,
    ): Insight? {
        val byCat = expenseByCategory(transactions, start, end)
            .filterKeys { it !in exclude }
        val (categoryId, amount) = byCat.maxByOrNull { it.value } ?: return null
        if (amount <= 0) return null
        val name = categoriesById[categoryId]?.name ?: return null
        return Insight(
            id = "top-category",
            kind = InsightKind.TOP_CATEGORY,
            severity = InsightSeverity.INFO,
            title = "Tu mayor gasto",
            message = msg(
                "$name es tu categoría con más gasto este mes: ",
                amount(amountMinor = amount, currencyCode = currencyCode), ".",
            ),
        )
    }

    private fun overdueBills(
        transactions: List<Transaction>,
        today: LocalDate,
        currencyCode: String,
    ): Insight? {
        val overdue = transactions.filter {
            it.status == TransactionStatus.PENDING &&
                it.type == TransactionType.EXPENSE &&
                it.estimatedDate != null && it.estimatedDate!! < today
        }
        if (overdue.isEmpty()) return null
        val total = overdue.sumOf { it.amountMinor }
        return Insight(
            id = "overdue-bills",
            kind = InsightKind.OVERDUE_BILLS,
            severity = InsightSeverity.WARNING,
            title = "Pagos vencidos",
            message = msg(
                "Tienes ${overdue.size} pago(s) vencido(s) por ", amount(total, currencyCode), ".",
            ),
        )
    }

    private fun upcomingBills(
        transactions: List<Transaction>,
        today: LocalDate,
        currencyCode: String,
    ): Insight? {
        val horizon = today.plusDays(7)
        val upcoming = transactions.filter {
            it.status == TransactionStatus.PENDING &&
                it.type == TransactionType.EXPENSE &&
                it.estimatedDate != null && it.estimatedDate!! in today..horizon
        }
        if (upcoming.isEmpty()) return null
        val total = upcoming.sumOf { it.amountMinor }
        return Insight(
            id = "upcoming-bills",
            kind = InsightKind.UPCOMING_BILLS,
            severity = InsightSeverity.INFO,
            title = "Pagos próximos",
            message = msg(
                "Tienes ${upcoming.size} pago(s) por ", amount(total, currencyCode),
                " en los próximos 7 días.",
            ),
        )
    }

    private fun expenseByCategory(
        transactions: List<Transaction>,
        start: LocalDate,
        end: LocalDate,
    ): Map<String, Long> = transactions
        .filter {
            it.type == TransactionType.EXPENSE &&
                it.status == TransactionStatus.PAID &&
                it.categoryId != null &&
                it.effectiveDate?.let { d -> d in start..end } == true
        }
        .groupBy { it.categoryId!! }
        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

    private fun sumInWindow(
        transactions: List<Transaction>,
        type: TransactionType,
        start: LocalDate,
        end: LocalDate,
    ): Long = transactions
        .filter {
            it.type == type &&
                it.status == TransactionStatus.PAID &&
                it.effectiveDate?.let { d -> d in start..end } == true
        }
        .sumOf { it.amountMinor }
}
