package com.imtaqin.fridago

import android.app.Application
import com.topjohnwu.superuser.Shell

/**
 * Application entry point. Configures the global libsu [Shell] factory so every
 * root command issued anywhere in the app reuses a single long-lived `su` session.
 */
class CorvoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                // Mount master gives the shell visibility of all mount namespaces,
                // which frida-server needs when attaching to other apps' processes.
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(20)
        )
    }
}
