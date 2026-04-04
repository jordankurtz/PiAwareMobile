package com.jordankurtz.piawaremobile.map.debug

data class TileCacheStats(
    val diskHits: Long = 0L,
    val networkFetches: Long = 0L,
    val errors: Long = 0L,
) {
    /** Total resolved tile requests (hits + misses, excluding errors). */
    val total: Long get() = diskHits + networkFetches

    /** Fraction of tile requests served from disk cache (0.0–1.0). */
    val hitRate: Float get() = if (total == 0L) 0f else diskHits.toFloat() / total
}
