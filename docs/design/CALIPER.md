# DeviceInsight — **CALIPER** Design Language

> *Save as `docs/design/CALIPER.md` · This document supersedes the "Elegant Glassmorphism" system in its entirety.*

```
        ┌──────────────────────────────────────────┐
        │                  ⌖                       │
        │            DeviceInsight                 │
        │                                          │
        │      CALIPER DESIGN LANGUAGE · REV A     │
        └──────────────────────────────────────────┘
```

| | |
|---|---|
| **Doc ID** | DI-DS-002 |
| **Status** | Adopted — mandatory for all surfaces (app, HUD, widgets, store) |
| **Supersedes** | "Elegant Glassmorphism" v1 (Tech-Noir, Cyberpunk Edge, Deep Ocean, Digital Matrix, Dracula's Castle) |
| **Applies to** | Jetpack Compose app · Glance widgets · SYSTEM_ALERT_WINDOW HUD · Play Store assets |
| **One-line thesis** | *The screen is a sheet of drafting paper. The phone is the instrument.* |

---

## §1 — Design Thesis

DeviceInsight **measures things**. So its interface should look like it was built by people who measure things for a living: test-bench engineers, draftsmen, instrument makers.

CALIPER is the visual language of **laboratory equipment and technical documentation** — a Braun multimeter, an HP oscilloscope's front panel, a numbered sheet in an engineering binder. It is deliberately *analog in spirit, digital in precision*:

- **Paper, not glass.** Flat surfaces, hairline rules, ink. No blur, no glow, no depth tricks.
- **Channels, not cards.** Every monitored metric is a numbered *channel* (CH-01 CPU, CH-02 Memory…) with its own trace color, hatch pattern, and label. The channel is the app's conceptual spine — it unifies the dashboard, charts, HUD, and widgets.
- **Documents, not screens.** Pages are numbered sheets (`№ 01 — OVERVIEW`) with a masthead, a revision mark, and an end-of-sheet mark when you scroll to the bottom.
- **Precision is the ornament.** Tabular figures, tick scales, leader-line annotations, peak-hold markers. The decoration *is* the data.
- **A serif with a pulse.** Big editorial page titles set in an italic serif give the app a hand-crafted, human voice against a disciplined mono grid.

The result should feel like a **one-of-a-kind instrument someone machined for you** — not a template, not a "dashboard app."

---

## §2 — Demolition List (what we are ripping out)

| Removed | Replaced by |
|---|---|
| `backdrop-filter` blur / Haze glassmorphism | Flat paper panels, 1dp hairline frames |
| Neon gradients, glows, OLED-only dark themes | Three media: **Paper**, **Carbon**, **Blueprint** |
| 16–28dp rounded cards | **0dp radius.** Sharp corners everywhere (LEDs are the only circles) |
| Elevation shadows | Hairlines, hatching, double rules |
| Floating rounded FAB | In-context **HardKeys** (this is a document, not a social app) |
| Pill chips & emoji headers | Mono caps labels, index numbers, stamps |
| Generic chart styling (rounded caps, soft grids) | **ScopeTrace**: square pens, engineering grid, crosshair, peak-hold |
| 5 vibe-based themes | 3 *drafting media* — same language, different paper |

One-time migration prompt for existing users:

```
┌────────────────────────────────────────┐
│  Your instrument has been recalibrated │
│  to the CALIPER standard.  [ INSPECT ] │
└────────────────────────────────────────┘
```

---

## §3 — Principles

1. **Data is the hero.** Numerals are the largest elements on any screen. Chrome whispers.
2. **Everything is a channel.** If it's measured, it has a CH number, a color, and a hatch. Always label it.
3. **The page is a sheet.** Masthead, sheet number, revision, end mark. Documents have dignity.
4. **Color is signal, never decoration.** Ink and paper carry the layout; color belongs to data and interactive states only.
5. **Motion obeys physics.** Needles settle with overshoot, counters roll like odometers, traces draw like pens. No fade-everything laziness.
6. **Say what you mean.** `NO SIGNAL`, `CHANNEL LOCKED`, `FAULT 0x2F`. Honest states, labeled everything, no mystery meat.

---

## §4 — Foundations

### §4.1 The Channel Registry

The single most important system in CALIPER. Every metric family is a channel:

| CH | Metric | Name (always shown) | Paper hex | Carbon hex | Hatch |
|----|--------|--------------------|-----------|------------|-------|
| 01 | CPU | `CH-01 · CPU` | `#E5482B` vermilion | `#FF6B4A` | solid |
| 02 | Memory | `CH-02 · MEMORY` | `#2E5BE0` ultramarine | `#6B8CFF` | diagonal ↗ |
| 03 | Network | `CH-03 · NETWORK` | `#0E9F6E` teal | `#2FD3B0` | cross-hatch ▨ |
| 04 | Power | `CH-04 · POWER` | `#F0A419` amber | `#FFB84D` | dots ⠿ |
| 05 | Storage | `CH-05 · STORAGE` | `#8757D6` violet | `#B08CFF` | vertical ▮ |
| 06 | GPU | `CH-06 · GPU` | `#D6409F` magenta | `#F06BB0` | horizontal ▬ |

**Rules**
- Channel color appears **only** on: the channel tick (3dp square), the trace/pen, the peak-hold marker, and the LED. Never on chrome, never as a background.
- Channel label is mandatory wherever the color appears. Color is never the sole encoder (CVD-safe; hatch is the redundant channel).
- **Thermal** is not a channel — it's a *ramp* (amber → vermilion → deep red) used only on gauges, always with a numeric readout.
- **Accent** (interactive) is a separate, non-channel color: **Signal Orange `#FF4D00`** (Paper) / `#FF5A1F` (Carbon). It marks *buttons, links, active nav, focus rings*. It is never used for data.

### §4.2 Color — three *media*, not five vibes

**DRAFTING (light, default)**

| Token | Hex | Use |
|---|---|---|
| `paper/0` | `#F4F1E8` | app background |
| `paper/1` | `#FBF9F3` | raised panels |
| `ink` | `#191713` | text, rules, active fills |
| `ink/60`, `ink/40` | alpha | secondary/tertiary text |
| `hairline` | ink @ 14% | 1dp rules |
| `accent` | `#FF4D00` | interactive only |
| `stampRed` | `#C8371F` | destructive, stamps, faults |

**CARBON (dark)** — warm charcoal, *not* OLED-black neon:

| Token | Hex |
|---|---|
| `carbon/0` | `#141310` background |
| `carbon/1` | `#1C1B17` panels |
| `ink` (inverted) | `#EDE7DA` |
| `hairline` | ink @ 18% |
| `accent` | `#FF5A1F` |
| channels | lightened variants from §4.1 |

**BLUEPRINT** — cyanotype sheet:

| Token | Hex |
|---|---|
| `bp/0` | `#0C2338` background |
| `bp/1` | `#12314E` panels |
| `line` | `#EAF2FF` ink |
| `hairline` | line @ 20% |
| `accent` | `#63C7FF` |
| traces | line color; channel identity via hatch + label |

**Usage ratio (enforced in review):** ≥ 88% paper/ink · ≤ 10% channel colors · ≤ 2% accent. If a screenshot looks "colorful," it's wrong.

### §4.3 Typography — two fonts, strict roles

| Role | Font | Style | Notes |
|---|---|---|---|
| **Voice** | *Instrument Serif* | 400, italic preferred | Page titles only. Max 8 words per screen. 40sp page / 28sp section. |
| **Everything else** | *IBM Plex Mono* | 300 / 400 / 500 | Labels, data, body, buttons, annotations. Tabular figures (`tnum`) always on. |

**Scale (sp, mono unless noted)**

| Token | Size | Weight | Tracking | Use |
|---|---|---|---|---|
| `display/1` | 40 serif italic | 400 | 0 | Page title ("Processor.") |
| `display/2` | 28 serif italic | 400 | 0 | Section title |
| `readout/xl` | 54 | 300 | 0 | Hero numerals |
| `readout/l` | 34 | 500 | 0 | Tile numerals |
| `data/m` | 22 | 400 | 0 | Table values |
| `data/s` | 16 | 400 | 0 | Inline values |
| `body` | 14 | 400 | 0 | Paragraphs (rare) |
| `label` | 12 | 400 | 0 | Field labels |
| `meta` | 11 | 500 | +0.08em | ALL-CAPS micro labels: `CH-01 · CPU` |

Rules: numerals always tabular. Italic mono for annotation notes. No font sizes outside the scale. Bundle both fonts (OFL) — never depend on downloadable fonts at runtime.

### §4.4 Grid, structure, radius

- **4dp base grid.** Phone margins 16dp, panel padding 16dp, panel gap 12dp.
- **Hairline:** 1dp, `hairline` token. **Double rule** (two hairlines 3dp apart) reserved for the masthead and sheet dividers — the editorial signature.
- **Radius: 0.** Hard corners on everything. Exceptions: LED dots (circle), stamps (2dp), avatars/process icons (2dp).
- **Focus ring:** 2dp accent, 2dp offset. Visible for keyboard/switch-access always.
- Background carries a **graph-paper grid**: 24dp fine grid at ink 3%, 120dp major grid at ink 5% (toggle: *Presentation → Grid*). On Carbon, ink-inverted.
- Scroll ends with `— END OF SHEET —` centered, mono 11sp, ink/40.

### §4.5 Hatching & pattern system

Procedural `PathEffect` patterns, 1dp weight, 4dp period:

| Pattern | Token | Use |
|---|---|---|
| Solid | `hatch/solid` | primary segment (apps bar, ON state) |
| Diagonal 45° | `hatch/diag` | secondary segment (CH-02) |
| Cross-hatch | `hatch/cross` | tertiary (system storage, danger zones) |
| Dots | `hatch/dots` | quaternary (cache) |
| Empty | `hatch/none` + `hairline` fill | free space, disabled |
| Vertical bars | `hatch/vert` | CH-05 legend, locked panels |

Hatching is a **first-class redundancy channel**: it disambiguates color for CVD users and prints beautifully. Storage maps, locked root panels, armed-destructive zones, and disabled keys all use it.

### §4.6 Iconography

Custom set, 24dp grid, 20dp live area, **1.5dp stroke, square caps, geometric**. No rounded-corner icon fonts, no filled icons, no emoji in UI.

Core glyphs: `crosshair` `core-grid` `memory-stack` `bolt` `wave` `thermo` `cylinder-hatched` `gpu-tri` `ledger` `phone-outline` `fader` `corner-brackets` `key` `radar-arc` `arrows-updown`. Naming: `di_ic_<name>`.

### §4.7 Motion — meters, not tweens

| Token | Value | Use |
|---|---|---|
| `ease/instrument` | `CubicBezier(0.2, 0, 0, 1)` | default |
| `spring/needle` | damping 0.82, stiffness 420 | gauges, bars — settles with slight overshoot |
| `spring/snap` | damping 1.0, stiffness 700 | toggles, keys |
| `t/fast` | 140ms | presses |
| `t/base` | 200ms | sheets, fades |
| `t/sweep` | 420ms | trace draw-in |

Signature motions:
- **Odometer roll** — readout digits roll vertically, 240ms, 24ms stagger per digit (right to left).
- **Pen draw** — charts appear by trimming the path left→right with a 3dp "pen" dot at the head; the pen persists as the live head of real-time traces.
- **Needle settle** — gauges/bars spring; bars carry a **peak-hold tick** that decays over 2s.
- **Stamp-in** — stamps enter at scale 1.12 → 1.0, -3° rotation, 180ms, with a haptic thunk.
- **LED pulse** — live indicator breathes on a 2s sine at 60–100% opacity.
- Screen changes: 160ms crossfade + 8dp rise. Nothing slides like a PowerPoint.

**Reduced motion:** sweeps→120ms fades, odometer→instant, needle→critically damped, LED static. Honored system-wide.

### §4.8 Haptics & sound

| Event | Pattern (VibrationEffect) |
|---|---|
| Toggle snap / nav key | tick — 8ms |
| Confirm | 15ms · 20ms gap · 15ms |
| Arm (safety latch) | 15/15/15ms, amplitude 40→80→120 |
| Fault / destructive | 40ms |
| Stamp-in | 12ms |

Sound (default **OFF**, *Settings → System → Key clicks*): 30ms 880Hz sine @ −20dB for toggles; 80ms filtered-noise "thunk" for stamps; 90ms 1200→400Hz sweep when a trace's pen engages. All ≤ −18 LUFS.

### §4.9 Data formatting (the grammar of numbers)

| Metric | Format | Example |
|---|---|---|
| Percent | integer + `%` | `38%` |
| Frequency | 2dp, auto GHz/MHz | `2.84 GHz` |
| Memory | auto-scale, 2dp | `6.81 GB` |
| Temperature | 1dp + `°C` | `46.2°C` |
| Rates | auto-scale + `/s` | `18.1 MB/s` |
| Duration | compact | `6h 12m` |
| Null / no data | em dash | `—` |
| Estimated value | `≈` prefix | `≈ -3.4 W` |
| Live indicator | `●` (LED) | `● LIVE` |
| Thousands | thin space | `1 447 MB` |

Long-press **any** metric label to open a **MarginNote** glossary definition (e.g., what ZRAM is). This is the app's teaching voice — quiet, on demand, never a tooltip barrage.

---

## §5 — Component Library

### §5.1 Masthead

```
┌──────────────────────────────────────────────┐
│ ⌖ DEVICEINSIGHT               14:32:07 UTC ● │
└──────────────────────────────────────────────┘
══════════════════════════════════════════════════ ← double rule
```

- 52dp tall. Crosshair mark + wordmark (mono caps 13sp) left; UTC clock right — colon blinks each second, ticking clock is the app's heartbeat. `●` = monitoring LED.
- **Degraded state:** red `DEGRADED` stamp replaces LED when a permission is lost; tap → MarginNote explaining the fault.
- **Root state:** adds `ROOT VERIFIED` mini-stamp left of the clock.

### §5.2 ModeRail (navigation)

```
┌───────────────────────────────────────────────────┐
│  [1] OVERVIEW   2 ACTIVITY   3 PROCESSES   4 DEVICE│
│                       ▲                            │
└───────────────────────────────────────────────────┘
```

Bottom bar, 64dp, hairline top edge. Five hardware-style **keys**: number + mono caps label. Active key = ink-filled square number + caret `▲` beneath + accent underline. LED dot per key lights when its channel reports a warning (e.g., thermal). Press: 120ms LED blink + tick haptic. On ≥600dp widths the rail becomes a left instrument rail with full labels; Processes/Device go two-pane.

### §5.3 PanelCard (the frame)

```
┌─ CH-01 · CPU ─────────────────────────── ● ─────┐
│  ...content...                                    │
└───────────────────────────────────────────────────┘
```

- `paper/1` fill, 1dp hairline border, 0dp radius, 16dp padding.
- Header row: channel tick (3dp square, channel color) + `meta` label + trailing status (`● LIVE`, value, or stamp). 1dp rule under header at 60% width (left-aligned — an intentional asymmetry).
- Interactive panels (dashboard tiles) show a `tap →` affordance in ink/40 on the header's right, and open the full channel page.

### §5.4 ReadoutTile

```
┌─ CH-02 · MEMORY ────────────────────── 57% ─────┐
│                                                   │
│   6.81 / 12 GB                                    │
│   ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░  zram 1.2 · swap 0.4       │
│                                                   │
└───────────────────────────────────────────────────┘
```

- Numerals `readout/l`, tabular, left-aligned baseline. Unit at `data/s`, ink/60, thin-space separated.
- Odometer roll on change; roll only the digits that changed.
- Bar (if present): 6dp, channel color, hairline track, peak-hold tick.

### §5.5 ScopeTrace (the chart — build custom, in Canvas)

```
┌ CH-01 · CPU LOAD · 60 s ────────────────────────┐
│100 ┤          ╭───╮                               │
│ 75 ┤      ╭───╯   ╰──╮         ╭──╮               │
│ 50 ┼──╮  ╭╯           ╰──╮  ╭──╯  ╰──             │
│ 25 ┤  ╰──╯                ╰──╯                    │
│  0 └──┬─────┬─────┬─────┬─────┬─────┬──           │
│    12:00  12:10  12:20  12:30  12:40             │
└────────────────────────────────────── pen ● 46.2%─┘
```

- **Grid:** 24dp minor (ink 4%), 120dp major (ink 8%). Y axis: 5 tick labels `meta`. X axis: 4–6 time labels.
- **Trace:** 2dp, square caps, channel color. Real-time traces end in a 3dp **pen dot**.
- **Crosshair:** touch/drag → vertical hairline + **LeaderNote** (value, time, min/max/avg of visible window pinned top-right). Hardware keyboards: arrow keys move the crosshair.
- **Dual trace** (network): down = channel color, up = ink @ 40%, each labeled at the trace's last value.
- Draw-in: pen sweep 420ms on first appearance. Empty window: flat line + `NO SIGNAL` `meta` label.
- A11y: `contentDescription` = "CPU load, 60 seconds. Min 12, max 71, average 38 percent."

### §5.6 CoreRail (per-core bars — mixing console)

```
C0 ▮▮▮▮▮▮▯▯▯▯▯▯▯▯▯▯  38%   1804 MHz
C1 ▮▮▮▮▮▮▮▮▮▮▮▯▯▯▯  72%   2188 MHz
C2 ▮▮▮▮▮▮▮▮▮▮▮▮▮▮▯  91% ⌃ 2416 MHz   ← ⌃ peak-hold, decays 2 s
```

- Row height 28dp, mono throughout. Bar 8dp tall with tick scale at 25/50/75/100.
- Long-press a core → MarginNote with that core's full frequency table (governor, min/max, idle states).

### §5.7 LinearGauge (battery "fuel")

```
  78%
  ├──┼────┼──■──┼────┼────┤   4.102 V
  0  25   50   75  100
```

- 12dp track, hairline ticks every 10%, labeled every 25. Fill = CH-04 amber; below 20% fill switches to `stampRed` + LED pulses.
- Needle knob (■) springs on change. Charging: knob blinks, `+` prefix on wattage.

### §5.8 HatchBar (storage & composition maps)

```
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▨▨▨▨▨▨▨▨▨▨░░░░░░░░░░│
   apps 38.2 GB · media 21.4 GB · system 12.9 GB · free 52.4 GB
```

- 16dp tall, 1dp hairline frame, segments rendered with §4.5 patterns + channel/ink colors. Segment min-width 8dp; smaller segments merge into `other` with a dotted key.
- Tap a segment → MarginNote with partition detail + drill-in.

### §5.9 LedgerTable (process list)

```
── USER APPS ────────────────────────────────────
 0142  com.android.chrome        4.2%   312 MB
       pid 22081 · 14:02:11 · ● foreground
 0147  com.spotify.music         1.1%   480 MB
       pid 23156 · 06:44:02 · ○ cached
── SYSTEM ───────────────────────────────────────
 0201  system                    2.0%   918 MB
```

- Index number column (mono, ink/40) — the ledger's row number, cited in the dossier and kill log.
- Row 56dp: line 1 = package + live CPU% + RSS; line 2 = `meta` pid/uptime/state. CPU%/RAM update in place, tabular, no layout shift.
- Sticky section headers with double-rule underlines. Self-row carries a `[SELF]` stamp. Rows with root-killed history show `†` after the index.
- TalkBack: full row semantics; sorting announced.

### §5.10 Dossier (process detail — clipped sheet)

See §7 S-09 for full wireframe. Bottom sheet with **perforated top edge** (dashed 1dp, like a tear-off slip). Contains mini traces (60s CPU/RAM history), PSS/USS, uid, oom_adj, seccomp state, and the SafetyLatch.

### §5.11 SafetyLatch (destructive confirmation)

```
 ▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚
 ▚   ARM — TERMINATE PROCESS 0142?  ▚
 ▚                                  ▚
 ▚   [ ABORT ]        [ KILL ⏻ ]   ▚   ← KILL only after arming
 ▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚
```

Two-step: **(1)** drag a square knob across a hatched rail to `ARM` (ascending haptic, rail fills with cross-hatch); **(2)** the red `KILL` HardKey unlocks with a stamp-in. `ABORT` always available, unhatches back with a 200ms fade. Copy stays clinical — never "Are you sure???".

### §5.12 DIPSwitch, FaderKey, SegKey, HardKey

```
 DIPSwitch                       FaderKey
 grid visibility                 sample rate
   OFF  [ ▢ ]  ON                  ├──┼──■──┼┤   1.0 s
        (knob snaps, LED dot)      0.25s        2.0s
```

- **DIPSwitch** — 48×32dp, square knob travels 12dp with `spring/snap`, LED dot right of label, state in `meta`. The app's signature toggle.
- **FaderKey** — slider as a hardware fader: hairline rail, tick marks, square knob, live `meta` value. No gradient tracks.
- **SegKey** — joined segmented buttons sharing 1dp borders; active = ink fill / paper text.
- **HardKey** — 48dp tall, 1.5dp ink border, mono caps 13sp +0.08em. Variants: *primary* (ink fill), *secondary* (outline), *destructive* (`stampRed` fill, requires SafetyLatch), *disabled* (**dashed** border + `hatch/dots` fill). Press: scale 0.98.

### §5.13 StampBadge

```
   ╔══════════════════╗
   ║  ROOT VERIFIED   ║      1.5dp stampRed border · caps mono
   ╚══════════════════╝      13sp · +0.12em · rotated −3° · 80% ink
```

Used for: `ROOT VERIFIED`, `DEGRADED`, `ARMED`, `SELF`, `CALIBRATED`, source tags (`SF`/`GFX` on the HUD), `NO SIGNAL`. Stamps are for **states**, never decoration. One stamp per region, maximum.

### §5.14 MarginNote (toasts, glossary, banners)

```
┌ NOTE 004 ────────────────────────────────────────┐
│ Usage access was revoked. CPU per-process figures │
│ are now estimated (≈).          [ RESTORE ]  ✕   │
└───────────────────────────────────────────────────┘
```

- Appears under the masthead. Hairline border, `NOTE nnn` doc-ref, error variant adds a 3dp `stampRed` left edge. 4s auto-dismiss (non-error), swipe to dismiss, never stacked (queued).

### §5.15 EmptyState / Loading / Fault

```
 LOADING          EMPTY                FAULT
 ┌─────────┐      ┌─────────────┐      ╔════════════╗
 │  ⌖ CALI- │      │ NO SIGNAL   │      ║ FAULT 0x2F ║
 │  BRATING │      │ no processes│      ╚════════════╝
 │  ▓▓░ 62% │      │ match filter│      sensorsvc un-
 └─────────┘      └─────────────┘      available [RETRY]
```

- Loading: rotating reticle + `CALIBRATING` + percent when known. Charts: flat line + sweeping pen dot.
- Empty: mini test-pattern bars (ink/channel ticks), one plain-language sentence, one HardKey to fix the emptiness.
- Fault: red stamp + code + one-sentence cause + `RETRY`.

---

## §6 — Information Architecture

```
CALIBRATION (first run)
└── DEVICEINSIGHT
    ├── 1 OVERVIEW      ....... system ledger (all channels)
    ├── 2 ACTIVITY
    │     ├── CH-01 CPU (+ thermal, GPU)
    │     ├── CH-02 MEMORY
    │     ├── CH-03 NETWORK
    │     ├── CH-04 POWER
    │     └── CH-05 STORAGE
    ├── 3 PROCESSES ....... ledger → dossier → safety latch
    ├── 4 DEVICE ......... dossier: summary/compute/display/
    │                      sensors/codecs + plates
    ├── 5 SETTINGS ....... control panel + colophon
    ├── HUD (overlay, global)
    └── WIDGETS (home screen)
```

---

## §7 — Screen Designs

### S-00 · Calibration (onboarding)

**Purpose:** grant permissions without dark patterns; establish the metaphor in 4 steps.

```
┌────────────────────────────────────────────┐
│                                            │
│         Calibration.                       │ ← Instrument Serif italic, 44sp
│                                            │
│   DOC № DI-0001 · REV 2.0                  │
│   Two minutes to grant your channels       │
│   the access they need. Every step is      │
│   optional — reduced accuracy is marked ≈. │
│                                            │
│   [ BEGIN CALIBRATION ]                    │
│                                            │
│   01 · 02 · 03 · 04 · ▓                    │ ← film-strip progress
└────────────────────────────────────────────┘
```

**Steps (each a numbered sheet):**
1. **01 · USAGE ACCESS** — annotated line diagram of the Settings path (phone outline, arrow, `meta` labels) + why, in serif body voice. `GRANT` HardKey + `SKIP (≈ estimates)` secondary.
2. **02 · OVERLAY** — HUD permission; preview of the HUD module rendered live on a screenshot of this very sheet.
3. **03 · ROOT PROBE** — optional; probe runs, result stamped `ROOT VERIFIED` or `CHANNELS PARTIALLY LOCKED` with hatched preview of locked features.
4. **04 · MEDIA** — pick Paper / Carbon / Blueprint from three *paper sample* swatches (real rendered mini-panels, not color dots).
5. **Certificate** — device name field (baseline input), date, `CALIBRATED` stamp slams in, `SHARE CERTIFICATE` (renders a PNG spec sheet). This shareable certificate is a growth hook.

Skipped steps surface later as MarginNotes, never blocking dialogs.

### S-01 · Overview (the system ledger)

```
┌──────────────────────────────────────────────┐
│ ⌖ DEVICEINSIGHT               14:32:07 UTC ● │
└──────────────────────────────────────────────┘
════════════════════════════════════════════════

№ 01 — OVERVIEW                              REV 2.0
     all channels nominal

┌─ CH-01 · CPU ────────────────────────── ● ─────┐
│                                                  │
│   38.4%         ▁▂▄▃▂▁▂▃▅▆▄▃▂                    │
│   2.84 GHz · 46.2°C · 8C/8T                     │
│                                                  │
└──────────────────────────────────────────────────┘
┌─ CH-02 · MEMORY ─────────────────────── 57% ───┐
│                                                  │
│   6.81 / 12 GB                                  │
│   ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░  zram 1.2 · swap 0.4      │
│                                                  │
└──────────────────────────────────────────────────┘
┌─ CH-03 · NETWORK ─────────────── ↑2.4 ↓18.1 ───┐
│                                                  │
│   ▂▄▆█▆▄▂▁  ▁▁▂▂▁▁▁▁   MB/s                      │
│                                                  │
└──────────────────────────────────────────────────┘
┌─ CH-04 · POWER ──────────────────────── 78% ───┐
│                                                  │
│   ≈ -3.42 W · 4.10 V · 39°C · 6h 12m remaining  │
│                                                  │
└──────────────────────────────────────────────────┘

— END OF SHEET —
```

**Spec**
- Header block: serif `Overview.` + `meta` sub-line reflecting overall state (`all channels nominal` / `1 channel warning` in stampRed).
- Each tile: ReadoutTile + 60s sparkline (pen dot live). Tap → channel page. Long-press label → glossary.
- Pull-to-refresh = **"re-ink the pen"**: all traces redraw with the 420ms sweep simultaneously.
- Warning states: any channel crossing its threshold adds a pulsing LED + its value rendered in stampRed.
- Foldable/tablet: 2-column tile grid (CH-01/02 | CH-03/04).

### S-02..S-06 · Channel Pages (shared template + deltas)

**Template:** serif title (`Processor.` `Memory.` `Network.` `Power.` `Storage.`), hero readout row, primary ScopeTrace full-width (timebase SegKey: `30s · 2m · 10m · 1h`), then channel-specific instrument blocks. Every page ends `— END OF SHEET —`.

**S-02 CPU — `CH-01`**
```
№ 02 — PROCESSOR

   2.84 GHz          gov schedutil · big.LITTLE 1+4+4
   ── odometer digits roll on change ──────────────────

┌ CORES ─────────────────────────────────────────┐
│ C0 ▮▮▮▮▮▮▯▯▯▯▯▯▯▯▯▯  38%   1804 MHz            │
│ C1 ▮▮▮▮▮▮▮▮▮▮▮▯▯▯▯  72%   2188 MHz            │
│ C2 ▮▮▮▮▮▮▮▮▮▮▮▮▮▮▯  91% ⌃ 2416 MHz            │
│ C3 ▮▮▮▮▮▯▯▯▯▯▯▯▯▯▯  29%   1804 MHz            │
│ …                                               │
└──────────────────────────────────────────────────┘
┌ CH-01 · LOAD · 60s ──────────────── pen ● 38.4% ┐
│ (full ScopeTrace, crosshair on touch)           │
└──────────────────────────────────────────────────┘
┌ THERMAL ────────────────────────────────────────┐
│  46.2°C   ░▒▓█ ramp gauge   zone: neutral       │
└──────────────────────────────────────────────────┘
```
Deltas: CoreRail (peak-hold), thermal ramp gauge (amber→red, zone label: `neutral/warm/moderate/severe/thermal throttling`), governor/kernel notes as spec rows. GPU block (`CH-06`) links here when present.

**S-03 Memory — `CH-02`**
Composition HatchBar (active/cached/free/ZRAM/swap), hero `6.81 / 12 GB`, swap/ZRAM mini-gauges, "top consumers" mini-ledger (5 rows, tap → Processes pre-filtered), OOM state MarginNote on long-press.

**S-04 Network — `CH-03`**
Dual-trace ScopeTrace (down channel color / up ink 40%), hero rates `↓ 18.1 ↑ 2.4 MB/s` with odometers, session/today counters as odometer blocks, per-app usage table (since API limits, labeled `≈` where sampled).

**S-05 Power — `CH-04`**
```
┌ FUEL ───────────────────────────────────────────┐
│  78%                                            │
│  ├──┼────┼──■──┼────┼────┤   4.102 V            │
│  0  25   50   75  100                          │
│  since unplugged 6h 12m · ≈ -3.42 W            │
└──────────────────────────────────────────────────┘
```
LinearGauge + wattage/voltage/current readouts + discharge ScopeTrace (negated area, ink 20% fill) + spec rows (health, cycle count, design capacity, charge counter). Estimates always `≈`.

**S-06 Storage — `CH-05`**
HatchBar allocation map + legend table with dotted leaders + partition drill-in (system/data/cache) + per-directory sizes on root. The hatched cadastral map is this page's signature.

### S-09 · Processes (ledger → dossier → kill)

```
№ 03 — PROCESSES                 147 listed · 4 211 threads

[ ALL ] [ APPS ] [ SYSTEM ] [ ROOT ⚿ ]     FIND: ▮

── USER APPS ──────────────────────────────────────
 0142  com.android.chrome        4.2%   312 MB
       pid 22081 · 14:02:11 · ● foreground
 0147  com.spotify.music         1.1%   480 MB
       pid 23156 · 06:44:02 · ○ cached
── SYSTEM ──────────────────────────────────────────
 0201  system                    2.0%   918 MB
 0204  surfaceflinger            0.8%    64 MB
── SELF ────────────────────────────────────────────
 0213  com.ivarna.deviceinsight  0.4%   112 MB  [SELF]
```

- Sort by tapping column headers (`▲/▼` caret, announced). Default: CPU desc, live re-rank **throttled to 2s** with rows crossfading position (no janky per-frame reordering).
- `FIND:` baseline-style filter input; kernel threads behind a DIP (*Processes → Show kernel threads*).
- Tap row → **Dossier** (clipped sheet, perforated edge):

```
┌ - - - - - - - - - - - - - - - - - - - - - - - - ┐   ← perforation
│ DOSSIER · 0142                                   │
│ com.android.chrome                               │
│ Chromium 126.0.6478.71 · up 14:02:11             │
│                                                  │
│ CPU  ▁▂▃▂▁  4.2%   avg 2.9 · max 11              │
│ MEM  ▓▓▓▓▓░  312 MB  pss 284 · uss 201           │
│ NET  ↑ 18 MB · ↓ 204 MB (session)                │
│                                                  │
│ uid 10247 · oom adj 0 · seccomp enforced         │
│                                                  │
│ [ FORCE STOP ]              [ TERMINATE ⏻ ]     │
└──────────────────────────────────────────────────┘
```

- `FORCE STOP` (package) = destructive HardKey + SafetyLatch. `TERMINATE` (root kill) additionally shows the hatched ARM rail. On kill: stamp `TERMINATED 14:32:07` in the sheet + MarginNote + entry logged to a `KILL LOG` (Settings → Processes), cited by ledger index.
- No root: `TERMINATE` renders disabled (dashed + hatch + key glyph); long-press explains via MarginNote.

### S-10 · Device Dossier (hardware)

```
№ 04 — DEVICE DOSSIER

[ SUMMARY ] [ COMPUTE ] [ DISPLAY ] [ SENSORS ] [ CODECS ]

DEVICE                       ─────────────────────
 model ............. Pixel 8 Pro
 codename .......... husky
 android ........... 15 (BP4A.250105.008)
 security patch .... 2025-01-05

COMPUTE                     ──────────────────────
 soc ................ Tensor G3 · zuma
 process ............ 4 nm
 cpu ................ 1× X4 3.1 · 4× A715 2.6 · 4× A510 1.7

[ FIG. 1 — CLUSTER TOPOLOGY ]
   ┌ BIG (1× X4) ──┐┌ MID (4× A715) ┐┌ LIT (4× A510) ┐
   │  ▢      L3 8 MB shared ─────────────────────── │
   └───────────────┘└────────────────┘└───────────────┘
```

- Spec sheets use **dotted leaders** (label …… value) like a datasheet TOC.
- **Plates:** schematic diagrams drawn as line art — CPU topology (clusters + shared L3), display signal chain, camera module map, sensor inventory. Each plate is numbered `FIG. n` with a serif caption. These plates are the page's soul; they must be *drawn*, not listed.
- Sensors tab: live **channel strips** — one row per sensor (index, name, raw values with units, 24pt sparkline, rate SegKey `normal/game/fast`). Magnetometer renders a compass-rose plate; barometer shows a 6h pressure trace.
- Widevine/DRM levels, codec lists, Vulkan extensions: monospace tables with hairline rules, sticky headers, `FIND:` filter.

### S-11 · HUD (performance overlay) + config

```
⌜──────────────────────────────⌝
│ FPS 119.8      [SF]          │   ← stamp = trace source
│ ────────────────────────────  │
│ CPU  42%   ▂▄▃                │   ← channel tick squares
│ GPU  71%   ▃▅▆                │      precede each row
│ RAM  6.8G      TEMP 58°C     │
│ NET  ↓18.1  ↑2.4 MB/s        │
⌞──────────────────────────────⌟
```

- Corner brackets (the only "frame"), 70% ink scrim + 1dp hairline; **the one sanctioned blur** (8dp, scrim only) for legibility over games — documented exception, never a style.
- All mono. FPS at `readout/l` + odometer. Rows are reordered by drag; each row can be toggled in config. Source stamp: `SF` (SurfaceFlinger, games) or `GFX` (gfxinfo) — honesty about the measurement.
- Drag by the crosshair handle; tap opens the **HUD config sheet**: size S/M/L SegKey, opacity fader (40–90%) with live preview, position reset, per-metric DIPs, **per-app profiles** (e.g., games → FPS+GPU+TEMP only), color mode (ink / paper / channel).
- Update cadence 500ms (values) / 100ms (FPS). Text carries a 1dp contrasting stroke for busy scenes.

### S-12 · Widgets ("bench instruments")

```
2×2 · SINGLE CHANNEL          4×2 · DUAL                4×4 · BENCH
┌ CH-01 CPU ────── ● ┐   ┌ CH-01 ─────┐ CH-02 ────┐   ┌ CH-01 · CPU ── 38.4% ┐
│      38.4%         │   │   38.4%    │  6.81 GB   │   │ ▁▂▄▃▂▁▂▃▅▆▄▃▂        │
│ ▁▂▄▃▂▁▂▃▅▆▄▃▂▁     │   │  ▂▄▃▂▁     │ ▓▓▓▓▓▓░░░  │   ├ CH-04 · PWR ── 78% ─┤
│ upd 14:32:07       │   │  upd 14:32 │ upd 14:32  │   │ ├──┼──■──┼──┤ 4.10V │
└────────────────────┘   └────────────┴────────────┘   ├ CH-03 ▂▄▆█▆▄▂ ↓18.1 ┤
                                                      └ upd 14:32:07 ────────┘
```

- Glance-based. Same components as the app (channel tick, tabular readout, hatched bars, pen-dot trace) — widgets must be *recognizably the same instrument*, just smaller.
- Picker previews render as cut-out cards lying on graph paper at a −3° tilt.
- Deep links: tapping a channel region opens that channel page. Update cadence 30s (static), 1s while charging only. `upd` timestamp always shown — a widget that hides its age is lying.

### S-13 · Settings + Colophon

```
№ 05 — SETTINGS

01 PRESENTATION
   media ........ [ PAPER ][ CARBON ][ BLUEPRINT ]
   grid ............ OFF [ ▢ ] ON
   hatching ........ OFF [ ▣ ] ON     (CVD-redundant patterns)
   key clicks ...... OFF [ ▢ ] ON

02 MONITORING
   sample rate ..... ├──┼──■──┼┤  1.0 s      (250ms–2s)
   history depth ... 60 s
   keep awake ...... OFF [ ▢ ] ON

03 HUD ... defaults, per-app profiles, position reset
04 PROCESSES ... kernel threads DIP · kill log
05 SYSTEM ... haptics DIP · reduced-motion (system) · locale
06 ABOUT → COLONHON
```

**Colophon** — the About page as a book colophon:

```
№ 06 — COLOPHON

Set in Instrument Serif & IBM Plex Mono.
Drawn on a 4pt grid. No gradients were used in
the making of this instrument.

REVISIONS
 REV A ... CALIPER design language adopted
 v1 ...... Elegant Glassmorphism (retired)
 v0 ...... first internal build

License GPL-3.0 · Built by Ivarna
```

### S-14 · System & edge states

| Situation | Treatment |
|---|---|
| Permission revoked | Masthead `DEGRADED` stamp + MarginNote + `≈` prefix on affected readings |
| Root unavailable | Locked controls: hatch fill + key glyph + dashed border; long-press MarginNote |
| Root verified | `ROOT VERIFIED` stamp slams into masthead once (stamp-in + haptic) |
| No data yet | ScopeTrace flat line + `NO SIGNAL`; readouts `—` |
| Sensor absent | Row struck through with a single hairline + `NOT FITTED` meta label (aerospace habit) |
| Work profile / multi-user | Ledger section headers per profile, `WORK` stamp on rows |
| Foldable / tablet | ModeRail → left rail; two-pane ledger + dossier |
| First launch signature | Full-screen **calibration sweep**: grid + one trace sweeps L→R (1.2s), `CALIBRATED · DI-0001` stamp lands, sheet 01 rises. Skippable by tap. |

---

## §8 — Theming System

- Exactly **three media**: Paper, Carbon, Blueprint — same tokens, same components, different *paper stock*. No per-theme layout differences; no accent customization (orange/cyan is part of the identity).
- Selection: Settings → Presentation (SegKey) + Quick Settings tile. Follows system dark mode by default: light→Paper, dark→Carbon; Blueprint is always a deliberate choice.
- Charts/LEDs/hatches pull from the same channel registry per media — a widget, the HUD, and the app never disagree about what CH-01 looks like.

---

## §9 — Voice & Copy

- **Serif thinks, mono reports.** Serif carries sentences with personality; mono carries facts.
- States, not vibes: `NO SIGNAL`, `CHANNEL LOCKED`, `CALIBRATING…`, `ROOT VERIFIED`, `FAULT 0x2F — sensorsvc unavailable.`
- Kill flow stays clinical: *"This terminates the process immediately. Unsaved state is lost."* No exclamation marks, no emoji in UI, no "Oops!".
- Numbers speak first: toasts lead with the value (`TERMINATED 0142 · 14:32:07`).
- Glossary (MarginNote) exists for: ZRAM, PSS/USS, oom_adj, Widevine L1/L3, SurfaceFlinger vs gfxinfo, governor, seccomp. Long-press to learn — the app respects curiosity.

---

## §10 — Accessibility

- **Contrast:** ink/paper ≥ 12:1; all channel traces ≥ 4.5:1 on their media (verified per hex); accent-on-paper ≥ 3:1 for large interactive targets only.
- **CVD:** color never sole-encoded — hatch + label redundancy everywhere; blue/amber pairs favored; red/green (CH-01/CH-03) always separated by hatch and label.
- **Touch:** ≥ 48dp targets; ledger rows 56dp; DIP 48×32dp with a 56dp touch slop.
- **TalkBack:** readouts read as "CPU load, 38.4 percent"; charts summarized (min/max/avg); crosshair has custom actions; stamps announce as "status: root verified."
- **Font scaling to 200%:** serif titles wrap freely; readout tiers step down (`xl`→`l`→`m`) above 130% scale; tables scroll horizontally rather than truncate.
- **Reduced motion / haptics / sound:** each independently honored; system reduced-motion respected automatically.
- **RTL:** mirror layout; channel tick stays on the leading edge; traces still read left→right (time direction is not localized).

---

## §11 — Icon & Store Presence

**Launcher icon:** paper square (2dp radius) · ink crosshair `⌖` centered · 3dp accent-orange center dot · optional hairline 24dp inner frame. Adaptive foreground on Paper/Carbon background layers. No wordmark, no gradient.

**Store assets:** feature graphic = a full CALIPER sheet (masthead, one channel panel, stamp `MEASURE EVERYTHING`) on graph paper; screenshots framed as numbered sheets with serif captions ("01 — The ledger." "02 — The scope." "03 — The overlay."); promo animation = the calibration sweep.

---

## §12 — Implementation Notes (Compose)

1. **Remove** Haze and all blur/glass surfaces (HUD scrim blur is the single exception).
2. **Fonts:** bundle `InstrumentSerif-Regular.ttf`, `InstrumentSerif-Italic.ttf`, `IBM Plex Mono` (300/400/500 + italics). Enable `tnum` via `FontFeatureSettings`.
3. **ScopeTrace:** custom `Canvas` composable — grid via `drawLine`, hatches via `PathEffect.dashPathEffect`/`strokePath`, crosshair via `pointerInput` + `detectDragGestures`, pen head via last point. Restyle or replace Vico instances; if Vico stays, force mono axis labels, square caps, no inset rounding.
4. **Hatching:** one `DrawScope` extension (`fun DrawScope.hatch(rect, pattern, color)`) shared by bars, locked panels, stamps, widgets.
5. **Odometer:** per-digit `AnimatedContent` with slide+fade, staggered 24ms, digits constrained to a fixed tabular column so digits never shift width.
6. **Motion tokens:** central `CaliperMotion` object — one easing, two springs, four durations. Nothing ad-hoc.
7. **Semantics:** every readout exports `contentDescription`; LedgerTable uses `Modifier.semantics { row/col }`.
8. **Widgets:** Glance, sharing the hatch painter as a static-rendered bitmap (pre-render patterns at 1x/2x/3x).
9. **Migration:** single `DataStore` flag `caliperMigrated`; on first 2.x launch show the "recalibrated" MarginNote, map old theme → nearest medium.

### Design tokens (starter)

```kotlin
object Caliper {
    val Paper = CaliperPalette(
        surface  = Color(0xFFF4F1E8), panel   = Color(0xFFFBF9F3),
        ink      = Color(0xFF191713), hairline= Color(0x24191713),
        accent   = Color(0xFFFF4D00), fault   = Color(0xFFC8371F),
        channels = channelMap(
            cpu=Color(0xFFE5482B), mem=Color(0xFF2E5BE0), net=Color(0xFF0E9F6E),
            pwr=Color(0xFFF0A419), sto=Color(0xFF8757D6), gpu=Color(0xFFD6409F))
    )
    val Carbon = /* … inverted set … */
    val Blueprint = /* … cyanotype set … */

    val EaseInstrument = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Needle = SpringSpec<Float>(dampingRatio = 0.82f, stiffness = 420f)
    val Snap   = SpringSpec<Float>(dampingRatio = 1.0f,  stiffness = 700f)
}
```

---

## §13 — Do / Don't

| ✗ Don't | ✓ Do |
|---|---|
| Glass card, 20dp radius, purple gradient, glow | Hairline panel, 0dp radius, paper + ink |
| Rounded-cap gradient chart line | Square-pen trace on engineering grid with crosshair |
| Neon "FPS 120!!!" pill | `FPS 119.8 [SF]` mono readout with corner brackets |
| "Are you sure?" center dialog | Hatched SafetyLatch — arm, then fire |
| Color-only legend dots | Hatch + label + color triplet |
| FAB floating over content | HardKey at the point of use |
| Emoji section headers | `№ 04 — DEVICE DOSSIER` + serif plate captions |
| Odometer-less jumping numbers | Digits roll; only changed digits move |

---

## §14 — QA Checklist (design review gate)

- [ ] Screenshot passes the **88/10/2 color ratio** test
- [ ] Every channel color appears with its label + hatch
- [ ] All numerals tabular; no layout shift on live update
- [ ] Every destructive action passes through SafetyLatch
- [ ] Reduced motion: no sweeps, no odometer rolls, no pulses
- [ ] TalkBack reads every readout and chart summary correctly
- [ ] Hairlines render at 1dp on all densities (no 0px lines on mdpi)
- [ ] `— END OF SHEET —` present on every scrollable page
- [ ] Widget/HUD/app agree on every channel's color and label
- [ ] Stamps: max one per region, −3°, never decorative

---

## §15 — Document History

| Rev | Date | Change |
|---|---|---|
| A | — | CALIPER adopted; glassmorphism system retired |
| v1 | — | "Elegant Glassmorphism" (superseded) |

---

*Set in Instrument Serif & IBM Plex Mono. Drawn on a 4pt grid. **Measure everything. Label everything.***
