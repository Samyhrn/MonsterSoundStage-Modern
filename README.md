# Monster SoundStage Modern 🎵🔊

Modern Android app to control **Monster SoundStage S3** speakers over WiFi.

## ✅ What's new (v2.0)

- **100% pure Kotlin** — no native libraries, no JNI, no outdated SDKs
- **SSDP discovery** — finds speakers on the local network via UPnP
- **UPnP control** — play, pause, stop, next, previous, volume
- **Android 14+ ready** — no foreground service required
- **Material Design 3** — Jetpack Compose UI
- **MIT License** — open source, free to use and modify

## 🛠️ Build

```bash
git clone https://github.com/Samyhrn/MonsterSoundStage-Modern.git
cd MonsterSoundStage-Modern
./build-apk.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Download

Latest APK: http://2.24.8.42:18925/MonsterSoundStage-v2.0.apk

## 🔧 How it works

The app uses **UPnP/DLNA** protocols over WiFi:

1. Sends SSDP M-SEARCH multicast to discover speakers
2. Reads device description XML for friendly name
3. Sends SOAP commands for playback and volume control

## ⚠️ Requirements

- Phone and speaker on the **same WiFi network**
- Location permission (Android 12+) or NEARBY_WIFI_DEVICES (Android 13+)

## 📄 License

MIT — use it, modify it, share it.
