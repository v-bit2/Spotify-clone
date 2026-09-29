package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    @Json(name = "title") val title: String = "",
    @Json(name = "artist") val artist: String = "",
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "url") val url: String = "", // Spotify track URL
    @Json(name = "duration") val duration: String? = null,
    val streamUrl: String? = null,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false
) {
    val id: String
        get() = if (url.isNotEmpty()) url else "${title}_${artist}".hashCode().toString()
}

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "status") val status: Boolean = false,
    @Json(name = "result") val result: List<TrackApiItem>? = null
)

@JsonClass(generateAdapter = true)
data class TrackApiItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class DownloaderSpotifyResponse(
    @Json(name = "status") val status: Boolean = false,
    @Json(name = "result") val result: DownloaderSpotifyResult? = null
)

@JsonClass(generateAdapter = true)
data class DownloaderSpotifyResult(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class DownloaderPlayResponse(
    @Json(name = "status") val status: Boolean = false,
    @Json(name = "result") val result: DownloaderPlayResult? = null
)

@JsonClass(generateAdapter = true)
data class DownloaderPlayResult(
    @Json(name = "download_url") val downloadUrl: String? = null
)
