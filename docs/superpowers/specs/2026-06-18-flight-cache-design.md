# Flight Cache Design

## Goal

Allow the app to cache flight details fetched from AeroAPI so they remain accessible when the device is offline. Two write paths exist: automatic write-through on every successful individual flight lookup, and a manual bulk prefetch that fetches all scheduled flights over a user-defined geographic region for a configurable number of days ahead.

## User-facing behaviour

- **Automatic caching**: every time a flight is looked up by ident and the API succeeds, the result is written to the local cache. No user action required.
- **Offline fallback**: if a flight lookup fails due to a network error and the ident is in the cache, the cached record is returned. The flight details sheet displays a "Cached · last updated X ago" banner so the user knows the data is not live.
- **Prefetch**: in Settings → Flight Cache, the user can create named cache regions. Each region stores a name, a bounding box, and a days-ahead window. Tapping "+" opens a bottom sheet where the user fills in the name, sets the slider (1–14 days), and taps "Select region" to open `MapRegionPickerScreen`. On confirming the region the sheet returns with the bounding box filled in. Tapping "Cache" kicks off the prefetch job and shows progress. Regions can be deleted; deletion removes flights that belong exclusively to that region.
- **Refresh**: deferred to a follow-up. `cached_at` is stored so the UI can show "last updated X ago" and the user can gauge staleness.

## Architecture

```
Settings → FlightCacheScreen
               ↓ add region
           FlightCacheAddSheet  ──→  MapRegionPickerScreen (reused)
               ↓ confirm
           FlightCacheViewModel
               ↓
           PrefetchFlightsUseCase
               ↓
           AeroApi.searchFlights()       ←── new endpoint
               ↓
           FlightCacheRepo               ←── new repository
               ↓
           FlightCache.sq                ←── new SQLDelight schema

LookupFlightUseCase (existing, modified)
    → try AeroApi.getFlight(ident)
        success  → FlightCacheRepo.cacheFlight()  →  return Async.Success(FlightResult(source=LIVE))
        failure  → FlightCacheRepo.getCachedFlight(ident)
                    found     →  return Async.Success(FlightResult(source=CACHED, cachedAt=...))
                    not found →  return Async.Error(...)
```

## FlightResult model

```kotlin
data class FlightResult(
    val flight: Flight,
    val source: Source,
    val cachedAt: Instant? = null,
) {
    enum class Source { LIVE, CACHED }
}
```

`LookupFlightUseCase` return type changes from `Async<Flight>` to `Async<FlightResult>`. All call sites (ViewModel, UI) are updated accordingly. When `source == CACHED`, the flight details sheet shows the banner; when `LIVE`, no banner.

## Data layer — SQLDelight

New file `FlightCache.sq` in `commonMain/sqldelight/`.

### `cached_flight` table

All `Flight` fields stored as typed columns. `FlightAirportRef` (origin and destination) is flattened inline with `origin_` / `destination_` prefixes. `codeshares` and `codeshares_iata` are stored as comma-separated `TEXT` (display-only, never queried). `Instant` fields use an `INTEGER` column adapter (epoch seconds).

```sql
CREATE TABLE cached_flight (
    ident                           TEXT    NOT NULL PRIMARY KEY,
    ident_icao                      TEXT,
    ident_iata                      TEXT,
    fa_flight_id                    TEXT    NOT NULL,
    operator                        TEXT,
    operator_icao                   TEXT,
    operator_iata                   TEXT,
    flight_number                   TEXT,
    registration                    TEXT,
    atc_ident                       TEXT,
    inbound_fa_flight_id            TEXT,
    codeshares                      TEXT,
    codeshares_iata                 TEXT,
    blocked                         INTEGER NOT NULL,
    diverted                        INTEGER NOT NULL,
    cancelled                       INTEGER NOT NULL,
    position_only                   INTEGER NOT NULL,
    origin_code                     TEXT,
    origin_code_icao                TEXT,
    origin_code_iata                TEXT,
    origin_timezone                 TEXT,
    origin_name                     TEXT,
    origin_city                     TEXT,
    destination_code                TEXT,
    destination_code_icao           TEXT,
    destination_code_iata           TEXT,
    destination_timezone            TEXT,
    destination_name                TEXT,
    destination_city                TEXT,
    scheduled_out                   INTEGER,
    estimated_out                   INTEGER,
    actual_out                      INTEGER,
    scheduled_off                   INTEGER,
    estimated_off                   INTEGER,
    actual_off                      INTEGER,
    scheduled_on                    INTEGER,
    estimated_on                    INTEGER,
    actual_on                       INTEGER,
    scheduled_in                    INTEGER,
    estimated_in                    INTEGER,
    actual_in                       INTEGER,
    departure_delay                 INTEGER,
    arrival_delay                   INTEGER,
    progress_percent                INTEGER,
    status                          TEXT    NOT NULL,
    aircraft_type                   TEXT,
    route_distance                  INTEGER,
    filed_airspeed                  INTEGER,
    filed_altitude                  INTEGER,
    filed_ete                       INTEGER,
    route                           TEXT,
    baggage_claim                   TEXT,
    gate_origin                     TEXT,
    gate_destination                TEXT,
    terminal_origin                 TEXT,
    terminal_destination            TEXT,
    type                            TEXT    NOT NULL,
    actual_runway_off               TEXT,
    actual_runway_on                TEXT,
    foresight_predictions_available INTEGER NOT NULL,
    cached_at                       INTEGER NOT NULL,
    individually_cached             INTEGER NOT NULL DEFAULT 0  -- 1 if written by write-through path
);
```

### `flight_cache_region` table

```sql
CREATE TABLE flight_cache_region (
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    min_lat      REAL    NOT NULL,
    max_lat      REAL    NOT NULL,
    min_lon      REAL    NOT NULL,
    max_lon      REAL    NOT NULL,
    days_ahead   INTEGER NOT NULL,
    flight_count INTEGER NOT NULL DEFAULT 0,
    cached_at    INTEGER NOT NULL
);
```

### `region_flight` junction table

Tracks which region each cached flight belongs to, enabling scoped deletion.

```sql
CREATE TABLE region_flight (
    region_id INTEGER NOT NULL REFERENCES flight_cache_region(id) ON DELETE CASCADE,
    ident     TEXT    NOT NULL REFERENCES cached_flight(ident) ON DELETE CASCADE,
    PRIMARY KEY (region_id, ident)
);
```

On region delete: cascade removes `region_flight` rows, then a follow-up query deletes orphaned prefetch-only flights:

```sql
DELETE FROM cached_flight
WHERE individually_cached = 0
AND ident NOT IN (SELECT ident FROM region_flight);
```

Flights written by the write-through path have `individually_cached = 1` and always survive region deletion. If a write-through lookup updates a flight that was also prefetched, `individually_cached` is set to 1 at that point (via INSERT OR REPLACE), so it survives even if all its regions are later deleted.

## FlightCacheRepo interface

```kotlin
interface FlightCacheRepo {
    suspend fun getCachedFlight(ident: String): CachedFlightEntry?   // null = not cached
    suspend fun cacheFlight(flight: Flight)                           // write-through from lookup
    suspend fun bulkCacheFlights(flights: List<Flight>, regionId: Long)
    fun getAllRegions(): Flow<List<CachedRegion>>
    suspend fun insertRegion(name: String, box: BoundingBox, daysAhead: Int): Long  // returns id
    suspend fun updateRegionFlightCount(regionId: Long, count: Int)
    suspend fun deleteRegion(regionId: Long)
}

data class CachedFlightEntry(val flight: Flight, val cachedAt: Instant)
data class CachedRegion(val id: Long, val name: String, val box: BoundingBox, val daysAhead: Int, val flightCount: Int, val cachedAt: Instant)
```

## API layer

One new method on `AeroApi`:

```kotlin
suspend fun searchFlights(
    query: String,          // e.g. "-latlong \"minLat minLon maxLat maxLon\""
    start: String?,         // ISO8601
    end: String?,           // ISO8601
    maxPages: Int? = 1,
    cursor: String? = null,
): FlightResponse
```

Maps to `GET /aeroapi/flights/search`. `PrefetchFlightsUseCase` constructs the `-latlong` expression from `BoundingBox` and computes `start = now`, `end = now + daysAhead`.

## Use cases

### `LookupFlightUseCase` (modified)

Return type changes to `Async<FlightResult>`. Logic:

1. Call `AeroApi.getFlight(ident)`
2. On success: call `FlightCacheRepo.cacheFlight(flight)`, return `Async.Success(FlightResult(flight, LIVE))`
3. On network error: call `FlightCacheRepo.getCachedFlight(ident)`
   - Found: return `Async.Success(FlightResult(flight, CACHED, cachedAt))`
   - Not found: return `Async.Error(...)`

### `PrefetchFlightsUseCase` (new)

```kotlin
interface PrefetchFlightsUseCase {
    suspend fun invoke(name: String, box: BoundingBox, daysAhead: Int): Async<Int>  // returns flight count
}
```

Implementation:

1. Insert region record via `FlightCacheRepo.insertRegion()` → get `regionId`
2. Build query string: `"-latlong \"${box.minLat} ${box.minLon} ${box.maxLat} ${box.maxLon}\""`
3. Call `AeroApi.searchFlights(query, start, end)` (handle pagination if `numPages > 1`)
4. Call `FlightCacheRepo.bulkCacheFlights(flights, regionId)`
5. Call `FlightCacheRepo.updateRegionFlightCount(regionId, flights.size)`
6. Return `Async.Success(flights.size)`

## UI

### Settings entry

New "Flight Cache" row in Settings alongside Offline Maps.

### `FlightCacheScreen`

- Top app bar with back navigation and title "Flight Cache"
- Empty state: icon + "No cached regions" message + description
- Region list: each row shows name, flight count, days ahead, "last updated X ago"
- FAB or top-bar `+` button to add a region
- Swipe-to-delete per row (with confirmation)

### `FlightCacheAddSheet` (bottom sheet)

Fields:
- Name text field
- Days ahead slider (1–14, integer steps, label showing current value e.g. "7 days")
- "Select region" button — opens `MapRegionPickerScreen`; on return, shows a region confirmation chip (e.g. "48.5°N–51.2°N, 2.1°W–5.4°E")
- "Cache" primary button (disabled until name is filled and region selected)
- Progress indicator while prefetch is running
- Error state if prefetch fails

### Flight details sheet (existing, modified)

When `FlightResult.source == CACHED`, show a banner at the top of the sheet:

> Cached · last updated 3 hours ago

No banner when source is `LIVE`.

## Testing

### Unit tests (`commonTest`)
- `LookupFlightUseCaseImpl`: verify live path writes to cache; verify offline path reads from cache and returns `FlightResult(source=CACHED)`; verify error when offline and cache empty
- `PrefetchFlightsUseCaseImpl`: verify region insert, API call, bulk cache write, flight count update
- `FlightCacheRepoImpl`: verify `getCachedFlight` returns null for unknown ident; verify `deleteRegion` removes exclusively-owned flights

### Desktop UI tests (`desktopTest`)
- `FlightCacheScreen`: empty state renders; region list renders with correct metadata
- `FlightCacheAddSheet`: "Cache" button disabled until name + region set; slider label updates
- Flight details sheet: cached banner appears when source is `CACHED`, absent when `LIVE`

## Out of scope (follow-up)

- Region refresh (re-fetching flights for an existing cached region)
- Automatic expiry based on flight arrival time
- Showing cached flights as an overlay on the map when offline
