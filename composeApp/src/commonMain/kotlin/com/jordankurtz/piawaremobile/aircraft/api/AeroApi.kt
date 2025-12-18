package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.model.FlightResponse

interface AeroApi {

    /**
     * Get information for a flight
     *
     * Returns the flight info status summary for a registration, ident, or fa_flight_id.  If a fa_flight_id is specified then a maximum of 1 flight is returned, unless the flight has been diverted in which case both the original flight and any diversions will be returned with a duplicate fa_flight_id. If a registration or ident is specified, approximately 14 days of recent and scheduled flight information is returned, ordered by `scheduled_out` (or `scheduled_off` if `scheduled_out` is missing) descending. Alternately, specify a start and end parameter to find your flight(s) of interest, including up to 10 days of flight history.
     *
     * @param ident The ident, registration, or fa_flight_id to fetch. If using a flight ident, it is highly recommended to specify ICAO flight ident rather than IATA flight ident to avoid ambiguity and unexpected results. Setting the ident_type can also be used to help disambiguate.
     * @param identType Type of ident provided in the ident parameter. By default, the passed ident is interpreted as a registration if possible. This parameter can force the ident to be interpreted as a designator instead.
     * @param start The starting date range for flight results, comparing against flights' `scheduled_out` field (or `scheduled_off` if `scheduled_out` is missing). The format is ISO8601 date or datetime, and the bound is inclusive. Specified start date must be no further than 10 days in the past and 2 days in the future. If not specified, will default to departures starting approximately 11 days in the past. If using date instead of datetime, the time will default to 00:00:00Z.
     * @param end The ending date range for flight results, comparing against flights' `scheduled_out` field (or `scheduled_off` if `scheduled_out` is missing). The format is ISO8601 date or datetime, and the bound is exclusive. Specified end date must be no further than 10 days in the past and 2 days in the future. If not specified, will default to departures starting approximately 2 days in the future. If using date instead of datetime, the time will default to 00:00:00Z.
     * @param maxPages Maximum number of pages to fetch. This is an upper limit and not a guarantee of how many pages will be returned.
     * @param cursor Opaque value used to get the next batch of data from a paged collection.
     * @return OK
     */
    suspend fun getFlight(
        ident: String,
        identType: String? = null,
        start: String? = null,
        end: String? = null,
        maxPages: Int? = 1,
        cursor: String? =null,
    ): FlightResponse
}
