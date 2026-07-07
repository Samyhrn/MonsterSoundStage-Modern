package com.monster.soundstage.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.qualcomm.qce.allplay.controllersdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllPlayService : Service() {

    companion object {
        const val TAG = "AllPlayService"
        const val CHANNEL_ID = "allplay_foreground"
        const val NOTIFICATION_ID = 1

        val speakerList = MutableStateFlow<List<Zone>>(emptyList())
        val selectedZone = MutableStateFlow<Zone?>(null)
        val playerState = MutableStateFlow(PlayerState.STOPPED)
        val currentVolume = MutableStateFlow(0)
        val maxVolume = MutableStateFlow(100)
        val isConnected = MutableStateFlow(false)
        val isScanning = MutableStateFlow(false)
    }

    private var playerManager: PlayerManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Initializing..."))
    }

    private var initialized = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized) {
            initialized = true
            scope.launch { initializeAllPlay() }
        }
        handleIntent(intent)
        return START_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val zone = selectedZone.value ?: return
        when (intent.action) {
            "PLAY" -> zone.play()
            "PAUSE" -> zone.pause()
            "NEXT" -> zone.next()
            "PREV" -> zone.previous()
            "SET_VOLUME" -> {
                val vol = intent.getIntExtra("volume", -1)
                if (vol >= 0) setVolume(vol)
            }
            "SELECT_ZONE" -> {
                val zoneId = intent.getStringExtra("zone_id")
                if (zoneId != null) {
                    val found = speakerList.value.find { it.id == zoneId }
                    if (found != null) selectZone(found)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeAllPlay() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT < 31) {
                // proceed
            }

            playerManager = PlayerManager.getInstance(this)
            playerManager?.setControllerEventListener(object : IControllerEventListener {
                override fun onZoneAdded(zone: Zone) {
                    Log.i(TAG, "Zone added: ${zone.displayName}")
                    updateSpeakerList()
                }

                override fun onZoneRemoved(zone: Zone) {
                    Log.i(TAG, "Zone removed: ${zone.displayName}")
                    updateSpeakerList()
                }

                override fun onZonePlayerStateChanged(zone: Zone, state: PlayerState) {
                    if (zone.id == selectedZone.value?.id) {
                        playerState.value = state
                    }
                }

                override fun onPlayerVolumeStateChanged(player: Player, volume: Int) {
                    currentVolume.value = volume
                    maxVolume.value = player.maxVolume
                }

                override fun onDeviceAdded(device: Device) {
                    Log.i(TAG, "Device: ${device.displayName}")
                }
            })

            playerManager?.start()
            isConnected.value = true

            updateNotification("Connected")
            startScanning()

        } catch (e: Exception) {
            Log.e(TAG, "Init error", e)
            isConnected.value = false
            updateNotification("Error: ${e.message}")
        }
    }

    private fun updateSpeakerList() {
        val zones = playerManager?.availableZones ?: emptyList()
        speakerList.value = zones
        if (zones.isNotEmpty() && selectedZone.value == null) {
            selectedZone.value = zones.first()
            val zone = zones.first()
            currentVolume.value = zone.volume
            maxVolume.value = zone.maxVolume
            playerState.value = zone.playerState
        }
    }

    private fun startScanning() {
        scanJob = scope.launch {
            isScanning.value = true
            while (isActive) {
                playerManager?.refreshPlayerList()
                updateSpeakerList()
                delay(5000)
            }
        }
    }

    fun selectZone(zone: Zone) {
        selectedZone.value = zone
        currentVolume.value = zone.volume
        maxVolume.value = zone.maxVolume
        playerState.value = zone.playerState
    }

    fun play() { selectedZone.value?.play() }
    fun pause() { selectedZone.value?.pause() }
    fun stop() { selectedZone.value?.stop() }
    fun next() { selectedZone.value?.next() }
    fun previous() { selectedZone.value?.previous() }

    fun setVolume(volume: Int) {
        selectedZone.value?.let { zone ->
            val clamped = volume.coerceIn(0, zone.maxVolume)
            zone.setVolume(clamped)
            currentVolume.value = clamped
        }
    }

    override fun onDestroy() {
        scanJob?.cancel()
        playerManager?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "SoundStage Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monster SoundStage")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
