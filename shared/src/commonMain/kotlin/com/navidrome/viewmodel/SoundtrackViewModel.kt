package com.navidrome.viewmodel

import kotlinx.coroutines.*
import com.navidrome.api.NavidromeClient
import com.navidrome.model.Artist
import com.navidrome.model.Album
import com.navidrome.model.Song

/**
 * Estado de la navegación
 */
sealed class Screen {
    object Login : Screen()
    object ArtistList : Screen()
    data class ArtistDetail(val artistId: String) : Screen()
    data class AlbumDetail(val albumId: String) : Screen()
    object Player : Screen()
}

/**
 * ViewModel principal de la aplicación
 */
class SoundtrackViewModel(
    private val client: NavidromeClient?
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Estado
    var currentScreen: Screen = Screen.ArtistList
        private set
    
    var artists: List<Artist> = emptyList()
        private set
    
    var currentArtist: Artist? = null
        private set
    
    var albums: List<Album> = emptyList()
        private set
    
    var currentAlbum: Album? = null
        private set
    
    var songs: List<Song> = emptyList()
        private set
    
    var isLoading: Boolean = false
        private set
    
    var errorMessage: String? = null
        private set
    
    // Reproductor
    var currentSong: Song? = null
        private set
    
    var isPlaying: Boolean = false
        private set
    
    var currentSongIndex: Int = -1
        private set

    // Navigation
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun goBack(): Boolean {
        currentScreen = when (currentScreen) {
            is Screen.ArtistList -> return false // No going back from root
            is Screen.ArtistDetail -> Screen.ArtistList
            is Screen.AlbumDetail -> currentArtist?.let { Screen.ArtistDetail(it.id) } ?: Screen.ArtistList
            is Screen.Player -> currentAlbum?.let { Screen.AlbumDetail(it.id) } ?: Screen.ArtistList
            else -> Screen.ArtistList
        }
        return true
    }

    // API Calls
    fun loadArtists() {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            client.getArtists().fold(
                onSuccess = { response ->
                    artists = response.artists.index.flatMap { it.artist }
                    currentScreen = Screen.ArtistList
                },
                onFailure = { e ->
                    errorMessage = "Error cargando artistas: ${e.message}"
                }
            )
            
            isLoading = false
        }
    }

    fun loadArtistAlbums(artistId: String) {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            client.getArtist(artistId).fold(
                onSuccess = { response ->
                    currentArtist = response.artist
                    // getArtist returns artist with albums
                    albums = response.artist.albumList ?: emptyList()
                    currentScreen = Screen.ArtistDetail(artistId)
                },
                onFailure = { e ->
                    errorMessage = "Error cargando álbumes: ${e.message}"
                }
            )
            
            isLoading = false
        }
    }

    fun loadAlbumSongs(albumId: String) {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            client.getAlbum(albumId).fold(
                onSuccess = { response ->
                    currentAlbum = response.album
                    songs = response.album.songList ?: emptyList()
                    currentScreen = Screen.AlbumDetail(albumId)
                },
                onFailure = { e ->
                    errorMessage = "Error cargando canciones: ${e.message}"
                }
            )
            
            isLoading = false
        }
    }

    // Player
    fun playSong(song: Song, playlist: List<Song> = songs) {
        currentSong = song
        currentSongIndex = playlist.indexOf(song)
        isPlaying = true
        currentScreen = Screen.Player
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun nextSong() {
        if (currentSongIndex < songs.size - 1) {
            currentSongIndex++
            currentSong = songs[currentSongIndex]
        }
    }

    fun previousSong() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            currentSong = songs[currentSongIndex]
        }
    }

    fun getStreamUrl(songId: String): String? = client?.getStreamUrl(songId)

    fun getCoverArtUrl(coverArtId: String?, size: Int = 300): String? {
        if (coverArtId == null) return null
        return client?.getCoverArtUrl(coverArtId, size)
    }

    fun cleanup() {
        scope.cancel()
    }
}
