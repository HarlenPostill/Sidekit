# Third-party apps branch audit

**Branch:** `feat/third-party-apps` (based on `upstream/main` at `3debe06`)
**Audited:** 11 November 2026
**Status:** Foundation is sound; not ready for PR until the items below are done.

## Branch strategy

- `upstream/main` is the canonical base for upstreamable work. `origin/main` is identical.
- `feat/third-party-apps` is based on `upstream/main` and stays there. Do not base it on
  `integration` or `release/1.3.0`.
- `release/1.3.0` already contains the original third-party-apps merge. It is the shipping
  target for fork builds, not a development base.
- `integration` is a long-lived merge branch, 183 commits ahead of main and not a superset of
  `release/1.3.0`. Do not use it as the base for this work.

## Decision

For v1 we accept **unrestricted sharing of the carefully selected preferences** through the
exported, read-only provider and the `normal` `READ_APP_CONFIG` permission.

- No allowlist, signature check, or opt-out in v1.
- No launch-only model: apps keep standalone launch and the provider stays the source of truth.
- The contract remains narrow: theme, compact layout, input mode, keypad module, custom module,
  keypad profiles, setup state, schema, host version, and capabilities.
- Hidden apps, home order, dock, mail/bank/SMS data, credentials, and T9 user words stay private.

The only transparency item we still want is the Settings/About disclosure, so users can see that
SDK-built apps can read the theme and keypad setup.

## Must fix before PR

### Merge and review

- [x] **Keep the branch based on `upstream/main`.** The branch is 5 commits ahead of
      `3debe06` and 0 behind. Rebase onto `upstream/main` again before opening the PR.
- [ ] **Drop formatter-only churn.** Unrelated files such as `VideoScreens.kt`,
      `OnboardingScreen.kt`, `SettingsViewModel.kt`, `BankingRepository.kt`, `MusicScreens.kt`,
      `PhotosScreens.kt`, and `ArrangeHomeScreen.kt` contain trailing-comma/line-wrap changes that
      inflate the diff and cause conflicts.

### Correctness and completeness

- [x] **Fix or remove `launchRoute`.** `NativeApp.launchIntent()` now sends
      `SidekitContract.EXTRA_LAUNCH_ROUTE` when the descriptor declares a route, and the launch
      tests cover both the intent and the started activity.
- [x] **Validate publishing locally.** The publish convention now stages artifacts in
      `build/local-repo`, and the `Published SDK consumer` CI job builds `:sample-app` with
      `-PsidekitUsePublished=true` against `hrln.interactive.sidekit:sidekit-sdk:<version>`.
      Publishing to the real host (GitHub Packages) is still pending.
- [x] **Add the Settings/About disclosure.** About now says that SDK-built apps can read the case
      colour and keypad setup, and names what stays private.
- [x] **Add a separate-APK acceptance test.** `:sample-app` is a separate APK and now has an
      instrumentation test that calls `SidekitConfigClient.status/read` from its own package.
- [x] **Verify `MIN_SCHEMA` metadata type.** `NativeApps` accepts both the manifest's string value
      and an Int bundle value, and a test covers the string form.

### Performance and API ergonomics

- [x] **Cache host configuration.** `CachingSidekitConfigSource` keeps one snapshot until the
      host notifies; `HostKeypadPreferences` and `HostSidekitThemeStore` use it.
- [x] **Filter provider notifications.** The provider only notifies for contract keys; hidden
      apps, setup step, list layout and the legacy T9 flag no longer wake clients.
- [x] **Move `NativeApps.discover()` off the main thread.** `rememberNativeApps` queries on
      `Dispatchers.IO`, caches the result, and refreshes on launcher resume.
- [x] **Add a host-or-local store factory.** `SidekitStores.forHostOrLocal(context, intent)` is
      used by `SidekitActivity`, so a consumer gets host-backed stores by default.
- [x] **Make `:sample-app` a real consumer.** It now extends `SidekitActivity`, calls
      `SidekitConfigClient.status/read`, reads the host theme, and falls back to local settings.
- [x] **Validate `EXTRA_HOST_AUTHORITY`.** The client only accepts known Sidekit authorities and
      rejects a provider whose package does not own the authority.
- [x] **Decide on explicit-authority fallback.** A missing or unsupported dev host falls back to
      the other known Sidekit builds; an unknown authority is rejected.
- [x] **Enforce capabilities.** `SidekitConfig.hasCapability()` gates theme and keypad reads.
- [ ] **Add a real permission-denial test.** The current test overrides `callerCanRead()`; add an
      instrumentation test with a second package/UID to exercise the real
      `checkCallingPermission` boundary.

### API surface cleanup

- [ ] **Review the `:sidekit-sdk` public surface.** It currently includes the contract, client,
      stores, shell, keypad settings UI, crypto, media, system utilities, and `UndoWindow`.
      Consider splitting contract/stores/shell from app-level utilities, or marking utilities
      `internal` where possible.
- [x] **Hide implementation details.** `SidekitPreferenceFiles` and `SidekitConfigCodec` are
      marked `@RestrictTo(LIBRARY_GROUP)`; a separate internal artifact is still an option.

## Accepted for v1

- Unrestricted `normal`-permission sharing of the selected contract.
- No allowlist, signature check, or user opt-out.
- No launch-only model; standalone launch remains supported.
- No migration of Sideplay credentials, queue, or history; a reset is accepted.

## Verification performed

- `:sidekit-sdk:compileDebugKotlin`, `:app:compileDebugKotlin` and `:sample-app:compileDebugKotlin` pass.
- Full unit suites for `:app`, `:sidekit-sdk` and `:sidekit` pass.
- `:sample-app:compileDebugAndroidTestKotlin` passes; the separate-APK client test is ready for a device.
- `spotlessCheck` passes.
- `:app:lintDebug`, `:sidekit-sdk:lintDebug` and `:sample-app:lintDebug` pass.
- `:sidekit` and `:sidekit-sdk` publish to `build/local-repo`, and `:sample-app:assembleDebug
  -PsidekitUsePublished=true` resolves the published `sidekit-sdk` plus its transitive
  `sidekit-design` dependency.

## Size

- 5 commits, 94 files, +4,211 / −964 (net +3,247).
- Net production code is roughly +2.1k lines, mostly the new `:sidekit-sdk` module (2,023
  insertions).
- Tests: 14 files, +570 / −56. Docs: 2 files, +476 / −4. Build/publish: 13 files, +307 / −55.
