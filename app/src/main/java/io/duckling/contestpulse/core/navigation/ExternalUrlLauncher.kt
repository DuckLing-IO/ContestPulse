package io.duckling.contestpulse.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun Context.openWebUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme !in WEB_SCHEMES || uri.host.isNullOrBlank()) return false

    return runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, uri)
    }.recoverCatching {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }.isSuccess
}

private val WEB_SCHEMES = setOf("https", "http")
