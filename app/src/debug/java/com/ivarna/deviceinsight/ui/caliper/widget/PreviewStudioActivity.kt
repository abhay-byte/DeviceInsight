package com.ivarna.deviceinsight.ui.caliper.widget

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.components.EndOfSheet
import com.ivarna.deviceinsight.ui.caliper.components.HardKey
import com.ivarna.deviceinsight.ui.caliper.components.HardKeyVariant
import com.ivarna.deviceinsight.ui.caliper.components.Masthead
import com.ivarna.deviceinsight.ui.caliper.components.PanelCard
import com.ivarna.deviceinsight.ui.caliper.components.ScreenHeader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Debug-only capture bench (DI-WF-001 §6/F7): runs BenchPreviewGenerator against the real
// Glance pipeline on-device; PNGs land in files/previews for pull into drawable-nodpi/.
class PreviewStudioActivity : ComponentActivity() {

    private val status = mutableStateOf("IDLE — pipeline ready")
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CaliperTheme(medium = Medium.PAPER) {
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    Masthead()
                    ScreenHeader(
                        "Preview Studio",
                        "real Glance→RemoteViews pipeline · 480dpi · fontScale 1.0"
                    )
                    PanelCard(title = "GENERATE") {
                        HardKey(
                            "CAPTURE ALL (5 WIDGETS × 3 MEDIA)",
                            variant = HardKeyVariant.PRIMARY,
                            enabled = !running,
                            onClick = { captureAll() }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(status.value, style = Caliper.type.dataS, color = Caliper.colors.ink60, maxLines = 2)
                    }
                    Spacer(Modifier.height(16.dp))
                    EndOfSheet()
                }
            }
        }
        // adb-driven capture: am start ... --ez autostart true (no UI tap needed in CI loop)
        if (intent?.getBooleanExtra(EXTRA_AUTOSTART, false) == true) captureAll()
    }

    private fun captureAll() {
        if (running) return
        running = true
        status.value = "CAPTURING…"
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val outDir = File(filesDir, "previews")
                val files = BenchPreviewGenerator.generateAll(applicationContext, outDir)
                status.value = "DONE — ${files.size}/${BenchPreviewGenerator.SHOT_MATRIX.size} PNGs → ${outDir.path}"
                Log.i(TAG, "capture complete: ${files.map { it.name }}")
            } catch (t: Throwable) {
                Log.e(TAG, "capture failed", t)
                status.value = "FAILED — ${t.message}"
            } finally {
                running = false
            }
        }
    }

    private companion object {
        const val TAG = "PreviewStudio"
        const val EXTRA_AUTOSTART = "autostart"
    }
}
