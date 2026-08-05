package pe.moneyflow.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The migrations, executed against real SQLite.
 *
 * `Migrations.kt` says a version bump must keep the user's data instead of wiping it, and
 * `DatabaseModule` still carries `fallbackToDestructiveMigration()` behind the real migrations.
 * That fallback is silent: if a migration is missing or throws, the user's ledger is dropped and
 * recreated empty, and the app comes up looking merely new rather than broken. Nothing else in the
 * suite can catch that — a unit test never opens a database, and by the time a person notices,
 * their data is gone.
 *
 * So these tests assert the two things the fallback would hide: that each step *runs*, and that
 * rows written before it are still there afterwards.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MoneyFlowDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /** A category row as v4 knew it — before `isFixed` existed. */
    private fun SupportSQLiteDatabase.insertCategoryV4(
        id: String,
        name: String,
        isDefault: Int,
    ) = execSQL(
        "INSERT INTO categories " +
            "(id, name, iconKey, colorHex, parentId, type, isDefault, sortOrder, archived) " +
            "VALUES ('$id', '$name', 'home', '#7E57C2', NULL, 'EXPENSE', $isDefault, 0, 0)",
    )

    private fun SupportSQLiteDatabase.insertPaymentMethodV4(id: String, name: String) = execSQL(
        "INSERT INTO payment_methods " +
            "(id, name, type, iconKey, colorHex, accountId, deepLinkPackage, playStoreId, " +
            "isDefault, sortOrder, archived) " +
            "VALUES ('$id', '$name', 'CASH', 'cash', '#26A69A', NULL, NULL, NULL, 1, 0, 0)",
    )

    private fun SupportSQLiteDatabase.insertTransactionV4(id: String, amountMinor: Long) = execSQL(
        "INSERT INTO transactions " +
            "(id, title, description, amountMinor, currencyCode, categoryId, paymentMethodId, " +
            "accountId, transferAccountId, type, status, priority, estimatedDate, actualDate, " +
            "recurringId, installmentPlanId, notes, isFavorite, isPinned, createdAt, updatedAt) " +
            "VALUES ('$id', 'Almuerzo', NULL, $amountMinor, 'PEN', 'c1', 'pm1', NULL, NULL, " +
            "'EXPENSE', 'PAID', 'NORMAL', NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 0)",
    )

    private fun SupportSQLiteDatabase.queryOne(sql: String): String? =
        query(sql).use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    /**
     * The whole chain at once, which is the upgrade an install sitting on v4 actually performs.
     * `runMigrationsAndValidate` also checks the result against the exported v8 schema, so a
     * migration that runs but produces the wrong shape fails here rather than at the first query.
     */
    @Test
    fun migratesAll_from4To9_preservingData() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.insertCategoryV4(id = "c1", name = "Alquiler", isDefault = 1)
            db.insertPaymentMethodV4(id = "pm1", name = "Efectivo")
            db.insertTransactionV4(id = "t1", amountMinor = 1_850)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, *ALL_MIGRATIONS)

        // The point of the exercise: the user's ledger survived four version bumps.
        assertEquals(1, db.count("transactions"))
        assertEquals("1850", db.queryOne("SELECT amountMinor FROM transactions WHERE id = 't1'"))
        assertEquals("Efectivo", db.queryOne("SELECT name FROM payment_methods WHERE id = 'pm1'"))
        assertEquals("Alquiler", db.queryOne("SELECT name FROM categories WHERE id = 'c1'"))
    }

    /** v4 → v5 adds a nullable column, so existing rows must read null rather than fail. */
    @Test
    fun migration4To5_addsCardKind_asNullOnExistingRows() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.insertPaymentMethodV4(id = "pm1", name = "Efectivo")
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        assertNull(db.queryOne("SELECT cardKind FROM payment_methods WHERE id = 'pm1'"))
    }

    /**
     * v5 → v6 is deliberately empty — it exists so the version bump has a migration at all. Without
     * it Room takes the destructive path, and that is precisely the wipe this file is here to catch,
     * so "it does nothing" is the behaviour worth pinning.
     */
    @Test
    fun migration5To6_isEmpty_andKeepsEverything() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.insertCategoryV4(id = "c1", name = "Comida", isDefault = 0)
            db.insertPaymentMethodV4(id = "pm1", name = "Efectivo")
            db.insertTransactionV4(id = "t1", amountMinor = 4_200)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        assertEquals(1, db.count("transactions"))
        assertEquals(1, db.count("categories"))
        assertEquals("4200", db.queryOne("SELECT amountMinor FROM transactions WHERE id = 't1'"))
    }

    @Test
    fun migration6To7_addsCardKind_toTransactionsAndRecurring() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.insertTransactionV4(id = "t1", amountMinor = 900)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        assertEquals(1, db.count("transactions"))
        assertNull(db.queryOne("SELECT cardKind FROM transactions WHERE id = 't1'"))
        // The recurring column is the half with no row to prove it: query it so a missing ALTER
        // fails here instead of the first time a template is read.
        assertEquals(0, db.count("recurring_expenses"))
        db.query("SELECT cardKind FROM recurring_expenses").close()
    }

    /**
     * v7 → v8's backfill is the only migration that makes a *decision* about existing data, so it
     * is the only one where "the column exists" is not enough. Three cases, because the WHERE
     * clause has three outcomes.
     */
    @Test
    fun migration7To8_backfillsIsFixed_onSeededFixedCategoriesOnly() {
        helper.createDatabase(TEST_DB, 7).use { db ->
            // A seeded fixed cost: must come out flagged.
            db.insertCategoryV4(id = "c1", name = "Alquiler", isDefault = 1)
            // A seeded category that is not a fixed cost: must not be flagged.
            db.insertCategoryV4(id = "c2", name = "Comida", isDefault = 1)
            // The user's own category that happens to share the name. isDefault = 0, so the
            // backfill must leave it alone rather than matching on the name.
            db.insertCategoryV4(id = "c3", name = "Alquiler", isDefault = 0)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        assertEquals("1", db.queryOne("SELECT isFixed FROM categories WHERE id = 'c1'"))
        assertEquals("0", db.queryOne("SELECT isFixed FROM categories WHERE id = 'c2'"))
        assertEquals("0", db.queryOne("SELECT isFixed FROM categories WHERE id = 'c3'"))
    }

    @Test
    fun migration8To9_replacesOnlyObsoleteBankPackageIds() {
        helper.createDatabase(TEST_DB, 8).use { db ->
            db.execSQL(
                "INSERT INTO payment_methods " +
                    "(id, name, type, cardKind, iconKey, colorHex, accountId, deepLinkPackage, " +
                    "playStoreId, isDefault, sortOrder, archived) VALUES " +
                    "('bcp', 'BCP', 'BANK', NULL, 'account_balance', '#EA7600', NULL, " +
                    "'pe.com.bcp.bancamovil', NULL, 1, 0, 0), " +
                    "('interbank', 'Interbank', 'BANK', NULL, 'account_balance', '#00A94F', NULL, " +
                    "'pe.interbank.mobilebanking', NULL, 1, 1, 0), " +
                    "('custom', 'Mi banco', 'BANK', NULL, 'account_balance', '#000000', NULL, " +
                    "'example.user.bank', NULL, 0, 2, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        assertEquals(
            "com.bcp.bank.bcp",
            db.queryOne("SELECT deepLinkPackage FROM payment_methods WHERE id = 'bcp'"),
        )
        assertEquals(
            "pe.com.interbank.mobilebanking",
            db.queryOne("SELECT deepLinkPackage FROM payment_methods WHERE id = 'interbank'"),
        )
        assertEquals(
            "example.user.bank",
            db.queryOne("SELECT deepLinkPackage FROM payment_methods WHERE id = 'custom'"),
        )
    }
}
