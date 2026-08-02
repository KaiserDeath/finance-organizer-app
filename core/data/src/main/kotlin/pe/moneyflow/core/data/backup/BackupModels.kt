package pe.moneyflow.core.data.backup

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.model.Priority
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.SavingsGoal
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/** LocalDate <-> epoch-day so backups stay compact and timezone-independent. */
internal object LocalDateEpochSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateEpochDay", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeLong(value.toEpochDay())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.ofEpochDay(decoder.decodeLong())
}

/** Instant <-> epoch-millis. */
internal object InstantEpochSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantEpochMillis", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilli())
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

private inline fun <reified T : Enum<T>> enumOr(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

/** Root backup document. [version] guards against parsing incompatible future formats. */
@Serializable
internal data class BackupData(
    val version: Int = 1,
    val transactions: List<TransactionDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val paymentMethods: List<PaymentMethodDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val recurring: List<RecurringDto> = emptyList(),
    val savingsGoals: List<SavingsGoalDto> = emptyList(),
    val exchangeRates: List<ExchangeRateDto> = emptyList(),
)

@Serializable
internal data class TransactionDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val cardKind: String? = null,
    val accountId: String? = null,
    val transferAccountId: String? = null,
    val type: String,
    val status: String,
    val priority: String,
    @Serializable(LocalDateEpochSerializer::class) val estimatedDate: LocalDate? = null,
    @Serializable(LocalDateEpochSerializer::class) val actualDate: LocalDate? = null,
    val recurringId: String? = null,
    val installmentPlanId: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    @Serializable(InstantEpochSerializer::class) val createdAt: Instant,
    @Serializable(InstantEpochSerializer::class) val updatedAt: Instant,
)

@Serializable
internal data class CategoryDto(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val parentId: String? = null,
    val type: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    // Default keeps backups from before the fixed-expense flag restorable.
    val isFixed: Boolean = false,
)

@Serializable
internal data class PaymentMethodDto(
    val id: String,
    val name: String,
    val type: String,
    val cardKind: String? = null,
    val iconKey: String,
    val colorHex: String,
    val accountId: String? = null,
    val deepLinkPackage: String? = null,
    val playStoreId: String? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Serializable
internal data class AccountDto(
    val id: String,
    val name: String,
    val type: String,
    val currencyCode: String,
    val openingBalanceMinor: Long,
    val colorHex: String,
    val iconKey: String,
    val archived: Boolean = false,
    @Serializable(InstantEpochSerializer::class) val createdAt: Instant,
)

@Serializable
internal data class BudgetDto(
    val id: String,
    val name: String,
    val categoryId: String? = null,
    val amountMinor: Long,
    val period: String,
    @Serializable(LocalDateEpochSerializer::class) val startDate: LocalDate,
    val rollover: Boolean = false,
    val currencyCode: String,
)

@Serializable
internal data class RecurringDto(
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val cardKind: String? = null,
    val accountId: String? = null,
    val type: String,
    val frequency: String,
    val interval: Int,
    @Serializable(LocalDateEpochSerializer::class) val nextRunDate: LocalDate,
    @Serializable(LocalDateEpochSerializer::class) val endDate: LocalDate? = null,
    val autoCreate: Boolean = false,
    @Serializable(LocalDateEpochSerializer::class) val lastGeneratedDate: LocalDate? = null,
)

@Serializable
internal data class SavingsGoalDto(
    val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long = 0,
    @Serializable(LocalDateEpochSerializer::class) val targetDate: LocalDate? = null,
    val accountId: String? = null,
    val colorHex: String,
    val iconKey: String,
)

@Serializable
internal data class ExchangeRateDto(
    val id: String,
    val base: String,
    val quote: String,
    val rate: Double,
    @Serializable(LocalDateEpochSerializer::class) val asOf: LocalDate,
)

/** JSON codec shared by the backup repository; lenient so older/newer files still parse. */
internal val backupJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// --- domain -> dto ---

internal fun Transaction.toDto() = TransactionDto(
    id = id, title = title, description = description, amountMinor = amountMinor,
    currencyCode = currencyCode, categoryId = categoryId, paymentMethodId = paymentMethodId,
    cardKind = cardKind?.name,
    accountId = accountId, transferAccountId = transferAccountId, type = type.name,
    status = status.name, priority = priority.name, estimatedDate = estimatedDate,
    actualDate = actualDate, recurringId = recurringId, installmentPlanId = installmentPlanId,
    notes = notes, isFavorite = isFavorite, isPinned = isPinned, createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Category.toDto() = CategoryDto(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex, parentId = parentId,
    type = type.name, isDefault = isDefault, sortOrder = sortOrder, archived = archived,
    isFixed = isFixed,
)

internal fun PaymentMethod.toDto() = PaymentMethodDto(
    id = id, name = name, type = type.name, cardKind = cardKind?.name, iconKey = iconKey, colorHex = colorHex,
    accountId = accountId, deepLinkPackage = deepLinkPackage, playStoreId = playStoreId,
    isDefault = isDefault, sortOrder = sortOrder, archived = archived,
)

internal fun Account.toDto() = AccountDto(
    id = id, name = name, type = type.name, currencyCode = currencyCode,
    openingBalanceMinor = openingBalanceMinor, colorHex = colorHex, iconKey = iconKey,
    archived = archived, createdAt = createdAt,
)

internal fun Budget.toDto() = BudgetDto(
    id = id, name = name, categoryId = categoryId, amountMinor = amountMinor, period = period.name,
    startDate = startDate, rollover = rollover, currencyCode = currencyCode,
)

internal fun RecurringExpense.toDto() = RecurringDto(
    id = id, title = title, amountMinor = amountMinor, currencyCode = currencyCode,
    categoryId = categoryId, paymentMethodId = paymentMethodId, cardKind = cardKind?.name,
    accountId = accountId,
    type = type.name, frequency = frequency.name, interval = interval, nextRunDate = nextRunDate,
    endDate = endDate, autoCreate = autoCreate, lastGeneratedDate = lastGeneratedDate,
)

internal fun SavingsGoal.toDto() = SavingsGoalDto(
    id = id, name = name, targetAmountMinor = targetAmountMinor,
    currentAmountMinor = currentAmountMinor, targetDate = targetDate, accountId = accountId,
    colorHex = colorHex, iconKey = iconKey,
)

internal fun ExchangeRate.toDto() = ExchangeRateDto(
    id = id, base = base, quote = quote, rate = rate, asOf = asOf,
)

// --- dto -> domain ---

internal fun TransactionDto.toDomain() = Transaction(
    id = id, title = title, description = description, amountMinor = amountMinor,
    currencyCode = currencyCode, categoryId = categoryId, paymentMethodId = paymentMethodId,
    cardKind = cardKind?.let { enumOr(it, CardKind.DEBIT) },
    accountId = accountId, transferAccountId = transferAccountId,
    type = enumOr(type, TransactionType.EXPENSE), status = enumOr(status, TransactionStatus.PAID),
    priority = enumOr(priority, Priority.NORMAL), estimatedDate = estimatedDate,
    actualDate = actualDate, recurringId = recurringId, installmentPlanId = installmentPlanId,
    notes = notes, isFavorite = isFavorite, isPinned = isPinned, createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CategoryDto.toDomain() = Category(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex, parentId = parentId,
    type = enumOr(type, CategoryType.EXPENSE), isDefault = isDefault, sortOrder = sortOrder,
    archived = archived, isFixed = isFixed,
)

internal fun PaymentMethodDto.toDomain() = PaymentMethod(
    id = id, name = name, type = enumOr(type, PaymentMethodType.CASH),
    cardKind = cardKind?.let { enumOr(it, CardKind.DEBIT) }, iconKey = iconKey,
    colorHex = colorHex, accountId = accountId, deepLinkPackage = deepLinkPackage,
    playStoreId = playStoreId, isDefault = isDefault, sortOrder = sortOrder, archived = archived,
)

internal fun AccountDto.toDomain() = Account(
    id = id, name = name, type = enumOr(type, AccountType.CASH), currencyCode = currencyCode,
    openingBalanceMinor = openingBalanceMinor, colorHex = colorHex, iconKey = iconKey,
    archived = archived, createdAt = createdAt,
)

internal fun BudgetDto.toDomain() = Budget(
    id = id, name = name, categoryId = categoryId, amountMinor = amountMinor,
    period = enumOr(period, BudgetPeriod.MONTHLY), startDate = startDate, rollover = rollover,
    currencyCode = currencyCode,
)

internal fun RecurringDto.toDomain() = RecurringExpense(
    id = id, title = title, amountMinor = amountMinor, currencyCode = currencyCode,
    categoryId = categoryId, paymentMethodId = paymentMethodId,
    cardKind = cardKind?.let { enumOr(it, CardKind.DEBIT) }, accountId = accountId,
    type = enumOr(type, TransactionType.EXPENSE),
    frequency = enumOr(frequency, RecurrenceFrequency.MONTHLY), interval = interval,
    nextRunDate = nextRunDate, endDate = endDate, autoCreate = autoCreate,
    lastGeneratedDate = lastGeneratedDate,
)

internal fun SavingsGoalDto.toDomain() = SavingsGoal(
    id = id, name = name, targetAmountMinor = targetAmountMinor,
    currentAmountMinor = currentAmountMinor, targetDate = targetDate, accountId = accountId,
    colorHex = colorHex, iconKey = iconKey,
)

internal fun ExchangeRateDto.toDomain() = ExchangeRate(
    id = id, base = base, quote = quote, rate = rate, asOf = asOf,
)
