package hrln.interactive.sidekit.sdk

/**
 * Stable identifiers shared by the Sidekit host and Sidekit-native apps.
 *
 * The configuration contract is additive: bump [SCHEMA_VERSION] only for breaking changes, and
 * never change the meaning of an existing key.
 */
object SidekitContract {
    /** Contract revision exposed as `schema` in `getVersion` and `getConfig`. */
    const val SCHEMA_VERSION = 1

    /** Normal permission every consumer is granted at install time. */
    const val PERMISSION_READ_APP_CONFIG = "hrln.interactive.sidekit.permission.READ_APP_CONFIG"

    /** Stable Sidekit build. */
    const val AUTHORITY_STABLE = "hrln.interactive.sidekit.config"

    /** Sidekit dev build (`hrln.interactive.sidekit.dev`). */
    const val AUTHORITY_DEV = "hrln.interactive.sidekit.dev.config"

    /** Authorities probed by the configuration client, stable first. */
    val AUTHORITIES: List<String> = listOf(AUTHORITY_STABLE, AUTHORITY_DEV)

    /** Stable content URI path used for `registerContentObserver`. */
    const val PATH_CONFIG = "config"

    /** Provider methods. Both return a [android.os.Bundle]. */
    const val METHOD_GET_VERSION = "getVersion"
    const val METHOD_GET_CONFIG = "getConfig"

    /** Optional intent extra naming the host authority that launched the app. */
    const val EXTRA_HOST_AUTHORITY = "hrln.interactive.sidekit.extra.HOST_AUTHORITY"

    /** Action advertised by Sidekit-native app activities for launcher discovery. */
    const val ACTION_NATIVE_APP = "hrln.interactive.sidekit.action.NATIVE_APP"

    /** Manifest metadata keys read from a [ACTION_NATIVE_APP] activity. */
    const val META_DATA_APP_ID = "hrln.interactive.sidekit.app.ID"
    const val META_DATA_MIN_SCHEMA = "hrln.interactive.sidekit.app.MIN_SCHEMA"
    const val META_DATA_LAUNCH_ROUTE = "hrln.interactive.sidekit.app.LAUNCH_ROUTE"
}
