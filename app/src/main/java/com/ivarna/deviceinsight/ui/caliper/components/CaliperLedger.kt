package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.ivarna.deviceinsight.ui.caliper.*
import kotlin.math.roundToInt

// ─────────────────────────── models ───────────────────────────

enum class ProcState { FOREGROUND, CACHED, SERVICE, BACKGROUND }

data class ProcRow(
    val index: Int, val pkg: String, val cpu: Float, val rssBytes: Long,
    val pid: Int, val uptime: String, val state: ProcState,
    val isSelf: Boolean = false, val isSystem: Boolean = false, val threads: Int = 1
)

data class LedgerSection(val title: String, val rows: List<ProcRow>)

// ─────────────────────────── LedgerTable (§5.9) ───────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerTable(
    sections: List<LedgerSection>,
    modifier: Modifier = Modifier,
    onRowTap: (ProcRow) -> Unit
) {
    LazyColumn(modifier) {
        sections.forEach { section ->
            stickyHeader(key = "h_${section.title}") {
                Column(Modifier.fillMaxWidth().background(Caliper.colors.surface)) {
                    Text("── ${section.title} ──", style = Caliper.type.meta,
                        color = Caliper.colors.ink60, modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
                    DoubleRule()
                }
            }
            items(section.rows, key = { it.index }) { row ->
                LedgerRow(row) { onRowTap(row) }
            }
        }
    }
}

@Composable
private fun LedgerRow(row: ProcRow, onClick: () -> Unit) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth()
            .background(c.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "${Fmt.index(row.index)} ${row.pkg}, " +
                    "cpu ${Fmt.pct(row.cpu, 1)}, ${Fmt.bytes(row.rssBytes)}, ${row.state.name.lowercase()}"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.index(row.index), style = Caliper.type.dataS, color = c.ink40, modifier = Modifier.width(44.dp))
            Text(row.pkg, style = Caliper.type.dataM, color = c.ink,
                maxLines = 1, modifier = Modifier.weight(1f))
            // B5: unknown cpu → — per §4.9, rss 0 → —
            Text(if (row.cpu == 0f) "—" else String.format(java.util.Locale.US, "%.1f%%", row.cpu), style = Caliper.type.dataM,
                color = if (row.cpu > 25f) c.fault else c.ink, modifier = Modifier.width(64.dp))
            Text(if (row.rssBytes == 0L) "—" else Fmt.bytes(row.rssBytes), style = Caliper.type.dataM, color = c.ink60)
            if (row.isSelf) Spacer(Modifier.width(8.dp))
            if (row.isSelf) StampBadge("SELF", color = c.accent, rotation = -3f, animateIn = false)
        }
            Text(
            "pid ${if (row.pid == 0) "—" else row.pid.toString()} · ${row.uptime} · ${if (row.state == ProcState.FOREGROUND) "●" else "○"} ${row.state.name.lowercase()}",
            style = Caliper.type.meta, color = c.ink40, modifier = Modifier.padding(start = 44.dp)
        )
    }
}

// ─────────────────────────── Dossier (§5.10 — clipped sheet) ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessDossier(
    row: ProcRow?,
    rootAvailable: Boolean,
    onDismiss: () -> Unit,
    onForceStop: (ProcRow) -> Unit,
    onTerminate: (ProcRow) -> Unit
) {
    if (row == null) return
    val c = Caliper.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = androidx.compose.ui.graphics.RectangleShape,          // 0dp radius — always
        containerColor = c.panel,
        scrimColor = c.ink.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        DossierBody(row, rootAvailable, onForceStop) { r -> onTerminate(r); onDismiss() }
    }
}

/** Dossier content shared by the bottom sheet (compact) and the two-pane pane (wide). */
@Composable
fun DossierBody(
    row: ProcRow,
    rootAvailable: Boolean,
    onForceStop: (ProcRow) -> Unit,
    onTerminate: (ProcRow) -> Unit
) {
    val c = Caliper.colors
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
        // perforated tear-off edge
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawLine(c.ink40, Offset.Zero, Offset(size.width, size.height / 2), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())))
        }
        Text("DOSSIER · ${Fmt.index(row.index)}", style = Caliper.type.meta, color = c.ink40)
        Text(row.pkg, style = Caliper.type.dataM, color = c.ink)
        Text("pid ${row.pid} · up ${row.uptime}", style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("CPU", Fmt.pct(row.cpu, 1), listOf(row.cpu / 3, row.cpu / 2, row.cpu), Channels.CPU)
            MiniStat("MEM", Fmt.bytes(row.rssBytes), listOf(0.6f, 0.7f, 0.65f), Channels.MEMORY)
        }
        Spacer(Modifier.height(12.dp))
        SpecRow("uid", "10247")
        SpecRow("oom adj", "0")
        SpecRow("seccomp", "enforced")
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HardKey("FORCE STOP", variant = HardKeyVariant.DESTRUCTIVE,
                modifier = Modifier.weight(1f),
                onClick = { onForceStop(row) })   // gate behind SafetyLatch in production
        }
        Spacer(Modifier.height(12.dp))
        SafetyLatch(
            prompt = "ARM — TERMINATE PROCESS ${Fmt.index(row.index)}?",
            killLabel = "TERMINATE ⏻",
            enabled = rootAvailable,
            onArmedKill = { onTerminate(row) }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, spark: List<Float>, channel: Channel) {
    Column(Modifier.width(96.dp)) {
        Text(label.uppercase(), style = Caliper.type.meta, color = Caliper.colors.ink40)
        Text(value, style = Caliper.type.dataS, color = Caliper.colors.ink)
        Spacer(Modifier.height(4.dp))
        Sparkline(spark, channel, Modifier.fillMaxWidth().height(16.dp))
    }
}

// ─────────────────────────── SafetyLatch (§5.11) ───────────────────────────

@Composable
fun SafetyLatch(
    prompt: String,
    modifier: Modifier = Modifier,
    killLabel: String = "KILL",
    enabled: Boolean = true,
    onArmedKill: () -> Unit,
    onAbort: () -> Unit = {}
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val tm = rememberTextMeasurer()
    var fraction by remember { mutableStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    val knob by animateFloatAsState(fraction, spring(dampingRatio = 1f, stiffness = 700f), label = "latch")
    val metaStyle = TextStyle(fontFamily = PlexMonoFamily, fontSize = 10.sp, color = c.ink40)

    Column(modifier.fillMaxWidth().border(1.dp, c.fault)) {
        Text(prompt.uppercase(), style = Caliper.type.meta, color = c.fault,
            modifier = Modifier.padding(12.dp))
        Canvas(
            Modifier.fillMaxWidth().height(56.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (!armed) {
                            fraction = (fraction + dragAmount / size.width).coerceIn(0f, 1f)
                            if (fraction >= 0.92f) { armed = true; fraction = 1f; haptics.arm() }
                        }
                    }
                }
        ) {
            val mid = size.height / 2
            drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
            // armed region fills with cross-hatch
            hatch(Rect(0f, mid - 8.dp.toPx(), size.width * knob, mid + 8.dp.toPx()),
                HatchPattern.CROSS, c.fault.copy(alpha = 0.5f))
            val ks = 28.dp.toPx()
            val kx = (size.width - ks) * knob
            drawRect(if (armed) c.fault else c.ink, topLeft = Offset(kx, (mid - ks / 2)), size = Size(ks, ks))
            drawText(tm, "SAFE", topLeft = Offset(4f, 4f), style = metaStyle)
            drawText(tm, "ARM", topLeft = Offset(size.width - 24.dp.toPx(), 4f), style = metaStyle)
        }
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HardKey("ABORT", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.weight(1f),
                onClick = { fraction = 0f; armed = false; onAbort() })
            HardKey(killLabel,
                variant = if (armed) HardKeyVariant.DESTRUCTIVE else HardKeyVariant.DISABLED,
                modifier = Modifier.weight(1f),
                onClick = { if (armed) { onArmedKill(); fraction = 0f; armed = false } })
        }
    }
}

// ─────────────────────────── Processes screen (assembly) ───────────────────────────

// P2-4: no embedded Masthead — global scaffold owns the masthead.
@Composable
fun ProcessesScreen(
    rows: List<ProcRow>,
    rootAvailable: Boolean,
    onForceStop: (ProcRow) -> Unit,
    onTerminate: (ProcRow) -> Unit
) {
    var filter by remember { mutableStateOf("APPS") }
    var query by remember { mutableStateOf("") }
    var sortDesc by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<ProcRow?>(null) }

    val filtered = remember(rows, filter, query, sortDesc) {
        rows.asSequence()
            .filter { filter == "ALL" || (filter == "APPS" && !it.isSystem) || (filter == "SYSTEM" && it.isSystem) }
            .filter { query.isEmpty() || it.pkg.contains(query, ignoreCase = true) }
            .sortedBy { if (sortDesc) -it.cpu else it.cpu }
            .toList()
    }
    val sections = buildList {
        filtered.filter { !it.isSystem && !it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("USER APPS", it)) }
        filtered.filter { it.isSystem && !it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("SYSTEM", it)) }
        filtered.filter { it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("SELF", it)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Caliper.colors.surface)) {
        // Two-pane ledger+dossier (§5.2/§7 S-14). App left rail already consumes
        // 232dp on wide windows, so fire at ≥560dp of CONTENT width: typical
        // tablets (≥800dp window) get two-pane; phones stay single-pane + sheet.
        val twoPane = maxWidth >= 560.dp
        val paneWidth = (maxWidth * 0.45f).coerceIn(280.dp, 352.dp)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Processes",
                "${rows.size} listed · ${rows.sumOf { it.threads }} threads")
            SegKey(listOf("ALL", "APPS", "SYSTEM"), filter, { filter = it },
                Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("FIND:", style = Caliper.type.meta, color = Caliper.colors.ink60)
                Spacer(Modifier.width(8.dp))
                BaselineField(query, { query = it }, Modifier.weight(1f))
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("IDX", style = Caliper.type.meta, color = Caliper.colors.ink40, modifier = Modifier.width(44.dp))
                Text("PACKAGE", style = Caliper.type.meta, color = Caliper.colors.ink40, modifier = Modifier.weight(1f))
                Text("SORT CPU ${if (sortDesc) "▼" else "▲"}", style = Caliper.type.meta, color = Caliper.colors.ink,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { sortDesc = !sortDesc })
            }
            if (twoPane) {
                // §5.2/§7 S-14: two-pane ledger + dossier on wide content.
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    LedgerTable(sections, Modifier.weight(1f)) { selected = it }
                    Column(Modifier.width(paneWidth).fillMaxHeight().background(Caliper.colors.panel)
                        .border(1.dp, Caliper.colors.hairline)) {
                        val selectedRow = selected
                        if (selectedRow != null) {
                            DossierBody(
                                row = selectedRow,
                                rootAvailable = rootAvailable,
                                onForceStop = { selected = null; onForceStop(it) },
                                onTerminate = { selected = null; onTerminate(it) }
                            )
                        } else {
                            EmptyState(
                                title = "SELECT A ROW",
                                message = "tap a ledger entry to open its dossier"
                            )
                        }
                    }
                }
            } else {
                LedgerTable(sections, Modifier.weight(1f)) { selected = it }
            }
            EndOfSheet()
        }
        // Compact: dossier opens as the perforated bottom sheet (§5.10).
        if (!twoPane && selected != null) {
            ProcessDossier(selected, rootAvailable,
                onDismiss = { selected = null },
                onForceStop = onForceStop, onTerminate = onTerminate)
        }
    }
}