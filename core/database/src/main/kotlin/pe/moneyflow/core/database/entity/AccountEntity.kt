package pe.moneyflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val currencyCode: String,
    val openingBalanceMinor: Long,
    val colorHex: String,
    val iconKey: String,
    val archived: Boolean,
    val createdAt: Instant,
)
