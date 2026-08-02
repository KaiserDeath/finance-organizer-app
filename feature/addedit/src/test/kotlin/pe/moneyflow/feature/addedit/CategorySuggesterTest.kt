package pe.moneyflow.feature.addedit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType

class CategorySuggesterTest {

    private fun category(name: String, type: CategoryType = CategoryType.EXPENSE) = Category(
        id = name.lowercase(),
        name = name,
        iconKey = "category",
        colorHex = "#FF7043",
        type = type,
    )

    private val categories = listOf(
        category("Comida"),
        category("Restaurantes"),
        category("Transporte"),
        category("Servicios"),
        category("Suscripciones"),
        category("Compras"),
        category("Salario", CategoryType.INCOME),
    )

    @Test
    fun `matches the obvious cases`() {
        assertEquals("comida", CategorySuggester.suggest("Almuerzo", categories)?.id)
        assertEquals("transporte", CategorySuggester.suggest("Taxi al centro", categories)?.id)
        assertEquals("servicios", CategorySuggester.suggest("Recibo de luz", categories)?.id)
        assertEquals("suscripciones", CategorySuggester.suggest("Netflix", categories)?.id)
    }

    @Test
    fun `is case and accent tolerant within the pattern`() {
        assertEquals("comida", CategorySuggester.suggest("ALMUERZO", categories)?.id)
        assertEquals("comida", CategorySuggester.suggest("menú del día", categories)?.id)
    }

    @Test
    fun `first matching pattern wins`() {
        // "almuerzo" (Comida) precedes "chifa" (Restaurantes) in the table, so Comida wins.
        assertEquals("comida", CategorySuggester.suggest("almuerzo en la chifa", categories)?.id)
    }

    @Test
    fun `no match yields null`() {
        assertNull(CategorySuggester.suggest("xyzzy", categories))
        assertNull(CategorySuggester.suggest("", categories))
    }

    @Test
    fun `never suggests an income category`() {
        val onlyIncome = listOf(category("Comida", CategoryType.INCOME))
        assertNull(CategorySuggester.suggest("almuerzo", onlyIncome))
    }

    @Test
    fun `missing category name yields null instead of an orphan`() {
        assertNull(CategorySuggester.suggest("gasolina", categories))
    }
}
