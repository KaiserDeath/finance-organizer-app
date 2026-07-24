package pe.moneyflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Enums are stored as their `name` string; mapping to/from domain enums happens in :core:data. */
@Entity(
    tableName = "categories",
    indices = [Index("parentId"), Index("type")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val parentId: String?,
    val type: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val archived: Boolean,
)
