# Third-party app permissions and the host config provider

The SDK exposes Sidekit's theme and keypad configuration through an exported, read-only provider.
Access is guarded by a `normal` permission that Android grants at install time:
`hrln.interactive.sidekit.permission.READ_APP_CONFIG` for the stable build and
`hrln.interactive.sidekit.dev.permission.READ_APP_CONFIG` for the dev build.

## Who declares what

The permission is **defined by the host**, in Sidekit's own manifest, next to the provider that
uses it, and its name follows the application id:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<permission
    android:name="${applicationId}.permission.READ_APP_CONFIG"
    android:protectionLevel="normal" />
```

The SDK manifest only **requests** both names, so every consumer app inherits the requests and
nothing else:

```xml
<!-- sidekit-sdk/src/main/AndroidManifest.xml -->
<uses-permission android:name="hrln.interactive.sidekit.permission.READ_APP_CONFIG" />
<uses-permission android:name="hrln.interactive.sidekit.dev.permission.READ_APP_CONFIG" />
```

A request for a name nothing defines is ignored, so a client can ask for both from the start and
whichever build is installed grants its own. `SidekitContract.readPermissionForPackage` derives the
name the host checks, and `SidekitContract.READ_PERMISSIONS` lists the ones a client requests.

A consumer therefore needs no manifest changes of its own. It can ask for the permission from the
moment it is built, and reading the provider starts working as soon as Sidekit is installed.

## Why the name follows the application id

Two installed packages cannot define one `normal` permission unless they share a signing
certificate. The stable build and the dev build are separate packages but would have declared the
same name, so a release-signed stable Sidekit plus a debug-signed dev build could not be installed
together:

```
INSTALL_FAILED_DUPLICATE_PERMISSION: Package hrln.interactive.sidekit.dev attempting to
redeclare permission hrln.interactive.sidekit.permission.READ_APP_CONFIG already owned by
hrln.interactive.sidekit
```

Scoping the name to the application id removes the shared definition entirely; each build owns its
own permission and checks its own (`SidekitConfigProvider.callerCanRead`). The stable name is
unchanged, so clients built against the contract keep working.

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
