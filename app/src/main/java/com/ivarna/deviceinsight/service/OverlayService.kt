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
import com.ivarna.deviceinsight.data.repository.DashboardRepositoryImpl
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var job: Job? = null
    private lateinit var dashboardRepository: DashboardRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dashboardRepository = DashboardRepositoryImpl(this)
        createOverlayView()
        startForegroundService()
        startUpdatingStats()
    }

    private var isOverlayPinned = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var contentTextView: TextView
    private lateinit var pinButton: android.widget.ImageButton
    
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
 
        // Create a container layout for the overlay
        val container = android.widget.FrameLayout(this).apply {
            setBackgroundResource(R.drawable.rounded_widget_background)
            setPadding(16, 16, 16, 16)
        }

        // Main content TextView
        val contentTextView = TextView(this).apply {
            text = "DeviceInsight\nCPU: --%"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
        }

        // Pin button - small icon in top-right corner
        val pinButton = android.widget.ImageButton(this).apply {
            setImageResource(com.ivarna.deviceinsight.R.drawable.keep_24) // Using custom pin icon
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            // Make it small and position in top-right
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.TOP
                setMargins(0, 0, 8, 8) // Small margins
            }
            this.layoutParams = layoutParams
            
            setOnClickListener {
                isOverlayPinned = !isOverlayPinned
                android.util.Log.d("OverlayService", "Pin state changed to: ${isOverlayPinned}")
                updatePinButtonAppearance()
            }
        }

        container.addView(contentTextView)
        container.addView(pinButton)

        overlayView = container
        this.contentTextView = contentTextView
        this.pinButton = pinButton
        
        // Make the overlay draggable by touching the content area
        contentTextView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (!isOverlayPinned) {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    } else {
                        false
                    }
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (!isOverlayPinned) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
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
            dashboardRepository.getDashboardMetrics().collect { metrics ->
                val cpuUsage = (metrics.cpuUsage * 100).toInt()
                val batteryLevel = metrics.batteryLevel
                val ramUsedMb = metrics.ramUsedBytes / (1024 * 1024)
                val ramTotalMb = metrics.ramTotalBytes / (1024 * 1024)
                val swapUsedMb = metrics.swapUsedBytes / (1024 * 1024)
                val swapTotalMb = metrics.swapTotalBytes / (1024 * 1024)
                
                // Removed text indication when pinned as requested
                android.util.Log.d("OverlayService", "Updating overlay text. Pin state: ${isOverlayPinned}")
                contentTextView.text = "DeviceInsight\nCPU: ${cpuUsage}%\nBattery: ${batteryLevel}%\nRAM: ${ramUsedMb}/${ramTotalMb} MB\nSwap: ${swapUsedMb}/${swapTotalMb} MB"
            }
        }
    }


    private fun updatePinButtonAppearance() {
        // Switch between pin and unpin icons
        if (isOverlayPinned) {
            pinButton.setImageResource(com.ivarna.deviceinsight.R.drawable.keep_24)
        } else {
            pinButton.setImageResource(com.ivarna.deviceinsight.R.drawable.keep_off_24)
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
