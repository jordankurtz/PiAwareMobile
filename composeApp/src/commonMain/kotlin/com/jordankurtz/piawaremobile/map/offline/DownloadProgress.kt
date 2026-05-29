package com.jordankurtz.piawaremobile.map.offline

data class DownloadProgress(
    val regionId: Long,
    val downloaded: Long,
    val total: Long,
) {
    val fraction: Float get() = if (total == 0L) 0f else downloaded.toFloat() / total
}
