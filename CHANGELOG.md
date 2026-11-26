# Changelog

## v1.3.0 — 2026-11-26

Changes since [v1.2.3](https://github.com/HarlenPostill/Sidekit/releases/tag/v1.2.3):

### New features

- **Hide the Sidekit button over original apps:** an app opened as Original in Sideport no longer has to show the floating Sidekit button, which could cover its own controls (Spotify's player bar, for example). Hold the app in Sideport and set **Sidekit button** to Off. **Open as** still brings the projection back. [#63](https://github.com/HarlenPostill/Sidekit/pull/63), fixes [#62](https://github.com/HarlenPostill/Sidekit/issues/62)
- **German, Spanish and French:** every screen is translated. **Settings → Language** chooses Phone language or a shipped language, and on Android 13+ the choice also appears in the phone's per-app language settings. [#59](https://github.com/HarlenPostill/Sidekit/pull/59)
- **Sidekit SDK:** third-party apps can use Sidekit's design system, keypad handling and configuration. The design library and host SDK are published as a Maven repository. [#52](https://github.com/HarlenPostill/Sidekit/pull/52), [#54](https://github.com/HarlenPostill/Sidekit/pull/54), [#58](https://github.com/HarlenPostill/Sidekit/pull/58)
- **Pin installed apps to the home screen:** pin any app from the Apps list so it opens straight from the launcher, with its own page. [#53](https://github.com/HarlenPostill/Sidekit/pull/53)
- **Sundial keypad:** the Sundial module is recognised, and its physical ring and hold actions work. [#44](https://github.com/HarlenPostill/Sidekit/pull/44)
- **Dictation from the keypad:** while typing on the keypad, the typing bar has a mic that inserts one spoken phrase at the cursor. [#35](https://github.com/HarlenPostill/Sidekit/pull/35)
- **Translate language lists:** each language row opens a full Translate from/to list, and picking the other side's language swaps the pair. [#50](https://github.com/HarlenPostill/Sidekit/pull/50)
- **Sideweather shows your town:** Locate names the place, for example *Current location · Sydney*. [#34](https://github.com/HarlenPostill/Sidekit/pull/34)
- **Compact layout:** a setting that trims the header and action bar to fit more on screen. [#30](https://github.com/HarlenPostill/Sidekit/pull/30)
- **Full-screen canvas:** Sidekit fills screens of any shape instead of letterboxing a fixed 480 × 640 frame. The Sidephone looks the same as before. [#33](https://github.com/HarlenPostill/Sidekit/pull/33)

### Fixes and polish

- **Action bar:** placement and tones are the same in every app. The left slot exits or holds the primary action, and the right slot commits or offers Undo. [#55](https://github.com/HarlenPostill/Sidekit/pull/55)
- **D-pad focus:** Up leaves the action bar again, and screens no longer trap focus. [#41](https://github.com/HarlenPostill/Sidekit/pull/41)
- **Screen titles:** a title stays in place while it changes, and no longer bounces or draws over the launcher clock. [#39](https://github.com/HarlenPostill/Sidekit/pull/39), [#40](https://github.com/HarlenPostill/Sidekit/pull/40)
- **Arrange apps:** clearer keypad hints, and Reset moves to Options. [#48](https://github.com/HarlenPostill/Sidekit/pull/48)
- **Launcher:** the Apps tile has its own icon, and home search matches T9 the same way as the Apps list. [#47](https://github.com/HarlenPostill/Sidekit/pull/47)
- **Sideport:** Sidekit builds can't be ported into Sideport, and the Apps and Add app screens share one row style. [#29](https://github.com/HarlenPostill/Sidekit/pull/29)

### For developers

- GitHub Actions CI: build, unit tests, lint, formatting, secret scan and CodeQL. [#27](https://github.com/HarlenPostill/Sidekit/pull/27)
- versionName comes from the git tag and versionCode from the commit count. [#28](https://github.com/HarlenPostill/Sidekit/pull/28)
- Kotlin 2.4.21, Gradle 9.7.0 and AndroidX updates. [#56](https://github.com/HarlenPostill/Sidekit/pull/56)
- MIT license and a README. [#32](https://github.com/HarlenPostill/Sidekit/pull/32), [#51](https://github.com/HarlenPostill/Sidekit/pull/51)

The GB/GBC/GBA emulator from earlier releases is still included.

## v1.2.3 — 2026-11-07

Changes since [v1.2.2](https://github.com/HarlenPostill/Sidekit/releases/tag/v1.2.2):

### New features

- **Arrange apps:** Settings → Arrange apps sets one order for the home grid, list and dock. Select an app, move it up or down with the on-row arrows or D-pad, then press Done; Back cancels and Reset restores the default. The dock is the first four apps, so moving an app into or out of the top four docks or undocks it. Hidden apps stay out of the list. [#16](https://github.com/HarlenPostill/Sidekit/pull/16)
- **Keypad navigation defaults:** Sidekit's own screens navigate with each keypad's natural keys: Mini QWERTY T/D/B/J move and Enter confirms, and a game controller's D-pad, A and B move, confirm and go back. T9 and Sundial keep their native keys, custom mappings still take priority, and letters keep typing while a text field or search screen has the keypad. [#16](https://github.com/HarlenPostill/Sidekit/pull/16)
- **System settings page:** device settings (Wi-Fi, Bluetooth, Display, Sounds, Battery, Storage, Date & time, Privacy) move to Settings → System settings, leaving the Settings home for Sidekit's own settings. [#17](https://github.com/HarlenPostill/Sidekit/pull/17)
- **Sideport app hub:** holding a ported app shows its icon, installed version and package (or *Not installed*), an **Open as** Sideport/Original choice, and confirmed **Remove from Sideport** and **Uninstall app** buttons, so apps can now be removed by touch. Options keeps **App info**. [#19](https://github.com/HarlenPostill/Sidekit/pull/19)
- **Redesigned Now Playing:** Previous, Play/Pause and Next are one row of full-size tiles; back, Album, Artist and Save sit in the action bar; and the album art grows to fill the screen. [#22](https://github.com/HarlenPostill/Sidekit/pull/22)

### Fixes

- **No more flicker while music plays:** action-button labels no longer blank for a frame whenever a screen refreshes, which made Now Playing's back button flicker twice a second. The progress bar keeps a fixed width as the elapsed time counts up, so the bar no longer appears to jump back. [#22](https://github.com/HarlenPostill/Sidekit/pull/22)
- **Action bar order:** a reordered dock shows in the requested order on returning home. [#16](https://github.com/HarlenPostill/Sidekit/pull/16)

### For developers

- **Dev build:** `./gradlew :app:installDev` installs **SideKit Dev** (`hrln.interactive.sidekit.dev`) alongside the stable app, built like a release (R8, baseline profile) so on-device testing doesn't stutter. [#15](https://github.com/HarlenPostill/Sidekit/pull/15)
- **Mail tests on every host:** the IMAP/SMTP TLS tests now pass on Linux and CI, not just Apple-silicon Macs. [#20](https://github.com/HarlenPostill/Sidekit/pull/20)

The GB/GBC/GBA emulator, custom keypad mappings and alarm Undo from earlier releases remain included. Android 12 or later is still required.

### Contributors

Thanks to everyone who contributed to this release:

- **@manuel-rtsch**: Arrange apps and keypad navigation defaults (#16), System settings page (#17), Sideport app hub (#19), dev build (#15) and mail test fix (#20)
- **@HarlenPostill**: Now Playing redesign and flicker fixes (#22)

[Full changelog: v1.2.2...v1.2.3](https://github.com/HarlenPostill/Sidekit/compare/v1.2.2...v1.2.3)

## v1.2.2 — 2026-11-06

Changes since [v1.2.1](https://github.com/HarlenPostill/Sidekit/releases/tag/v1.2.1):

- **Custom keypad mappings:** Settings → Keypad mapping supports unrecognised keypads and custom profiles for T9, Mini QWERTY, Sundial and Game controller layouts. Learn keys by pressing them, inspect events in the key tester, and reset mappings or return to automatic detection. Profiles persist across restarts. [#13](https://github.com/HarlenPostill/Sidekit/pull/13)
- **Alarm deletion with Undo:** swipe an alarm to reveal Delete, or delete from its editor, then restore it within five seconds. Removal is saved immediately so deleted alarms cannot ring during the Undo window or return after an app restart. Clock actions also gain icons. [#12](https://github.com/HarlenPostill/Sidekit/pull/12)
- **Precise location:** Maps, Weather Current location and Compass can request GPS access. Coarse and fine permissions are requested together; approximate network location remains usable. Network and GPS lookups run concurrently so a slow network provider cannot block GPS. [#11](https://github.com/HarlenPostill/Sidekit/pull/11)
- **Consistent Undo colour:** Undo uses the muted action tone across messaging, reminders, saved headings, weather and trip planning. [#10](https://github.com/HarlenPostill/Sidekit/pull/10)
- **Readable action labels:** long labels wrap to two lines at a smaller type size, keeping icons visible and preserving accessible text labels. [#8](https://github.com/HarlenPostill/Sidekit/pull/8)
- **Clearer green case themes:** Sage and Lime accents use a distinct teal confirmation colour so primary actions and confirmations remain distinguishable. [#7](https://github.com/HarlenPostill/Sidekit/pull/7)

The GB/GBC/GBA emulator, guided setup, hidden-app controls and launcher keypad search from v1.2.1 remain included. Android 12 or later is still required; this release does not add Android 11 support.

Thanks to all contributors: @manuel-rtsch and @HarlenPostill.

[Full changelog: v1.2.1...v1.2.2](https://github.com/HarlenPostill/Sidekit/compare/v1.2.1...v1.2.2)
