package pe.moneyflow.feature.analytics

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes an already-rendered CSV string into the app's cache and launches the system share
 * sheet through a [FileProvider] grant. Kept out of the ViewModel so no Android [Context]
 * leaks into it.
 */
object CsvExporter {

    private const val EXPORT_DIR = "exports"

    fun share(context: Context, csv: String, fileName: String) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        // Prepend a UTF-8 BOM so Excel opens accented Spanish text correctly.
        file.writeText("﻿$csv", Charsets.UTF_8)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Exportar movimientos")
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(chooser)
    }
}
