package com.monster.soundstage.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
        val statusMessage = MutableStateFlow("Initializing...")
    }

    private var playerManager: PlayerManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var initialized = false

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isWifiConnected() && !initialized) {
                Log.i(TAG, "WiFi detected, starting init")
                scope.launch { initializeAllPlay() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "=== Service onCreate ===")
        statusMessage.value = "Starting..."

        // Don't use foreground service yet - avoid Android 14 FG restrictions
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))

        registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        if (isWifiConnected()) {
            scope.launch { initializeAllPlay() }
        } else {
            statusMessage.value = "Connect to WiFi"
            updateNotification("Connect to WiFi")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return START_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        try {
            when (intent.action) {
                "RETRY" -> { initialized = false; scope.launch { initializeAllPlay() } }
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
                        speakerList.value.find { it.id == zoneId }?.let { selectZone(it) }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Handle intent error", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun initializeAllPlay() {
        if (initialized) return
        initialized = true
        Log.i(TAG, "=== initializeAllPlay() ===")

        try {
            withTimeout(10000L) {
                Log.i(TAG, "Step 1: Loading native library...")
                statusMessage.value = "Loading SDK..."

                // Test native lib loading FIRST
                try {
                    // Trigger static initializer of PlayerManager to load the native lib
                    val test = PlayerManager.getInstance(this@AllPlayService)
                    // If we get here, lib loaded ok
                    playerManager = test
                    Log.i(TAG, "Native lib loaded OK")
                } catch (e: UnsatisfiedLinkError) {
                    statusMessage.value = "Native library incompatible"
                    throw e
                }

                statusMessage.value = "Setting up..."
                playerManager?.setControllerEventListener(object : IControllerEventListener {
                    override fun onDeviceAdded(device: Device) {
                        Log.i(TAG, "Device added: ${device.displayName}")
                        updateSpeakerList()
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
                        currentVolume.value = volume; maxVolume.value = player.maxVolume
                    }
                })

                statusMessage.value = "Starting engine..."
                playerManager?.start()
                Log.i(TAG, "AllPlay engine started!")
            }

            isConnected.value = true
            statusMessage.value = "Connected"
            updateNotification("Connected")
            startScanning()

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "TIMEOUT: Engine did not respond")
            isConnected.value = false
            statusMessage.value = "Engine timeout - incompatible Android version"
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "NATIVE LIB FAILED: ${e.message}")
            isConnected.value = false
            statusMessage.value = "Native library too old for this Android version"
        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, "INIT CRASH: ${e.message}")
            isConnected.value = false
            statusMessage.value = "SDK crash: ${e.cause?.message ?: e.message}"
        } catch (e: Throwable) {
            Log.e(TAG, "CRASH: ${e.message}", e)
            isConnected.value = false
            statusMessage.value = "Error: ${e.message}"
        }
    }

    private fun updateSpeakerList() {
        try {
            val zones = playerManager?.availableZones ?: emptyList()
            speakerList.value = zones
            if (zones.isNotEmpty() && selectedZone.value == null) {
                selectZone(zones.first())
            }
        } catch (_: Throwable) {}
    }

    private fun startScanning() {
        scanJob = scope.launch {
            isScanning.value = true
            while (isActive) {
                try { playerManager?.refreshPlayerList(); updateSpeakerList() } catch (_: Throwable) {}
                delay(5000)
            }
        }
    }

    private fun selectZone(zone: Zone) {
        selectedZone.value = zone
        currentVolume.value = zone.volume
        maxVolume.value = zone.maxVolume
        playerState.value = zone.playerState
    }

    private fun setVolume(volume: Int) {
        selectedZone.value?.let { zone ->
            val clamped = volume.coerceIn(0, zone.maxVolume)
            zone.setVolume(clamped); currentVolume.value = clamped
        }
    }

    override fun onDestroy() {
        scanJob?.cancel()
        try { playerManager?.stop() } catch (_: Throwable) {}
        try { unregisterReceiver(connectivityReceiver) } catch (_: Throwable) {}
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
        try { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text)) } catch (_: Throwable) {}
    }
}
