package pe.moneyflow.core.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens a payment method's official app. If [packageName] is installed it launches directly;
 * otherwise it falls back to the Play Store (details page when we know the id, else a search by
 * name), and finally to the Play Store website. Safe to call for methods without an app (Cash).
 */
fun launchPaymentApp(
    context: Context,
    packageName: String?,
    appName: String,
    playStoreId: String? = packageName,
): Boolean {
    val packageManager = context.packageManager

    if (!packageName.isNullOrBlank()) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }
    }

    val storeUri = if (!playStoreId.isNullOrBlank()) {
        Uri.parse("market://details?id=$playStoreId")
    } else {
        Uri.parse("market://search?q=${Uri.encode(appName)}")
    }
    val storeIntent = Intent(Intent.ACTION_VIEW, storeUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return try {
        context.startActivity(storeIntent)
        true
    } catch (_: ActivityNotFoundException) {
        val webUri = if (!playStoreId.isNullOrBlank()) {
            Uri.parse("https://play.google.com/store/apps/details?id=$playStoreId")
        } else {
            Uri.parse("https://play.google.com/store/search?q=${Uri.encode(appName)}&c=apps")
        }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(webIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
