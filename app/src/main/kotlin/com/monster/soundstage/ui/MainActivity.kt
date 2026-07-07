package com.monster.soundstage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qualcomm.qce.allplay.controllersdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MonsterSoundStage"
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
    private var initAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "=== onCreate ===")
        statusMessage.value = "Starting..."

        // Demander les permissions (non-bloquant)
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissions(perms.toTypedArray(), 100)

        // UI directe - pas de foreground service
        setContent { SoundStageApp() }

        // Init AllPlay dans 2 secondes (temps que l'UI s'affiche)
        scope.launch {
            delay(2000)
            initializeAllPlay()
        }
    }

    private suspend fun initializeAllPlay() {
        if (initAttempted) return
        initAttempted = true
        Log.i(TAG, "=== initializeAllPlay() ===")

        try {
            withTimeout(10000L) {
                statusMessage.value = "Loading SDK (step 1/3)..."
                Log.i(TAG, "Step 1: Loading PlayerManager")
                playerManager = PlayerManager.getInstance(this@MainActivity)
                Log.i(TAG, "Step 1 OK")

                statusMessage.value = "Setting up (step 2/3)..."
                playerManager?.setControllerEventListener(object : IControllerEventListener {
                    override fun onDeviceAdded(d: Device) { Log.i(TAG, "Device: ${d.displayName}"); updateList() }
                    override fun onZoneAdded(z: Zone) { Log.i(TAG, "Zone: ${z.displayName}"); updateList() }
                    override fun onZoneRemoved(z: Zone) { Log.i(TAG, "Zone removed"); updateList() }
                    override fun onZonePlayerStateChanged(z: Zone, s: PlayerState) {
                        if (z.id == selectedZone.value?.id) playerState.value = s
                    }
                    override fun onPlayerVolumeStateChanged(p: Player, v: Int) {
                        currentVolume.value = v; maxVolume.value = p.maxVolume
                    }
                })

                statusMessage.value = "Starting engine (step 3/3)..."
                playerManager?.start()
                Log.i(TAG, "Engine started!")
            }

            isConnected.value = true
            statusMessage.value = "Connected - scanning..."
            startScanning()

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "TIMEOUT: engine not responding")
            statusMessage.value = "Engine timeout - incompatible Android version"
            isConnected.value = false
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "NATIVE LIB FAILED: ${e.message}")
            statusMessage.value = "Native library too old"
            isConnected.value = false
        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, "INIT CRASH: ${e.message}")
            statusMessage.value = "SDK crash: ${e.cause?.message ?: e.message}"
            isConnected.value = false
        } catch (e: Throwable) {
            Log.e(TAG, "CRASH: ${e.message}", e)
            statusMessage.value = "Error: ${e.message}"
            isConnected.value = false
        }
    }

    private fun updateList() {
        try {
            val zones = playerManager?.availableZones ?: emptyList()
            speakerList.value = zones
            if (zones.isNotEmpty() && selectedZone.value == null) {
                val z = zones.first()
                selectedZone.value = z
                currentVolume.value = z.volume
                maxVolume.value = z.maxVolume
                playerState.value = z.playerState
            }
        } catch (_: Throwable) {}
    }

    private fun startScanning() {
        scanJob = scope.launch {
            isScanning.value = true
            while (isActive) {
                try { playerManager?.refreshPlayerList(); updateList() } catch (_: Throwable) {}
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        scanJob?.cancel()
        try { playerManager?.stop() } catch (_: Throwable) {}
        scope.cancel()
        super.onDestroy()
    }
}

// ===================== UI =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundStageApp() {
    val zones by MainActivity.speakerList.collectAsState()
    val selected by MainActivity.selectedZone.collectAsState()
    val state by MainActivity.playerState.collectAsState()
    val volume by MainActivity.currentVolume.collectAsState()
    val maxVol by MainActivity.maxVolume.collectAsState()
    val connected by MainActivity.isConnected.collectAsState()
    val scanning by MainActivity.isScanning.collectAsState()
    val statusMsg by MainActivity.statusMessage.collectAsState()

    var showVolume by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF1C1B1F),
        topBar = {
            TopAppBar(
                title = { Text("Monster SoundStage", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1B1F),
                    titleContentColor = Color.White
                ),
                actions = {
                    if (connected) {
                        IconButton(onClick = { /* refresh */ }) {
                            Icon(Icons.Default.Refresh, "", tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (connected) {
                NavigationBar(containerColor = Color(0xFF2E2D33)) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Speaker, "Speakers") },
                        label = { Text("Speakers") },
                        selected = !showVolume,
                        onClick = { showVolume = false }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Volume") },
                        label = { Text("Volume") },
                        selected = showVolume,
                        onClick = { showVolume = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selected != null && connected) {
                // Playback bar
                Surface(color = Color(0xFF2E2D33), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(selected!!.displayName ?: "Speaker",
                            fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = { selected!!.previous() }) {
                                Icon(Icons.Default.SkipPrevious, "", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(onClick = {
                                if (state == PlayerState.PLAYING) selected!!.pause() else selected!!.play()
                            }, modifier = Modifier.size(64.dp)) {
                                Icon(
                                    if (state == PlayerState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    "", tint = Color(0xFFFF5722), modifier = Modifier.size(40.dp)
                                )
                            }
                            IconButton(onClick = { selected!!.next() }) {
                                Icon(Icons.Default.SkipNext, "", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                if (showVolume) {
                    VolumeView(volume, maxVol) { v ->
                        selected?.let { z ->
                            val c = v.coerceIn(0, z.maxVolume)
                            z.setVolume(c); MainActivity.currentVolume.value = c
                        }
                    }
                } else {
                    SpeakerListView(zones, selected, scanning)
                }
            } else {
                // Status / error screen
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, "", modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text(statusMsg, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        if (statusMsg.startsWith("Error") || statusMsg.startsWith("SDK") || statusMsg.startsWith("Native") || statusMsg.startsWith("Engine")) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                MainActivity.isConnected.value = false
                                MainActivity.statusMessage.value = "Retrying..."
                                // Will trigger on next launch
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpeakerListView(zones: List<Zone>, selected: Zone?, scanning: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speakers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                if (scanning) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFFF5722)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (zones.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, "", modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Text("Searching for speakers...", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            }
        }
        items(zones) { zone ->
            val sel = zone.id == selected?.id
            val bgColor = if (sel) Color(0xFFFF5722).copy(alpha = 0.2f) else Color(0xFF2E2D33)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                onClick = {
                    MainActivity.selectedZone.value = zone
                    MainActivity.currentVolume.value = zone.volume
                    MainActivity.maxVolume.value = zone.maxVolume
                    MainActivity.playerState.value = zone.playerState
                }
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFF5722).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Speaker, "", tint = Color(0xFFFF5722))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(zone.displayName ?: "Unknown", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                        Text("Zone", color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, "", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun VolumeView(volume: Int, maxVolume: Int, onVolumeChange: (Int) -> Unit) {
    val effectiveMax = if (maxVolume > 0) maxVolume else 100
    val percent = volume * 100 / effectiveMax

    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, "", modifier = Modifier.size(80.dp), tint = Color(0xFFFF5722))
        Spacer(Modifier.height(16.dp))
        Text("$percent%", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(32.dp))
        Slider(value = volume.toFloat(), onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..effectiveMax.toFloat(), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5722), activeTrackColor = Color(0xFFFF5722), inactiveTrackColor = Color(0xFF2E2D33)))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", color = Color.Gray, fontSize = 12.sp); Text("$effectiveMax", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
