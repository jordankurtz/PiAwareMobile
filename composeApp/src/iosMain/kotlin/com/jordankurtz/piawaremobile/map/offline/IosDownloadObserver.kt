package com.jordankurtz.piawaremobile.map.offline

interface IosDownloadObserver {
    fun onDownloadStarting(regionName: String)

    fun onProgress(
        downloaded: Long,
        total: Long,
    )

    fun onComplete(regionName: String)

    fun onFailed(regionName: String)

    fun onCancelled()
}
