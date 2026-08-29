package com.athr.karaoketv.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads a release APK and hands it to the system installer.
 *
 * TV boxes cannot be assumed to have one. Stock Android TV ships no activity for
 * the usual install intent — and, as with the folder picker, `startActivity` does
 * not throw when nothing handles it, so the app would report success while the
 * viewer sees nothing happen. Hence the up-front check, and hence the download
 * landing in the public Downloads folder: when there is no installer to hand it
 * to, the viewer can still open the file with whatever file manager they used to
 * sideload the app in the first place.
 */
object ApkInstaller {

    private const val MIME_APK = "application/vnd.android.package-archive"

    data class Downloaded(val file: File, val userVisiblePath: String?)

    suspend fun download(
        context: Context,
        url: String,
        versionName: String,
        onProgress: suspend (Int) -> Unit,
    ): Downloaded = withContext(Dispatchers.IO) {
        val fileName = "KaraokeTV-$versionName.apk"
        val publicDir = publicDownloadDir()
        val target = if (publicDir != null) File(publicDir, fileName) else File(context.cacheDir, fileName)
        if (target.exists()) target.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
        }
        try {
            val total = connection.contentLengthLong.takeIf { it > 0 }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastReported = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total != null) {
                            val pct = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastReported) {
                                lastReported = pct
                                onProgress(pct)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        Downloaded(target, if (publicDir != null) target.absolutePath else null)
    }

    private fun publicDownloadDir(): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            return null
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return dir?.takeIf { (it.exists() || it.mkdirs()) && it.canWrite() }
    }

    /** From Android 8 the box must trust this app specifically to install APKs. */
    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun installPermissionIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    /** Is there a real installer UI on this box, rather than a no-op stub? */
    fun hasInstallerUi(context: Context): Boolean =
        installIntents(context, Uri.EMPTY).any { intent ->
            context.packageManager.queryIntentActivities(intent, 0).any { match ->
                val pkg = match.activityInfo?.packageName
                pkg != null && pkg != "android" && !pkg.contains("frameworkpackagestubs")
            }
        }

    private fun installIntents(context: Context, uri: Uri): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        @Suppress("DEPRECATION")
        Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )

    fun install(context: Context, apk: File): Boolean {
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        }.getOrNull() ?: return false

        return installIntents(context, uri).any { intent ->
            val handled = context.packageManager.queryIntentActivities(intent, 0).any { match ->
                val pkg = match.activityInfo?.packageName
                pkg != null && pkg != "android" && !pkg.contains("frameworkpackagestubs")
            }
            handled && runCatching { context.startActivity(intent); true }.getOrDefault(false)
        }
    }
}
