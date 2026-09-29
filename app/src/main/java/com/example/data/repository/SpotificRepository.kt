package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.SpotificApiService
import com.example.data.local.DownloadedTrackEntity
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.SpotificDao
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class SpotificRepository(
    private val apiService: SpotificApiService,
    private val dao: SpotificDao,
    private val context: Context
) {
    private val TAG = "SpotificRepository"
    private val okHttpClient = OkHttpClient()

    val favoritesFlow: Flow<List<Track>> = dao.getAllFavorites().map { list ->
        list.map { it.toTrack() }
    }

    val downloadsFlow: Flow<List<Track>> = dao.getAllDownloads().map { list ->
        list.map { it.toTrack() }
    }

    fun isFavoriteFlow(id: String): Flow<Boolean> = dao.isFavorite(id)

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        val isFav = dao.isFavoriteSync(track.id)
        if (isFav) {
            dao.deleteFavorite(track.id)
            false
        } else {
            dao.insertFavorite(FavoriteTrackEntity.fromTrack(track))
            true
        }
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSpotify(query.trim())
            if (response.status && response.result != null) {
                response.result.mapNotNull { item ->
                    if (!item.title.isNullOrBlank()) {
                        Track(
                            title = item.title,
                            artist = item.artist ?: "Unknown Artist",
                            thumbnail = item.thumbnail,
                            url = item.url ?: "",
                            duration = item.duration
                        )
                    } else null
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for '$query': ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Resolves playable/downloadable stream URL.
     * Rule: Try primary (with retries on network failure), and only fall back to the query-based endpoint
     * if the primary genuinely fails or returns bad data.
     */
    suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        // If track is already downloaded locally, return the local file path!
        val downloaded = dao.getDownload(track.id)
        if (downloaded != null && File(downloaded.localFilePath).exists()) {
            return@withContext downloaded.localFilePath
        }

        // 1. Try primary endpoint if track has a Spotify URL
        if (track.url.isNotEmpty()) {
            var retries = 2
            while (retries >= 0) {
                try {
                    val response = apiService.getSpotifyStream(track.url)
                    val streamUrl = response.result?.url
                    if (response.status && !streamUrl.isNullOrBlank()) {
                        Log.d(TAG, "Primary stream resolved: $streamUrl")
                        return@withContext streamUrl
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Primary attempt failed (retries left $retries): ${e.message}")
                    if (retries > 0) delay(400)
                }
                retries--
            }
        }

        // 2. Fallback stream lookup: query-based endpoint
        try {
            val query = "${track.title} ${track.artist}".trim()
            Log.d(TAG, "Attempting fallback for: $query")
            val fallbackResponse = apiService.getSpotifyPlayFallback(query)
            val fallbackUrl = fallbackResponse.result?.downloadUrl
            if (fallbackResponse.status && !fallbackUrl.isNullOrBlank()) {
                Log.d(TAG, "Fallback stream resolved: $fallbackUrl")
                return@withContext fallbackUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback stream lookup failed: ${e.message}", e)
        }

        return@withContext null
    }

    suspend fun downloadTrack(
        track: Track,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val streamUrl = resolveStreamUrl(track)
                ?: return@withContext Result.failure(Exception("Could not resolve stream URL for download"))

            val musicDir = File(context.filesDir, "downloads")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            // Sanitize file name
            val safeName = "${track.artist}_${track.title}"
                .replace(Regex("[^a-zA-Z0-9.-]"), "_")
                .take(60)
            val file = File(musicDir, "${safeName}_${track.id.hashCode()}.mp3")

            val request = Request.Builder().url(streamUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body response"))
            val totalBytes = body.contentLength()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var downloadedBytes: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                if (totalBytes > 0) {
                    val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Save to Room DB
            dao.insertDownload(
                DownloadedTrackEntity.fromTrack(
                    track = track,
                    filePath = file.absolutePath,
                    size = file.length()
                )
            )

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeDownload(trackId: String) = withContext(Dispatchers.IO) {
        val download = dao.getDownload(trackId)
        if (download != null) {
            val file = File(download.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteDownload(trackId)
        }
    }
}
