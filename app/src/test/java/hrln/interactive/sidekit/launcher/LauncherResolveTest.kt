package hrln.interactive.sidekit.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import hrln.interactive.sidekit.apps.settings.visibilityId
import hrln.interactive.sidekit.apps.sideport.registry.PortedApp
import hrln.interactive.sidekit.home.HomePreferences
import hrln.interactive.sidekit.navigation.SubApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Glue between [HomePreferences] and the launcher renderer: the configured home order and dock
 * ids must resolve to [LauncherEntry]s (builtin, ported or native) in the correct order, dropping
 * anything the launcher does not know about.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class LauncherResolveTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val subsById = SubApp.entries.associateBy { it.visibilityId() }
    private val portedApps = listOf(
        PortedApp("test.matrix", "Matrix"),
        PortedApp("test.calendar", "Foreign Calendar")
    )
    private val portedById = portedApps.associateBy { it.visibilityId() }
    private val nativeApps = listOf(
        NativeApp("test.sideplay", "test.sideplay.MainActivity", "Sideplay")
    )
    private val nativeById = nativeApps.associateBy { it.visibilityId }
    private val known = subsById.keys + portedById.keys + nativeById.keys

    @Before
    fun cleanPrefs() {
        HomePreferences(context).reset()
    }

    @Test
    fun `defaults resolve to builtins in declaration order then ported and native apps`() {
        val home = HomePreferences(context)
        val entries = toLauncherEntries(home.homeOrderFor(known), subsById, portedById, nativeById)
        assertEquals(SubApp.entries.map { it.visibilityId() } + portedById.keys + nativeById.keys, entries.map { it.id })
        assertTrue(entries.filterIsInstance<PortedEntry>().map { it.id } == listOf("ported:test.matrix", "ported:test.calendar"))
        assertTrue(entries.last() is NativeAppEntry)
    }

    @Test
    fun `newly installed native app is appended and renders in the launcher`() {
        val home = HomePreferences(context)
        val order = home.homeOrderFor(known)

        assertEquals("native:test.sideplay", order.last())
        val entries = toLauncherEntries(order, subsById, portedById, nativeById)
        assertEquals("native:test.sideplay", entries.last().id)
        assertEquals("Sideplay", entries.last().label)
    }

    @Test
    fun `default dock resolves to the four built-ins in home order`() {
        val home = HomePreferences(context)
        val dock = toLauncherEntries(home.dockFor(known), subsById, portedById, nativeById)
        assertEquals(SubApp.dock.take(HomePreferences.DOCK_MAX).map { it.visibilityId() }, dock.map { it.id })
    }

    @Test
    fun `custom dock derives from the reordered home order and can hold a ported app`() {
        val home = HomePreferences(context)
        // Reordering Mail, Camera and a ported app into the top four changes the dock automatically
        // (the arrange screen commits such an order via saveOrder).
        val customHome = listOf("builtin:Mail", "builtin:Camera", "ported:test.calendar", "builtin:Phonebook") + builtinsExcept("builtin:Mail", "builtin:Camera", "builtin:Phonebook")
        home.saveOrder(customHome)

        val dock = toLauncherEntries(home.dockFor(known), subsById, portedById, nativeById)
        // Dock = the first four of the committed home order, in that order, incl. the ported app.
        assertEquals(listOf("builtin:Mail", "builtin:Camera", "ported:test.calendar", "builtin:Phonebook"), dock.map { it.id })
    }

    @Test
    fun `native app can be moved into the dock`() {
        val home = HomePreferences(context)
        home.saveOrder(listOf("native:test.sideplay", "builtin:Mail", "builtin:Camera", "builtin:Phonebook") + builtinsExcept("builtin:Mail", "builtin:Camera", "builtin:Phonebook"))

        val dock = toLauncherEntries(home.dockFor(known), subsById, portedById, nativeById)
        assertEquals("native:test.sideplay", dock.first().id)
        assertTrue(dock.first() is NativeAppEntry)
    }

    private fun builtinsExcept(vararg drop: String): List<String> = SubApp.entries.map { it.visibilityId() }.filterNot { it in drop }

    @Test
    fun `reordering home order reorders the shared grid and list order`() {
        val home = HomePreferences(context)
        val sideticks = SubApp.Sideticks.visibilityId()
        val phonebook = SubApp.Phonebook.visibilityId()
        // Move Sideticks (index 2) to the end-ish; move Phonebook (index 3) before it.
        home.move(sideticks, 20)
        home.move(phonebook, -2)

        val entries = toLauncherEntries(home.homeOrderFor(known), subsById, portedById, nativeById)
        // Sideticks moved later, so Phonebook now comes first of the two.
        assertTrue(entries.map { it.id }.indexOf(phonebook) < entries.map { it.id }.indexOf(sideticks))
    }

    @Test
    fun `unknown ids are dropped at the boundary`() {
        val entries = toLauncherEntries(
            listOf("builtin:Sidemessage", "builtin:DoesNotExist", "ported:unknown.pkg"),
            subsById,
            portedById,
            nativeById
        )
        assertEquals(listOf("builtin:Sidemessage"), entries.map { it.id })
    }
}
