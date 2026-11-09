package hrln.interactive.sidekit.apps.spotify.data

import hrln.interactive.sidekit.apps.spotify.LibraryFilter
import hrln.interactive.sidekit.apps.spotify.SearchScope
import hrln.interactive.sidekit.apps.spotify.SpotifyAlbum
import hrln.interactive.sidekit.apps.spotify.SpotifyArtist
import hrln.interactive.sidekit.apps.spotify.SpotifyCollection
import hrln.interactive.sidekit.apps.spotify.SpotifyCollectionType
import hrln.interactive.sidekit.apps.spotify.SpotifySearchResults
import hrln.interactive.sidekit.apps.spotify.SpotifyTrack

/**
 * Read side of the Spotify account, as the UI needs it. The production implementation is the Web
 * API; tests supply their own catalog. No screen or ViewModel may call an endpoint directly.
 */
interface SpotifyCatalog {
    suspend fun recentlyPlayed(): List<SpotifyTrack>
    suspend fun library(filter: LibraryFilter): List<Any>
    suspend fun collection(type: SpotifyCollectionType, id: String): SpotifyCollection?
    suspend fun artist(id: String): SpotifyArtist?
    suspend fun albumsBy(artist: SpotifyArtist): List<SpotifyAlbum>
    suspend fun topTracks(artist: SpotifyArtist): List<SpotifyTrack>
    suspend fun search(query: String, scopes: Set<SearchScope>): SpotifySearchResults
}

/**
 * Write side of the user's Spotify library. Kept separate from [SpotifyCatalog] so read-only
 * fallbacks and test catalogs do not have to pretend they can mutate an account.
 */
interface SpotifyLibraryWriter {
    suspend fun isTrackSaved(id: String): Boolean?
    suspend fun isSaved(type: SpotifyCollectionType, id: String): Boolean?
    suspend fun isArtistFollowed(id: String): Boolean?
    suspend fun setTrackSaved(id: String, saved: Boolean)
    suspend fun setSaved(type: SpotifyCollectionType, id: String, saved: Boolean)
    suspend fun setArtistFollowed(id: String, followed: Boolean)
}
