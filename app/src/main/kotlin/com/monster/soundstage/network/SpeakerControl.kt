package com.monster.soundstage.network

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class SpeakerControl(private val speaker: SpeakerInfo) {
    companion object {
        const val TAG = "SpeakerControl"
        const val AV_TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"
        const val RENDERING_CONTROL = "urn:schemas-upnp-org:service:RenderingControl:1"
    }

    private fun soapRequest(service: String, action: String, body: String): String? {
        return try {
            val url = URL("${speaker.location.replace("/device.xml", "")}/upnp/control/$service")
            val soap = """
                <?xml version="1.0"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:$action xmlns:u="$service">
                            $body
                        </u:$action>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            conn.setRequestProperty("SOAPAction", "\"$service#$action\"")
            conn.outputStream.write(soap.toByteArray())
            val resp = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            resp
        } catch (e: Exception) {
            Log.w(TAG, "SOAP $action failed: ${e.message}")
            null
        }
    }

    fun play() {
        soapRequest(AV_TRANSPORT, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
        Log.i(TAG, "Play")
    }

    fun pause() {
        soapRequest(AV_TRANSPORT, "Pause", "<InstanceID>0</InstanceID>")
        Log.i(TAG, "Pause")
    }

    fun stop() {
        soapRequest(AV_TRANSPORT, "Stop", "<InstanceID>0</InstanceID>")
        Log.i(TAG, "Stop")
    }

    fun next() {
        soapRequest(AV_TRANSPORT, "Next", "<InstanceID>0</InstanceID>")
    }

    fun previous() {
        soapRequest(AV_TRANSPORT, "Previous", "<InstanceID>0</InstanceID>")
    }

    fun setVolume(vol: Int) {
        soapRequest(RENDERING_CONTROL, "SetVolume",
            "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>$vol</DesiredVolume>")
        Log.i(TAG, "Volume: $vol")
    }

    fun getVolume(): Int? {
        val resp = soapRequest(RENDERING_CONTROL, "GetVolume",
            "<InstanceID>0</InstanceID><Channel>Master</Channel>")
        if (resp != null) {
            val vol = Regex("<CurrentVolume>(\\d+)</CurrentVolume>").find(resp)
            return vol?.groupValues?.get(1)?.toIntOrNull()
        }
        return null
    }
}
