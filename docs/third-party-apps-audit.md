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
- [ ] **Add the Settings/About disclosure.** State that apps built with the Sidekit SDK can read
      the case colour and keypad setup.
- [ ] **Add a separate-APK acceptance test.** The current tests are Robolectric/in-process. Add an
      instrumentation test or a second sample app that calls `SidekitConfigClient.status/read/observe`
      from another package.
- [ ] **Verify `MIN_SCHEMA` metadata type.** The sample uses `android:value="1"`; `Bundle.getInt`
      on a string-typed value can return `0`. The test fixture uses `putInt`, so it will not catch
      this. Document the expected type and verify on device.

### Performance and API ergonomics

- [ ] **Cache host configuration.** `HostKeypadPreferences` calls `source.read()` per getter, and
      each `read()` probes `getVersion` and then calls `getConfig`. `KeyMapper.load()` can trigger
      ~10 binder calls. Cache the snapshot per `notifyChange`.
- [ ] **Filter provider notifications.** `SidekitConfigProvider.kt:39` notifies on every
      `sidekit_settings`/`sidekit_keypad` change, including hidden apps, setup step, and list
      layout. Notify only for keys in the contract.
- [ ] **Move `NativeApps.discover()` off the main thread.** It runs inside Compose `remember` in
      `LauncherRoute`, `ArrangeHomeScreen`, `HiddenAppsScreen`, and `OnboardingScreen`. Use a
      coroutine/IO and cache the result.
- [ ] **Add a host-or-local store factory.** `SidekitActivity` defaults to local stores
      (`SidekitActivity.kt:36-38`), so a consumer that forgets to override silently ignores the
      host. Add `SidekitStores.forHostOrLocal(context, intent)` and use it in the sample.
- [ ] **Make `:sample-app` a real consumer.** It currently renders a text and a schema constant.
      It should use `SidekitActivity`, call `SidekitConfigClient.status()`, read the host theme,
      and show a local fallback when Sidekit is absent.
- [ ] **Validate `EXTRA_HOST_AUTHORITY`.** A malicious app can launch a native app with a fake
      authority pointing at its own provider. Validate against known packages or verify the
      provider signature.
- [ ] **Decide on explicit-authority fallback.** If a dev Sidekit launched the app and is later
      uninstalled, `status(context, devAuthority)` returns `NotInstalled` even when stable Sidekit
      is installed. Fall back to stable or document the behavior.
- [ ] **Enforce capabilities.** `status()` returns `Available` for any compatible schema, even if
      `capabilities` is empty. Add `hasCapability()` and use it before reading theme/keypad.
- [ ] **Add a real permission-denial test.** The current test overrides `callerCanRead()`; add an
      instrumentation test with a second package/UID to exercise the real
      `checkCallingPermission` boundary.

### API surface cleanup

- [ ] **Review the `:sidekit-sdk` public surface.** It currently includes the contract, client,
      stores, shell, keypad settings UI, crypto, media, system utilities, and `UndoWindow`.
      Consider splitting contract/stores/shell from app-level utilities, or marking utilities
      `internal` where possible.
- [ ] **Hide implementation details.** `SidekitPreferenceFiles` and `SidekitConfigCodec` are
      public but are not intended as stable API. Consider `@RestrictTo` or a separate internal
      artifact.

## Accepted for v1

- Unrestricted `normal`-permission sharing of the selected contract.
- No allowlist, signature check, or user opt-out.
- No launch-only model; standalone launch remains supported.
- No migration of Sideplay credentials, queue, or history; a reset is accepted.

## Verification performed

- `:sidekit-sdk:compileDebugKotlin` and `:app:compileDebugKotlin` pass.
- Targeted unit tests for config, launcher, and home pass.
- `spotlessCheck` passes.
- `:app:lintDebug` and `:sidekit-sdk:lintDebug` pass.
- `:sidekit` and `:sidekit-sdk` publish to `build/local-repo`, and `:sample-app:assembleDebug
  -PsidekitUsePublished=true` resolves the published `sidekit-sdk` plus its transitive
  `sidekit-design` dependency.

## Size

- 5 commits, 94 files, +4,211 / −964 (net +3,247).
- Net production code is roughly +2.1k lines, mostly the new `:sidekit-sdk` module (2,023
  insertions).
- Tests: 14 files, +570 / −56. Docs: 2 files, +476 / −4. Build/publish: 13 files, +307 / −55.
