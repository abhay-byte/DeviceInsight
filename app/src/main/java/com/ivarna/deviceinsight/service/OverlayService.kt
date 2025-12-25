package com.ivarna.deviceinsight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.R
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
        startForegroundService()
        startUpdatingStats()
    }

    private fun createOverlayView() {
        // Since we don't have XML layout, we'll create view programmatically or inflate if we had one.
        // For simplicity in this environment, let's create a simple TextView container.
        // Wait, creating purely programmatic view with Compose in Service is harder without XML.
        // I will use a simple specialized View or FrameLayout.
        // Actually, simpler to just inflate a layout if I create one, or build view hierarchy code.
        // I'll build a simple TextView hierarchy in code.
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        overlayView = TextView(this).apply {
            text = "DeviceInsight\nCPU: --%"
            textSize = 14f
            setTextColor(android.graphics.Color.GREEN)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            setPadding(16, 16, 16, 16)
        }
        
        // Make it draggable later? For now just static.
        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startUpdatingStats() {
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Mock cpu for overlay or read real if repo injected (Service injection is possible but overhead here)
                // For this demo, just random or simple uptime
                val uptime = android.os.SystemClock.elapsedRealtime() / 1000
                (overlayView as? TextView)?.text = "DeviceInsight\nUp: ${uptime}s"
                delay(1000)
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DeviceInsight Overlay")
            .setContentText("Performance monitor is running")
            .setSmallIcon(R.mipmap.ic_launcher) // Use default icon
            .build()
        
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
