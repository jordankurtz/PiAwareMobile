package com.jordankurtz.piawaremobile.squawk

object SquawkCodes {
    val all =
        mapOf(
            "7700"
                to
                SquawkInfo(
                    name = "General Emergency",
                    description = "Declared emergency; pilot requires immediate ATC assistance.",
                    severity = SquawkSeverity.EMERGENCY,
                ),
            "7600"
                to
                SquawkInfo(
                    name = "Lost Communications",
                    description = "Radio failure; aircraft NORDO.",
                    severity = SquawkSeverity.EMERGENCY,
                ),
            "7500"
                to
                SquawkInfo(
                    name = "Unlawful Interference",
                    description = "Hijacking or other unlawful interference in progress.",
                    severity = SquawkSeverity.EMERGENCY,
                ),
            "7400"
                to
                SquawkInfo(
                    name = "UAS Lost Link",
                    description = "Unmanned aircraft lost control link.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "7777"
                to
                SquawkInfo(
                    name = "Military Intercept",
                    description = "Fighter intercept in progress.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "7003"
                to
                SquawkInfo(
                    name = "Head of State",
                    description = "Aircraft carrying head of state (e.g., Air Force One).",
                    severity = SquawkSeverity.CAUTION,
                ),
            "0021"
                to
                SquawkInfo(
                    name = "NORAD Hijack",
                    description = "NORAD hijack code (North American).",
                    severity = SquawkSeverity.CAUTION,
                ),
            "0022"
                to
                SquawkInfo(
                    name = "NORAD SAR",
                    description = "NORAD search and rescue (North American).",
                    severity = SquawkSeverity.CAUTION,
                ),
            "0000"
                to
                SquawkInfo(
                    name = "Code Not Assigned",
                    description = "No assigned squawk code.",
                    severity = SquawkSeverity.INFO,
                ),
            "0010"
                to
                SquawkInfo(
                    name = "Search and Rescue",
                    description = "Search and rescue operation.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "1200"
                to
                SquawkInfo(
                    name = "VFR No Radar",
                    description = "VFR flight; no assigned radar code.",
                    severity = SquawkSeverity.INFO,
                ),
            "1202"
                to
                SquawkInfo(
                    name = "VFR Glider",
                    description = "VFR glider operation.",
                    severity = SquawkSeverity.INFO,
                ),
            "1205"
                to
                SquawkInfo(
                    name = "VFR Hang Glider / Balloon",
                    description = "VFR hang glider or balloon operation.",
                    severity = SquawkSeverity.INFO,
                ),
            "1234"
                to
                SquawkInfo(
                    name = "Practice Approaches",
                    description = "Practice approaches and training flights.",
                    severity = SquawkSeverity.INFO,
                ),
            "1255"
                to
                SquawkInfo(
                    name = "Firefighting",
                    description = "Firefighting or aerial application operations.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "3100"
                to
                SquawkInfo(
                    name = "Parachute Operations",
                    description = "Skydiving or parachute jump operations.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "0033"
                to
                SquawkInfo(
                    name = "Military Formation",
                    description = "Military aircraft formation flight.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "7001"
                to
                SquawkInfo(
                    name = "Military Low Level",
                    description = "Military low-level training flight.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "7002"
                to
                SquawkInfo(
                    name = "Military Low Level",
                    description = "Military low-level training flight.",
                    severity = SquawkSeverity.CAUTION,
                ),
            "7004"
                to
                SquawkInfo(
                    name = "Departing Uncontrolled",
                    description = "Departing from an uncontrolled airport.",
                    severity = SquawkSeverity.INFO,
                ),
        )

    operator fun get(code: String): SquawkInfo? = all[code]
}
