# Squawk Code Lookup — Design Spec

## Overview

Tapping a squawk code on the flight details bottom sheet opens a dialog explaining what the code means. All data is bundled locally — no network call required.

## Architecture

Three new files, two modified:

```
composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/
  SquawkInfo.kt          — data class + SquawkSeverity enum
  SquawkCodes.kt         — static Map<String, SquawkInfo> (~30 entries)
  ui/
    SquawkInfoDialog.kt  — pure composable dialog, no ViewModel

Modified:
  ui/AircraftComponents.kt         — add onSquawkClick callback to AircraftSecondaryDetails
  map/FlightDetailsBottomSheet.kt  — hold dialog state, wire up callback
```

No ViewModel, no repository, no coroutines. The lookup is a pure function: `SquawkCodes[code]` returns `SquawkInfo?`.

## Data Model

```kotlin
enum class SquawkSeverity { EMERGENCY, CAUTION, INFO, NORMAL }

data class SquawkInfo(
    val name: String,
    val description: String,
    val severity: SquawkSeverity,
)
```

## Squawk Code Table

All entries in `SquawkCodes` as `mapOf(code to SquawkInfo(...))`. Source: FAA Order 7110.66 (National Beacon Code Allocation Plan) and ICAO Doc 4444. To add codes in the future, add entries to this map.

### Emergency — `SquawkSeverity.EMERGENCY`

| Code | Name | Description |
|------|------|-------------|
| 7700 | General Emergency | Declared emergency; pilot requires immediate ATC assistance |
| 7600 | Lost Communications | Radio failure; aircraft is NORDO (no radio) |
| 7500 | Unlawful Interference | Hijacking or other unlawful interference in progress |

### Caution — `SquawkSeverity.CAUTION`

| Code | Name | Description |
|------|------|-------------|
| 7400 | UAS Lost Link | Unmanned aircraft has lost its control link |
| 7777 | Military Intercept | Fighter intercept in progress; do not simulate |
| 7003 | Head of State | Aircraft carrying head of state (e.g., Air Force One) |
| 0021 | NORAD Hijack | NORAD hijack code (North American military) |
| 0022 | NORAD SAR | NORAD search and rescue (North American military) |

### Info — `SquawkSeverity.INFO`

| Code | Name | Description |
|------|------|-------------|
| 1200 | VFR Flight (US) | Standard VFR squawk in the United States and Canada |
| 7000 | VFR Flight (ICAO) | Standard VFR squawk in Europe and most ICAO regions |
| 2000 | Arriving IFR, No Code | IFR aircraft entering controlled airspace without an assigned code |
| 1000 | Mode C Only | Transponder replying to Mode C altitude queries only; no Mode A code |
| 0000 | Code Not Assigned | Default/null code; transponder on but no code assigned |
| 0010 | Search and Rescue | SAR operation in progress |
| 1202 | VFR Glider | Glider/motorglider operating VFR (US) |
| 1205 | VFR Hang Glider / Balloon | Hang glider, paraglider, or balloon operating VFR (US) |
| 1234 | Practice Approaches | VFR flight conducting practice instrument approaches |
| 1255 | Firefighting | Aerial firefighting or air tanker operations |
| 3100 | Parachute Operations | Parachute jump aircraft |
| 0033 | Military Formation | Military aircraft operating in formation |
| 7001 | Military Low Level | Military low-level flight (UK/ICAO) |
| 7004 | Aerobatics | Aerobatic display or practice |
| 7006 | Military (Special) | Reserved for military special operations |
| 7007 | Military (Special) | Reserved for military special operations |
| 2100 | VFR (some regions) | VFR squawk used in certain ICAO regions outside US/Europe |
| 0100 | Departing Uncontrolled | Departing from an uncontrolled airport (some regions) |

### Normal — `SquawkSeverity.NORMAL`

No entries at this severity; used only for the unknown-code fallback.

## UI — `SquawkInfoDialog`

```
┌─────────────────────────────┐
│  7700                       │  ← title (titleLarge)
│  ● EMERGENCY                │  ← colored chip (omitted for NORMAL)
│                             │
│  General Emergency          │  ← name (bodyLarge, bold)
│  Declared emergency; pilot  │  ← description (bodyMedium)
│  requires immediate ATC     │
│  assistance.                │
│                             │
│              [ Dismiss ]    │
└─────────────────────────────┘
```

- Severity chip colors: EMERGENCY → `AppColors.emergency` (same red used for squawk value color today) · CAUTION → amber · INFO → `MaterialTheme.colorScheme.secondary` · chip omitted for NORMAL/unknown
- Unknown code: title = the code, no chip, name = "Unknown Code", description = "No specific meaning is assigned to this squawk code."
- Dialog is stateless: caller passes `squawk: String` + `onDismiss: () -> Unit`

## `AircraftSecondaryDetails` changes

Add `onSquawkClick: ((String) -> Unit)? = null` parameter. When non-null, wrap the squawk `LabeledValue` in a `Modifier.clickable { onSquawkClick(squawk) }`. No change to existing callers that don't pass the callback — they stay non-interactive.

## `FlightDetailsBottomSheet` changes

```kotlin
var squawkForDialog by remember { mutableStateOf<String?>(null) }
```

Pass `onSquawkClick = { squawkForDialog = it }` to `AircraftSecondaryDetails` (via `FlightDetailsSheetContent`). Show `SquawkInfoDialog` when `squawkForDialog != null`, dismiss sets it back to null.

## Strings

Add to `strings.xml`:
- `squawk_dialog_unknown_name` = "Unknown Code"
- `squawk_dialog_unknown_description` = "No specific meaning is assigned to this squawk code."
- `squawk_dialog_dismiss` = "Dismiss"
- Severity chip labels: `squawk_severity_emergency`, `squawk_severity_caution`, `squawk_severity_info`
- Each code's `name` and `description` are hardcoded in `SquawkCodes.kt` (not string resources — they don't need localization for this app)

## Testing

- **Unit:** `SquawkCodesTest` — verify every severity bucket has ≥1 entry; verify 7700/7600/7500 are EMERGENCY; verify unknown code returns null; verify all three emergency codes present
- **Desktop UI:** `SquawkInfoDialogTest` — renders known code (7700), renders unknown code, dismiss callback fires

## Out of Scope

- Squawk lookup from the aircraft list screen (future — `AircraftSecondaryDetails` already supports it via the optional callback)
- Facility-assigned discrete code ranges (FAA Order 7110.66 Appendix)
- Localization of code names/descriptions
