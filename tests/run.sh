#!/usr/bin/env bash
set -eu
mkdir -p /tmp/kantago-test
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/MidiSong.java tests/MidiTest.java
java -cp /tmp/kantago-test com.kantago.app.MidiTest
node --check app/src/main/assets/app.js
