# Changelog

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
