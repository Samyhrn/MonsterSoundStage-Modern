package com.monster.soundstage.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
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
import com.monster.soundstage.network.SpeakerControl
import com.monster.soundstage.network.SpeakerDiscovery
import com.monster.soundstage.network.SpeakerInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MonsterSS"
        val statusText = MutableStateFlow("Starting...")
        val speakers = MutableStateFlow<List<SpeakerInfo>>(emptyList())
        val selectedSpeaker = MutableStateFlow<SpeakerInfo?>(null)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "=== App launched ===")

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.POST_NOTIFICATIONS
            ), 100)
        }

        setContent { AppUI() }

        scope.launch {
            delay(1000)
            statusText.value = "Discovering speakers..."
            val discovery = SpeakerDiscovery(this@MainActivity)
            val found = discovery.discover()

            if (found.isEmpty()) {
                statusText.value = "No speakers found"
                Log.w(TAG, "No speakers via SSDP")
                // Try a second pass
                delay(2000)
                statusText.value = "Retrying discovery..."
                val found2 = discovery.discover()
                if (found2.isNotEmpty()) {
                    speakers.value = found2
                    statusText.value = "${found2.size} speaker(s) found"
                    Log.i(TAG, "Found ${found2.size} speakers on 2nd pass")
                } else {
                    statusText.value = "No speakers found on WiFi"
                }
            } else {
                speakers.value = found
                statusText.value = "${found.size} speaker(s) found"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI() {
    val status by MainActivity.statusText.collectAsState()
    val speakerList by MainActivity.speakers.collectAsState()
    val selected by MainActivity.selectedSpeaker.collectAsState()

    Scaffold(
        containerColor = Color(0xFF1C1B1F),
        topBar = {
            TopAppBar(
                title = { Text("Monster SoundStage", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1B1F), titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (speakerList.isEmpty()) {
                // Status / discovery screen
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, "", modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text(status, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                        if (status.contains("No speakers") || status.contains("not connected")) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { /* re-trigger discovery */ }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            } else if (selected == null) {
                // Speaker list
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Text("Speakers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(speakerList) { speaker ->
                        SpeakerCard(speaker) {
                            MainActivity.selectedSpeaker.value = speaker
                        }
                    }
                }
            } else {
                // Control view
                ControlView(selected!!)
            }
        }
    }
}

@Composable
fun SpeakerCard(speaker: SpeakerInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2E2D33)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFF5722).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Speaker, "", tint = Color(0xFFFF5722))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(speaker.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                Text(speaker.ip, color = Color.Gray, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, "", tint = Color.Gray)
        }
    }
}

@Composable
fun ControlView(speaker: SpeakerInfo) {
    val scope = rememberCoroutineScope()
    var volume by remember { mutableIntStateOf(50) }
    var isPlaying by remember { mutableStateOf(true) }
    var showVolume by remember { mutableStateOf(false) }

    val control = remember { SpeakerControl(speaker) }

    Column(Modifier.fillMaxSize()) {
        // Header
        Surface(color = Color(0xFF2E2D33), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { MainActivity.selectedSpeaker.value = null }) {
                    Icon(Icons.Default.ArrowBack, "", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(speaker.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(speaker.ip, color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        // Playback controls
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showVolume) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, "",
                        modifier = Modifier.size(80.dp), tint = Color(0xFFFF5722))
                    Spacer(Modifier.height(16.dp))
                    Text("$volume%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(24.dp))
                    Slider(
                        value = volume.toFloat(), valueRange = 0f..100f,
                        onValueChange = {
                            volume = it.toInt()
                            scope.launch { control.setVolume(it.toInt()) }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF5722),
                            activeTrackColor = Color(0xFFFF5722),
                            inactiveTrackColor = Color(0xFF2E2D33)
                        )
                    )
                } else {
                    // Playback buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            scope.launch { control.previous() }
                        }) { Icon(Icons.Default.SkipPrevious, "", tint = Color.White, modifier = Modifier.size(40.dp)) }

                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                scope.launch { if (isPlaying) control.play() else control.pause() }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5722))
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "", modifier = Modifier.size(40.dp), tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            scope.launch { control.next() }
                        }) { Icon(Icons.Default.SkipNext, "", tint = Color.White, modifier = Modifier.size(40.dp)) }
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Toggle volume/playback
                TextButton(onClick = { showVolume = !showVolume }) {
                    Icon(if (showVolume) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.VolumeUp,
                        "", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showVolume) "Playback" else "Volume",
                        color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
