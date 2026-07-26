package pe.moneyflow.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.usecase.ExportBackupUseCase
import pe.moneyflow.core.domain.usecase.ImportBackupUseCase
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    /** Serializes the whole database; the caller writes it to the chosen file. */
    suspend fun exportJson(): String = exportBackup()

    fun onExported() {
        _status.value = "Copia de seguridad guardada"
    }

    fun onExportFailed() {
        _status.value = "No se pudo guardar la copia"
    }

    fun import(json: String) {
        viewModelScope.launch {
            importBackup(json).fold(
                onSuccess = { _status.value = "Restaurados ${it.total} registros" },
                onFailure = { _status.value = "Archivo inválido o dañado" },
            )
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
