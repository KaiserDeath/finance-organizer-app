package pe.moneyflow.core.domain.model

/** A single smart suggestion surfaced to the user. */
data class Insight(
    val id: String,
    val kind: InsightKind,
    val severity: InsightSeverity,
    val title: String,
    val message: String,
)

enum class InsightKind {
    GETTING_STARTED,
    CASHFLOW,
    SPENDING_SPIKE,
    TOP_CATEGORY,
    UPCOMING_BILLS,
    OVERDUE_BILLS,
}

/** Tone of an insight, driving color/ordering in the UI. */
enum class InsightSeverity { WARNING, INFO, POSITIVE }
