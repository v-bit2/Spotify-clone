package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "favorites")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val url: String,
    val duration: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        title = title,
        artist = artist,
        thumbnail = thumbnail,
        url = url,
        duration = duration,
        isFavorite = true
    )

    companion object {
        fun fromTrack(track: Track): FavoriteTrackEntity = FavoriteTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            thumbnail = track.thumbnail,
            url = track.url,
            duration = track.duration
        )
    }
}

@Entity(tableName = "downloads")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val url: String,
    val duration: String?,
    val localFilePath: String,
    val fileSize: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        title = title,
        artist = artist,
        thumbnail = thumbnail,
        url = url,
        duration = duration,
        localFilePath = localFilePath,
        isDownloaded = true
    )

    companion object {
        fun fromTrack(track: Track, filePath: String, size: Long): DownloadedTrackEntity = DownloadedTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            thumbnail = track.thumbnail,
            url = track.url,
            duration = track.duration,
            localFilePath = filePath,
            fileSize = size
        )
    }
}
