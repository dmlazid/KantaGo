# KantaGo 🎤
**Kanta anytime. Saya anywhere.**

Offline Android videoke with a second phone as the remote. Navy, turquoise and gold interface.

**KantaGo 0.2.4** is an **in-place update of the already installed KantaGo Preview 0.2.2**, keeping its app data and song library. The **app icon label is now just KantaGo**. For compatibility its internal Android application ID remains `com.kantago.app.preview` and the existing signing certificate is unchanged. Do not change this application ID or signing key.

The separate original KantaGo 0.1.0 installation (`com.kantago.app`) cannot be upgraded by this APK because its debug signing key differed. Keep it installed to preserve its private songs. If both icons appear on the home screen, use the phone's **Remove shortcut** option on the original icon (never Uninstall); this only removes a home-screen shortcut, not the installed original app. Both installed apps may still appear in the app drawer, with the same display name. These two applications do not share their private data.

## Download
[**Download the newest test APK from Releases**](https://github.com/dmlazid/KantaGo/releases)

Open the newest release and tap **KantaGo-0.2.4.apk** under Assets. Android 8 or newer. Allow installation from your browser when prompted. This is an early test build, not a Play Store release. Install over the existing **KantaGo Preview 0.2.2** and preserve its data; the installed app will be renamed **KantaGo**. **Do not uninstall your original KantaGo 0.1.0 installation.**

## First test
1. Install the APK on your main Android device.
2. Tap **Start Videoke**, enter **000001**, and tap **Play now**. The original 17-second sound-check demo includes timed lyrics.
3. Import your own `.mid` / `.kar` files or `.mp3` / `.mp4` / `.wav` / `.ogg` / `.m4a` files. For media lyrics, import a matching `.lrc` file with the same basename. Folder imports include subfolders. Extract ZIP packs before importing.
4. Choose **Import ZIP song pack** for a ZIP containing song files you have permission to use, optionally with matching `.lrc` lyric files. Subfolders are supported, but names are flattened and duplicate filenames are skipped. Select a language and optional release year when importing. Use the ⋯ button on a song to correct its tags. Names in `Artist - Title.ext` format are separated automatically.
5. Connect another phone to the same private Wi-Fi or hotspot. Tap **Pair remote** on the main device. Scan its QR with the other phone's camera to open the browser remote, or install this same APK, choose **Connect as Remote**, enter the displayed `IP:port`, and use the six-digit pairing code.
6. Keep the main app open. Song numbers, play/pause, queue, next, replay, volume, key and tempo work from the remote. Restarting the host app changes the session token and pairing code.

No internet is needed after you import the songs. Only metadata and control commands are needed for the remote; audio plays from the main device. No account, activation, ads, analytics, or cloud storage.

## Included in 0.2.4
- Dedicated paired-phone remote modeled on the supplied sample's workflow: digital song-number display, 12-key keypad, four transport buttons, Play Now / Reserve / Play First / Cancel Reservation, and separate Remote / Song List / Queue / Lyrics / Settings tabs.
- Live remote volume, key, tempo, melody and reservation controls. Browser or another KantaGo installation can use the same private Wi-Fi connection. Browser QR pairing uses the phone's camera; there is no in-app QR scanner.
- Remote has a lightweight current-lyrics preview, not a second streamed karaoke video.

- **ZIP song-pack import** with safe streaming extraction, path traversal protection, no overwriting existing songs, and limits of 2,500 entries / 1 GB expanded per pack. ZIP packs require user-owned or licensed files.
- Offline MIDI format 0/1 and KAR playback with a lightweight synthesized sound engine.
- Embedded timed MIDI lyrics, and line-timed LRC for audio/video files.
- Song numbers, search, favorites, editable language/year tags, sortable library, file-type filters, and surprise song.
- Queue reorder (up/down), shuffle, and clear, supported through the paired phone remote.
- Seek in audio/video tracks, adjust shared lyric timing ±0.5 seconds (up to ±10 seconds), and progressive timed-LRC line highlighting.
- MP4 video stream now uses the selected song video rather than the background.
- English, OPM, Bisaya, Foreign/Other filters; release-year/decade filters. **These organize imported music, not a supplied catalog.**
- Key ±6 semitones, speed 50–150%, master volume; melody toggle mutes MIDI channel 1.
- Default vector backdrop plus selectable local background image or muted MP4.
- Local paired remote in a browser or the same APK; QR linking and six-digit code fallback.

## Current limits
- **One original demo only. No commercial OPM, Bisaya, foreign, or English song collection is bundled.** A verified licensed catalog remains outstanding.
- Built-in MIDI tone quality is basic. SoundFont/SF2, full General MIDI instrument fidelity, pitch-bend, and advanced controller support are not implemented yet.
- No microphone input/effects, recording, scoring, online song-pack updates, or Android TV certification yet.
- Keep the app in the foreground for reliable hosting/playback. Host backgrounds and lyrics use the main screen.
- MP4 preview uses a muted video synchronized to the audio engine; device compatibility needs real-phone testing. MIDI seek is not yet supported.
- Local connections use HTTP on your private network. Pair only trusted devices; do not expose the port on the internet.
- The original KantaGo 0.1.x app was signed with a temporary debug key, so these 0.2.x builds cannot upgrade it directly. The 0.2.4 app retains the internal `com.kantago.app.preview` ID to upgrade 0.2.2 in place, but displays only **KantaGo**.
- The signing certificate remains in GitHub Actions cache and its public fingerprint is pinned by `signing/preview-cert.sha256`. If the cache expires, CI **fails** rather than publishing an incompatible build. This is not durable signing storage; set up encrypted-keystore release signing before broad distribution.
- Original 0.1.0 and updated 0.2.x apps have separate libraries. Keep the original installed until its files are safely backed up. Uninstalling can delete its private song copies.

## Build
Java 17, Android SDK 35 and Gradle 8.9:

```sh
bash tests/run.sh
node --check app/src/main/assets/app.js
gradle :app:assembleDebug :app:lintDebug
```
GitHub Actions publishes an installable KantaGo test APK as a release and workflow artifact on each code push to main. It caches the Preview debug signing key and compares its SHA-256 certificate fingerprint (once pinned). Preserve the keystore separately before relying on Preview long term; use GitHub encrypted secrets and a secure keystore for a real production release.

## Assets and credits
- KantaGo sound-check MIDI and lyrics: original demo authored for this project; CC0.
- Microphone/play icon: project-authored vector based on the approved KantaGo design direction.
- Default background: project-authored vector. Uploaded photos and videos can be selected through Settings and are not bundled in this small APK.
- QR encoding: ZXing Core 3.5.3 (Apache-2.0).

For importing songs, use files you have permission to use. A download listing does not automatically provide redistribution rights.
