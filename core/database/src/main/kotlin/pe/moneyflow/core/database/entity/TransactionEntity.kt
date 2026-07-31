package pe.moneyflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["transferAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("paymentMethodId"),
        Index("accountId"),
        Index("transferAccountId"),
        Index("type"),
        Index("status"),
        Index("actualDate"),
        Index("estimatedDate"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String?,
    val paymentMethodId: String?,
    val cardKind: String?,
    val accountId: String?,
    val transferAccountId: String?,
    val type: String,
    val status: String,
    val priority: String,
    val estimatedDate: LocalDate?,
    val actualDate: LocalDate?,
    val recurringId: String?,
    val installmentPlanId: String?,
    val notes: String?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
