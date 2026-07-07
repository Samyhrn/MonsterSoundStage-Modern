# Monster SoundStage Modern 🎵🔊

[🇫🇷 Français](#fr) · [🇬🇧 English](#en)

---

<a name="fr"></a>
## 🇫🇷 Monster SoundStage Modern

Application Android moderne de contrôle pour les enceintes **Monster SoundStage S3**.

*Reverse-engineered* à partir de l'application originale (v1.3, 2016) qui n'est plus compatible avec Android récent.

### ✨ Fonctionnalités

- ✅ Découverte automatique des enceintes sur le réseau WiFi
- ✅ Contrôle lecture : Play / Pause / Stop / Next / Previous
- ✅ Contrôle du volume (individuel ou zone)
- ✅ Sélection de zone/speaker
- ✅ Interface Material Design 3 (Jetpack Compose)
- ✅ SDK Android 34+ (Android 14)
- ✅ Architecture : Kotlin + Coroutines + StateFlow
- ✅ Open source (MIT)

### 🧠 Comment ça marche

L'enceinte SoundStage S3 utilise le protocole **AllPlay** (Qualcomm) basé sur **AllJoyn** (OCF).

| Composant | Technologie |
|---|---|
| Découverte | AllJoyn multicast (réseau WiFi local) |
| Transport | ARDP (UDP/TCP fiable) |
| Contrôle | D-Bus over TCP |
| SDK natif | Qualcomm AllPlay Controller SDK (C++/JNI) |

### 🛠️ Build

```bash
git clone https://github.com/Samyhrn/MonsterSoundStage-Modern.git
cd MonsterSoundStage-Modern
./build-apk.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

**Prérequis :** Android Studio Hedgehog+, JDK 17+, Android SDK 34

### 📱 Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Permissions requises :**
- `NEARBY_WIFI_DEVICES` (Android 13+) ou `ACCESS_FINE_LOCATION` (Android 12)
- `CHANGE_WIFI_MULTICAST_STATE` (découverte AllJoyn)
- `POST_NOTIFICATIONS` (service foreground)

### 🔬 Reverse Engineering

L'APK originale a été décompilée avec **jadx** + **apktool** (~2690 classes analysées).

**Points clés :**
1. **Package :** `com.embience.allplay.soundstage`
2. **SDK :** Qualcomm AllPlay Controller (AllJoyn/ARDP)
3. **Protocole :** AllJoyn D-Bus over TCP + découverte multicast
4. **Lib native :** `libAllPlayControllerSDK.so` (arm64-v8a, 2.8MB)
5. **Réécriture JNI :** Les classes du SDK ont été réécrites pour être autonomes (sans Skifta, Mortbay, OSGi)

### ⚠️ Problèmes connus

- L'enceinte doit être sur le **même réseau WiFi** que le téléphone
- Certaines fonctionnalités avancées (onboarding, firmware update) ne sont pas implémentées
- Le protocole AllPlay n'est plus maintenu par Qualcomm

### 📄 Licence

**MIT License** — Copyright © 2026 Samyhrn
Utilisation, modification, distribution, usage commercial libres.

### 🤝 Contribuer

PRs bienvenues ! Compatibilité avec d'autres enceintes Monster bienvenue.

---

<a name="en"></a>
## 🇬🇧 Monster SoundStage Modern

Modern Android control app for **Monster SoundStage S3** speakers.

Reverse-engineered from the original Monster SoundStage app (v1.3, 2016) which is no longer compatible with modern Android versions.

### ✨ Features

- ✅ Automatic speaker discovery over WiFi
- ✅ Playback controls: Play / Pause / Stop / Next / Previous
- ✅ Volume control (per-zone)
- ✅ Speaker/zone selection
- ✅ Material Design 3 UI (Jetpack Compose)
- ✅ Android SDK 34+ (Android 14)
- ✅ Clean architecture: Kotlin + Coroutines + StateFlow
- ✅ Open source (MIT)

### 🧠 How It Works

The SoundStage S3 speaker uses **AllPlay** (Qualcomm), built on **AllJoyn** (OCF).

| Component | Technology |
|---|---|
| Discovery | AllJoyn multicast (local WiFi) |
| Transport | ARDP (reliable UDP/TCP) |
| Control | D-Bus over TCP |
| Native SDK | Qualcomm AllPlay Controller SDK (C++/JNI) |

### 🛠️ Build

```bash
git clone https://github.com/Samyhrn/MonsterSoundStage-Modern.git
cd MonsterSoundStage-Modern
./build-apk.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:** Android Studio Hedgehog+, JDK 17+, Android SDK 34

### 📱 Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Required permissions:**
- `NEARBY_WIFI_DEVICES` (Android 13+) or `ACCESS_FINE_LOCATION` (Android 12)
- `CHANGE_WIFI_MULTICAST_STATE` (AllJoyn discovery)
- `POST_NOTIFICATIONS` (foreground service)

### 🔬 Reverse Engineering

The original APK was decompiled using **jadx** + **apktool** (~2690 classes analyzed).

**Key findings:**
1. **Package:** `com.embience.allplay.soundstage`
2. **SDK:** Qualcomm AllPlay Controller (AllJoyn/ARDP)
3. **Protocol:** AllJoyn D-Bus over TCP + multicast discovery
4. **Native lib:** `libAllPlayControllerSDK.so` (arm64-v8a, 2.8MB)
5. **JNI rewrite:** SDK classes rewritten to remove heavy dependencies (Skifta, Mortbay, OSGi)

### ⚠️ Known Issues

- Speaker must be on the **same WiFi network** as the phone
- Advanced features (onboarding, firmware update) are not implemented
- AllPlay protocol is no longer maintained by Qualcomm

### 📄 License

**MIT License** — Copyright © 2026 Samyhrn
Free to use, modify, distribute, and use commercially.

### 🤝 Contributing

PRs welcome! Compatibility with other Monster speakers appreciated.

### 🔗 Links

- [Monster SoundStage on Google Play (archive)](https://apkpure.com/monster-soundstage/com.embience.allplay.soundstage)
- [AllJoyn Framework (OCF)](https://openconnectivity.org/developer/reference-material/alljoyn/)
