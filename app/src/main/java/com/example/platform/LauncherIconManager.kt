package com.example.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.data.preferences.AppIconOption

/**
 * Switches the active Android launcher icon using <activity-alias> components
 * declared in AndroidManifest.xml.
 */
class LauncherIconManager(private val context: Context) {

    fun applyLauncherIcon(selected: AppIconOption): Boolean {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            // Enable the selected alias first so there is always an active launcher component
            pm.setComponentEnabledSetting(
                ComponentName(pkg, selected.aliasClassName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            // Disable all other aliases
            for (option in AppIconOption.entries) {
                if (option != selected) {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, option.aliasClassName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }
}
