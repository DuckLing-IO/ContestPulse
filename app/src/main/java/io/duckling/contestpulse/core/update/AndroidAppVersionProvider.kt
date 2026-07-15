package io.duckling.contestpulse.core.update

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.domain.update.AppVersion
import io.duckling.contestpulse.domain.update.AppVersionProvider
import javax.inject.Inject

class AndroidAppVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionProvider {
    override fun currentVersion(): AppVersion {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppVersion(
            name = packageInfo.versionName.orEmpty(),
            code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
        )
    }
}
