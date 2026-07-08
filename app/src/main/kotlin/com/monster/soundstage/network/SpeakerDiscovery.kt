package com.monster.soundstage.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.io.ByteArrayInputStream
import java.net.*
import javax.xml.parsers.DocumentBuilderFactory

data class SpeakerInfo(
    val uuid: String,
    val name: String,
    val location: String,
    val ip: String,
    val port: Int = 80
)

class SpeakerDiscovery(private val context: Context) {
    companion object {
        const val TAG = "SSDP"
        const val ADDR = "239.255.255.250"
        const val PORT = 1900
        const val TIMEOUT_MS = 4000L
    }

    private var lock: WifiManager.MulticastLock? = null
    private var socket: MulticastSocket? = null

    fun discover(): List<SpeakerInfo> {
        val found = mutableListOf<SpeakerInfo>()
        try {
            Log.i(TAG, "Discovery starting...")
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            lock = wifi.createMulticastLock("ssdp").apply {
                setReferenceCounted(false); acquire()
            }

            socket = MulticastSocket(PORT).apply {
                reuseAddress = true; soTimeout = TIMEOUT_MS.toInt()
                joinGroup(InetAddress.getByName(ADDR))
            }

            for (st in listOf("ssdp:all", "upnp:rootdevice",
                     "urn:schemas-upnp-org:device:MediaRenderer:1")) {
                val msg = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: $ADDR:$PORT\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: $st\r\n\r\n"
                socket?.send(DatagramPacket(msg.toByteArray(), msg.length,
                    InetAddress.getByName(ADDR), PORT))
            }

            val buf = ByteArray(4096)
            val deadline = System.currentTimeMillis() + TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                try {
                    val p = DatagramPacket(buf, buf.size)
                    socket?.receive(p)
                    val text = String(p.data, 0, p.length)
                    val info = parse(text, (p.address.hostAddress ?: ""))
                    if (info != null && found.none { it.uuid == info.uuid }) {
                        Log.i(TAG, "Found: ${info.name} @ ${info.ip}")
                        found.add(info)
                    }
                } catch (_: SocketTimeoutException) { break }
            }
        } catch (e: Exception) { Log.e(TAG, "Error", e) }
        finally {
            try { lock?.release() } catch (_: Exception) {}
            try { socket?.leaveGroup(InetAddress.getByName(ADDR)) } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
        Log.i(TAG, "Found ${found.size} speakers")
        return found
    }

    private fun parse(response: String, srcIp: String): SpeakerInfo? {
        val headers = mutableMapOf<String, String>()
        response.lines().forEach { line ->
            val c = line.indexOf(':')
            if (c > 0) headers[line.substring(0, c).trim().lowercase()] =
                line.substring(c + 1).trim()
        }
        val loc = headers["location"] ?: return null
        val usn = headers["usn"] ?: return null
        val uuid = usn.split("::").first()

        var name = try {
            val conn = URL(loc).openConnection().apply {
                connectTimeout = 2000; readTimeout = 2000
            }
            val xml = conn.getInputStream().bufferedReader().readText()
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray()))
            val el = doc.getElementsByTagName("friendlyName")
            if (el.length > 0) el.item(0).textContent else null
        } catch (_: Exception) { null }

        val url = try { URL(loc) } catch (_: Exception) { return null }
        return SpeakerInfo(uuid, name ?: "Speaker", loc, url.host,
            if (url.port > 0) url.port else 80)
    }
}
