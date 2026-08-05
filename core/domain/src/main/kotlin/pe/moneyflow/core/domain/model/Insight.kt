package pe.moneyflow.core.domain.model

import pe.moneyflow.core.common.Money

/**
 * A piece of an insight's message: either literal prose, or an amount left unformatted.
 *
 * The domain used to build these sentences with `Money.format` inlined, which buried a
 * presentation decision — how an amount is rendered — inside a `String` the UI could no longer
 * take apart. Discreet mode is what exposed it: masking a figure meant matching it back out of
 * finished prose with a regular expression, which holds only until someone changes the money
 * format.
 *
 * Keeping the amount as a number until the last moment puts that decision back where it belongs:
 * masked, in another currency, or in a style the domain has never heard of.
 */
sealed interface MessagePart {
    /** Literal text, already in the user's language. */
    @JvmInline
    value class Text(val value: String) : MessagePart

    /** An amount in minor units, for the UI to format — or to mask. */
    data class Amount(val amountMinor: Long, val currencyCode: String) : MessagePart
}

/** A single smart suggestion surfaced to the user. */
data class Insight(
    val id: String,
    val kind: InsightKind,
    val severity: InsightSeverity,
    val title: String,
    /**
     * The message, in parts. Render it with `core:ui`'s `insightMessage(...)`, which honours
     * discreet mode. [plainMessage] is the fallback for callers with no composition.
     */
    val message: List<MessagePart>,
    /** Category this suggestion is about, when tapping it can open filtered movements. */
    val categoryId: String? = null,
) {
    /**
     * The message with every amount formatted normally.
     *
     * For contexts that cannot consult the composition and must not mask anyway — tests, and
     * exports, which the user explicitly asked for. UI code should not use this: it is the one
     * path that ignores discreet mode.
     */
    val plainMessage: String
        get() = message.joinToString("") { part ->
            when (part) {
                is MessagePart.Text -> part.value
                is MessagePart.Amount -> Money.format(part.amountMinor, part.currencyCode)
            }
        }
}

/**
 * Builds a message from alternating prose and amounts:
 * `msg("Llevas ", amount(spent, code), " este mes.")`.
 *
 * Strings become [MessagePart.Text] so call sites read as sentences rather than as a list of
 * wrapper constructors.
 */
fun msg(vararg parts: Any): List<MessagePart> = parts.map { part ->
    when (part) {
        is MessagePart -> part
        is String -> MessagePart.Text(part)
        else -> error("unsupported message part: ${part::class.simpleName}")
    }
}

/** Shorthand for an amount part. */
fun amount(amountMinor: Long, currencyCode: String): MessagePart =
    MessagePart.Amount(amountMinor, currencyCode)

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
