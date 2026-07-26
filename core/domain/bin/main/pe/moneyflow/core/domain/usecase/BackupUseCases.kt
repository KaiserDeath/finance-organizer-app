package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.domain.model.BackupSummary
import pe.moneyflow.core.domain.repository.BackupRepository
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val repository: BackupRepository,
) {
    suspend operator fun invoke(): String = repository.exportJson()
}

class ImportBackupUseCase @Inject constructor(
    private val repository: BackupRepository,
) {
    suspend operator fun invoke(json: String): Result<BackupSummary> = repository.importJson(json)
}
