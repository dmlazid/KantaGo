# KantaGo 🎤
**Kanta anytime. Saya anywhere.**

Offline Android videoke with a second phone as the remote. Navy, turquoise and gold interface.

**KantaGo 0.5.2 – playback and remote fixes.** Version 0.5.2 updates 0.5.1 in place using the same package and signing certificate. Earlier incompatible builds remain separate.

**Recovery installation introduced in 0.5.1.** This version has the normal visible app name **KantaGo**, but an intentionally new Android package ID `com.kantago.player`. Android installs it **beside** earlier KantaGo installations. This is necessary because older builds were signed with different keys and cannot update each other.

**Do not uninstall older KantaGo** unless you have independently backed up imported songs and want to remove its old app icon. Earlier song data stays safely inside its original installation; it **cannot automatically transfer** to the newly installed KantaGo. You can import original copies of MIDI/KAR/ZIP/7z files again, or use Settings → Export ZIP in older versions that have that feature.

The newer installation is designed to update in place from version 0.5.1 onward **only while its pinned signing key remains available**. The current GitHub Actions cache is not permanent signing-key storage; configure encrypted long-term signing storage before distributing widely.

## APK signing compatibility incident (October 11, 2026)

**Important for existing installs:** Android requires exactly the same application ID and APK signing certificate to update an installed app without uninstalling it. Early KantaGo debug APKs **were incorrectly signed using Android Gradle Plugin's default signing key at `/home/runner/.config/.android/debug.keystore`**, while the workflow checked a DIFFERENT key at `/home/runner/.android/debug.keystore`. Because the actual signing key changed per CI runner, GitHub Releases from builds 6, 7, 8, 17 and 19 were signed with different certificates. The keystore fingerprint checks in those workflows were therefore insufficient.

The corrective change explicitly binds Gradle's debug signing configuration to the keystore whose fingerprint is pinned in `signing/preview-cert.sha256`, and verifies the **actual signed APK** using Android `apksigner verify --print-certs`. A different certificate causes CI to fail before publication.

**This repair is for future builds only.** It cannot make a new APK update an old install signed with a transient, discarded private key. Users must **not uninstall** an app containing valuable imported songs before backing up the song library. On existing versions that offer it, use Settings → Save backup ZIP first. For earlier versions, recover existing files through their original sources, or use advanced ADB backup methods where available, before any reinstall. A separate future app ID can install side-by-side but will not automatically inherit private app data.

The pinned debug key currently survives through GitHub Actions caching, which is not durable or suitable for production app updates. Before broad distribution, migrate the same private key into encrypted GitHub Actions secrets / secure keystore storage, and keep the expected signer fingerprint unchanged.

## Readable karaoke lyrics in version 0.7.1
Karaoke lyrics now display like a conventional videoke player: **white upcoming words and bright yellow sung words**, outlined dark over a translucent black caption strip near the bottom of the moving video. The next lyric line is large and legible. The animated footage stays visible above the captions, and the remote displays the same contrast. This intentionally uses solid text colors rather than Android WebView's unreliable text gradient clipping.

The original SongHub brand, assets, and proprietary videos are not included; KantaGo independently follows the familiar high-contrast karaoke presentation.

## Animated karaoke stage in version 0.7.0
KantaGo now includes two **original moving video backgrounds** bundled into its APK: neon dance room and rooftop sunset, both showing an animated geometric dancer. The graphics are procedural creations, not copied clips from SongHub or commercial karaoke videos. Choose Auto, Neon dance, or Sunset stage in Settings. The videos are silent; MIDI/MP3 media supplies the audio. A user's own MP4 karaoke song or custom background always takes priority.

The karaoke line appears near the bottom of the moving video, with a gradual cyan-blue highlight and the next line beneath it. Bundled KAR files now include word-level lyric events. Existing MIDI files and external LRC tracks remain supported; accuracy depends on the timing metadata in each song. The compact stage and numeric remote controls remain accessible underneath.

These are lightweight stylized geometric animations, **not photorealistic 3D music videos**. Original 3D model cinematics, microphone scoring, real vocalist removal and full professional SoundFont samples are not included in this version.

## Built-in karaoke library in version 0.6.0
KantaGo installs **48 playable, offline songs inside the APK** on first launch or when updating an existing library: 40 original KantaGo compositions (including English, Filipino/OPM-style, and Bisaya lyric tracks) plus eight traditional/public-domain melodies newly arranged for KantaGo. Every included song is a local MIDI/KAR track with timed lyrics; no extra pack, activation key, internet connection, or manual importing is needed.

Traditional titles include **Twinkle Twinkle Little Star, Mary Had a Little Lamb, Frère Jacques, Row Row Row Your Boat, London Bridge, Ode to Joy, Jingle Bells, and Old MacDonald Had a Farm**. These are independent MIDI arrangements, not copyrighted commercial karaoke recordings.

The songbook lists songs immediately and the home screen has ready-to-play favorites. **Existing personal imports are preserved by updates**, not silently deleted. To erase files imported into this installation, open **Settings → Delete imported music**, confirm the permanent deletion, and leave the 48 built-in songs untouched. This cannot erase files belonging to separately installed old KantaGo packages.

The repository builds the karaoke library using `tools/build_builtin_library.py`; CI checks all 48 songs with the built-in MIDI parser and verifies that all files are actually present inside the published APK. No third-party commercial collections are redistributed.

## Download
[**Download the newest test APK from Releases**](https://github.com/dmlazid/KantaGo/releases)

Open the newest release and tap **KantaGo-0.6.0.apk** under Assets. Android 8 or newer. Allow installation from your browser when prompted. This is an early test build, not a Play Store release. This APK is a **new, side-by-side installation**; it does not upgrade old incompatible installations. **Do not uninstall any older KantaGo versions**, since they may hold your song data. Import songs from their original media copies into the new app.

## First test
1. Install the APK on your main Android device.
2. Tap **Start Videoke**, enter **000001**, and tap **Play now**. The original 17-second sound-check demo includes timed lyrics.
3. Import your own `.mid` / `.kar` files or `.mp3` / `.mp4` / `.wav` / `.ogg` / `.m4a` files. For media lyrics, import a matching `.lrc` file with the same basename. Folder imports include subfolders. ZIP packs can now be imported directly.
4. Choose **Import ZIP / 7z song pack** for a ZIP containing song files you have permission to use, optionally with matching `.lrc` lyric files. Subfolders are supported, but names are flattened and duplicate filenames are skipped. Select a language and optional release year when importing. Use the ⋯ button on a song to correct its tags. Names in `Artist - Title.ext` format are separated automatically.
5. Connect another phone to the same private Wi-Fi or hotspot. Tap **Pair remote** on the main device. On your other phone, open KantaGo → **Connect as Remote** → **Scan QR using camera**. Or enter the displayed IP address and six-digit PIN. Browser QR pairing remains available.
6. Keep the main app open. Song numbers, play/pause, queue, next, replay, volume, key and tempo work from the remote. Restarting the host app changes the session token and pairing code.

No internet is needed after you import the songs. Only metadata and control commands are needed for the remote; audio plays from the main device. No account, ads, analytics, or cloud storage. An activation code is required only when importing encrypted KantaGo content packs.

## Included in 0.5.0
- **Download one KantaGo .kgpack song collection** from GitHub Releases; open **Add Song Packs → Import a KantaGo song collection**, enter the key from release notes and select the pack file. Offline activation verifies every encrypted chunk before importing. Incorrect keys and damaged packs are rejected.
- **Starter Collection** has eight original practice MIDI/KAR tracks with timed lyric prompts. It does not contain commercial, pirated, English chart, OPM, or Bisaya chart-song collections.
- **Bundle builder:** `tools/create_activated_pack.py` packages authorized individual song files or ZIP/7z archives as one encrypted file. The key controls imports, but is not a music distribution license or permanent online account activation.
- Song files imported from an authorized activated pack join the existing library; they do not delete old imports. No sign-up or internet needed after downloading.

- **Karaoke-first home:** Enter Karaoke, Song Search, Artist Search, Recently Added, Media Player, Phone Remote, Add Song Packs, and shortcuts for language filters.
- **Karaoke stage:** four original animated backgrounds (Prism, Neon, Retro, Spotlight), selected song display, synchronized MIDI/KAR/LRC lyrics; user-selected images/videos still supported.
- **Compact SongHub-style handset workflow (original KantaGo artwork):** live stage, 5 transport controls, numeric keypad, play/reserve/cancel/first, mixer and live queue.
- **Remote stability:** lower network request frequency and automatic reconnect with backoff. Still requires both phones on the same Wi-Fi/hotspot with the host in the foreground.
- **ZIP and 7z song-pack import** supports compatible media and timed lyric files with limits, duplicate handling, and safe path checks. Neither format grants redistribution rights to third-party songs.
- **Installed song library remains entirely offline.** External download-source tiles were removed from settings; no unauthorized third-party collections are included.

- **In-app QR scanning** on the second Android phone using its rear camera. No separate browser needed; KantaGo checks QR links point to a private Wi-Fi/hotspot IP.
- **One-tap backup ZIP** for imported song files and matching lyrics, with a JSON catalog included for records. Import ZIP restores playable media; individual song language/year tags may need to be set again.
- **Saved reservation queue** survives closing and reopening the app; songs do not auto-play on restart.
- **Disconnect remote** control and remembered host Wi-Fi address, while maintaining manual pairing as a fallback.
- Dedicated paired-phone remote modeled on the supplied sample's workflow: digital song-number display, 12-key keypad, four transport buttons, Play Now / Reserve / Play First / Cancel Reservation, and separate Remote / Song List / Queue / Lyrics / Settings tabs.
- Live remote volume, key, tempo, melody and reservation controls. Browser or another KantaGo installation can use the same private Wi-Fi connection. In-app QR scanning and manual PIN pairing are both available.
- Remote has a lightweight current-lyrics preview, not a second streamed karaoke video.

- **ZIP / 7z song-pack import** with safe streaming extraction, path traversal protection, no overwriting existing songs, and limits of 2,500 entries / 1 GB expanded per pack. ZIP packs require user-owned or licensed files.
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
- Versions 0.5.1 and 0.5.2 use `com.kantago.player`. Earlier installations with different package IDs or signing keys remain separate.
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

## KantaGo original starter song pack
GitHub's newest KantaGo release includes a separate `KantaGo-Starter-Collection.kgpack` download beside the APK. This is one pack file, not thousands of commercial songs.

**Activation key: `KANTA-START-2026`** (starter pack only).

To import on Android: KantaGo → **Add Song Packs** → enter the activation key → **Unlock & Import Song Collection** → choose the downloaded `.kgpack` file. Your original songs remain installed.

To create a single package from music you are *authorized to redistribute*, copy playable files and/or ZIP/7z archives to a folder. Run:
```bash
python -m pip install cryptography py7zr
python tools/create_activated_pack.py --source /path/to/authorized-music --output /path/to/KantaGo-Music.kgpack --activation-key YOUR-KEY-HERE --confirm-rights
```
The activation code is a file passphrase. It does **not** confer copyright permissions, prevent an authorized recipient from re-sharing extracted media, or validate a paid purchase. The free starter code is public in the release notes. Packs are limited to 768 MB of ZIP-compressed media and 2,500 entries; larger collections require several bundles.


## 0.5.2 fixes
- Failed reservations and failed playback no longer show success messages; invalid tag edits stay open.
- Remote commands run in order. Key, tempo, volume and lyric adjustments use the current host value, and old polling responses cannot overwrite newer controls.
- MIDI resume requests audio focus; interruptions during loading remain paused.
- Android WebView fullscreen now displays and exits its custom view; Back exits fullscreen first.
- Version code increments to 2; APK signing and package identity checks remain enabled.

Validation: automated command-state regressions plus existing parser, pack and pairing tests. Physical-phone audio, QR camera and two-phone playback still need device verification.

## 0.6.0 recording comparison
The supplied 47-second recording shows SongHub with 42,772 installed songs, a compact keypad and video backgrounds; KantaGo was showing original demos with controls below the viewport. This update keeps the host keypad beside playback controls, moves advanced mixer/background controls into Settings, increases lyric contrast and refreshes active host lyrics every 200 ms. Existing user-selected MP4 backgrounds remain supported; the recording itself is not bundled as an asset. This UI change does not supply the SongHub catalog or its instrument sounds.

Source checks: Karaokeden returned HTTP 403; MidKar returned HTTP 406 for direct access; the Blogspot collection points to MediaFire downloads which returned an unavailable page here; FreeMIDIs initially returned HTML, then six Air Supply RAR archives downloaded successfully on retry; extraction confirmed MIDI backing tracks with no timed lyric events; MIDI-Song showed paid credits for a tested song; Scribd is a songbook PDF; Song Service is a music storefront. Those six source files were inspected locally and were not bundled or imported on a user device. No new lyric-ready karaoke collection is included. Browser layout checks passed at 393×800, 360×640 and 800×393, with the full keypad above navigation and no horizontal overflow.
