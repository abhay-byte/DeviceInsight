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
import android.widget.ProgressBar
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.repository.DashboardRepositoryImpl
import com.ivarna.deviceinsight.domain.model.CpuDataPoint
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var job: Job? = null
    private lateinit var dashboardRepository: DashboardRepository
    
    // Default values for parameters
    private var showCpu: Boolean = true
    private var showBattery: Boolean = true
    private var showRam: Boolean = true
    private var showSwap: Boolean = true
    private var showCpuTemp: Boolean = true
    private var showBatteryTemp: Boolean = true
    private var showCpuGraph: Boolean = true
    private var showPower: Boolean = true
    private var showCpuFreq: Boolean = true
    private var showNetwork: Boolean = true
    private var scaleFactor: Float = 1.0f
    private var metricOrder: List<String> = listOf("cpu", "power", "battery", "ram", "swap", "cpuTemp", "batteryTemp", "cpuGraph", "cpuFreq", "network")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dashboardRepository = DashboardRepositoryImpl(this, CpuUtilizationUtils(this))
        
        createOverlayView()
        startForegroundService()
        startUpdatingStats()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Read parameters from intent
        intent?.let {
            showCpu = it.getBooleanExtra("showCpu", true)
            showBattery = it.getBooleanExtra("showBattery", true)
            showRam = it.getBooleanExtra("showRam", true)
            showSwap = it.getBooleanExtra("showSwap", true)
            showCpuTemp = it.getBooleanExtra("showCpuTemp", true)
            showBatteryTemp = it.getBooleanExtra("showBatteryTemp", true)
            showCpuGraph = it.getBooleanExtra("showCpuGraph", true)
            showPower = it.getBooleanExtra("showPower", true)
            showCpuFreq = it.getBooleanExtra("showCpuFreq", true)
            showNetwork = it.getBooleanExtra("showNetwork", true)
            scaleFactor = it.getFloatExtra("scaleFactor", 1.0f)
            metricOrder = it.getStringExtra("metricOrder")?.split(",") 
                ?: listOf("cpu", "power", "battery", "ram", "swap", "cpuTemp", "batteryTemp", "cpuGraph", "cpuFreq", "network")
            
            // Update the overlay with new parameters
            updateOverlayWithNewParameters()
        }
        
        return START_STICKY
    }

    private var isCollapsed = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var collapseButton: android.widget.ImageButton
    private lateinit var expandButton: android.widget.ImageButton
    private lateinit var contentLayout: LinearLayout
    private lateinit var cpuProgressBar: ProgressBar
    private lateinit var batteryProgressBar: ProgressBar
    private lateinit var ramProgressBar: ProgressBar
    private lateinit var swapProgressBar: ProgressBar
    
    private fun createOverlayView() {
        // Since we don't have XML layout, we'll create view programmatically or inflate if we had one.
        // For simplicity in this environment, let's create a simple TextView container.
        // Wait, creating purely programmatic view with Compose in Service is harder without XML.
        // I will use a simple specialized View or FrameLayout.
        // Actually, simpler to just inflate a layout if I create one, or build view hierarchy code.
        // I'll build a simple TextView hierarchy in code.
         
        // Base size for the overlay (in pixels)
        val baseWidth = WindowManager.LayoutParams.WRAP_CONTENT
        val baseHeight = WindowManager.LayoutParams.WRAP_CONTENT
        
        android.util.Log.d("OverlayService", "Creating overlay with WRAP_CONTENT dimensions, scaleFactor=$scaleFactor")
        
        val params = WindowManager.LayoutParams(
            baseWidth,
            baseHeight,
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

        // Create a LinearLayout for content with progress bars
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40) // Leave space for pin button
            }
        }
        
        // Create progress bars - they will size dynamically based on content width
        val progressBarWidth = LinearLayout.LayoutParams.MATCH_PARENT
        
        cpuProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                progressBarWidth,
                (8 * scaleFactor).toInt()
            ).apply {
                setMargins(0, (4 * scaleFactor).toInt(), 0, (4 * scaleFactor).toInt())
            }
            max = 100
            progress = 0
            scaleY = 1.5f * scaleFactor
        }
        
        batteryProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                progressBarWidth,
                (8 * scaleFactor).toInt()
            ).apply {
                setMargins(0, (4 * scaleFactor).toInt(), 0, (4 * scaleFactor).toInt())
            }
            max = 100
            progress = 0
            scaleY = 1.5f * scaleFactor
        }
        
        ramProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                progressBarWidth,
                (8 * scaleFactor).toInt()
            ).apply {
                setMargins(0, (4 * scaleFactor).toInt(), 0, (4 * scaleFactor).toInt())
            }
            max = 100
            progress = 0
            scaleY = 1.5f * scaleFactor
        }
        
        swapProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                progressBarWidth,
                (8 * scaleFactor).toInt()
            ).apply {
                setMargins(0, (4 * scaleFactor).toInt(), 0, (4 * scaleFactor).toInt())
            }
            max = 100
            progress = 0
            scaleY = 1.5f * scaleFactor
        }
        
        // Collapse button - minimize overlay
        val collapseButton = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_up_float)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.TOP
                setMargins(0, 0, 16, 16) // Better spacing
            }
            this.layoutParams = layoutParams
            
            setOnClickListener {
                collapseOverlay(params)
            }
        }
        
        // Expand button - shown when collapsed
        val expandButton = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_down_float)
            setBackgroundColor(0x80000000.toInt()) // Semi-transparent background
            setPadding(24, 24, 24, 24)
            visibility = android.view.View.GONE
            
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            this.layoutParams = layoutParams
            
            setOnClickListener {
                expandOverlay(params)
            }
        }

        container.addView(contentLayout)
        container.addView(collapseButton)
        container.addView(expandButton)

        overlayView = container
        this.contentLayout = contentLayout
        this.collapseButton = collapseButton
        this.expandButton = expandButton
        this.cpuProgressBar = cpuProgressBar
        this.batteryProgressBar = batteryProgressBar
        this.ramProgressBar = ramProgressBar
        this.swapProgressBar = swapProgressBar
        
        // Make the overlay draggable by touching the content area
        contentLayout.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if (!isCollapsed) {
                        snapToEdge(params)
                    }
                    false
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
                val batteryTemperature = metrics.temperature
                val cpuTemperature = metrics.cpuTemperature
                val powerConsumption = metrics.powerConsumption
                val cpuCoreFrequencies = metrics.cpuCoreFrequencies
                val cpuHistory = metrics.cpuHistory
                val netUpload = metrics.networkUploadSpeed
                val netDownload = metrics.networkDownloadSpeed
                
                // Debug log to check temperature values
                android.util.Log.d("OverlayService", "CPU Temp: ${cpuTemperature}°C, Battery Temp: ${batteryTemperature}°C, Power: ${powerConsumption}W")
                android.util.Log.d("OverlayService", "Flags - showCpu: $showCpu, showBattery: $showBattery, showRam: $showRam, showSwap: $showSwap, showCpuGraph: $showCpuGraph")
                
                // Clear and rebuild layout
                contentLayout.removeAllViews()
                
                // Add title
                addTextView("DeviceInsights", true)
                addSeparator()
                
                // Display metrics in custom order
                metricOrder.forEach { metricId ->
                    when (metricId) {
                        "cpu" -> if (showCpu) {
                            addTextView("CPU: ${cpuUsage}%")
                            cpuProgressBar.progress = cpuUsage
                            contentLayout.addView(cpuProgressBar)
                            addSeparator()
                        }
                        "cpuGraph" -> if (showCpu && showCpuGraph && cpuHistory.isNotEmpty()) {
                            addTextView(createCpuUsageGraph(cpuHistory))
                            addSeparator()
                        }
                        "cpuFreq" -> if (showCpu && showCpuFreq && cpuCoreFrequencies.isNotEmpty()) {
                            val freqBuilder = StringBuilder("Freq:")
                            cpuCoreFrequencies.forEachIndexed { index, freq ->
                                if (index % 2 == 0) freqBuilder.append("\n")
                                else freqBuilder.append("  ")
                                freqBuilder.append("Core $index:$freq")
                            }
                            addTextView(freqBuilder.toString())
                            addSeparator()
                        }
                        "cpuTemp" -> if (showCpuTemp) {
                            if (cpuTemperature > 0) {
                                addTextView("CPU Temp: ${cpuTemperature}°C")
                            } else {
                                addTextView("CPU Temp: N/A")
                            }
                            addSeparator()
                        }
                        "power" -> if (showPower) {
                            val powerText = if (powerConsumption > 0) "+%.2f".format(powerConsumption)
                                           else "%.2f".format(powerConsumption)
                            addTextView("Power: ${powerText}W")
                            addSeparator()
                        }
                        "battery" -> if (showBattery) {
                            addTextView("Battery: ${batteryLevel}%")
                            batteryProgressBar.progress = batteryLevel
                            contentLayout.addView(batteryProgressBar)
                            addSeparator()
                        }
                        "batteryTemp" -> if (showBatteryTemp) {
                            if (batteryTemperature > 0) {
                                addTextView("Battery Temp: ${batteryTemperature}°C")
                            } else {
                                addTextView("Battery Temp: N/A")
                            }
                            addSeparator()
                        }
                        "ram" -> if (showRam) {
                            val ramPercentage = if (ramTotalMb > 0) ((ramUsedMb.toFloat() / ramTotalMb) * 100).toInt() else 0
                            addTextView("RAM: ${ramUsedMb}/${ramTotalMb} MB (${ramPercentage}%)")
                            ramProgressBar.progress = ramPercentage
                            contentLayout.addView(ramProgressBar)
                            addSeparator()
                        }
                        "swap" -> if (showSwap) {
                            val swapPercentage = if (swapTotalMb > 0) ((swapUsedMb.toFloat() / swapTotalMb) * 100).toInt() else 0
                            addTextView("Swap: ${swapUsedMb}/${swapTotalMb} MB (${swapPercentage}%)")
                            swapProgressBar.progress = swapPercentage
                            contentLayout.addView(swapProgressBar)
                            addSeparator()
                        }
                        "network" -> if (showNetwork) {
                            addTextView("↑ $netUpload   ↓ $netDownload")
                            addSeparator()
                        }
                    }
                }
            }
        }
    }
    
    private fun addTextView(text: String, isBold: Boolean = false) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 14f * scaleFactor
            setTextColor(android.graphics.Color.WHITE)
            if (isBold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, (2 * scaleFactor).toInt(), 0, (2 * scaleFactor).toInt())
            }
        }
        contentLayout.addView(textView)
    }
    
    private fun addSeparator() {
        val separator = android.view.View(this).apply {
            setBackgroundColor(0x40FFFFFF.toInt()) // Semi-transparent white
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (2 * scaleFactor).toInt().coerceAtLeast(1)
            ).apply {
                setMargins(0, (8 * scaleFactor).toInt(), 0, (8 * scaleFactor).toInt())
            }
        }
        contentLayout.addView(separator)
    }

    private fun collapseOverlay(params: WindowManager.LayoutParams) {
        isCollapsed = true
        
        // Fade out animation for content and collapse button
        contentLayout.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                contentLayout.visibility = android.view.View.GONE
                collapseButton.visibility = android.view.View.GONE
                
                // Show expand button with fade in
                expandButton.visibility = android.view.View.VISIBLE
                expandButton.alpha = 0f
                expandButton.animate().alpha(1f).setDuration(200).start()
                
                // Snap to nearest edge
                snapToEdge(params)
            }
            .start()
        
        collapseButton.animate().alpha(0f).setDuration(200).start()
    }
    
    private fun expandOverlay(params: WindowManager.LayoutParams) {
        isCollapsed = false
        
        // Fade out expand button
        expandButton.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                expandButton.visibility = android.view.View.GONE
                
                // Show content and collapse button with fade in
                contentLayout.visibility = android.view.View.VISIBLE
                collapseButton.visibility = android.view.View.VISIBLE
                
                contentLayout.alpha = 0f
                collapseButton.alpha = 0f
                
                contentLayout.animate().alpha(1f).setDuration(200).start()
                collapseButton.animate().alpha(1f).setDuration(200).start()
            }
            .start()
    }
    
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val currentX = params.x
        val currentY = params.y
        
        // Determine which edge is closest
        val distanceToLeft = currentX
        val distanceToRight = screenWidth - currentX - overlayView.width
        val distanceToTop = currentY
        val distanceToBottom = screenHeight - currentY - overlayView.height
        
        val minDistance = minOf(distanceToLeft, distanceToRight, distanceToTop, distanceToBottom)
        
        // Snap to the closest edge with animation
        val targetX: Int
        val targetY: Int
        
        when (minDistance) {
            distanceToLeft -> {
                targetX = -overlayView.width / 2 // Snap to left, half off-screen
                targetY = currentY
            }
            distanceToRight -> {
                targetX = screenWidth - overlayView.width / 2 // Snap to right, half off-screen
                targetY = currentY
            }
            distanceToTop -> {
                targetX = currentX
                targetY = -overlayView.height / 2 // Snap to top, half off-screen
            }
            else -> {
                targetX = currentX
                targetY = screenHeight - overlayView.height / 2 // Snap to bottom, half off-screen
            }
        }
        
        // Animate to target position
        android.animation.ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 300
            addUpdateListener { animator ->
                params.x = animator.animatedValue as Int
                windowManager.updateViewLayout(overlayView, params)
            }
        }.start()
        
        android.animation.ValueAnimator.ofInt(currentY, targetY).apply {
            duration = 300
            addUpdateListener { animator ->
                params.y = animator.animatedValue as Int
                windowManager.updateViewLayout(overlayView, params)
            }
        }.start()
    }
    
    private fun createCpuUsageGraph(cpuHistory: List<CpuDataPoint>): String {
        if (cpuHistory.isEmpty()) return ""
        
        // Simple CPU trend indicator
        val recentHistory = cpuHistory.takeLast(Math.min(10, cpuHistory.size))
        
        if (recentHistory.size < 2) {
            return "\nCPU Trend: N/A"
        }
        
        // Calculate trend
        val firstUsage = recentHistory.first().utilization
        val lastUsage = recentHistory.last().utilization
        val trend = lastUsage - firstUsage
        
        // Determine trend direction
        val trendIndicator = when {
            trend > 5 -> "↑ Increasing"
            trend < -5 -> "↓ Decreasing"
            else -> "→ Stable"
        }
        
        // Show current and trend
        return "\nCPU Trend: ${lastUsage.toInt()}% $trendIndicator"
    }
    
    private fun updateOverlayWithNewParameters() {
        // Update the overlay layout with new scale factor - use WRAP_CONTENT
        val params = overlayView.layoutParams as WindowManager.LayoutParams
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        
        android.util.Log.d("OverlayService", "Updated overlay to WRAP_CONTENT dimensions")
        windowManager.updateViewLayout(overlayView, params)
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
            .setContentTitle("DeviceInsights Overlay")
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
