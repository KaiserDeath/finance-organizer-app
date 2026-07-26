package pe.moneyflow.core.model

/**
 * A spending/income bucket. [iconKey] is a stable string resolved to a Material icon in the
 * design system; [colorHex] is an ARGB/RGB hex string like "#FF7043".
 */
data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val parentId: String? = null,
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)
