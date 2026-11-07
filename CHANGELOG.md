# Changelog

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
