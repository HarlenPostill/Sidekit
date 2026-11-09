package hrln.interactive.sidekit.apps.spotify

import androidx.test.ext.junit.runners.AndroidJUnit4
import hrln.interactive.sidekit.apps.spotify.bridge.SpotifyNative
import hrln.interactive.sidekit.apps.spotify.data.NativeSpotifyCatalog
import hrln.interactive.sidekit.apps.spotify.data.SpotifyFailure
import hrln.interactive.sidekit.apps.spotify.data.SpotifyGatewayException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSpotifyCatalogTest {
    @Test fun `track save writes the collection set`() = runTest {
        val native = FakeNative()
        val catalog = NativeSpotifyCatalog(native) {}

        catalog.setTrackSaved("abc", true)

        assertEquals("collection", native.set)
        assertEquals("[\"spotify:track:abc\"]", native.uris)
        assertFalse(native.removed)
    }

    @Test fun `album and show saves use their collection sets`() = runTest {
        val native = FakeNative()
        val catalog = NativeSpotifyCatalog(native) {}

        catalog.setSaved(SpotifyCollectionType.Album, "album1", false)
        assertEquals("collection", native.set)
        assertTrue(native.removed)

        catalog.setSaved(SpotifyCollectionType.Show, "show1", true)
        assertEquals("show", native.set)
        assertFalse(native.removed)
    }

    @Test fun `playlist save uses the rootlist endpoint`() = runTest {
        val native = FakeNative()
        val catalog = NativeSpotifyCatalog(native) {}

        catalog.setSaved(SpotifyCollectionType.Playlist, "playlist1", true)

        assertEquals("playlist1", native.playlistId)
        assertTrue(native.playlistSave)
    }

    @Test fun `contains responses seed saved state`() = runTest {
        val native = FakeNative().apply {
            contains = "[true]"
            playlistSaved = "true"
        }
        val catalog = NativeSpotifyCatalog(native) {}

        assertTrue(catalog.isTrackSaved("abc") == true)
        assertTrue(catalog.isSaved(SpotifyCollectionType.Playlist, "playlist1") == true)
    }

    @Test fun `failed write surfaces a network failure`() = runTest {
        val native = FakeNative().apply { writeOk = false }
        val catalog = NativeSpotifyCatalog(native) {}

        val error = runCatching { catalog.setTrackSaved("abc", true) }.exceptionOrNull()

        assertTrue(error is SpotifyGatewayException)
        assertEquals(SpotifyFailure.Network, (error as SpotifyGatewayException).failure)
    }

    private class FakeNative : SpotifyNative {
        override val isAvailable = true
        var set: String? = null
        var uris: String? = null
        var removed = false
        var writeOk = true
        var contains = "[false]"
        var playlistSaved = "false"
        var playlistId: String? = null
        var playlistSave = false

        override fun initialize() = true
        override fun setTmpDir(path: String) = Unit
        override fun connect(accessToken: String) = true
        override fun setAccessToken(accessToken: String) = true
        override fun disconnect() = Unit
        override fun createPlayer(bitrateKbps: Int) = true
        override fun load(uri: String, positionMs: Long, autoplay: Boolean) = true
        override fun play() = true
        override fun pause() = true
        override fun stop() = true
        override fun seek(positionMs: Long) = true
        override fun readAudio(timeoutMs: Long): ByteArray? = null
        override fun audioFormat(): String? = null
        override fun pollEvent(): String? = null
        override fun catalogLibrary(filter: String): String? = null
        override fun catalogCollection(kind: String, id: String): String? = null
        override fun catalogArtist(id: String): String? = null
        override fun catalogAlbumsBy(id: String): String? = null
        override fun catalogTopTracks(id: String): String? = null
        override fun catalogSearch(query: String, scopes: String, limit: Long): String? = null
        override fun collectionContains(set: String, uris: String): String? = contains
        override fun collectionWrite(set: String, uris: String, removed: Boolean): Boolean {
            this.set = set
            this.uris = uris
            this.removed = removed
            return writeOk
        }
        override fun playlistSaved(id: String): String? = playlistSaved
        override fun playlistSave(id: String, save: Boolean): Boolean {
            playlistId = id
            playlistSave = save
            return writeOk
        }
    }
}
