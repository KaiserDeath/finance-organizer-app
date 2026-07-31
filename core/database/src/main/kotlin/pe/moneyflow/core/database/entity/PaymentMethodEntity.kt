package pe.moneyflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_methods",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId")],
)
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val cardKind: String?,
    val iconKey: String,
    val colorHex: String,
    val accountId: String?,
    val deepLinkPackage: String?,
    val playStoreId: String?,
    val isDefault: Boolean,
    val sortOrder: Int,
    val archived: Boolean,
)
