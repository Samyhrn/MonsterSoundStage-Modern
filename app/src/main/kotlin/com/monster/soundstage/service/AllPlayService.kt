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
            val wifiOk = isWifiConnected()
            Log.i(TAG, "Connectivity changed, WiFi=$wifiOk, initialized=$initialized")
            if (wifiOk && !initialized) {
                scope.launch { initializeAllPlay() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "=== Service onCreate ===")
        statusMessage.value = "Starting service..."
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        val wifiOk = isWifiConnected()
        Log.i(TAG, "WiFi connected at startup: $wifiOk")
        if (wifiOk) {
            scope.launch { initializeAllPlay() }
        } else {
            statusMessage.value = "Not connected to WiFi"
            updateNotification("Connect to WiFi to use SoundStage")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: action=${intent?.action}")
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
                "RETRY" -> {
                    initialized = false
                    scope.launch { initializeAllPlay() }
                }
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
        } catch (e: Throwable) {
            Log.e(TAG, "Handle intent error", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeAllPlay() {
        if (initialized) return
        initialized = true
        Log.i(TAG, "=== initializeAllPlay() ===")

        try {
            Log.i(TAG, "Step 1: PlayerManager.getInstance()")
            statusMessage.value = "Step 1/3: Loading SDK..."
            playerManager = PlayerManager.getInstance(this)
            Log.i(TAG, "Step 1 OK")

            statusMessage.value = "Step 2/3: Setting up listeners..."
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
                    currentVolume.value = volume
                    maxVolume.value = player.maxVolume
                }
            })

            Log.i(TAG, "Step 3: PlayerManager.start()")
            statusMessage.value = "Step 3/3: Starting AllPlay engine..."
            playerManager?.start()
            Log.i(TAG, "PlayerManager started OK!")

            isConnected.value = true
            statusMessage.value = "Connected - scanning..."
            updateNotification("Connected")
            startScanning()

        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "NATIVE LIB CRASH: ${e.message}", e)
            isConnected.value = false
            statusMessage.value = "Native lib error: ${e.message}"
        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, "INIT CRASH: ${e.message}", e)
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
                val zone = zones.first()
                selectZone(zone)
            }
        } catch (e: Throwable) {
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
                } catch (e: Throwable) {
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
        Log.i(TAG, "Service onDestroy")
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
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: Throwable) {}
    }
}
