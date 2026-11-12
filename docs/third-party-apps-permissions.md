# Third-party app permissions and the host config provider

The SDK exposes Sidekit's theme and keypad configuration through an exported, read-only provider.
Access is guarded by `hrln.interactive.sidekit.permission.READ_APP_CONFIG`, a `normal` permission
that Android grants at install time.

## Who declares what

The permission is **defined by the host**, in Sidekit's own manifest, next to the provider that
uses it:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<permission
    android:name="hrln.interactive.sidekit.permission.READ_APP_CONFIG"
    android:protectionLevel="normal" />
```

The SDK manifest only **requests** it, so every consumer app inherits the request and nothing else:

```xml
<!-- sidekit-sdk/src/main/AndroidManifest.xml -->
<uses-permission android:name="hrln.interactive.sidekit.permission.READ_APP_CONFIG" />
```

A consumer therefore needs no manifest changes of its own. It can ask for the permission from the
moment it is built, and reading the provider starts working as soon as Sidekit is installed.

## Why the definition does not live in the SDK

Android allows a `normal` permission to be declared by exactly one installed package. When the SDK
declared it, manifest merging copied the declaration into **every** consumer, so two SDK-built
apps installed side by side — or one such app beside Sidekit — failed to install unless they
happened to share a signing certificate:

```
INSTALL_FAILED_DUPLICATE_PERMISSION: Package <client> attempting to redeclare permission
hrln.interactive.sidekit.permission.READ_APP_CONFIG already owned by hrln.interactive.sidekit
```

This was observed with a release-signed Sideplay installed beside Sidekit. Debug builds shared the
debug certificate, so the conflict stayed hidden during development.

## Order of installation

A client installed before Sidekit requests a permission that is not defined yet. Android ignores
the request until the defining package is installed; the grant then applies to the client. The
client still works in the meantime: `SidekitConfigClient.status` reports `NotInstalled` and
`SidekitStores.forHostOrLocal` falls back to the app's own writable settings. Nothing needs to be
reinstalled once Sidekit appears, and the client re-resolves the host on its next start.

## Apps built against the 1.4.1 SDK

`1.4.1` was published with the declaration still inside the SDK, so an app built against it and
signed with a different key than Sidekit must strip the inherited element:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <permission
        android:name="hrln.interactive.sidekit.permission.READ_APP_CONFIG"
        tools:node="remove" />
    <uses-permission android:name="hrln.interactive.sidekit.permission.READ_APP_CONFIG" />
    ...
</manifest>
```

That workaround becomes unnecessary — and should be removed — when the app moves to an SDK built
after this change.
