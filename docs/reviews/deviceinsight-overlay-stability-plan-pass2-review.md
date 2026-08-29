# Plan Review: deviceinsight-overlay-stability — PASS 2 ITER 2

## Verdict: REVISE
## Counts: CRITICAL 0 MAJOR 3 MINOR 0 SUGGESTIONS 0

### Findings
#### [MAJOR] Position validation has no defined coordinate contract
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:36-38,42,84,88`
- Problem: Plan requires parser to “bound coordinates” and host to use bounded x/y, but names neither bounds nor authority. Stored window coordinates require current display bounds and measured HUD size; neither exists in `Preferences` parser. No policy covers off-screen legacy values, density/orientation changes, or whether correction is persisted versus only applied.
- Evidence: Current parser candidate has no display/window input (`app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudModel.kt:11-28`). Current service directly loads raw values and applies them to `WindowManager.LayoutParams` (`app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:228-245`); drag persists values without validation (`291-303`). Plan promises first-frame persisted x/y and no visible jump (`83,88`) while only saying “bound coordinates” (`36`).
- Impact: Worker must invent geometry behavior. Invalid or old coordinates can attach HUD offscreen or correction can cause visible first-frame jump.
- Required planner change: Define split contract: parser validates persisted integer format/default only; `OverlayWindowHost` clamps x/y against current display usable bounds after measured `WRAP_CONTENT` HUD dimensions, before first visible attach. State exact edge policy for negative/offscreen and orientation/density changes, plus whether changed position writes back atomically/debounced. Add unit geometry cases and manual saved-position/no-jump cases.

#### [MAJOR] Typed FPS configuration ownership remains unresolved
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:36-37,52-54`
- Problem: Plan says shared `HudRuntimeConfig` includes `FpsMode`, but does not name canonical enum/package or conversion contract. Current `FpsMode` lives in presentation overlay, while shared HUD model stores free-form `String`; data-layer `HudSettingsCache` exposes another free-form string to `FpsRepository`.
- Evidence: `FpsMode` is declared in `presentation/overlay/OverlayViewModel.kt:43`; `HudConfig.fpsMode` is `String` in `ui/caliper/hud/HudModel.kt:11-22`; `HudSettingsCache` collects raw `hudFpsModeFlow` in `data/monitor/HudSettingsCache.kt:18-29`; `FpsRepository` consumes that raw string at `data/fps/FpsRepository.kt:33-36`.
- Impact: Worker must choose cross-layer ownership and can retain invalid/mismatched FPS state despite shared-parser acceptance.
- Required planner change: Name one non-presentation `FpsMode` enum owner (HUD model/config package or data model), parser fallback (`AUTO`), persisted string conversion, and exact updates for `OverlayViewModel`, `OverlayUiState`, `HudSettingsCache`, and `FpsRepository`/`ShellGateway`. Require cache initial snapshot and live collection use same parser flow.

#### [MAJOR] Dialog window configuration omits interaction and decor contract
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:38-39,42,51,87-88`
- Problem: Plan changes overlay transport from raw `WindowManager.LayoutParams` to `Dialog.window`, but only specifies type, size, gravity, position, and blur. It does not specify transparent/no-title decor or required window flags and live attribute updates for existing drag, lock/pass-through, and configuration relayout behavior.
- Evidence: Current behavior depends on raw flags `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_NO_LIMITS`, and dynamic `FLAG_NOT_TOUCHABLE` (`app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:172-188,247-261`). Dialog default window decor/background is not present in current implementation. Plan still requires these interactions unchanged (`42,87-88`) without assigning resulting `WindowManager.LayoutParams` operations to host.
- Impact: Dialog migration can add default dialog chrome/dimming, lose touch pass-through, or fail to relayout on lock/scale/config changes. Worker must rediscover exact contract.
- Required planner change: Specify `OverlayWindowHost` Dialog setup: transparent/no-title HUD decor; exact baseline flags and any deliberately removed flag; how `FLAG_NOT_TOUCHABLE`, x/y, and `WRAP_CONTENT` relayout update `dialog.window.attributes`; which config changes call host update; and idempotent show/dismiss ordering. Add host tests for flags, lock toggling, and update failure logging.

## Next Agent: Planner
## Next Action: Define coordinate geometry/persistence, canonical typed FPS path, and complete Dialog window interaction/decor contract; then resubmit plan.
