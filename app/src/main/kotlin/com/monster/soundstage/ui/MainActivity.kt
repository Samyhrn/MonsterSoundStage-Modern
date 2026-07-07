package com.monster.soundstage.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.monster.soundstage.service.AllPlayService
import com.qualcomm.qce.allplay.controllersdk.PlayerState
import com.qualcomm.qce.allplay.controllersdk.Zone

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var service: AllPlayService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {}
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                ), 100
            )
        }

        val intent = Intent(this, AllPlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme(
                colorScheme = customDarkColorScheme()
            ) {
                SoundStageApp()
            }
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundStageApp() {
    val context = LocalContext.current
    val zones by AllPlayService.speakerList.collectAsState()
    val selected by AllPlayService.selectedZone.collectAsState()
    val state by AllPlayService.playerState.collectAsState()
    val volume by AllPlayService.currentVolume.collectAsState()
    val maxVol by AllPlayService.maxVolume.collectAsState()
    val connected by AllPlayService.isConnected.collectAsState()
    val scanning by AllPlayService.isScanning.collectAsState()

    var showSpeakerList by remember { mutableStateOf(true) }
    var showVolume by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Monster SoundStage", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (connected) {
                        IconButton(onClick = {
                            context.sendBroadcast(Intent("com.monster.soundstage.SCAN"))
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Speaker, "Speakers") },
                    label = { Text("Speakers") },
                    selected = showSpeakerList,
                    onClick = { showSpeakerList = true; showVolume = false }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.VolumeUp, "Volume") },
                    label = { Text("Volume") },
                    selected = showVolume,
                    onClick = { showVolume = true; showSpeakerList = false }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selected != null) {
                PlaybackBar(
                    zone = selected!!,
                    state = state,
                    onPlay = { sendAction(context, "PLAY") },
                    onPause = { sendAction(context, "PAUSE") },
                    onNext = { sendAction(context, "NEXT") },
                    onPrevious = { sendAction(context, "PREV") }
                )
            }

            if (!connected) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, "Disconnected",
                            modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("Not connected to WiFi",
                            color = Color.Gray, fontSize = 16.sp)
                        Text("Make sure you're on the same network as your speaker",
                            color = Color.Gray, fontSize = 14.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            } else if (showSpeakerList) {
                SpeakerList(zones = zones, selected = selected, scanning = scanning) { zone ->
                    sendAction(context, "SELECT_ZONE", "zone_id" to zone.id)
                }
            } else {
                VolumeControl(
                    volume = volume,
                    maxVolume = maxVol,
                    onVolumeChange = { v -> sendAction(context, "SET_VOLUME", "volume" to v) }
                )
            }
        }
    }
}

private fun sendAction(context: Context, action: String, vararg extras: Pair<String, Any>) {
    val intent = Intent(context, AllPlayService::class.java).apply {
        this.action = action
        for ((k, v) in extras) {
            when (v) {
                is Int -> putExtra(k, v)
                is String -> putExtra(k, v)
            }
        }
    }
    context.startService(intent)
}

@Composable
fun PlaybackBar(
    zone: Zone,
    state: PlayerState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(zone.displayName ?: "Speaker",
                fontWeight = FontWeight.Bold, color = Color.White,
                fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White,
                        modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = { if (state == PlayerState.PLAYING) onPause() else onPlay() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (state == PlayerState.PLAYING) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        "Play/Pause",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White,
                        modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpeakerList(
    zones: List<Zone>,
    selected: Zone?,
    scanning: Boolean,
    onSelect: (Zone) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speakers", fontWeight = FontWeight.Bold, color = Color.White,
                    fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (zones.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, "Searching",
                            modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Text("Searching for speakers...",
                            color = Color.Gray, fontSize = 16.sp)
                        Text("This may take a moment",
                            color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }

        items(zones) { zone ->
            val isSelected = zone.id == selected?.id
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelect(zone) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speaker, "",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            zone.displayName ?: "Unknown Speaker",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White, fontSize = 16.sp
                        )
                        Text("Zone", color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, "", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun VolumeControl(
    volume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit
) {
    val effectiveMax = if (maxVolume > 0) maxVolume else 100
    val displayVol = (volume * 100 / effectiveMax)

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.VolumeUp,
            "Volume",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "$displayVol%",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(32.dp))

        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..effectiveMax.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", color = Color.Gray, fontSize = 12.sp)
            Text("$effectiveMax", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

fun customDarkColorScheme(): ColorScheme {
    val primary = Color(0xFFFF5722)
    val surface = Color(0xFF2E2D33)
    val bg = Color(0xFF1C1B1F)
    return lightColorScheme().copy(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.3f),
        onPrimaryContainer = primary,
        secondary = Color(0xFF03DAC5),
        onSecondary = Color.Black,
        background = bg,
        onBackground = Color.White,
        surface = surface,
        onSurface = Color.White,
        surfaceVariant = surface,
        onSurfaceVariant = Color.LightGray,
        outline = Color.Gray,
        error = Color(0xFFCF6679),
        onError = Color.Black,
        inverseSurface = Color.White,
        inverseOnSurface = Color.Black,
        inversePrimary = primary,
    ).also {
        // Force it to be dark
    }
}
