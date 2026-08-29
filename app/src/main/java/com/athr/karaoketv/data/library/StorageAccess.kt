package com.athr.karaoketv.data.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Works out which door into the filesystem is actually open on this box.
 *
 * Neither route can be assumed. Plenty of Android TV boxes ship without
 * DocumentsUI, so ACTION_OPEN_DOCUMENT_TREE resolves to nothing and the system
 * silently shows a "you don't have an app that can do this" toast instead of
 * throwing — which is why availability is checked up front rather than caught.
 * And from Android 11 the plain File API cannot reach a USB drive without
 * All-files access. So the setup screen asks this object what to offer.
 */
object StorageAccess {

    /**
     * Is there a real system folder picker to hand the tree grant to?
     *
     * On Android TV, usually not. Stock TV builds ship no DocumentsUI; instead
     * `com.android.tv.frameworkpackagestubs` registers for these intents and does
     * nothing but show a "you don't have an app that can do this" toast. Because a
     * stub answers, both `resolveActivity` and a plain `queryIntentActivities`
     * report the picker as present and `launch()` never throws — so the stubs have
     * to be recognised by name, or the app offers a button that quietly fails.
     */
    fun documentPickerAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        return context.packageManager
            .queryIntentActivities(intent, 0)
            .any { isRealHandler(it.activityInfo?.packageName) }
    }

    private fun isRealHandler(packageName: String?): Boolean = when {
        packageName == null -> false
        packageName == "android" -> false // the platform resolver, not a picker
        packageName.contains("frameworkpackagestubs") -> false // Android TV's no-op stub
        else -> true
    }

    /** Can we walk arbitrary paths with the File API right now? */
    fun hasFileSystemAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
        else -> ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when the only thing standing between us and the drive is All-files access. */
    fun needsAllFilesAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()

    /**
     * The All-files-access settings screen, app-specific if the box has it. Some
     * TV ROMs ship neither, hence the null.
     */
    fun allFilesAccessIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val appSpecific = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        if (appSpecific.resolveActivity(context.packageManager) != null) return appSpecific

        val generic = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        return generic.takeIf { it.resolveActivity(context.packageManager) != null }
    }
}
