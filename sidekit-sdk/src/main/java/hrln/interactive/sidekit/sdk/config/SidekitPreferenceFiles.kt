package hrln.interactive.sidekit.sdk.config

/**
 * The `SharedPreferences` files Sidekit stores its user configuration in.
 *
 * The host provider reads the same files the SDK's local stores write, so the names live here
 * instead of being repeated in each implementation.
 */
object SidekitPreferenceFiles {
    /** Theme, setup state and input mode. */
    const val SETTINGS = "sidekit_settings"

    /** Keypad module, custom mapping and per-module key profiles. */
    const val KEYPAD = "sidekit_keypad"
}
