package pe.moneyflow.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Seeds default Peru-oriented categories, payment methods and a cash account the first time the
 * database is created. Uses raw SQL (execSQL) so it runs safely inside the Room onCreate callback
 * without needing the DAOs to be ready.
 *
 * NOTE: Bank-app package ids below are best-known values and should be VERIFIED before release —
 * if a package is wrong, the launcher simply falls back to a Play Store search by name.
 */
internal object SeedData {

    private data class SeedCategory(
        val name: String,
        val icon: String,
        val color: String,
        val type: String = "EXPENSE",
    )

    private data class SeedPayment(
        val name: String,
        val type: String,
        val icon: String,
        val color: String,
        val pkg: String? = null,
        /** DEBIT/CREDIT for card methods; null for cash, wallets and banks. */
        val cardKind: String? = null,
    )

    private val categories = listOf(
        SeedCategory("Comida", "food", "#FF7043"),
        SeedCategory("Restaurantes", "restaurant", "#FF8A65"),
        SeedCategory("Transporte", "transport", "#42A5F5"),
        SeedCategory("Combustible", "fuel", "#26A69A"),
        SeedCategory("Alquiler", "home", "#7E57C2"),
        SeedCategory("Hipoteca", "account_balance", "#5C6BC0"),
        SeedCategory("Servicios", "bolt", "#FFA726"),
        SeedCategory("Internet", "wifi", "#29B6F6"),
        SeedCategory("Teléfono", "phone", "#66BB6A"),
        SeedCategory("Entretenimiento", "movie", "#EC407A"),
        SeedCategory("Gaming", "sports_esports", "#AB47BC"),
        SeedCategory("Compras", "shopping", "#EF5350"),
        SeedCategory("Salud", "health", "#26C6DA"),
        SeedCategory("Educación", "school", "#8D6E63"),
        SeedCategory("Suscripciones", "subscriptions", "#5C6BC0"),
        SeedCategory("Seguros", "shield", "#78909C"),
        SeedCategory("Viajes", "flight", "#FFCA28"),
        SeedCategory("Mascotas", "pets", "#A1887F"),
        SeedCategory("Impuestos", "receipt", "#90A4AE"),
        SeedCategory("Negocio", "business", "#607D8B"),
        SeedCategory("Inversiones", "trending_up", "#66BB6A"),
        SeedCategory("Ahorros", "savings", "#4DB6AC"),
        SeedCategory("Otros", "category", "#B0BEC5"),
        SeedCategory("Salario", "payments", "#66BB6A", type = "INCOME"),
        SeedCategory("Freelance", "work", "#4DB6AC", type = "INCOME"),
        SeedCategory("Regalos", "gift", "#F06292", type = "INCOME"),
        SeedCategory("Otros ingresos", "attach_money", "#9CCC65", type = "INCOME"),
    )

    private val paymentMethods = listOf(
        SeedPayment("Efectivo", "CASH", "cash", "#66BB6A"),
        SeedPayment("Visa débito", "CARD", "card", "#1A237E", cardKind = "DEBIT"),
        SeedPayment("Visa crédito", "CARD", "card", "#283593", cardKind = "CREDIT"),
        SeedPayment("Mastercard crédito", "CARD", "card", "#FF6F00", cardKind = "CREDIT"),
        SeedPayment("American Express", "CARD", "card", "#2E7D32", cardKind = "CREDIT"),
        SeedPayment("Yape", "EWALLET", "wallet", "#6A1B9A", pkg = "com.bcp.innovacxion.yapeapp"),
        SeedPayment("Plin", "EWALLET", "wallet", "#00897B"),
        SeedPayment("Tunki", "EWALLET", "wallet", "#00C2A8"),
        SeedPayment("BCP", "BANK", "account_balance", "#EA7600", pkg = "pe.com.bcp.bancamovil"),
        SeedPayment("Interbank", "BANK", "account_balance", "#00A94F", pkg = "pe.interbank.mobilebanking"),
        SeedPayment("BBVA", "BANK", "account_balance", "#1464A5", pkg = "com.bbva.nxt_peru"),
        SeedPayment("Scotiabank", "BANK", "account_balance", "#EC111A"),
        SeedPayment("Banco de la Nación", "BANK", "account_balance", "#E30613"),
    )

    /**
     * Reseeds defaults only when the database comes up completely bare — the state left by a
     * destructive migration (which recreates empty tables without calling [seed] via onCreate).
     * Requiring every core table to be empty means deliberately pruning one (e.g. deleting all
     * payment methods) won't trigger an unwanted reseed.
     */
    fun seedIfEmpty(db: SupportSQLiteDatabase) {
        if (isEmpty(db, "categories") && isEmpty(db, "payment_methods") && isEmpty(db, "accounts")) {
            seed(db)
        }
    }

    private fun isEmpty(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            !cursor.moveToFirst() || cursor.getInt(0) == 0
        }

    fun seed(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        val cashAccountId = UUID.randomUUID().toString()
        db.execSQL(
            "INSERT INTO accounts " +
                "(id, name, type, currencyCode, openingBalanceMinor, colorHex, iconKey, archived, createdAt) " +
                "VALUES (?, 'Efectivo', 'CASH', 'PEN', 0, '#66BB6A', 'cash', 0, ?)",
            arrayOf(cashAccountId, now),
        )

        categories.forEachIndexed { index, c ->
            db.execSQL(
                "INSERT INTO categories " +
                    "(id, name, iconKey, colorHex, parentId, type, isDefault, sortOrder, archived) " +
                    "VALUES (?, ?, ?, ?, NULL, ?, 1, ?, 0)",
                arrayOf(UUID.randomUUID().toString(), c.name, c.icon, c.color, c.type, index),
            )
        }

        paymentMethods.forEachIndexed { index, p ->
            val isDefault = if (p.name == "Efectivo") 1 else 0
            db.execSQL(
                "INSERT INTO payment_methods " +
                    "(id, name, type, cardKind, iconKey, colorHex, accountId, deepLinkPackage, playStoreId, isDefault, sortOrder, archived) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                arrayOf(
                    UUID.randomUUID().toString(),
                    p.name,
                    p.type,
                    p.cardKind,
                    p.icon,
                    p.color,
                    if (p.name == "Efectivo") cashAccountId else null,
                    p.pkg,
                    p.pkg,
                    isDefault,
                    index,
                ),
            )
        }

        // Approximate starting exchange rates against PEN. Users can edit or add their own.
        // 1 unit of `base` = `rate` units of `quote`.
        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        exchangeRates.forEach { (base, quote, rate) ->
            db.execSQL(
                "INSERT INTO exchange_rates (id, base, quote, rate, asOf) VALUES (?, ?, ?, ?, ?)",
                arrayOf(UUID.randomUUID().toString(), base, quote, rate, todayEpochDay),
            )
        }
    }

    private val exchangeRates = listOf(
        Triple("USD", "PEN", 3.75),
        Triple("EUR", "PEN", 4.05),
    )
}
