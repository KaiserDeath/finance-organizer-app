package pe.moneyflow.core.data.mapper

import pe.moneyflow.core.database.entity.AccountEntity
import pe.moneyflow.core.database.entity.BudgetEntity
import pe.moneyflow.core.database.entity.CategoryEntity
import pe.moneyflow.core.database.entity.PaymentMethodEntity
import pe.moneyflow.core.database.entity.RecurringExpenseEntity
import pe.moneyflow.core.database.entity.TransactionEntity
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.model.Priority
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType

/** Parses an enum by name, falling back to [default] for unknown/corrupt values. */
private inline fun <reified T : Enum<T>> enumOr(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

// --- Transaction ---

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    title = title,
    description = description,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    paymentMethodId = paymentMethodId,
    accountId = accountId,
    type = enumOr(type, TransactionType.EXPENSE),
    status = enumOr(status, TransactionStatus.PAID),
    priority = enumOr(priority, Priority.NORMAL),
    estimatedDate = estimatedDate,
    actualDate = actualDate,
    recurringId = recurringId,
    installmentPlanId = installmentPlanId,
    notes = notes,
    isFavorite = isFavorite,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    description = description,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    paymentMethodId = paymentMethodId,
    accountId = accountId,
    type = type.name,
    status = status.name,
    priority = priority.name,
    estimatedDate = estimatedDate,
    actualDate = actualDate,
    recurringId = recurringId,
    installmentPlanId = installmentPlanId,
    notes = notes,
    isFavorite = isFavorite,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- Category ---

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    parentId = parentId,
    type = enumOr(type, CategoryType.EXPENSE),
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    parentId = parentId,
    type = type.name,
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
)

// --- PaymentMethod ---

fun PaymentMethodEntity.toDomain(): PaymentMethod = PaymentMethod(
    id = id,
    name = name,
    type = enumOr(type, PaymentMethodType.CASH),
    iconKey = iconKey,
    colorHex = colorHex,
    accountId = accountId,
    deepLinkPackage = deepLinkPackage,
    playStoreId = playStoreId,
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
)

fun PaymentMethod.toEntity(): PaymentMethodEntity = PaymentMethodEntity(
    id = id,
    name = name,
    type = type.name,
    iconKey = iconKey,
    colorHex = colorHex,
    accountId = accountId,
    deepLinkPackage = deepLinkPackage,
    playStoreId = playStoreId,
    isDefault = isDefault,
    sortOrder = sortOrder,
    archived = archived,
)

// --- Account ---

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = enumOr(type, AccountType.CASH),
    currencyCode = currencyCode,
    openingBalanceMinor = openingBalanceMinor,
    colorHex = colorHex,
    iconKey = iconKey,
    archived = archived,
    createdAt = createdAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    currencyCode = currencyCode,
    openingBalanceMinor = openingBalanceMinor,
    colorHex = colorHex,
    iconKey = iconKey,
    archived = archived,
    createdAt = createdAt,
)

// --- Budget ---

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    categoryId = categoryId,
    amountMinor = amountMinor,
    period = enumOr(period, BudgetPeriod.MONTHLY),
    startDate = startDate,
    rollover = rollover,
    currencyCode = currencyCode,
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    amountMinor = amountMinor,
    period = period.name,
    startDate = startDate,
    rollover = rollover,
    currencyCode = currencyCode,
)

// --- RecurringExpense ---

fun RecurringExpenseEntity.toDomain(): RecurringExpense = RecurringExpense(
    id = id,
    title = title,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    paymentMethodId = paymentMethodId,
    accountId = accountId,
    type = enumOr(type, TransactionType.EXPENSE),
    frequency = enumOr(frequency, RecurrenceFrequency.MONTHLY),
    interval = interval,
    nextRunDate = nextRunDate,
    endDate = endDate,
    autoCreate = autoCreate,
    lastGeneratedDate = lastGeneratedDate,
)

fun RecurringExpense.toEntity(): RecurringExpenseEntity = RecurringExpenseEntity(
    id = id,
    title = title,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    paymentMethodId = paymentMethodId,
    accountId = accountId,
    type = type.name,
    frequency = frequency.name,
    interval = interval,
    nextRunDate = nextRunDate,
    endDate = endDate,
    autoCreate = autoCreate,
    lastGeneratedDate = lastGeneratedDate,
)
