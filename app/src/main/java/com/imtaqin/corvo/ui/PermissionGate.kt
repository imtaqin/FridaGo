package com.imtaqin.corvo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.imtaqin.corvo.ui.theme.Status

/**
 * Hard permission gate. FridaGo asks for notifications + "display over other
 * apps" on launch and blocks the main UI until both are granted — re-checking
 * every time the screen resumes (e.g. after returning from system settings).
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun notifOk() = !needsNotif ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    fun overlayOk() = Settings.canDrawOverlays(ctx)

    var notifGranted by remember { mutableStateOf(notifOk()) }
    var overlayGranted by remember { mutableStateOf(overlayOk()) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifGranted = it }

    // Re-check on resume: overlay is granted in Settings, notifications via dialog.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = notifOk()
                overlayGranted = overlayOk()
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    if (notifGranted && overlayGranted) {
        content()
        return
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(42.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "FridaGo needs permission",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Allow everything below to continue — the app won't start until all are granted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            PermCard(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                desc = "Shows the live server and floating-logs status in your shade.",
                granted = notifGranted,
                onGrant = {
                    if (needsNotif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            )
            Spacer(Modifier.height(12.dp))
            PermCard(
                icon = Icons.Filled.Layers,
                title = "Display over other apps",
                desc = "Floats the log window on top of the app you inject into.",
                granted = overlayGranted,
                onGrant = {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "Root (su) is requested separately the first time you start the server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PermCard(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    CorvoCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (granted) Status.Ok else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "granted", tint = Status.Ok)
            } else {
                Button(onClick = onGrant, shape = MaterialTheme.shapes.large) { Text("Allow") }
            }
        }
    }
}
