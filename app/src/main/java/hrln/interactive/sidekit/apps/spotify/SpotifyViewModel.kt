package hrln.interactive.sidekit.apps.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hrln.interactive.sidekit.apps.spotify.data.SpotifyCatalog
import hrln.interactive.sidekit.apps.spotify.data.SpotifyFailure
import hrln.interactive.sidekit.apps.spotify.data.SpotifyGatewayException
import hrln.interactive.sidekit.apps.spotify.data.SpotifyHistoryRepository
import hrln.interactive.sidekit.apps.spotify.data.SpotifyLibraryWriter
import hrln.interactive.sidekit.apps.spotify.data.SpotifyQueueRepository
import hrln.interactive.sidekit.apps.spotify.data.SpotifySession
import hrln.interactive.sidekit.ui.UndoWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** The last save/follow change, offered back by the action bar for a few seconds. */
sealed interface SpotifyUndo {
    val contentDescription: String

    data class Track(val id: String, val label: String?) : SpotifyUndo {
        override val contentDescription: String = if (label == null) "Undo like change" else "Undo save to $label"
    }

    data class Collection(val type: SpotifyCollectionType, val id: String) : SpotifyUndo {
        override val contentDescription: String = "Undo library change"
    }

    data class Artist(val id: String, val name: String) : SpotifyUndo {
        override val contentDescription: String = "Undo follow change"
    }
}

data class SpotifyUiState(
    val signedIn: Boolean = false,
    val current: SpotifyMediaItem? = null,
    val upcoming: List<SpotifyMediaItem> = emptyList(),
    val history: List<SpotifyMediaItem> = emptyList(),
    val recent: List<SpotifyTrack> = emptyList(),
    val libraryItems: List<Any> = emptyList(),
    val searchResults: SpotifySearchResults = SpotifySearchResults(),
    val searchPending: Boolean = false,
    val authPending: Boolean = false,
    val libraryLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val savedTrackIds: Set<String> = emptySet(),
    val savedCollectionKeys: Set<String> = emptySet(),
    val followedArtistIds: Set<String> = emptySet(),
    val collections: Map<String, SpotifyCollection> = emptyMap(),
    val artists: Map<String, SpotifyArtist> = emptyMap(),
    val artistAlbums: Map<String, List<SpotifyAlbum>> = emptyMap(),
    val artistTracks: Map<String, List<SpotifyTrack>> = emptyMap(),
    val pendingCollections: Set<String> = emptySet(),
    val pendingArtists: Set<String> = emptySet(),
    val filter: LibraryFilter = LibraryFilter.LikedSongs,
    val searchScopes: Set<SearchScope> = SearchScope.entries.toSet(),
    val query: String = "",
    val audioQuality: AudioQuality = AudioQuality.High,
    val notice: String? = null,
    val undo: SpotifyUndo? = null
) {
    val savedCurrent: Boolean get() = current?.let { it.id in savedTrackIds } ?: false
}

/**
 * State holder for the Sidetracks UI. Catalog reads, queue ordering and local history live in
 * their own domain objects; this class only maps them to the single immutable screen state and
 * implements the optimistic save/follow behaviour the action bars expect.
 */
class SpotifyViewModel(
    private val catalog: SpotifyCatalog,
    private val writer: SpotifyLibraryWriter? = null,
    private val queue: SpotifyQueueRepository = SpotifyQueueRepository(),
    private val history: SpotifyHistoryRepository = SpotifyHistoryRepository(),
    private val session: SpotifySession? = null,
    private val openBrowser: (suspend (String) -> Unit)? = null,
    private val clearArtworkCache: () -> Unit = {}
) : ViewModel() {
    private val _state = MutableStateFlow(SpotifyUiState(signedIn = session?.signedIn?.value ?: false))
    val state: StateFlow<SpotifyUiState> = _state.asStateFlow()

    /** The five-second offer; leaving the screen commits the change instead of carrying Undo away. */
    val undoWindow = UndoWindow(viewModelScope) { _state.update { it.copy(undo = null) } }

    private var searchJob: Job? = null
    private val savedTrackTouched = mutableSetOf<String>()
    private val savedCollectionTouched = mutableSetOf<String>()
    private val followedArtistTouched = mutableSetOf<String>()
    private val nextHandler = { next() }
    private val previousHandler = { previous() }
    private val seekHandler = { position: Long -> seekTo(position) }

    init {
        session?.let {
            viewModelScope.launch {
                it.signedIn.collect { signed ->
                    _state.update { state -> state.copy(signedIn = signed) }
                    if (signed) loadAccount() else clearAccount()
                }
            }
            viewModelScope.launch {
                it.settings.collect { settings ->
                    _state.update { state -> state.copy(audioQuality = settings.audioQuality) }
                }
            }
            it.playback.onNext = nextHandler
            it.playback.onPrevious = previousHandler
            it.playback.onSeek = seekHandler
        }
        restorePlaybackState()
        if (session == null || !session.nativeAvailable) {
            // Local fixtures and builds without the native library still need a moving progress bar.
            viewModelScope.launch {
                while (true) {
                    delay(1_000L)
                    tick()
                }
            }
        } else {
            viewModelScope.launch {
                while (true) {
                    delay(300L)
                    val raw = withContext(Dispatchers.IO) { session.pollPlaybackEvent() } ?: continue
                    handlePlaybackEvent(raw)
                }
            }
        }
    }

    /** Adopts the process-wide player and queue after the Sidetracks screen was recreated. */
    private fun restorePlaybackState() {
        val session = session ?: return
        val playback = session.playback
        val snapshot = queue.snapshot
        val current = playback.current ?: snapshot.current
        if (current != null && snapshot.current?.id != current.id) {
            queue.restore(snapshot.copy(current = current))
        }
        val restored = queue.snapshot
        _state.update {
            it.copy(
                current = current,
                upcoming = restored.upcoming,
                history = restored.history,
                shuffle = restored.shuffle,
                repeat = restored.repeat,
                isPlaying = playback.isActive,
                positionMs = playback.position,
                recent = history.items().filterIsInstance<SpotifyTrack>()
            )
        }
    }

    override fun onCleared() {
        session?.playback?.let {
            if (it.onNext === nextHandler) it.onNext = null
            if (it.onPrevious === previousHandler) it.onPrevious = null
            if (it.onSeek === seekHandler) it.onSeek = null
        }
        super.onCleared()
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun refresh() = loadAccount()

    fun notice(text: String) = _state.update { it.copy(notice = text) }

    fun collection(type: SpotifyCollectionType, id: String): SpotifyCollection? = _state.value.collections[collectionKey(type, id)]

    fun artist(id: String): SpotifyArtist? = _state.value.artists[id]

    fun albumsBy(artist: SpotifyArtist): List<SpotifyAlbum> = _state.value.artistAlbums[artist.id].orEmpty()

    fun topTracks(artist: SpotifyArtist): List<SpotifyTrack> = _state.value.artistTracks[artist.id].orEmpty()

    fun ensureCollection(type: SpotifyCollectionType, id: String) {
        val key = collectionKey(type, id)
        val snapshot = _state.value
        if (key in snapshot.collections || key in snapshot.pendingCollections) return
        _state.update { it.copy(pendingCollections = it.pendingCollections + key) }
        viewModelScope.launch {
            try {
                val collection = catalog.collection(type, id)
                val saved = if (collection == null) null else writer?.isSaved(type, id)
                _state.update {
                    val keys = it.savedCollectionKeys.toMutableSet()
                    if (saved != null && key !in savedCollectionTouched) {
                        if (saved) keys.add(key) else keys.remove(key)
                    }
                    it.copy(
                        collections = if (collection == null) it.collections else it.collections + (key to collection),
                        savedCollectionKeys = keys,
                        pendingCollections = it.pendingCollections - key
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(pendingCollections = it.pendingCollections - key, notice = playbackNotice(e))
                }
            }
        }
    }

    fun ensureArtist(id: String) {
        val snapshot = _state.value
        if (id in snapshot.artists || id in snapshot.pendingArtists) return
        _state.update { it.copy(pendingArtists = it.pendingArtists + id) }
        viewModelScope.launch {
            try {
                val artist = catalog.artist(id)
                if (artist == null) {
                    _state.update { it.copy(pendingArtists = it.pendingArtists - id) }
                    return@launch
                }
                val albums = catalog.albumsBy(artist)
                val tracks = catalog.topTracks(artist)
                val followed = writer?.isArtistFollowed(id)
                _state.update {
                    val ids = it.followedArtistIds.toMutableSet()
                    if (followed != null && id !in followedArtistTouched) {
                        if (followed) ids.add(id) else ids.remove(id)
                    }
                    it.copy(
                        artists = it.artists + (id to artist),
                        artistAlbums = it.artistAlbums + (id to albums),
                        artistTracks = it.artistTracks + (id to tracks),
                        followedArtistIds = ids,
                        pendingArtists = it.pendingArtists - id
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(pendingArtists = it.pendingArtists - id, notice = playbackNotice(e))
                }
            }
        }
    }

    fun play(items: List<SpotifyMediaItem>, index: Int) {
        if (index !in items.indices) return
        val item = items[index]
        if (!item.playable) {
            notice("Not available on this account")
            return
        }
        if (_state.value.current?.id == item.id) {
            if (!_state.value.isPlaying) togglePlayPause()
            _state.update { it.copy(notice = null) }
            return
        }
        val snapshot = queue.play(items, index)
        if (item is SpotifyTrack) ensureTrackSaved(item.id)
        recorded(item)
        applyQueue(snapshot, positionMs = 0L, playing = true)
        _state.update { it.copy(notice = null) }
        startRemote(item, 0L)
    }

    fun playItem(item: SpotifyMediaItem) = play(listOf(item), 0)

    fun playCollection(collection: SpotifyCollection, index: Int) = play(collection.items, index)

    fun playArtist(artist: SpotifyArtist) = play(topTracks(artist), 0)

    fun playLibraryTrack(track: SpotifyTrack) {
        val recent = _state.value.recent
        val at = recent.indexOfFirst { it.id == track.id }
        if (at >= 0) play(recent, at) else playItem(track)
    }

    fun togglePlayPause() {
        val s = _state.value
        val item = s.current ?: return
        val play = !s.isPlaying
        _state.update { it.copy(isPlaying = play) }
        if (item.uri == null || session == null) return
        viewModelScope.launch {
            try {
                if (play) session.resume() else session.pause()
                session.playback.updateState(play, _state.value.positionMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPlaying = false, notice = playbackNotice(e)) }
            }
        }
    }

    fun next() {
        val before = _state.value.current
        val snapshot = queue.next(auto = false)
        if (snapshot.current !== before) {
            snapshot.current?.let(::recorded)
            applyQueue(snapshot, positionMs = 0L, playing = snapshot.current?.playable == true)
            snapshot.current?.let { startRemote(it, 0L) }
        }
    }

    fun previous() {
        val s = _state.value
        if (s.current == null) return
        if (s.positionMs > 3_000L) {
            _state.update { it.copy(positionMs = 0L) }
            s.current?.uri?.let { uri ->
                session?.let { session ->
                    viewModelScope.launch { runCatching { session.seek(0L) } }
                }
            }
            return
        }
        val snapshot = queue.previous(s.positionMs)
        if (snapshot.current !== s.current) {
            snapshot.current?.let(::recorded)
            applyQueue(snapshot, positionMs = 0L, playing = snapshot.current?.playable == true)
            snapshot.current?.let { startRemote(it, 0L) }
        } else {
            _state.update { it.copy(positionMs = 0L) }
            s.current?.uri?.let { uri ->
                session?.let { session ->
                    viewModelScope.launch { runCatching { session.seek(0L) } }
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val current = _state.value.current ?: return
        val target = positionMs.coerceIn(0L, current.durationMs)
        _state.update { it.copy(positionMs = target) }
        if (current.uri != null && session != null) {
            viewModelScope.launch { runCatching { session.seek(target) } }
        }
    }

    fun toggleShuffle() = applyQueue(queue.toggleShuffle())

    fun cycleRepeat() = applyQueue(queue.cycleRepeat())

    fun toggleSaveCurrent() {
        val item = _state.value.current ?: return
        // A like is a simple toggle: no notice and no Undo offer.
        toggleTrackSaved(item.id, null, offerUndo = false)
    }

    fun toggleSaved(track: SpotifyTrack) = toggleTrackSaved(track.id, null)

    private fun toggleTrackSaved(id: String, label: String?, offerUndo: Boolean = true) {
        val wasSaved = id in _state.value.savedTrackIds
        val saved = !wasSaved
        val action = SpotifyUndo.Track(id, label)
        savedTrackTouched += id
        _state.update {
            val ids = it.savedTrackIds.toMutableSet()
            if (saved) ids.add(id) else ids.remove(id)
            it.copy(
                savedTrackIds = ids,
                notice = label?.let { name -> if (saved) "Saved to $name" else "Removed from $name" },
                undo = if (offerUndo) action else it.undo
            )
        }
        if (offerUndo) undoWindow.start()
        val writer = writer ?: return
        viewModelScope.launch {
            try {
                writer.setTrackSaved(id, saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearUndo(action)
                _state.update {
                    val ids = it.savedTrackIds.toMutableSet()
                    if (wasSaved) ids.add(id) else ids.remove(id)
                    it.copy(savedTrackIds = ids, notice = playbackNotice(e))
                }
            }
        }
    }

    fun savedCollection(collection: SpotifyCollection): Boolean = collectionKey(collection.type, collection.id) in _state.value.savedCollectionKeys

    fun toggleSaved(collection: SpotifyCollection) = toggleSaved(collection.type, collection.id)

    private fun toggleSaved(type: SpotifyCollectionType, id: String, offerUndo: Boolean = true) {
        val key = collectionKey(type, id)
        val wasSaved = key in _state.value.savedCollectionKeys
        val saved = !wasSaved
        val action = SpotifyUndo.Collection(type, id)
        savedCollectionTouched += key
        _state.update {
            val keys = it.savedCollectionKeys.toMutableSet()
            if (saved) keys.add(key) else keys.remove(key)
            it.copy(
                savedCollectionKeys = keys,
                notice = if (saved) "Saved to Library" else "Removed from Library",
                undo = if (offerUndo) action else it.undo
            )
        }
        if (offerUndo) undoWindow.start()
        val writer = writer ?: return
        viewModelScope.launch {
            try {
                writer.setSaved(type, id, saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearUndo(action)
                _state.update {
                    val keys = it.savedCollectionKeys.toMutableSet()
                    if (wasSaved) keys.add(key) else keys.remove(key)
                    it.copy(savedCollectionKeys = keys, notice = playbackNotice(e))
                }
            }
        }
    }

    fun followed(artist: SpotifyArtist): Boolean = artist.id in _state.value.followedArtistIds

    fun toggleFollow(artist: SpotifyArtist) = toggleFollow(artist.id, artist.name)

    private fun toggleFollow(id: String, name: String, offerUndo: Boolean = true) {
        val wasFollowing = id in _state.value.followedArtistIds
        val following = !wasFollowing
        val action = SpotifyUndo.Artist(id, name)
        followedArtistTouched += id
        _state.update {
            val ids = it.followedArtistIds.toMutableSet()
            if (following) ids.add(id) else ids.remove(id)
            it.copy(
                followedArtistIds = ids,
                notice = if (following) "Following $name" else "Unfollowed $name",
                undo = if (offerUndo) action else it.undo
            )
        }
        if (offerUndo) undoWindow.start()
        val writer = writer ?: return
        viewModelScope.launch {
            try {
                writer.setArtistFollowed(id, following)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearUndo(action)
                _state.update {
                    val ids = it.followedArtistIds.toMutableSet()
                    if (wasFollowing) ids.add(id) else ids.remove(id)
                    it.copy(followedArtistIds = ids, notice = playbackNotice(e))
                }
            }
        }
    }

    fun undo() {
        val action = _state.value.undo ?: return
        undoWindow.cancel()
        _state.update { it.copy(undo = null) }
        when (action) {
            is SpotifyUndo.Track -> toggleTrackSaved(action.id, action.label, offerUndo = false)
            is SpotifyUndo.Collection -> toggleSaved(action.type, action.id, offerUndo = false)
            is SpotifyUndo.Artist -> toggleFollow(action.id, action.name, offerUndo = false)
        }
    }

    /** Leaving the screen finalises the change instead of offering Undo when you come back. */
    fun commitUndo() {
        undoWindow.cancel()
        _state.update { it.copy(undo = null) }
    }

    private fun clearUndo(action: SpotifyUndo) {
        if (_state.value.undo == action) {
            undoWindow.cancel()
            _state.update { it.copy(undo = null) }
        }
    }

    private fun ensureTrackSaved(id: String) {
        val writer = writer ?: return
        if (id in _state.value.savedTrackIds || id in savedTrackTouched) return
        viewModelScope.launch {
            val saved = runCatching { writer.isTrackSaved(id) }.getOrNull() ?: return@launch
            if (saved && id !in savedTrackTouched) {
                _state.update { it.copy(savedTrackIds = it.savedTrackIds + id) }
            }
        }
    }

    fun setFilter(filter: LibraryFilter) {
        _state.update { it.copy(filter = filter) }
        loadLibrary(filter)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        search(debounceMs = 250L)
    }

    fun clearQuery() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", searchResults = SpotifySearchResults(), searchPending = false) }
    }

    fun toggleScope(scope: SearchScope) {
        _state.update {
            val scopes = if (scope in it.searchScopes) it.searchScopes - scope else it.searchScopes + scope
            it.copy(searchScopes = scopes)
        }
        search(debounceMs = 0L)
    }

    fun cycleAudioQuality() {
        val next = _state.value.audioQuality.next()
        _state.update { it.copy(audioQuality = next) }
        session?.setAudioQuality(next)
    }

    fun clearHistory() {
        history.clear()
        val snapshot = queue.clearHistory()
        applyQueue(snapshot)
        _state.update { it.copy(recent = emptyList(), notice = "Listening history cleared") }
    }

    fun clearCache() {
        clearArtworkCache()
        _state.update { it.copy(notice = "Artwork cache cleared") }
    }

    fun moveToNext(item: SpotifyMediaItem) {
        applyQueue(queue.playNext(item))
    }

    fun removeUpcoming(item: SpotifyMediaItem) {
        applyQueue(queue.removeUpcoming(item))
    }

    fun addToQueue(item: SpotifyMediaItem) {
        applyQueue(queue.addToQueue(item))
    }

    fun clearUpcoming(): List<SpotifyMediaItem> {
        val removed = queue.clearUpcoming()
        applyQueue(queue.snapshot)
        return removed
    }

    fun restoreUpcoming(items: List<SpotifyMediaItem>) = applyQueue(queue.restoreUpcoming(items))

    fun jumpTo(id: String) {
        val s = _state.value
        if (s.current?.id == id) {
            _state.update { it.copy(positionMs = 0L, isPlaying = true) }
            return
        }
        val snapshot = queue.jumpTo(id)
        if (snapshot.current !== s.current) {
            snapshot.current?.let(::recorded)
            applyQueue(snapshot, positionMs = 0L, playing = snapshot.current?.playable == true)
            snapshot.current?.let { startRemote(it, 0L) }
        }
    }

    fun signOut() {
        session?.signOut()
        queue.stop()
        _state.update { SpotifyUiState(signedIn = false, recent = history.items().filterIsInstance<SpotifyTrack>()) }
    }

    fun signIn() {
        val session = session
        val openBrowser = openBrowser
        if (session == null || openBrowser == null) {
            _state.update { it.copy(notice = "Sign-in is unavailable in this build") }
            return
        }
        if (_state.value.authPending) return
        _state.update { it.copy(authPending = true, notice = null) }
        viewModelScope.launch {
            try {
                session.signIn(openBrowser)
                _state.update { it.copy(signedIn = true, authPending = false) }
                loadAccount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(authPending = false, notice = e.message ?: "Sign-in failed")
                }
            }
        }
    }

    private fun loadAccount() {
        if (!_state.value.signedIn) return
        _state.update { it.copy(libraryLoading = true) }
        viewModelScope.launch {
            try {
                val filter = _state.value.filter
                val recent = catalog.recentlyPlayed()
                val library = catalog.library(filter)
                _state.update {
                    it.withLibrarySaved(filter, library).copy(
                        recent = recent.ifEmpty { history.items().filterIsInstance<SpotifyTrack>() },
                        libraryItems = library,
                        libraryLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(libraryLoading = false, notice = playbackNotice(e)) }
            }
        }
    }

    private fun loadLibrary(filter: LibraryFilter) {
        if (!_state.value.signedIn) return
        _state.update { it.copy(libraryLoading = true) }
        viewModelScope.launch {
            try {
                val items = catalog.library(filter)
                _state.update {
                    if (it.filter == filter) {
                        it.withLibrarySaved(filter, items).copy(libraryItems = items, libraryLoading = false)
                    } else {
                        it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(libraryLoading = false, notice = playbackNotice(e)) }
            }
        }
    }

    private fun clearAccount() {
        undoWindow.cancel()
        queue.stop()
        history.clear()
        savedTrackTouched.clear()
        savedCollectionTouched.clear()
        followedArtistTouched.clear()
        _state.update {
            it.copy(
                recent = emptyList(),
                libraryItems = emptyList(),
                searchResults = SpotifySearchResults(),
                collections = emptyMap(),
                artists = emptyMap(),
                artistAlbums = emptyMap(),
                artistTracks = emptyMap(),
                savedTrackIds = emptySet(),
                savedCollectionKeys = emptySet(),
                followedArtistIds = emptySet()
            )
        }
    }

    private fun SpotifyUiState.withLibrarySaved(filter: LibraryFilter, items: List<Any>): SpotifyUiState = when (filter) {
        LibraryFilter.LikedSongs -> copy(savedTrackIds = items.filterIsInstance<SpotifyTrack>().map { it.id }.toSet())

        LibraryFilter.Albums -> copy(
            savedCollectionKeys = savedCollectionKeys.withoutPrefix("${SpotifyCollectionType.Album.route}:") +
                items.filterIsInstance<SpotifyAlbum>().map { collectionKey(SpotifyCollectionType.Album, it.id) }
        )

        LibraryFilter.Playlists -> copy(
            savedCollectionKeys = savedCollectionKeys.withoutPrefix("${SpotifyCollectionType.Playlist.route}:") +
                items.filterIsInstance<SpotifyPlaylist>().map { collectionKey(SpotifyCollectionType.Playlist, it.id) }
        )

        LibraryFilter.Shows -> copy(
            savedCollectionKeys = savedCollectionKeys.withoutPrefix("${SpotifyCollectionType.Show.route}:") +
                items.filterIsInstance<SpotifyShow>().map { collectionKey(SpotifyCollectionType.Show, it.id) }
        )

        LibraryFilter.Artists -> copy(followedArtistIds = items.filterIsInstance<SpotifyArtist>().map { it.id }.toSet())
    }

    private fun Set<String>.withoutPrefix(prefix: String): Set<String> = filterNot { it.startsWith(prefix) }.toSet()

    private fun applyQueue(
        snapshot: SpotifyQueueRepository.Snapshot,
        positionMs: Long? = null,
        playing: Boolean? = null
    ) {
        _state.update {
            it.copy(
                current = snapshot.current,
                upcoming = snapshot.upcoming,
                history = snapshot.history,
                shuffle = snapshot.shuffle,
                repeat = snapshot.repeat,
                positionMs = positionMs ?: it.positionMs,
                isPlaying = playing ?: it.isPlaying
            )
        }
        session?.playback?.setNowPlaying(snapshot.current, positionMs ?: _state.value.positionMs)
    }

    private fun recorded(item: SpotifyMediaItem) {
        if (item !is SpotifyTrack) return
        history.record(item, System.currentTimeMillis())
        _state.update { it.copy(recent = history.items().filterIsInstance<SpotifyTrack>()) }
    }

    private fun search(debounceMs: Long) {
        searchJob?.cancel()
        val query = _state.value.query
        if (query.trim().length < 2) {
            _state.update { it.copy(searchResults = SpotifySearchResults(), searchPending = false) }
            return
        }
        _state.update { it.copy(searchPending = true) }
        searchJob = viewModelScope.launch {
            delay(debounceMs)
            val scopes = _state.value.searchScopes
            val results = try {
                if (_state.value.signedIn) catalog.search(query, scopes) else SpotifySearchResults()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(notice = playbackNotice(e)) }
                SpotifySearchResults()
            }
            _state.update {
                if (it.query == query && it.searchScopes == scopes) {
                    it.copy(searchResults = results, searchPending = false)
                } else {
                    it
                }
            }
        }
    }

    /**
     * librespot is the source of truth while it is playing: its position events keep the seek bar
     * honest through buffering and seeks, and EndOfTrack advances the Sidekit queue exactly once.
     */
    private fun handlePlaybackEvent(raw: String) {
        val event = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val position = event.optLong("positionMs", _state.value.positionMs)
        when (event.optString("type")) {
            "loading" -> if (matchesCurrent(event)) {
                _state.update { it.copy(positionMs = position) }
            }

            "playing" -> if (matchesCurrent(event)) {
                _state.update { it.copy(isPlaying = true, positionMs = position) }
                session?.playback?.updateState(true, position)
            }

            "paused" -> if (matchesCurrent(event)) {
                _state.update { it.copy(isPlaying = false, positionMs = position) }
                session?.playback?.updateState(false, position)
            }

            "position" -> _state.update { it.copy(positionMs = position) }

            "ended" -> if (matchesCurrent(event)) onPlaybackEnded()

            "stopped" -> if (matchesCurrent(event)) {
                // Stop from the notification or lock screen: clear both the UI and the queue so
                // reopening Sidetracks cannot restore the track that was stopped.
                session?.playback?.updateState(false, 0L)
                applyQueue(queue.stop(), positionMs = 0L, playing = false)
            }

            "unavailable" -> if (matchesCurrent(event)) {
                _state.update { it.copy(isPlaying = false, notice = "This track isn't available") }
            }
        }
    }

    private fun matchesCurrent(event: JSONObject): Boolean {
        val uri = event.optString("uri").takeIf { it.isNotEmpty() } ?: return true
        return _state.value.current?.uri?.equals(uri, ignoreCase = true) ?: true
    }

    private fun onPlaybackEnded() {
        val snapshot = _state.value
        val current = snapshot.current ?: return
        if (snapshot.repeat == RepeatMode.Track) {
            _state.update { it.copy(positionMs = 0L, isPlaying = true) }
            startRemote(current, 0L)
        } else {
            advance(auto = true, forceContextLoop = snapshot.repeat == RepeatMode.Context)
        }
    }

    private fun tick() {
        val s = _state.value
        val current = s.current ?: return
        if (!s.isPlaying) return
        val nextPosition = s.positionMs + 1_000L
        if (nextPosition < current.durationMs) {
            _state.update { it.copy(positionMs = nextPosition) }
        } else {
            when (s.repeat) {
                RepeatMode.Track -> _state.update { it.copy(positionMs = 0L) }
                RepeatMode.Context -> advance(auto = true, forceContextLoop = true)
                RepeatMode.Off -> advance(auto = true, forceContextLoop = false)
            }
        }
    }

    private fun advance(auto: Boolean, forceContextLoop: Boolean) {
        val s = _state.value
        val current = s.current ?: return
        val hasNext = s.upcoming.isNotEmpty()
        if (!hasNext && forceContextLoop) {
            val snapshot = queue.next(auto = true)
            snapshot.current?.let(::recorded)
            applyQueue(snapshot, positionMs = 0L, playing = snapshot.current?.playable == true)
            snapshot.current?.let { startRemote(it, 0L) }
            return
        }
        if (!hasNext && !auto) return
        if (!hasNext) {
            _state.update { it.copy(isPlaying = false, positionMs = 0L) }
            return
        }
        val snapshot = queue.next(auto = true)
        if (snapshot.current !== current) {
            snapshot.current?.let(::recorded)
            applyQueue(snapshot, positionMs = 0L, playing = snapshot.current?.playable == true)
            snapshot.current?.let { startRemote(it, 0L) }
        } else {
            _state.update { it.copy(isPlaying = false, positionMs = 0L) }
        }
    }

    /** Starts a real Spotify item; local test fixtures have no URI and keep the local simulation. */
    private fun startRemote(item: SpotifyMediaItem, positionMs: Long) {
        val uri = item.uri ?: return
        val session = session ?: return
        session.playback.setNowPlaying(item, positionMs)
        viewModelScope.launch {
            try {
                session.play(uri, positionMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPlaying = false, notice = playbackNotice(e)) }
            }
        }
    }

    private fun playbackNotice(error: Exception): String = when ((error as? SpotifyGatewayException)?.failure) {
        SpotifyFailure.Unavailable -> "Spotify playback is unavailable in this build"
        SpotifyFailure.NotConnected -> "Playback is not ready — try again"
        SpotifyFailure.AuthenticationExpired -> "Spotify session expired — sign in again"
        SpotifyFailure.Network -> "Couldn't reach Spotify"
        SpotifyFailure.RateLimited -> "Spotify is rate-limiting requests — try again shortly"
        SpotifyFailure.TrackUnavailable -> "This track isn't available"
        SpotifyFailure.Unknown, null -> error.message ?: "Playback failed"
    }

    private fun collectionKey(type: SpotifyCollectionType, id: String) = "${type.route}:$id"
}
