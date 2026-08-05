package pe.moneyflow.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Real migrations so a version bump keeps the user's data instead of wiping it. Each one applies the
 * exact schema delta of that step; adding a nullable column is safe and needs no data backfill.
 * Keep this list in lockstep with the entity/@Database version — every bump needs a migration here.
 */

/** v4 → v5: debit/credit kind on a card payment method. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE payment_methods ADD COLUMN cardKind TEXT")
    }
}

/** v5 → v6: no schema change (added the reseed-on-open callback only). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Intentionally empty — schema identical to v5.
    }
}

/** v6 → v7: record the debit/credit characteristic on each movement and recurring template. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN cardKind TEXT")
        db.execSQL("ALTER TABLE recurring_expenses ADD COLUMN cardKind TEXT")
    }
}

/** v7 → v8: fixed-expense flag on categories, backfilled for the seeded fixed-cost defaults. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN isFixed INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE categories SET isFixed = 1 WHERE isDefault = 1 AND name IN " +
                "('Alquiler','Hipoteca','Servicios','Internet','Teléfono','Suscripciones','Seguros')",
        )
    }
}

/** v8 → v9: replace obsolete Android package ids without changing user-created methods. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE payment_methods SET deepLinkPackage = 'com.bcp.bank.bcp' " +
                "WHERE deepLinkPackage = 'pe.com.bcp.bancamovil'",
        )
        db.execSQL(
            "UPDATE payment_methods SET deepLinkPackage = 'pe.com.interbank.mobilebanking' " +
                "WHERE deepLinkPackage = 'pe.interbank.mobilebanking'",
        )
    }
}

/** Every migration, in order, for the Room builder. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
)
