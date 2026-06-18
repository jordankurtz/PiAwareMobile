# Overlay Zoom Limit Setting

## Summary

Add a boolean setting that, when enabled, constrains the map's zoom limits to the intersection of the zoom ranges of all currently active overlays. The zoom snaps immediately when the setting is toggled on or when an overlay is toggled while the setting is active.

---

## Behavior

- **Setting always available** in Settings, not grayed out when no overlays are active.
- **Only takes effect when at least one overlay is active.** With no active overlays the setting is a no-op and the user's global min/max zoom applies as usual.
- **Multiple active overlays:** intersection (max of all active minZooms, min of all active maxZooms). Further intersected with the user's global min/max.
- **Empty intersection** (two active overlays with non-overlapping ranges): fall back to global min/max — don't crash or lock the map.
- **Immediate snap:** when the setting is toggled on, or when an overlay is toggled while this setting is on, the current zoom is clamped into the new effective range right away.

### Overlay zoom ranges (from ArcGIS service metadata)

| Overlay | minZoom | maxZoom |
|---|---|---|
| FAA VFR Sectional | 8 | 12 |
| FAA IFR Low Enroute | 7 | 12 |
| FAA IFR High Enroute | 5 | 9 |
| Airspace (OpenAIP) | 7 | 14 |

### Example

IFR High (5–9) + IFR Low (7–12) both active, user global min/max = 3–16:
effective range = max(5,7)–min(9,12) = **7–9**

---

## Architecture

### New shared constants file: `OverlayZoomRanges.kt`

Location: `composeApp/src/commonMain/.../map/OverlayZoomRanges.kt`

Defines `IntRange` constants for each overlay, referenced by both `MapLibreMap.kt` (for `TileSetOptions`) and `MapViewModel` (for limit computation). Eliminates duplication.

```
FAA_SECTIONAL_ZOOM_RANGE = 8..12
FAA_IFR_LOW_ZOOM_RANGE   = 7..12
FAA_IFR_HIGH_ZOOM_RANGE  = 5..9
AIRSPACE_ZOOM_RANGE      = 7..14
```

### Settings layer

- `Settings.kt`: add `limitZoomToOverlay: Boolean = false`
- `SettingsService`: add `suspend fun setLimitZoomToOverlay(enabled: Boolean)`
- `SettingsServiceImpl`: implement write
- `SettingsRepository`: add `LIMIT_ZOOM_TO_OVERLAY_KEY` preference key and read/write

### MapViewModel

In `onSettingsLoaded`, after calling `setZoomLimits`, compute and apply the effective limits:

```
fun computeEffectiveZoomLimits(settings): IntRange
  if !settings.limitZoomToOverlay → return settings.minZoomLevel..settings.maxZoomLevel
  
  collect ranges for each active overlay:
    showFaaCharts  → FAA_SECTIONAL_ZOOM_RANGE
    showFaaIfrLow  → FAA_IFR_LOW_ZOOM_RANGE
    showFaaIfrHigh → FAA_IFR_HIGH_ZOOM_RANGE
    showAirspace   → AIRSPACE_ZOOM_RANGE
  
  if none active → return settings.minZoomLevel..settings.maxZoomLevel
  
  intersection = max(all .first)..min(all .last)
  
  if intersection is empty → return settings.minZoomLevel..settings.maxZoomLevel
  
  return intersection.first.coerceAtLeast(settings.minZoomLevel)..
         intersection.last.coerceAtMost(settings.maxZoomLevel)
```

After computing the limits, snap current zoom into range:
```
mapStateController.zoom = mapStateController.zoom.coerceIn(effectiveMin, effectiveMax)
mapStateController.setZoomLimits(effectiveMin, effectiveMax)
```

### Settings UI (`MainScreen.kt`)

Add a `SettingsSwitch` below the existing zoom max input (same map section), after a `HorizontalDivider`:

- Title: "Limit zoom to overlay"
- Description: "Constrains zoom to the range of active overlays"

---

## Testing

### Unit tests (`commonTest`)
- `computeEffectiveZoomLimits` with:
  - `limitZoomToOverlay = false` → global min/max
  - setting on, no overlays active → global min/max
  - setting on, one overlay → that overlay's range intersected with global
  - setting on, two overlays with overlapping ranges → intersection
  - setting on, two overlays with non-overlapping ranges → global min/max fallback

### Desktop UI tests (`desktopTest`)
- `OverlaySheet` unchanged — no new UI there, no new test needed
- Verify the new `SettingsSwitch` renders in `MainScreen` (if there's a desktop test for that screen)
