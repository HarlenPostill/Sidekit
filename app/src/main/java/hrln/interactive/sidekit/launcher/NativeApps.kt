package hrln.interactive.sidekit.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import hrln.interactive.sidekit.sdk.SidekitContract

/**
 * A Sidekit-native app installed as its own APK.
 *
 * The app declares an exported activity for [SidekitContract.ACTION_NATIVE_APP]; its metadata
 * supplies [appId], [minSchema] and an optional [launchRoute]. The launcher only uses the
 * descriptor for placement, label and icon: launching is an explicit intent to [component], so a
 * tile can never start anything other than the activity that advertised itself.
 */
data class NativeApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val appId: String = packageName,
    val minSchema: Int = 0,
    val launchRoute: String? = null
) {
    /** Stable launcher id, independent of the package label or activity name. */
    val visibilityId: String get() = "native:$packageName"

    val component: ComponentName get() = ComponentName(packageName, activityName)

    /**
     * The explicit launch intent. [hostAuthority] tells the app which Sidekit build launched it,
     * so a dev Sidekit does not make it read the stable configuration.
     */
    fun launchIntent(hostAuthority: String? = null): Intent = Intent(SidekitContract.ACTION_NATIVE_APP).apply {
        component = this@NativeApp.component
        if (!hostAuthority.isNullOrBlank()) putExtra(SidekitContract.EXTRA_HOST_AUTHORITY, hostAuthority)
    }
}

/** Discovers the native apps installed alongside this Sidekit build. */
object NativeApps {
    /**
     * Every installed app advertising [SidekitContract.ACTION_NATIVE_APP] that this Sidekit
     * understands, sorted by label. One entry per package: the first advertised activity wins.
     *
     * An app whose [NativeApp.minSchema] is newer than [SidekitContract.SCHEMA_VERSION] is skipped,
     * because it has declared that this host cannot read the configuration it needs.
     */
    fun discover(context: Context): List<NativeApp> {
        val intent = Intent(SidekitContract.ACTION_NATIVE_APP).addCategory(Intent.CATEGORY_DEFAULT)
        val resolved = context.packageManager.queryIntentActivities(intent)
        return resolved
            .mapNotNull { toNativeApp(context, it) }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun toNativeApp(context: Context, resolved: ResolveInfo): NativeApp? {
        val activity = resolved.activityInfo ?: return null
        val packageName = activity.packageName ?: return null
        val activityName = activity.name ?: return null
        if (packageName == context.packageName || !activity.enabled) return null

        val minSchema = activity.metaData?.getInt(SidekitContract.META_DATA_MIN_SCHEMA, 0) ?: 0
        if (minSchema > SidekitContract.SCHEMA_VERSION) return null

        val label = runCatching { activity.loadLabel(context.packageManager).toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: packageName
        return NativeApp(
            packageName = packageName,
            activityName = activityName,
            label = label,
            appId = activity.metaData?.getString(SidekitContract.META_DATA_APP_ID)?.takeIf { it.isNotBlank() } ?: packageName,
            minSchema = minSchema,
            launchRoute = activity.metaData?.getString(SidekitContract.META_DATA_LAUNCH_ROUTE)?.takeIf { it.isNotBlank() }
        )
    }

    private fun PackageManager.queryIntentActivities(intent: Intent): List<ResolveInfo> = runCatching {
        // GET_META_DATA fills ActivityInfo.metaData, which carries the descriptor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }
    }.getOrDefault(emptyList())
}

/**
 * Launches [app] with an explicit intent and names this Sidekit build as the configuration host.
 * Returns false when the app disappeared or cannot be started, so the launcher can ignore a stale
 * tile rather than crash.
 */
fun NativeApp.launch(context: Context): Boolean = runCatching {
    val intent = launchIntent(SidekitContract.authorityForPackage(context.packageName))
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
}.getOrDefault(false)
