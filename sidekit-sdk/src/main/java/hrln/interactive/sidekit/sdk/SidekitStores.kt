package hrln.interactive.sidekit.sdk

import android.content.Context
import android.content.Intent
import hrln.interactive.sidekit.design.input.KeypadPreferences
import hrln.interactive.sidekit.sdk.config.SidekitConfigClient
import hrln.interactive.sidekit.sdk.input.HostKeypadPreferences
import hrln.interactive.sidekit.sdk.input.SharedPreferencesKeypadPreferences
import hrln.interactive.sidekit.sdk.theme.HostSidekitThemeStore
import hrln.interactive.sidekit.sdk.theme.SharedPreferencesSidekitThemeStore
import hrln.interactive.sidekit.sdk.theme.SidekitThemeStore

/**
 * Picks the host-backed or local stores for a Sidekit-native app.
 *
 * A host-backed store is read-only and follows Sidekit's settings. When no compatible Sidekit is
 * installed, the app gets its own writable files instead, so it still works standalone.
 */
object SidekitStores {
    /** The stores an app should use, plus the host authority they came from (null when local). */
    data class Stores(
        val theme: SidekitThemeStore,
        val keypad: KeypadPreferences,
        val hostAuthority: String?
    ) {
        val isHostBacked: Boolean get() = hostAuthority != null
    }

    /**
     * Resolves the stores for [context].
     *
     * Pass the launch [intent] so a dev Sidekit that started the app stays the host. Without one,
     * the stable build is preferred, then the dev build.
     */
    fun forHostOrLocal(context: Context, intent: Intent? = null): Stores {
        val requested = SidekitConfigClient.authorityFrom(intent)
        val authority = SidekitConfigClient.hostAuthority(context, requested)
        if (authority != null) {
            val source = SidekitConfigClient.source(context, authority)
            return Stores(
                theme = HostSidekitThemeStore(source),
                keypad = HostKeypadPreferences(source),
                hostAuthority = authority
            )
        }
        return Stores(
            theme = SharedPreferencesSidekitThemeStore(context),
            keypad = SharedPreferencesKeypadPreferences(context),
            hostAuthority = null
        )
    }
}
