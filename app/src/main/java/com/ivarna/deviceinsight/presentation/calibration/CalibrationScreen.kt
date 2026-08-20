package com.ivarna.deviceinsight.presentation.calibration

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*

/**
 * S-00 Calibration onboarding — four numbered sheets ended by the certificate.
 * Skipped steps surface later as MarginNotes, never blocking dialogs.
 */
@Composable
fun CalibrationScreen(
    initialMedium: Medium?,
    onMedium: (Medium) -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val haptics = rememberCaliperHaptics()
    val chosen by remember { mutableStateOf(initialMedium ?: Medium.PAPER) }

    Column(
        Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())
    ) {
        when (step) {
            0 -> CalibSheet(
                num = "01 · USAGE ACCESS",
                title = "Calibration.",
                body = "Two minutes to grant your channels the access they need. Every step is optional — reduced accuracy is marked ≈.",
                actionLabel = "GRANT",
                onAction = { step = 1 },
                skipLabel = "SKIP (≈ estimates)",
                onSkip = { step = 1 }
            )
            1 -> CalibSheet(
                num = "02 · OVERLAY",
                title = "Calibration.",
                body = "The HUD module floats over other apps. Grant display-over-apps when the system asks.",
                actionLabel = "NEXT → OVERLAY",
                onAction = { step = 2 },
                skipLabel = "SKIP",
                onSkip = { step = 2 }
            )
            2 -> CalibSheet(
                num = "03 · ROOT PROBE",
                title = "Calibration.",
                body = "Optional. Probe runs and stamps the result. No root is fine — locked channels hatch and stay honest.",
                actionLabel = "NEXT → MEDIA",
                onAction = { step = 3 },
                skipLabel = "SKIP",
                onSkip = { step = 3 }
            )
            3 -> CalibSheet(
                num = "04 · MEDIA",
                title = "Calibration.",
                body = "Pick your drafting paper. Paper, Carbon or Blueprint — same language, different stock.",
                actionLabel = "INSPECT CERTIFICATE",
                onAction = { step = 4 },
                onMedium = {
                    onMedium(chosen)
                    haptics.confirm()
                }
            )
            else -> CertificateSheet(
                deviceName = "DEVICEINSIGHT",
                onFinish = onFinish
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("01", "02", "03", "04").forEachIndexed { i, s ->
                Text(
                    if (i < step) "$s · ✓" else s,
                    style = Caliper.type.meta,
                    color = if (i <= step) Caliper.colors.ink else Caliper.colors.ink40,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CalibSheet(
    num: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    skipLabel: String? = null,
    onSkip: () -> Unit = {},
    onMedium: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text(num, style = Caliper.type.meta, color = Caliper.colors.ink40)
        Spacer(Modifier.height(8.dp))
        Text(title, style = Caliper.type.display1, color = Caliper.colors.ink)
        Spacer(Modifier.height(16.dp))
        Text(body, style = Caliper.type.body, color = Caliper.colors.ink60)
        Spacer(Modifier.height(24.dp))
        onMedium?.invoke()
        HardKey(actionLabel, variant = HardKeyVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(), onClick = onAction)
        if (skipLabel != null) {
            Spacer(Modifier.height(8.dp))
            HardKey(skipLabel, variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(), onClick = onSkip)
        }
    }
}

@Composable
private fun CertificateSheet(deviceName: String, onFinish: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text("DOC № DI-0001 · REV 2.0", style = Caliper.type.meta, color = Caliper.colors.ink40)
        Spacer(Modifier.height(8.dp))
        Text("Calibration.", style = Caliper.type.display1, color = Caliper.colors.ink)
        Spacer(Modifier.height(12.dp))
        Text(deviceName, style = Caliper.type.dataM, color = Caliper.colors.ink)
        Text("date · ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}",
            style = Caliper.type.dataS, color = Caliper.colors.ink60)
        Spacer(Modifier.height(12.dp))
        StampBadge("CALIBRATED · DI-0001")
        Spacer(Modifier.height(20.dp))
        HardKey("INSPECT THE INSTRUMENT", variant = HardKeyVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(), onClick = onFinish)
    }
}