package com.example

import android.app.Application
import com.example.channel.SpotificPlatformChannel
import com.example.data.api.SpotificApiService
import com.example.data.local.SpotificDatabase
import com.example.data.repository.SpotificRepository
import com.example.playback.PlaybackController

class SpotificApplication : Application() {

    lateinit var database: SpotificDatabase
        private set

    lateinit var apiService: SpotificApiService
        private set

    lateinit var repository: SpotificRepository
        private set

    lateinit var playbackController: PlaybackController
        private set

    lateinit var platformChannel: SpotificPlatformChannel
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = SpotificDatabase.getInstance(this)
        apiService = SpotificApiService.create()
        repository = SpotificRepository(apiService, database.spotificDao(), this)
        playbackController = PlaybackController(this)
        platformChannel = SpotificPlatformChannel(this, playbackController)
    }

    companion object {
        lateinit var instance: SpotificApplication
            private set
    }
}
