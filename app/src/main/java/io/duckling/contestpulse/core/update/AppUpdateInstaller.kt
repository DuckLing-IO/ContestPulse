package io.duckling.contestpulse.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

fun Context.canInstallAppUpdates(): Boolean = packageManager.canRequestPackageInstalls()

fun Context.openAppUpdateInstallSettings() {
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:$packageName"),
    )
    startActivity(intent)
}

fun Context.installAppUpdate(apkFile: File) {
    val apkUri = FileProvider.getUriForFile(
        this,
        "$packageName.updates",
        apkFile,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, APK_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
