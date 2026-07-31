package pe.moneyflow.feature.addedit

import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType

/**
 * Guesses a category from what the user typed: "almuerzo" already implies Comida, so making them
 * pick it again is asking for a decision they've already made. Suggestions never overwrite a
 * category the user chose by hand.
 *
 * Order matters — the first matching pattern wins, so the narrower food senses precede the
 * generic "compra".
 */
object CategorySuggester {

    private val patterns: List<Pair<Regex, String>> = listOf(
        Regex("almuerz|cena|desayun|comida|menu|menú|pollo", RegexOption.IGNORE_CASE) to "Comida",
        Regex("mercado|verdur|fruta|abarrote", RegexOption.IGNORE_CASE) to "Comida",
        Regex("restaur|cevich|chifa|pizza", RegexOption.IGNORE_CASE) to "Restaurantes",
        Regex("pasaje|taxi|uber|bus|combi|metro", RegexOption.IGNORE_CASE) to "Transporte",
        Regex("gasolin|grifo|combustib|petról", RegexOption.IGNORE_CASE) to "Combustible",
        Regex("farmac|clinic|clínic|doctor|medic", RegexOption.IGNORE_CASE) to "Salud",
        Regex("luz|agua|gas|recibo", RegexOption.IGNORE_CASE) to "Servicios",
        Regex("internet|wifi|fibra", RegexOption.IGNORE_CASE) to "Internet",
        Regex("netflix|spotify|suscrip|plan", RegexOption.IGNORE_CASE) to "Suscripciones",
        Regex("cine|concierto|juego|salida", RegexOption.IGNORE_CASE) to "Entretenimiento",
        Regex("alquiler|renta|depa", RegexOption.IGNORE_CASE) to "Alquiler",
        Regex("ropa|zapat|tienda|compra", RegexOption.IGNORE_CASE) to "Compras",
    )

    /** The expense category [title] implies, or null when nothing matches or the name is gone. */
    fun suggest(title: String, categories: List<Category>): Category? {
        if (title.isBlank()) return null
        val name = patterns.firstOrNull { (pattern, _) -> pattern.containsMatchIn(title) }?.second
            ?: return null
        return categories.firstOrNull {
            it.type == CategoryType.EXPENSE && it.name.equals(name, ignoreCase = true)
        }
    }
}
