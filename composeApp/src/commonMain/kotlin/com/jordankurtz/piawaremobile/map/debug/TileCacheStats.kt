package com.jordankurtz.piawaremobile.map.debug

data class TileCacheStats(
    val diskHits: Int = 0,
    val networkFetches: Int = 0,
    val errors: Int = 0,
) {
    /** Total resolved tile requests (hits + misses, excluding errors). */
    val total: Int get() = diskHits + networkFetches

    /** Fraction of tile requests served from disk cache (0.0–1.0). */
    val hitRate: Float get() = if (total == 0) 0f else diskHits.toFloat() / total
}
