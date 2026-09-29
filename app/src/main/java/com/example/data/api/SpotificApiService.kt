package com.example.data.api

import com.example.data.model.DownloaderPlayResponse
import com.example.data.model.DownloaderSpotifyResponse
import com.example.data.model.SearchResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SpotificApiService {

    @GET("search/spotify")
    suspend fun searchSpotify(
        @Query("q") query: String
    ): SearchResponse

    @GET("downloader/spotify")
    suspend fun getSpotifyStream(
        @Query("url") url: String
    ): DownloaderSpotifyResponse

    @GET("downloader/spotifyplay")
    suspend fun getSpotifyPlayFallback(
        @Query("q") query: String
    ): DownloaderPlayResponse

    companion object {
        private const val BASE_URL = "https://api.nexray.eu.cc/"

        fun create(): SpotificApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "SpotificAndroid/1.0")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SpotificApiService::class.java)
        }
    }
}
