# Sidekit Emulator

Open **Emulator** from the launcher, choose **Add ROM**, and select an uncompressed
`.gb`, `.gbc`, or `.gba` file (maximum 32 MB). Imported ROMs are copied into private
app storage; the original document need not remain available. No games or external
BIOS images are supplied. The mGBA built-in BIOS implementation is used.

Select a game to play. D-pad moves, A/B act, X selects, Y starts, and L1/R1 are the
GBA shoulders. Dedicated Start/Select buttons also work. **Controls** pauses the
game and lets you assign physical keys to all ten actions. This accommodates
keypads with different Android key codes. On-screen hold buttons for L/R cover keypads with only one shoulder key. Back/Exit returns to the library.

Battery saves are keyed by the ROM's SHA-256, written atomically every five seconds
and when playback pauses or exits, and restored when the game opens. These are
in-game cartridge saves, not save states; use the game's save menu. A process kill
can lose the latest five seconds. Pausing, switching apps, and losing window focus
release held buttons and stop audio. Uninstalling/clearing app data deletes the
imported games and saves.

## Upstream

- Repository: https://github.com/mgba-emu/mgba
- Version: 0.10.5, commit `26b7884bc25a5933960f3cdcd98bac1ae14d42e2`
- License: MPL-2.0; bundled dependencies retain their upstream licenses, including
  LGPL blip-buf and BSD inih. Full upstream source and notices are preserved in
  `app/src/main/cpp/vendor/mgba-0.10.5.tar.gz`. APK notices are accessible from the
  library's **About mGBA & licenses** button.
- Archive SHA-256: `9475c26e9fa2f4b30c07ab6636e4b0a5b62e4baee2109ede7b2fecc52edae366`

CMake verifies and extracts the local archive, builds the unmodified mGBA library,
and links a small JNI adapter. No network/CDN or emulator download is needed at
runtime. Build using Android NDK 27.1.12297006 and CMake 3.22.1:

```
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

`EmulatorCoreTest` runs on an Android device and uses original tiny diagnostic
programs to check GB/GBC frame and audio output, cartridge save round-tripping,
GBA video changing on A press/release, and rejection of invalid ROMs. It does not
substitute for testing a physical keypad or compatibility testing full games.

When distributing APKs, also provide this exact corresponding source (including
this archive and the build/JNI files), preserving the bundled license notices so
recipients can rebuild/relink the LGPL component.
