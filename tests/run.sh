#!/usr/bin/env bash
set -eu
mkdir -p /tmp/kantago-test
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/MidiSong.java tests/MidiTest.java
java -cp /tmp/kantago-test com.kantago.app.MidiTest
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/SongPack.java tests/SongPackTest.java
java -cp /tmp/kantago-test com.kantago.app.SongPackTest
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/PairingUrl.java tests/PairingUrlTest.java
java -cp /tmp/kantago-test com.kantago.app.PairingUrlTest
node --check app/src/main/assets/app.js
