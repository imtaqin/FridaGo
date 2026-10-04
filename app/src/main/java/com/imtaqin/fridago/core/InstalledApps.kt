package com.imtaqin.fridago.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A target the user can inject into. */
data class TargetApp(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
)

object InstalledApps {

    /**
     * Lists installed packages sorted by label. User apps first, then system apps.
     * [includeSystem] controls whether framework/system packages are included.
     */
    suspend fun list(ctx: Context, includeSystem: Boolean): List<TargetApp> =
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val flags = PackageManager.ApplicationInfoFlags.of(0L)
            pm.getInstalledApplications(flags)
                .asSequence()
                .mapNotNull { info ->
                    val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (system && !includeSystem) return@mapNotNull null
                    TargetApp(
                        label = pm.getApplicationLabel(info).toString(),
                        packageName = info.packageName,
                        isSystem = system,
                    )
                }
                .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
                .toList()
        }
}
