package com.monster.soundstage.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.qualcomm.qce.allplay.controllersdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

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
    private var initialized = false

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= 23) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            val info = cm.activeNetworkInfo ?: return false
            return info.type == ConnectivityManager.TYPE_WIFI && info.isConnected
        }
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isWifiConnected()) {
                Log.i(TAG, "WiFi detected via ConnectivityManager")
                if (!initialized) scope.launch { initializeAllPlay() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Initializing..."))

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        if (isWifiConnected()) {
            scope.launch { initializeAllPlay() }
        } else {
            isConnected.value = false
            updateNotification("Connect to WiFi to use SoundStage")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized && isWifiConnected()) {
            scope.launch { initializeAllPlay() }
        }
        handleIntent(intent)
        return START_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        try {
            when (intent.action) {
                "PLAY" -> selectedZone.value?.play()
                "PAUSE" -> selectedZone.value?.pause()
                "NEXT" -> selectedZone.value?.next()
                "PREV" -> selectedZone.value?.previous()
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
        } catch (e: Exception) {
            Log.e(TAG, "Handle intent error", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeAllPlay() {
        if (initialized) return
        initialized = true
        Log.i(TAG, "=== Starting AllPlay initialization ===")

        try {
            Log.i(TAG, "Loading PlayerManager...")
            playerManager = PlayerManager.getInstance(this)

            playerManager?.setControllerEventListener(object : IControllerEventListener {
                override fun onDeviceAdded(device: Device) {
                    Log.i(TAG, "Device added: ${device.displayName}")
                }
                override fun onZoneAdded(zone: Zone) {
                    Log.i(TAG, "Zone added: ${zone.displayName}")
                    updateSpeakerList()
                }
                override fun onZoneRemoved(zone: Zone) {
                    Log.i(TAG, "Zone removed: ${zone.displayName}")
                    updateSpeakerList()
                }
                override fun onZonePlayerStateChanged(zone: Zone, state: PlayerState) {
                    if (zone.id == selectedZone.value?.id) playerState.value = state
                }
                override fun onPlayerVolumeStateChanged(player: Player, volume: Int) {
                    currentVolume.value = volume
                    maxVolume.value = player.maxVolume
                }
            })

            Log.i(TAG, "Calling PlayerManager.start()...")
            playerManager?.start()
            Log.i(TAG, "PlayerManager started successfully!")
            isConnected.value = true
            updateNotification("Connected")
            startScanning()

        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library load FAILED: ${e.message}", e)
            isConnected.value = false
            updateNotification("Library error: native lib incompatible")
        } catch (e: Exception) {
            Log.e(TAG, "Init error: ${e.message}", e)
            isConnected.value = false
            updateNotification("Error: ${e.message}")
        }
    }

    private fun updateSpeakerList() {
        try {
            val zones = playerManager?.availableZones ?: emptyList()
            speakerList.value = zones
            if (zones.isNotEmpty() && selectedZone.value == null) {
                val zone = zones.first()
                selectZone(zone)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateSpeakerList error", e)
        }
    }

    private fun startScanning() {
        scanJob = scope.launch {
            isScanning.value = true
            while (isActive) {
                try {
                    playerManager?.refreshPlayerList()
                    updateSpeakerList()
                } catch (e: Exception) {
                    Log.e(TAG, "Scan error", e)
                }
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

    fun setVolume(volume: Int) {
        selectedZone.value?.let { zone ->
            val clamped = volume.coerceIn(0, zone.maxVolume)
            zone.setVolume(clamped)
            currentVolume.value = clamped
        }
    }

    override fun onDestroy() {
        scanJob?.cancel()
        try {
            playerManager?.stop()
        } catch (_: Exception) {}
        try { unregisterReceiver(connectivityReceiver) } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SoundStage Service",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: Exception) {}
    }
}
