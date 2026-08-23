# DeviceInsight — **BENCH** Widget System
### Design Plan · The CALIPER Extension for the Home Screen

> *Doc ID: DI-WD-001 · Rev A · Extends CALIPER (DI-DS-002) §S-12 · Precedes implementation*

---

## §1 — Thesis

Every monitor app ships the same widget: a rounded glass card with a gradient ring. **BENCH ships bench instruments** — flat panels with hairline frames, channel labels, tabular numerals, hatched bars and a live pen trace. Dropped onto any launcher, a BENCH widget should read instantly as *"a piece of test equipment sitting on my home screen"* — sharp corners among a sea of rounded cards.

**Five instruments, one family:**

| ID | Name | Channel | One-line personality |
|---|---|---|---|
| **WT-01** | **SCOPE** | CH-01 · CPU | The pen is always mid-sentence — a live trace, never a static ring. |
| **WT-02** | **STACK** | CH-02 · MEMORY | A cadastral map of your memory, drawn in hatch patterns. |
| **WT-03** | **FUEL** | CH-04 · POWER | Wattage is the hero — the *flow*, not the tank. |
| **WT-04** | **RASTER** | CH-06 · GPU | The honest instrument — says `NOT FITTED` when the SoC won't talk. |
| **WT-05** | **BENCH** | ALL | The whole instrument panel in one glance. |

### Widget principles (inherit CALIPER §3, plus these)

1. **Same instrument, smaller bench.** Identical channel registry, tokens, hatches, type scale, honesty (`upd` timestamp). Never a "lite" visual language.
2. **Growth, never stretch.** When a widget is resized larger, it *gains* sub-instruments (core rail, thermal ramp, history). It never just scales up its text.
3. **A widget that hides its age is lying.** Every widget stamps `upd HH:MM:SS`. Stale data degrades visibly (LED off → `SIGNAL LOST`).
4. **The frame survives any wallpaper.** Every panel carries a full 1dp ink border — contrast comes from the frame, not from the wallpaper. Cream widget on cream wallpaper stays legible.
5. **No decorative state.** LED, hatching, and stamps only mean things: live, locked, charging, critical.

---

## §2 — Shared Anatomy

Every widget is built from the same five bands, top to bottom:

```
┌─ [BAND 1 · HEADER]  channel tick · CH-xx · NAME ─── status ─┐
│ [BAND 2 · HERO]     the one big number this instrument exists for │
│ [BAND 3 · BODY]     trace / hatch bar / gauge / rail              │
│ [BAND 4 · SUBLINE]  secondary readings, spec-style                 │
│ [BAND 5 · FOOTER]   upd HH:MM:SS · window label                    │
└────────────────────────────────────────────────────────────────────┘
```

- **Band 1** — 3dp channel color tick + `meta` caps label + status (`●` LED, `%`, stamp, or `⚷` when root-locked).
- **Band 2** — the hero readout (`readout/l`, tabular, odometer where Glance allows animated replacement).
- **Band 3** — the signature graphic. Rendered as a **bitmap** (see §6): trace with pen dot, hatched composition bar, fuel gauge, or core rail.
- **Band 4** — one or two `data/s` rows, `·`-separated, never wrapped.
- **Band 5** — `upd` timestamp right- or bottom-aligned; trace window label on the opposite side. This band is the family signature across all five widgets.

**Margins:** 12dp outer padding, 16dp between bands. **Radius 0dp everywhere** (OEM launchers may clip corners — the 12dp inset keeps hairlines and hatches clear of any clip).

---

## §3 — The Three Media (all widgets, all states)

Three media render from the **same token set** — no per-widget custom colors. The picker and the config screen both offer: `PAPER · CARBON · BLUEPRINT · FOLLOW SYSTEM` (follow = Paper in light mode, Carbon in dark; Blueprint is always a deliberate choice).

### WT-01 SCOPE, rendered in all three media

```
   PAPER                       CARBON                      BLUEPRINT
┌─ ▪ CH-01 · CPU ──── ● ─┐  ┌─ ▪ CH-01 · CPU ──── ● ─┐  ┌─ ▪ CH-01 · CPU ──── ● ─┐
│                         │  │                         │  │                         │
│   38.4%                 │  │   38.4%                 │  │   38.4%                 │
│   ▁▂▄▃▂▁▂▃▅▆▄▃▂ ●       │  │   ▁▂▄▃▂▁▂▃▅▆▄▃▂ ●       │  │   ▁▂▄▃▂▁▂▃▅▆▄▃▂ ●       │
│   2.84 GHz · 46.2°C     │  │   2.84 GHz · 46.2°C     │  │   2.84 GHz · 46.2°C     │
│   upd 14:32:07 · 60s    │  │   upd 14:32:07 · 60s    │  │   upd 14:32:07 · 60s    │
└─────────────────────────┘  └─────────────────────────┘  └─────────────────────────┘
 cream #F4F1E8 panel         charcoal #1C1B17 panel       cyanotype #12314E panel
 trace #E5482B vermilion    trace #FF6B4A lightened      trace = ink #EAF2FF,
 warm near-black ink        cream ink #EDE7DA            channel ID via hatch ↗ + label
```

### Master token table (drives every bitmap and text render)

| Token | PAPER | CARBON | BLUEPRINT |
|---|---|---|---|
| surface (widget bg) | `#F4F1E8` | `#141310` | `#0C2338` |
| panel (inner) | `#FBF9F3` | `#1C1B17` | `#12314E` |
| ink / ink60 / ink40 | `#191713` + alpha | `#EDE7DA` + alpha | `#EAF2FF` + alpha |
| hairline (frame, rules) | ink @ 14% | ink @ 18% | line @ 20% |
| accent (LED, refresh key) | `#FF4D00` | `#FF5A1F` | `#63C7FF` |
| fault (critical, stamps) | `#C8371F` | `#FF6B4A` | `#FF7759` |
| CH-01 trace | `#E5482B` | `#FF6B4A` | ink — hatch ID ↗ |
| CH-02 | `#2E5BE0` | `#6B8CFF` | ink — hatch ↗ |
| CH-03 | `#0E9F6E` | `#2FD3B0` | ink — hatch ▨ |
| CH-04 | `#F0A419` | `#FFB84D` | ink — hatch ⠿ |
| CH-05 | `#8757D6` | `#B08CFF` | ink — hatch ▮ |
| CH-06 | `#D6409F` | `#F06BB0` | ink — hatch ▬ |

**Blueprint rule:** in the cyanotype medium, *all* traces render in ink — channel identity is carried by the hatch pattern and the mandatory `CH-xx` label. This is the CVD-redundancy system doing double duty as an aesthetic.

**Every widget ships in every medium.** No media is "premium," no media is locked behind settings beyond the picker.

---

## §4 — The Five Instruments

---

### WT-01 · SCOPE — `CH-01 · CPU`

**Purpose:** live load at a glance; the trace is the point — a monitor widget without history is just a number.

**Tier 1 — 2×2 (base)**

```
┌─ ▪ CH-01 · CPU ────────── ● ─┐
│                                │
│   38.4%                        │
│   ▁▂▄▃▂▁▂▃▅▆▄▃▂▁ ●             │
│   2.84 GHz · 46.2°C            │
│   upd 14:32:07        60 s     │
└────────────────────────────────┘
```

**Tier 2 — 4×2 (wide):** hero readout left; full gridded trace right (engineering grid, y-labels 0/25/50/75/100); thermal ramp bar under the hero.

```
┌─ ▪ CH-01 · CPU ─────────────────────── ● ─┐
│                                              │
│   38.4%      100 ┤   ╭──╮                   │
│   2.84 GHz    50 ┼──╯  ╰──╮        ╭─╮      │
│   46.2°C       0 └─────────╰──╯───╯─── ●    │
│   ░▒▓█ thermal: warm                         │
│   gov schedutil · 8C/8T     upd 14:32:07     │
└──────────────────────────────────────────────┘
```

**Tier 3 — 4×4 / 5×4 (large):** hero, full ScopeTrace with y-axis, **CoreRail** (all cores, load + freq, peak-hold carets), thermal gauge with zone label.

**Fields:** load % (hero) · freq · temp · trace (window configurable 60s/5m) · core rows (T3) · governor (T2+).
**Tap zones:** header/hero → CPU page (S-02) · trace → CPU page · footer `upd` → app.
**Signature element:** the pen dot at the trace's live head.
**Config:** media · cadence · trace window.

---

### WT-02 · STACK — `CH-02 · MEMORY`

**Purpose:** not just "how full" but **what's in it** — the composition map is the hero, since memory health is about composition, not just total.

**Tier 1 — 2×2 (base)**

```
┌─ ▪ CH-02 · MEMORY ───────── 57% ─┐
│                                     │
│   6.81 / 12 GB                      │
│   ▓▓▓▓▓▓▓▓▓▓▒▒▒▒▒▨▨▨░░░░░░░░░░░     │
│   zram 1.2 · swap 0.4               │
│   upd 14:32:07                      │
└─────────────────────────────────────┘
   ▓ active  ▒ cached  ▨ zram  ░ free
```

**Tier 2 — 4×2 (wide):** composition bar doubles in height with segment labels inside; adds 60s **pressure trace** (used %) beneath; `kswapd activity` note when thrashing.

**Tier 3 — 4×4 (large):** adds **top consumers** — a five-row mini ledger (index number, package, RSS), each row tappable → that process's dossier. Plus ZRAM depth gauge (fill/compression ratio) and swap-in/out rates.

```
┌─ ▪ CH-02 · MEMORY ───────────────────── 57% ─┐
│   6.81 / 12 GB                                 │
│   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▒▒▒▒▒▒▒▒▒▨▨▨▨▨▨▨░░░░░░░░░░░     │
│    active 4.2 · cached 1.9 · zram 1.2 · free 5.2│
│   pressure 60s  ▁▂▂▂▃▃▂▂▃▄▄▃▃▂  used %         │
│   ── TOP CONSUMERS ──                          │
│   0142  com.android.chrome      312 MB        │
│   0147  com.spotify.music       480 MB        │
│   0201  system                  918 MB        │
│   upd 14:32:07 · tap row for dossier          │
└─────────────────────────────────────────────────┘
```

**Fields:** used/total (hero) · composition segments (active/cached/zram/swap/free, each with pattern+color+label) · pressure trace · consumers (T3).
**Tap zones:** header → Memory page (S-03) · each consumer row → process dossier · bar → Memory page.
**Signature element:** the hatched cadastral bar — instantly distinguishable from every ring-donut memory widget on the store.

---

### WT-03 · FUEL — `CH-04 · POWER`

**Purpose:** the only battery widget whose hero is **wattage** — the flow through the device, not just the tank level. Percent is secondary; this is what makes it *DeviceInsight's* widget and not a battery widget.

**Tier 1 — 2×2 (base)**

```
┌─ ▪ CH-04 · POWER ───────── ● ────┐
│                                     │
│   ≈ -3.42 W                         │
│   78%                               │
│   ├──┼────┼──■──┼────┼────┤         │
│   -812 mA · 4.102 V                 │
│   6h 12m remaining                  │
│   upd 14:32:07                      │
└─────────────────────────────────────┘
```

**Tier 2 — 4×2 (wide):** adds **6-hour wattage history trace** (signed: discharge below the zero line, charge above — a rare, gorgeous chart) and temperature.

```
┌─ ▪ CH-04 · POWER ─────────────────────── ● ─┐
│    ≈ -3.42 W         78%                     │
│    ├──┼────┼──■──┼────┼────┤   4.102 V       │
│   0 ┼────────────────────────────────────    │
│        ╰──╮  ╭╯   6 h wattage   ╰──╮  ╭──    │
│   -812 mA · 36.9°C · 6h 12m remaining        │
│   upd 14:32:07                               │
└───────────────────────────────────────────────┘
```

**Tier 3 — 4×4 (large):** adds discharge-curve (% history), spec rows (health · cycle count · design capacity) and a charge-since-unplugged session ledger.

**States (critical to this widget):**
- **Charging:** LED solid accent · wattage prefixed `+` in amber · `CHARGING` mini stamp · fuel knob renders filled.
- **Critical (<20%):** gauge fill and % flip to fault red · LED pulses (via cadence-swap of two pre-rendered frames).
- **No battery data:** `NOT FITTED` strike-through row (desktop-mode devices).

**Fields:** wattage (hero, always `≈` — it's estimated) · % + fuel gauge · current mA · voltage · time remaining · history (T2+).
**Tap zones:** anywhere → Power page (S-05).
**Config:** media · cadence · **hero toggle: wattage / percent** (for users who want the conventional read).

---

### WT-04 · RASTER — `CH-06 · GPU`

**Purpose:** a GPU instrument honest about hardware reality. On many SoCs live GPU clocks need root — this widget is designed so that its *locked* state is still a good-looking, useful object (spec sheet mode), not a broken widget.

**Tier 1 — 2×2 (base)**

```
┌─ ▪ CH-06 · GPU ─────────── ● ────┐
│                                     │
│   71%   ·   848 MHz                 │
│   ▃▅▆▅▇▆▅▇▆▅▇▆ 60s                  │
│   adreno 740 · vulkan 1.3           │
│   upd 14:32:07                      │
└─────────────────────────────────────┘
```

**Tier 2 — 4×2 (wide):** dual-trace body — load (channel color) over frequency (ink 40%, own scale) — plus spec rows (`opengl 4.6 · shader cores`).

**Tier 3 — 4×4 (large):** adds per-clock table (bus freq, shader clock), GPU thermal, and memory-bandwidth readout where exposed.

**The locked state (root required, not granted):**

```
┌─ ▪ CH-06 · GPU ─────────── ⚿ ────┐
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   │
│   CHANNEL LOCKED                  │
│   live clocks need root —         │
│   showing static capabilities     │
│   ────────────────────────────    │
│   adreno 740 · vulkan 1.3         │
│   opengl es 3.2 · 2 shader procs  │
│   [ GRANT IN APP ]                │
└─────────────────────────────────────┘
```

Hatched (dots) body, key glyph in the header, capabilities as a spec sheet — the widget *degrades into a datasheet* rather than dying. `NOT FITTED` for devices with no readable GPU node at all.

**Fields:** load % + freq (co-heroes) · dual trace · SoC/GPU name · vulkan/GL version · clock table (T3).
**Tap zones:** header → CPU/GPU section (S-02) · `GRANT IN APP` → calibration.
**Signature element:** the locked-state datasheet — an honest empty state as a feature.

---

### WT-05 · BENCH — ALL CHANNELS · the flagship

**Purpose:** the complete instrument panel. What the user asked for: *"complete dashboard of phone."*

**Tier 1 — 4×2 (compact, ledger strip):** four user-selectable channels as ledger rows. Dense, scannable, one line each.

```
┌ DEVICEINSIGHT · BENCH ────────── ● 14:32 ─┐
│  ▪ CH-01 CPU      38.4%    2.84 GHz       │
│  ▪ CH-02 MEMORY   57%      6.81/12 GB     │
│  ▪ CH-04 POWER    78%      ≈ -3.42 W      │
│  ▪ CH-03 NET      ↓18.1    ↑2.4 MB/s      │
│  upd 14:32:07 · tap a row to open         │
└─────────────────────────────────────────────┘
```

**Tier 2 — 4×3 / 4×4 (full bench):** masthead strip + six channel tiles (two columns × three rows), each tile a miniature ReadoutTile with its own sub-instrument.

```
┌ DEVICEINSIGHT · BENCH ────────── ● 14:32 ─┐
│ ┌ ▪ CH-01 · CPU ─── 38.4% ┐ ┌ ▪ CH-02 · MEM ─ 57% ┐ │
│ │ ▁▂▄▃▂▁▂▃▅▆▄▃ 2.84 GHz   │ │ ▓▓▓▓▓▓░░░░ 6.81/12   │ │
│ │ 46.2°C · 8C/8T          │ │ zram 1.2 · swap 0.4  │ │
│ └─────────────────────────┘ └──────────────────────┘ │
│ ┌ ▪ CH-03 · NET ─ ↓18.1 ──┐ ┌ ▪ CH-04 · PWR ─ 78% ──┐ │
│ │ ▂▄▆█▆▄▂ ↑2.4 MB/s       │ │ ├─┼─■─┼─┤ ≈ -3.42 W   │ │
│ │ session ↓ 2.4 GB        │ │ 4.102 V · 6h 12m      │ │
│ └─────────────────────────┘ └──────────────────────┘ │
│ ┌ ▪ CH-05 · STO ─── 62% ──┐ ┌ ▪ CH-06 · GPU ── 71% ─┐ │
│ │ ▓▓▒▒▒▨▨░░ 78.4/128 GB   │ │ 848 MHz · ▃▅▆ load    │ │
│ └─────────────────────────┘ └──────────────────────┘ │
│ upd 14:32:07 · all channels nominal                  │
└────────────────────────────────────────────────────────┘
```

**Tier 3 — 5×4 (XL):** adds a full-width bottom band: **CoreRail** (CPU) + network history trace + storage detail line. The full bench.

**Tier behavior:** 4×2 shows rows 1–4 (configurable which four); 4×3 shows five tiles + footer; 4×4 shows all six; 5×4 adds the bench band.

**Fields:** every channel's primary value + one subline each; footer carries overall state (`all channels nominal` / `1 channel warning` in fault red, matching the Overview masthead logic).
**Tap zones:** each row/tile → its channel page · masthead → Overview (S-01) · footer → app.
**Signature element:** the only widget on the Play Store that looks like a six-channel data acquisition unit.
**Config:** media · cadence · which four channels in compact tier · toggle per-tile sub-instruments in XL.

---

## §5 — Responsive Size Matrix

Glance `SizeMode.Responsive` breakpoints (approximate dp):

| Tier | DpSize | WT-01 | WT-02 | WT-03 | WT-04 | WT-05 |
|---|---|---|---|---|---|---|
| 1 | 140×140 | spark + hero | map + hero | wattage hero + gauge | dual hero + spark | — |
| 2 | 280×140 | gridded trace + thermal | labeled map + pressure | + 6h watt trace | dual trace + specs | 4-row ledger strip |
| 3 | 280×210 | + core rail (2-col) | + consumers (3 rows) | + discharge curve | + clock table | 5 tiles |
| 4 | 280×280 | full scope + rail | + consumers (5) + zram gauge | + spec rows | full | 6 tiles |
| 5 | 350×280 | + y-axis labels | + full legend | + session ledger | + bandwidth | + bench band |

Min supported: each widget's tier-1 size. Below it, widgets render their base layout (never clipped content).

---

## §6 — Rendering & Cadence (design-relevant tech)

**Bitmap pipeline.** Glance has no Canvas — every Band-3 graphic (traces, hatch bars, gauges, core rails, thermal ramps) is pre-rendered to a `Bitmap` by a shared off-screen renderer using the exact CALIPER drawing code (`DrawScope.hatch`, ScopeTrace grid/pen) at 1×/2×/3× density, then fed via `ImageProvider`. **Consequence for design:** graphics are static between updates — "animation" comes from cadence, not motion. No pulsing, no shimmer. The `upd` stamp and LED state carry the liveness.

**Cadence tiers (visible in the footer, configurable per widget):**

| Mode | Condition | Values | Traces | Mechanism |
|---|---|---|---|---|
| LIVE | charging, or monitoring service running | 1 s | 5 s | service push |
| AMBIENT | screen on, service off | 30 s | 30 s | service push |
| BUDGET | background | 15 min | on-update | WorkManager |
| **SIGNAL LOST** | now − upd > 2× expected | frozen | flat | none |

**SIGNAL LOST treatment:** LED off, all numerals rendered at ink/40 with the last value, `SIGNAL LOST` stamp in footer position. Honesty over blankness — the user sees *when* it died.

**Fonts:** IBM Plex Mono via Glance `TextStyle` where supported; system monospace fallback is acceptable (widget text is short — tabular figures are the hard requirement, set via `fontFeatureSettings` where available).

---

## §7 — States Summary (all widgets)

| State | Header | Body | Footer |
|---|---|---|---|
| Loading (first placement) | channel label, LED off | `CALIBRATING…` + sweeping-pen bitmap | `--:--:--` |
| Live | `●` | normal render | `upd HH:MM:SS` |
| Warning (threshold) | LED fault-red | affected value in fault red | normal |
| Critical (battery <20%) | `●` red | fault-red fill | normal |
| Charging (WT-03) | `●` accent solid | `+` wattage, `CHARGING` stamp | normal |
| Root locked | `⚷` | hatched + `CHANNEL LOCKED` + datasheet | normal |
| Not fitted | `⚷` gray | `NOT FITTED` strike-through | normal |
| No signal (metric empty) | normal | flat line + `NO SIGNAL` | normal |
| Signal lost | LED off | frozen @ ink/40 | `SIGNAL LOST` stamp |

---

## §8 — Picker & Previews

- **Preview images:** one per widget per medium (15 total) — rendered as CALIPER panels lying on graph paper at a −3° tilt with a soft drop shadow (the *only* shadow in the entire design system, used solely for picker marketing renders, never in the widget itself).
- **Picker description lines** (mono voice):
  - `SCOPE — live CPU load, frequency, thermal`
  - `STACK — memory composition, ZRAM, consumers`
  - `FUEL — wattage, amperage, fuel gauge`
  - `RASTER — GPU load and clocks`
  - `BENCH — all channels, one instrument panel`
- **Config activity** on placement (optional, skippable — defaults are always valid): media selector rendered as three live mini-panels (real components, not color dots, per S-00 media step), cadence SegKey, widget-specific options from §4.

---

## §9 — Accessibility

- Every widget exposes a full spoken summary via `contentDescription`, e.g. BENCH: *"Bench. CPU 38.4 percent, memory 57 percent, power 78 percent, network 18.1 megabytes per second down. Updated 14:32."*
- Tap zones carry individual labels; consumers rows announce package + memory.
- Color never sole-encodes: composition maps rely on hatch + label (critical at widget size); Blueprint medium proves the system works with zero channel color.
- Minimum text size 11sp (`meta`) — no text below it at any tier.

---

## §10 — Do / Don't (widget-specific)

| ✗ Don't | ✓ Do |
|---|---|
| Gradient progress ring, rounded glass card | Hairline-framed flat panel, hatched bar, square corners |
| Anonymous percent with no context | Channel-labeled, unit-annotated, `upd`-stamped readings |
| Blank/gray widget when data is unavailable | `NOT FITTED` / `CHANNEL LOCKED` datasheet mode |
| Scale up text when resized | Add sub-instruments when resized |
| Hide staleness to look "live" | `upd` stamp + `SIGNAL LOST` degradation |
| Battery % as the power hero | Wattage as hero, % as the gauge |

---

## §11 — Deliverables Checklist

- [ ] 5 Glance widgets + receivers, responsive tiers per §5
- [ ] Shared bitmap renderer (ScopeTrace, HatchBar, LinearGauge, CoreRail, thermal ramp) — same code as app, off-screen
- [ ] 3 media token sets applied to all widgets + follow-system mapping
- [ ] Config activity (media · cadence · per-widget options)
- [ ] 15 picker previews (5 widgets × 3 media, tilted-card renders)
- [ ] All §7 states implemented, including SIGNAL LOST and datasheet-degrade
- [ ] Deep links: channel pages, process dossier, calibration
- [ ] Spoken summaries for all five widgets

---

## §12 — Document History

| Rev | Change |
|---|---|
| A | BENCH plan: 5 instruments, 3 media, responsive tiers, state & cadence system |

---

*Next document: **DI-WI-001 — Bench Widgets Implementation** (Glance code for all five instruments, the bitmap renderer, and the config activity).*
