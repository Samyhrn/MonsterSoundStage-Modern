# Monster SoundStage Modern 🎵🔊

Application Android moderne de contrôle pour les enceintes **Monster SoundStage S3**.

Reverse-engineered à partir de l'application Monster SoundStage originale (v1.3, 2016) qui n'est plus compatible avec les versions récentes d'Android.

## ✨ Fonctionnalités

- ✅ Découverte automatique des enceintes sur le réseau WiFi
- ✅ Contrôle lecture : Play / Pause / Stop / Next / Previous
- ✅ Contrôle du volume
- ✅ Sélection de zone/speaker
- ✅ Interface Material Design 3 (Jetpack Compose)
- ✅ SDK Android 34+ (Android 14)
- ✅ Architecture propre : Kotlin + Coroutines + StateFlow

## 🧠 Comment ça marche

L'enceinte SoundStage S3 utilise le protocole **AllPlay** (Qualcomm) basé sur **AllJoyn** (AllSeen Alliance / OCF).

| Composant | Technologie |
|---|---|
| Découverte | AllJoyn multicast (réseau local WiFi) |
| Transport | ARDP (AllJoyn Reliable Data Protocol) sur UDP/TCP |
| Contrôle | D-Bus over TCP |
| SDK | Qualcomm AllPlay Controller SDK (JNI + lib native C++) |

L'application charge la librairie native `libAllPlayControllerSDK.so` extraite de l'APK originale, avec les classes JNI minimales réécrites pour être autonomes.

## 🛠️ Build

### Prérequis
- Android Studio Hedgehog (2023.1.1+) ou Gradle 8.5+
- JDK 17+
- Android SDK 34

### Build rapide
```bash
git clone https://github.com/Samyhrn/MonsterSoundStage-Modern.git
cd MonsterSoundStage-Modern
./build-apk.sh
```

L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`

## 📱 Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Permissions requises :**
- `ACCESS_FINE_LOCATION` (Android 12+ pour le scan WiFi)
- `CHANGE_WIFI_MULTICAST_STATE` (découverte AllJoyn)
- `POST_NOTIFICATIONS` (service foreground)

## 🔬 Reverse Engineering

L'APK originale a été décompilée avec **jadx** et **apktool**. Points clés :

1. **Package** : `com.embience.allplay.soundstage`
2. **SDK** : Qualcomm AllPlay Controller SDK (basé sur AllJoyn Framework)
3. **Lib native** : `libAllPlayControllerSDK.so` (C++, arm64-v8a)
4. **Protocole** : AllJoyn / ARDP (AllSeen Alliance / OCF)
5. **2690 classes** analysées au total

Les classes JNI du SDK (`com.qualcomm.qce.allplay.controllersdk`) ont été réécrites pour éliminer les dépendances lourdes (Skifta UPnP, Mortbay Jetty, OSGi).

## 📄 Licence

**MIT License** — Copyright © 2026 Samyhrn

Vous êtes libre de :
- ✅ Utiliser, modifier, distribuer
- ✅ Usage commercial
- ✅ Intégrer dans vos propres projets

Seule condition : conserver la notice de copyright.

## 🤝 Contribuer

PRs bienvenues ! Si vous avez d'autres enceintes Monster compatibles, n'hésitez pas à les ajouter.

## 🔗 Liens

- [Monster SoundStage sur Google Play (archive)](https://apkpure.com/monster-soundstage/com.embience.allplay.soundstage)
- [AllJoyn Framework (OCF)](https://openconnectivity.org/developer/reference-material/alljoyn/)
