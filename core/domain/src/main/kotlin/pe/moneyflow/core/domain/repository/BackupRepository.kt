package pe.moneyflow.core.domain.repository

import pe.moneyflow.core.domain.model.BackupSummary

/** Exports the whole database to a portable JSON string and restores it back. */
interface BackupRepository {
    suspend fun exportJson(): String

    /** Parses [json] and upserts every record, returning per-type counts, or a failure. */
    suspend fun importJson(json: String): Result<BackupSummary>
}
