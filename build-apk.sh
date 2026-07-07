#!/bin/bash
set -e
export ANDROID_HOME=/opt/android-sdk

# Use Java 21 for Gradle
if [ -d /usr/lib/jvm/java-21-openjdk-amd64 ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
elif [ -d /usr/lib/jvm/java-21-openjdk-arm64 ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
fi

export PATH=$JAVA_HOME/bin:$PATH

cd /opt/soundstage-modern
echo "Using Java: $(java -version 2>&1 | head -1)"
echo "Using Gradle: $(./gradlew --version 2>&1 | grep "Gradle " | head -1)"
./gradlew clean assembleDebug "$@"
