# Squawk Code Lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tapping a squawk code in the flight details bottom sheet opens a dialog explaining the code's meaning, using a bundled local lookup table.

**Architecture:** A static `SquawkCodes` object maps 4-digit string codes to `SquawkInfo` (name, description, severity). A pure composable `SquawkInfoDialog` renders the lookup result. `AircraftSecondaryDetails` gains an optional click callback; `FlightDetailsBottomSheet` holds dialog state and wires it up.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Mokkery (not needed — no mocking required), `runComposeUiTest` for desktop UI tests.

## Global Constraints

- All Kotlin must pass `./gradlew :composeApp:ktlintCheck` — run ktlintFormat to auto-fix formatting
- All Kotlin must pass `./gradlew :composeApp:detekt` — max 4 return statements per function (`ReturnCount` rule)
- Unit tests: `./gradlew :composeApp:testDebugUnitTest`
- Desktop UI tests: `./gradlew :composeApp:desktopTest`
- No comments except where WHY is non-obvious
- No new ViewModel, repository, or coroutines — this is pure local data
- New files go in `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/`
- Tests go in `composeApp/src/commonTest/` and `composeApp/src/desktopTest/`
- Severity chip omitted for `NORMAL` / unknown codes; unknown codes still open the dialog

---

### Task 1: Data model and squawk code table

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkInfo.kt`
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodes.kt`
- Create: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodesTest.kt`

**Interfaces:**
- Produces: `SquawkSeverity`, `SquawkInfo`, `SquawkCodes[code: String]: SquawkInfo?`

- [ ] **Step 1: Write the failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodesTest.kt
package com.jordankurtz.piawaremobile.squawk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SquawkCodesTest {

    @Test
    fun `7700 is EMERGENCY`() {
        val info = SquawkCodes["7700"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `7600 is EMERGENCY`() {
        val info = SquawkCodes["7600"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `7500 is EMERGENCY`() {
        val info = SquawkCodes["7500"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `1200 is INFO`() {
        val info = SquawkCodes["1200"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.INFO, info.severity)
    }

    @Test
    fun `7400 is CAUTION`() {
        val info = SquawkCodes["7400"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.CAUTION, info.severity)
    }

    @Test
    fun `unknown code returns null`() {
        assertNull(SquawkCodes["9999"])
    }

    @Test
    fun `at least one entry per severity bucket`() {
        val severities = SquawkCodes.all.values.map { it.severity }.toSet()
        assertTrue(SquawkSeverity.EMERGENCY in severities)
        assertTrue(SquawkSeverity.CAUTION in severities)
        assertTrue(SquawkSeverity.INFO in severities)
    }

    @Test
    fun `all entries have non-blank name and description`() {
        SquawkCodes.all.forEach { (code, info) ->
            assertTrue(info.name.isNotBlank(), "Code $code has blank name")
            assertTrue(info.description.isNotBlank(), "Code $code has blank description")
        }
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SquawkCodesTest"
```

Expected: FAIL with "Unresolved reference: SquawkCodes"

- [ ] **Step 3: Create `SquawkInfo.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkInfo.kt
package com.jordankurtz.piawaremobile.squawk

enum class SquawkSeverity { EMERGENCY, CAUTION, INFO, NORMAL }

data class SquawkInfo(
    val name: String,
    val description: String,
    val severity: SquawkSeverity,
)
```

- [ ] **Step 4: Create `SquawkCodes.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodes.kt
package com.jordankurtz.piawaremobile.squawk

object SquawkCodes {

    val all: Map<String, SquawkInfo> = mapOf(
        // Emergency
        "7700" to SquawkInfo(
            name = "General Emergency",
            description = "Declared emergency; pilot requires immediate ATC assistance.",
            severity = SquawkSeverity.EMERGENCY,
        ),
        "7600" to SquawkInfo(
            name = "Lost Communications",
            description = "Radio failure; aircraft is NORDO (no radio).",
            severity = SquawkSeverity.EMERGENCY,
        ),
        "7500" to SquawkInfo(
            name = "Unlawful Interference",
            description = "Hijacking or other unlawful interference in progress.",
            severity = SquawkSeverity.EMERGENCY,
        ),
        // Caution
        "7400" to SquawkInfo(
            name = "UAS Lost Link",
            description = "Unmanned aircraft has lost its control link.",
            severity = SquawkSeverity.CAUTION,
        ),
        "7777" to SquawkInfo(
            name = "Military Intercept",
            description = "Fighter intercept in progress; do not simulate.",
            severity = SquawkSeverity.CAUTION,
        ),
        "7003" to SquawkInfo(
            name = "Head of State",
            description = "Aircraft carrying head of state (e.g., Air Force One).",
            severity = SquawkSeverity.CAUTION,
        ),
        "0021" to SquawkInfo(
            name = "NORAD Hijack",
            description = "NORAD hijack code (North American military).",
            severity = SquawkSeverity.CAUTION,
        ),
        "0022" to SquawkInfo(
            name = "NORAD SAR",
            description = "NORAD search and rescue (North American military).",
            severity = SquawkSeverity.CAUTION,
        ),
        // Info
        "1200" to SquawkInfo(
            name = "VFR Flight (US)",
            description = "Standard VFR squawk in the United States and Canada.",
            severity = SquawkSeverity.INFO,
        ),
        "7000" to SquawkInfo(
            name = "VFR Flight (ICAO)",
            description = "Standard VFR squawk in Europe and most ICAO regions.",
            severity = SquawkSeverity.INFO,
        ),
        "2000" to SquawkInfo(
            name = "Arriving IFR, No Code",
            description = "IFR aircraft entering controlled airspace without an assigned code.",
            severity = SquawkSeverity.INFO,
        ),
        "1000" to SquawkInfo(
            name = "Mode C Only",
            description = "Transponder replying to Mode C altitude queries only; no Mode A code.",
            severity = SquawkSeverity.INFO,
        ),
        "0000" to SquawkInfo(
            name = "Code Not Assigned",
            description = "Default code; transponder on but no discrete code assigned.",
            severity = SquawkSeverity.INFO,
        ),
        "0010" to SquawkInfo(
            name = "Search and Rescue",
            description = "SAR operation in progress.",
            severity = SquawkSeverity.INFO,
        ),
        "1202" to SquawkInfo(
            name = "VFR Glider",
            description = "Glider or motorglider operating VFR (US).",
            severity = SquawkSeverity.INFO,
        ),
        "1205" to SquawkInfo(
            name = "VFR Hang Glider / Balloon",
            description = "Hang glider, paraglider, or balloon operating VFR (US).",
            severity = SquawkSeverity.INFO,
        ),
        "1234" to SquawkInfo(
            name = "Practice Approaches",
            description = "VFR flight conducting practice instrument approaches.",
            severity = SquawkSeverity.INFO,
        ),
        "1255" to SquawkInfo(
            name = "Firefighting",
            description = "Aerial firefighting or air tanker operations.",
            severity = SquawkSeverity.INFO,
        ),
        "3100" to SquawkInfo(
            name = "Parachute Operations",
            description = "Parachute jump aircraft.",
            severity = SquawkSeverity.INFO,
        ),
        "0033" to SquawkInfo(
            name = "Military Formation",
            description = "Military aircraft operating in formation.",
            severity = SquawkSeverity.INFO,
        ),
        "7001" to SquawkInfo(
            name = "Military Low Level",
            description = "Military low-level flight (UK/ICAO).",
            severity = SquawkSeverity.INFO,
        ),
        "7002" to SquawkInfo(
            name = "Military Low Level",
            description = "Military low-level flight (UK/ICAO).",
            severity = SquawkSeverity.INFO,
        ),
        "7004" to SquawkInfo(
            name = "Aerobatics",
            description = "Aerobatic display or practice.",
            severity = SquawkSeverity.INFO,
        ),
        "7006" to SquawkInfo(
            name = "Military (Special)",
            description = "Reserved for military special operations.",
            severity = SquawkSeverity.INFO,
        ),
        "7007" to SquawkInfo(
            name = "Military (Special)",
            description = "Reserved for military special operations.",
            severity = SquawkSeverity.INFO,
        ),
        "2100" to SquawkInfo(
            name = "VFR (Regional)",
            description = "VFR squawk used in certain ICAO regions outside US/Europe.",
            severity = SquawkSeverity.INFO,
        ),
        "0100" to SquawkInfo(
            name = "Departing Uncontrolled",
            description = "Departing from an uncontrolled airport (some regions).",
            severity = SquawkSeverity.INFO,
        ),
    )

    operator fun get(code: String): SquawkInfo? = all[code]
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SquawkCodesTest"
```

Expected: 8 tests pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkInfo.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodes.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkCodesTest.kt
git commit -m "Add squawk code data model and lookup table"
```

---

### Task 2: `SquawkInfoDialog` composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/ui/SquawkInfoDialog.kt`
- Create: `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkInfoDialogTest.kt`

**Interfaces:**
- Consumes: `SquawkCodes[code]`, `SquawkInfo`, `SquawkSeverity`, `AppTheme.colors` from `com.jordankurtz.piawaremobile.ui.AppTheme`
- Produces:
  ```kotlin
  @Composable
  fun SquawkInfoDialog(squawk: String, onDismiss: () -> Unit)
  ```

**Severity chip colors:**
- `EMERGENCY` → `AppTheme.colors.aircraftEmergency`
- `CAUTION` → `AppTheme.colors.caution`
- `INFO` → `MaterialTheme.colorScheme.secondary`
- `NORMAL` / unknown (null) → no chip shown

- [ ] **Step 1: Write the failing desktop UI tests**

```kotlin
// composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/squawk/SquawkInfoDialogTest.kt
package com.jordankurtz.piawaremobile.squawk

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.ui.Theme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SquawkInfoDialogTest {

    @Test
    fun `known code shows name and description`() = runComposeUiTest {
        setContent {
            Theme {
                SquawkInfoDialog(squawk = "7700", onDismiss = {})
            }
        }
        onNodeWithText("7700").assertExists()
        onNodeWithText("General Emergency").assertExists()
        onNodeWithText("Declared emergency; pilot requires immediate ATC assistance.").assertExists()
    }

    @Test
    fun `unknown code shows fallback text`() = runComposeUiTest {
        setContent {
            Theme {
                SquawkInfoDialog(squawk = "9999", onDismiss = {})
            }
        }
        onNodeWithText("9999").assertExists()
        onNodeWithText("Unknown Code").assertExists()
        onNodeWithText("No specific meaning is assigned to this squawk code.").assertExists()
    }

    @Test
    fun `dismiss button fires callback`() = runComposeUiTest {
        var dismissed = false
        setContent {
            Theme {
                SquawkInfoDialog(squawk = "7700", onDismiss = { dismissed = true })
            }
        }
        onNodeWithText("Dismiss").performClick()
        assertTrue(dismissed)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew :composeApp:desktopTest --tests "*.SquawkInfoDialogTest"
```

Expected: FAIL with "Unresolved reference: SquawkInfoDialog"

- [ ] **Step 3: Determine the Theme composable import**

The app wraps composables in `Theme { }` for tests. Check the import used in existing desktop tests — it is `com.jordankurtz.piawaremobile.ui.Theme`. Verify with:

```bash
grep -r "fun Theme" composeApp/src/commonMain/kotlin/
```

Expected output confirms the function is in `com.jordankurtz.piawaremobile.ui.Theme.kt`.

- [ ] **Step 4: Create `SquawkInfoDialog.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/ui/SquawkInfoDialog.kt
package com.jordankurtz.piawaremobile.squawk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.squawk.SquawkCodes
import com.jordankurtz.piawaremobile.squawk.SquawkSeverity
import com.jordankurtz.piawaremobile.ui.AppTheme

@Composable
fun SquawkInfoDialog(
    squawk: String,
    onDismiss: () -> Unit,
) {
    val info = SquawkCodes[squawk]
    val name = info?.name ?: "Unknown Code"
    val description = info?.description ?: "No specific meaning is assigned to this squawk code."
    val chipColor = info?.severity?.let { severityColor(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(squawk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                chipColor?.let { color ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(info!!.severity.label) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = color.copy(alpha = 0.15f),
                            labelColor = color,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
}

@Composable
private fun severityColor(severity: SquawkSeverity): Color? =
    when (severity) {
        SquawkSeverity.EMERGENCY -> AppTheme.colors.aircraftEmergency
        SquawkSeverity.CAUTION -> AppTheme.colors.caution
        SquawkSeverity.INFO -> MaterialTheme.colorScheme.secondary
        SquawkSeverity.NORMAL -> null
    }
```

- [ ] **Step 5: Add `label` property to `SquawkSeverity`**

Modify `SquawkInfo.kt` to add a display label to the enum:

```kotlin
package com.jordankurtz.piawaremobile.squawk

enum class SquawkSeverity(val label: String) {
    EMERGENCY("Emergency"),
    CAUTION("Caution"),
    INFO("Info"),
    NORMAL(""),
}

data class SquawkInfo(
    val name: String,
    val description: String,
    val severity: SquawkSeverity,
)
```

- [ ] **Step 6: Run tests to confirm they pass**

```bash
./gradlew :composeApp:desktopTest --tests "*.SquawkInfoDialogTest"
```

Expected: 3 tests pass.

- [ ] **Step 7: Run all tests to confirm nothing broke**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/squawk/ \
        composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/squawk/
git commit -m "Add SquawkInfoDialog composable"
```

---

### Task 3: Wire up tap interaction in the flight details sheet

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/ui/AircraftComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/FlightDetailsBottomSheet.kt`

**Interfaces:**
- Consumes: `SquawkInfoDialog(squawk: String, onDismiss: () -> Unit)` from Task 2
- Produces: no new public API — changes are internal to the bottom sheet

**Context:**
`AircraftSecondaryDetails` (in `AircraftComponents.kt`) renders the squawk via `LabeledValue`. It is called from:
- `AircraftTab` (private, in `FlightDetailsBottomSheet.kt`) — the "Aircraft" tab; this is where the dialog is wired
- `FlightDetailsBottomSheet` (top-level, same file) — called directly in the non-Success states
- `TabletAircraftDetails.kt` and `AircraftListScreen.kt` — not wired for now (callback is optional)

The dialog state (`squawkForDialog: String?`) lives in `FlightDetailsSheetContent` and is passed down through `AircraftTab`.

**Current `AircraftSecondaryDetails` signature** (line 144 in `AircraftComponents.kt`):
```kotlin
@Composable
fun AircraftSecondaryDetails(
    aircraft: Aircraft,
    modifier: Modifier = Modifier,
    squawkValueColor: Color = Color.Unspecified,
)
```

**Current `AircraftTab` signature** (line 372 in `FlightDetailsBottomSheet.kt`):
```kotlin
@Composable
private fun AircraftTab(
    aircraft: Aircraft?,
    flight: Flight,
)
```

**Current `FlightDetailsSheetContent` signature** (line 124 in `FlightDetailsBottomSheet.kt`):
```kotlin
@Composable
fun FlightDetailsSheetContent(
    aircraft: Aircraft?,
    flightDetails: Async<Flight>,
    isFollowing: Boolean = false,
    userLocation: Location? = null,
    onFollowToggle: () -> Unit = {},
    onOpenFlightPage: () -> Unit = {},
)
```

- [ ] **Step 1: Add `onSquawkClick` to `AircraftSecondaryDetails`**

In `AircraftComponents.kt`, update the function signature and make the squawk `LabeledValue` clickable when a callback is provided. The squawk is currently rendered at the end of the `aircraft.squawk?.let { }` block. Add `clickable` modifier and import.

Replace the existing `AircraftSecondaryDetails` function (lines 144–170) with:

```kotlin
@Composable
fun AircraftSecondaryDetails(
    aircraft: Aircraft,
    modifier: Modifier = Modifier,
    squawkValueColor: Color = Color.Unspecified,
    onSquawkClick: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aircraft.baroRate?.let {
            LabeledValue(
                label = stringResource(Res.string.label_vertical_speed),
                value = stringResource(Res.string.value_vertical_speed_fpm, it),
            )
        }
        aircraft.squawk?.let { squawk ->
            val squawkModifier = if (onSquawkClick != null) {
                Modifier.clickable { onSquawkClick(squawk) }
            } else {
                Modifier
            }
            LabeledValue(
                label = stringResource(Res.string.label_squawk),
                value = squawk,
                valueColor = squawkValueColor,
                modifier = squawkModifier,
            )
        }
    }
}
```

Add the missing import at the top of `AircraftComponents.kt`:
```kotlin
import androidx.compose.foundation.clickable
```

- [ ] **Step 2: Add `onSquawkClick` to `AircraftTab` and wire state in `FlightDetailsSheetContent`**

In `FlightDetailsBottomSheet.kt`, make two changes:

**Change 1** — update `AircraftTab` to accept and forward the callback:

```kotlin
@Composable
private fun AircraftTab(
    aircraft: Aircraft?,
    flight: Flight,
    onSquawkClick: ((String) -> Unit)? = null,
) {
    val emergencySquawkCodes = setOf("7500", "7600", "7700")
    val emergencyColor = AppTheme.colors.aircraftEmergency
    val defaultSquawkColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        FlightAircraftDetails(flight = flight)
        Spacer(modifier = Modifier.height(8.dp))
        aircraft?.let {
            val squawkColor = if (it.squawk in emergencySquawkCodes) emergencyColor else defaultSquawkColor
            AircraftSecondaryDetails(
                aircraft = it,
                squawkValueColor = squawkColor,
                onSquawkClick = onSquawkClick,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AircraftSignalDetails(aircraft = it)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
```

**Change 2** — add dialog state to `FlightDetailsSheetContent` and show `SquawkInfoDialog`. Add `var squawkForDialog by remember { mutableStateOf<String?>(null) }` at the top of the function body, pass `onSquawkClick = { squawkForDialog = it }` to `AircraftTab` inside the `Async.Success` branch, and show the dialog at the bottom of the composable.

The full updated `FlightDetailsSheetContent` — only the changed parts shown (the rest is unchanged):

At the top of `FlightDetailsSheetContent`, after `var tabIndex by remember { mutableStateOf(0) }`, add:
```kotlin
var squawkForDialog by remember { mutableStateOf<String?>(null) }
```

Inside the `is Async.Success` branch, update the `AircraftTab` call (inside `AnimatedContent`, `index == 1` branch):
```kotlin
1 -> AircraftTab(aircraft, flight, onSquawkClick = { squawkForDialog = it })
```

After the closing brace of the top-level `Column` in `FlightDetailsSheetContent`, add:
```kotlin
squawkForDialog?.let { squawk ->
    SquawkInfoDialog(
        squawk = squawk,
        onDismiss = { squawkForDialog = null },
    )
}
```

Add the import at the top of `FlightDetailsBottomSheet.kt`:
```kotlin
import com.jordankurtz.piawaremobile.squawk.ui.SquawkInfoDialog
```

- [ ] **Step 3: Run ktlintFormat to fix any formatting issues**

```bash
./gradlew :composeApp:ktlintFormat
```

- [ ] **Step 4: Run all checks**

```bash
./gradlew :composeApp:ktlintCheck :composeApp:detekt :composeApp:testDebugUnitTest :composeApp:desktopTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/ui/AircraftComponents.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/FlightDetailsBottomSheet.kt
git commit -m "Wire squawk tap to SquawkInfoDialog in flight details sheet"
```
