#!/usr/bin/env bash
set -eu
mkdir -p /tmp/kantago-test
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/MidiSong.java tests/MidiTest.java
java -cp /tmp/kantago-test com.kantago.app.MidiTest
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/SongPack.java tests/SongPackTest.java
java -cp /tmp/kantago-test com.kantago.app.SongPackTest
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/PairingUrl.java tests/PairingUrlTest.java
java -cp /tmp/kantago-test com.kantago.app.PairingUrlTest
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/SongPack.java app/src/main/java/com/kantago/app/ActivatedPack.java tests/ActivatedPackTest.java
java -cp /tmp/kantago-test com.kantago.app.ActivatedPackTest
python3 tools/build_builtin_library.py --output app/src/main/assets/builtin
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/MidiSong.java tests/BuiltinLibraryTest.java
java -cp /tmp/kantago-test com.kantago.app.BuiltinLibraryTest app/src/main/assets/builtin
javac -d /tmp/kantago-test app/src/main/java/com/kantago/app/MidiSong.java app/src/main/java/com/kantago/app/KaraokeCaptions.java tests/KaraokeCaptionsTest.java
java -cp /tmp/kantago-test com.kantago.app.KaraokeCaptionsTest
node --check app/src/main/assets/app.js

node tests/ui-state.cjs
node tests/lyric-contrast.cjs
