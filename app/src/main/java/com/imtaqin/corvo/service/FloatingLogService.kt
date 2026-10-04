package com.imtaqin.corvo.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.imtaqin.corvo.MainActivity
import com.imtaqin.corvo.R
import com.imtaqin.corvo.core.LogBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Draggable log panel drawn over other apps via [WindowManager]. Reads the same
 * [LogBus] the in-app Logs screen does, so output stays live while the user is
 * inside the injected target app. Needs the "draw over other apps" permission.
 */
class FloatingLogService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wm: WindowManager? = null
    private var root: View? = null
    private var logView: TextView? = null
    private var scroller: ScrollView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotif()
        if (root == null) addOverlay()
        return START_STICKY
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics,
    ).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlay() {
        val wmgr = getSystemService(WINDOW_SERVICE) as WindowManager
        wm = wmgr

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xF2141821.toInt())
                setStroke(dp(1), 0xFF2C3242.toInt())
            }
            setPadding(0, 0, 0, dp(8))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(8), dp(10))
        }
        val title = TextView(this).apply {
            text = "FridaGo · logs"
            setTextColor(0xFFE7EAF2.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(0xFFA3ABB9.toInt())
            textSize = 16f
            setPadding(dp(12), dp(2), dp(12), dp(2))
            setOnClickListener { stopSelf() }
        }
        header.addView(title)
        header.addView(close)

        scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(dp(12), 0, dp(12), 0)
        }
        logView = TextView(this).apply {
            setTextColor(0xFFC8CEDA.toInt())
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
        }
        scroller!!.addView(logView)

        panel.addView(header)
        panel.addView(scroller)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            dp(300), dp(380), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(10)
            y = dp(90)
        }

        header.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x; startY = params.y
                        touchX = e.rawX; touchY = e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (e.rawX - touchX).toInt()
                        params.y = startY + (e.rawY - touchY).toInt()
                        runCatching { wmgr.updateViewLayout(panel, params) }
                    }
                }
                return true
            }
        })

        val added = runCatching { wmgr.addView(panel, params) }.isSuccess
        if (!added) {
            running.value = false
            stopSelf()
            return
        }
        root = panel
        running.value = true

        scope.launch { LogBus.log.collect { render(it) } }
    }

    private fun render(lines: List<String>) {
        val tv = logView ?: return
        val sb = SpannableStringBuilder()
        for (line in lines.takeLast(250)) {
            val start = sb.length
            sb.append(line).append('\n')
            val color = when {
                line.contains("[!]") -> 0xFFEF4444.toInt()
                line.contains("[+]") -> 0xFF22C55E.toInt()
                line.contains("[*]") -> 0xFF9B8AFB.toInt()
                else -> 0xFFC8CEDA.toInt()
            }
            sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tv.text = sb
        scroller?.post { scroller?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startForegroundNotif() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Floating logs", NotificationManager.IMPORTANCE_MIN),
            )
        }
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2,
            Intent(this, FloatingLogService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Floating logs")
            .setContentText("FridaGo logs are floating over other apps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Close", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        root?.let { v -> runCatching { wm?.removeView(v) } }
        root = null
        running.value = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "floating_logs"
        private const val NOTIF_ID = 43
        const val ACTION_STOP = "com.imtaqin.corvo.FLOAT_STOP"

        /** Reflects whether the overlay is currently shown, for the UI toggle. */
        val running = MutableStateFlow(false)

        fun start(ctx: Context) {
            val i = Intent(ctx, FloatingLogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, FloatingLogService::class.java).setAction(ACTION_STOP))
        }
    }
}
