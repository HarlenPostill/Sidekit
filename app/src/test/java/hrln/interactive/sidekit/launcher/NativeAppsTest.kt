package hrln.interactive.sidekit.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import hrln.interactive.sidekit.sdk.SidekitContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NativeAppsTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application

    private fun addNativeApp(
        packageName: String,
        label: String,
        activityName: String = "$packageName.MainActivity",
        appId: String? = null,
        minSchema: Int? = null,
        launchRoute: String? = null
    ) = addNativeAppForTest(context, packageName, label, activityName, appId, minSchema, launchRoute)

    @Test
    fun `discovers native apps with their contract metadata sorted by label`() {
        addNativeApp("test.zeta", "Zeta", appId = "zeta", minSchema = 1, launchRoute = "home")
        addNativeApp("test.alpha", "Alpha", minSchema = 1)

        val apps = NativeApps.discover(context)

        assertEquals(listOf("test.alpha", "test.zeta"), apps.map { it.packageName })
        assertEquals("native:test.alpha", apps[0].visibilityId)
        assertEquals("test.zeta", apps[1].packageName)
        assertEquals("zeta", apps[1].appId)
        assertEquals(1, apps[1].minSchema)
        assertEquals("home", apps[1].launchRoute)
    }

    @Test
    fun `skips apps that need a newer schema`() {
        addNativeApp("test.future", "Future", minSchema = SidekitContract.SCHEMA_VERSION + 1)
        addNativeApp("test.current", "Current", minSchema = SidekitContract.SCHEMA_VERSION)

        assertEquals(listOf("test.current"), NativeApps.discover(context).map { it.packageName })
    }

    @Test
    fun `skips this Sidekit package and keeps one entry per app`() {
        addNativeApp(context.packageName, "Sidekit itself")
        addNativeApp("test.twice", "Twice", activityName = "test.twice.First")
        addNativeApp("test.twice", "Twice again", activityName = "test.twice.Second")

        assertEquals(listOf("test.twice"), NativeApps.discover(context).map { it.packageName })
    }

    @Test
    fun `launch intent is explicit and names the host authority`() {
        val app = NativeApp("test.alpha", "test.alpha.MainActivity", "Alpha")

        val intent = app.launchIntent(SidekitContract.AUTHORITY_DEV)

        assertEquals(SidekitContract.ACTION_NATIVE_APP, intent.action)
        assertEquals(ComponentName("test.alpha", "test.alpha.MainActivity"), intent.component)
        assertEquals(SidekitContract.AUTHORITY_DEV, intent.getStringExtra(SidekitContract.EXTRA_HOST_AUTHORITY))
        assertNull(app.launchIntent().getStringExtra(SidekitContract.EXTRA_HOST_AUTHORITY))
    }

    @Test
    fun `launch starts the explicit activity with this build as the configuration host`() {
        val app = NativeApp("test.alpha", "test.alpha.MainActivity", "Alpha")

        assertTrue(app.launch(application))

        val started = shadowOf(application).nextStartedActivity
        assertEquals(ComponentName("test.alpha", "test.alpha.MainActivity"), started.component)
        assertEquals(
            SidekitContract.authorityForPackage(context.packageName),
            started.getStringExtra(SidekitContract.EXTRA_HOST_AUTHORITY)
        )
    }
}
