package com.navidrome.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.security.MessageDigest

/**
 * Cliente para la API Subsonic/Navidrome
 * 
 * Documentación: https://www.navidrome.org/docs/developers/subsonic-api/
 */
class NavidromeClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val httpClient = HttpClient()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
    }
    
    private var salt: String = ""
    private var token: String = ""

    init {
        generateAuth()
    }

    private fun generateAuth() {
        salt = (1..10).map { ('a'..'z').random() }.joinToString("")
        token = md5(password + salt)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun buildUrl(endpoint: String, params: Map<String, String> = emptyMap()): String {
        val queryParams = params.toMutableMap()
        queryParams["u"] = username
        queryParams["t"] = token
        queryParams["s"] = salt
        queryParams["v"] = "1.16.1"
        queryParams["c"] = "NavidromeApp"
        queryParams["f"] = "json"

        val queryString = queryParams.toList().joinToString("&") { "${it.key}=${it.value}" }
        return "$baseUrl/rest/$endpoint?$queryString"
    }

    private inline fun <reified T> request(endpoint: String, params: Map<String, String> = emptyMap()): T {
        val url = buildUrl(endpoint, params)
        val response = httpClient.get(url)
        val body = response.bodyAsText()
        
        // La respuesta viene envuelta en "subsonic-response"
        val wrapper = json.decodeFromString<SubsonicResponse<T>>(body)
        return wrapper.subsonicResponse
    }

    /**
     * Test de conectividad
     */
    suspend fun ping(): Result<PingResponse> = runCatching {
        request<PingResponse>("ping.view")
    }

    /**
     * Obtener todos los artistas
     */
    suspend fun getArtists(): Result<ArtistsResponse> = runCatching {
        request<ArtistsResponse>("getArtists.view")
    }

    /**
     * Obtener detalles de un artista
     */
    suspend fun getArtist(id: String): Result<ArtistResponse> = runCatching {
        request<ArtistResponse>("getArtist.view", mapOf("id" to id))
    }

    /**
     * Obtener álbum
     */
    suspend fun getAlbum(id: String): Result<AlbumResponse> = runCatching {
        request<AlbumResponse>("getAlbum.view", mapOf("id" to id))
    }

    /**
     * Obtener canción
     */
    suspend fun getSong(id: String): Result<SongResponse> = runCatching {
        request<SongResponse>("getSong.view", mapOf("id" to id))
    }

    /**
     * URL para stream de audio
     */
    fun getStreamUrl(id: String, format: String = "mp3"): String {
        return buildUrl("stream.view", mapOf("id" to id, "format" to format))
    }

    /**
     * URL para portada de álbum
     */
    fun getCoverArtUrl(id: String, size: Int = 300): String {
        return buildUrl("getCoverArt.view", mapOf("id" to id, "size" to size.toString()))
    }

    /**
     * Marcar como favorita
     */
    suspend fun star(id: String): Result<Unit> = runCatching {
        request<StarResponse>("star.view", mapOf("id" to id))
    }

    /**
     * Quitar de favoritos
     */
    suspend fun unstar(id: String): Result<Unit> = runCatching {
        request<StarResponse>("unstar.view", mapOf("id" to id))
    }

    /**
     * Lista de álbumes (recientes, favoritos, etc.)
     * Tipos: random, newest, recent, frequent, alphabetialByName, alphabetialByArtist
     */
    suspend fun getAlbumList(type: String, size: Int = 20): Result<AlbumListResponse> = runCatching {
        request<AlbumListResponse>("getAlbumList2.view", mapOf("type" to type, "size" to size.toString()))
    }

    /**
     * Buscar
     */
    suspend fun search(query: String): Result<SearchResponse> = runCatching {
        request<SearchResponse>("search3.view", mapOf("query" to query))
    }

    /**
     * Cerrar el cliente
     */
    fun close() {
        httpClient.close()
    }
}

// ============== WRAPPER ==============

@Serializable
data class SubsonicResponse<T>(
    @SerialName("subsonic-response")
    val subsonicResponse: T
)

@Serializable
data class StarResponse(val status: String = "ok")

@Serializable
data class PingResponse(
    val status: String = "ok",
    val version: String = "",
    val type: String = ""
)

@Serializable
data class ArtistsResponse(val artists: ArtistsIndex)

@Serializable
data class ArtistsIndex(val index: List<ArtistIndexEntry> = listOf())

@Serializable
data class ArtistIndexEntry(
    val name: String = "",
    val artist: List<Artist> = listOf()
)

@Serializable
data class ArtistResponse(val artist: Artist)

@Serializable
data class Artist(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null
)

@Serializable
data class AlbumResponse(val album: Album)

@Serializable
data class Album(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null
)

@Serializable
data class SongResponse(val song: Song)

@Serializable
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val album: String = "",
    val albumId: String? = null,
    val coverArt: String? = null,
    val duration: Int = 0,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val size: Long = 0,
    val suffix: String? = null,
    val bitRate: Int? = null,
    val path: String? = null
)

@Serializable
data class AlbumListResponse(val albumList: AlbumList)

@Serializable
data class AlbumList(val album: List<Album> = listOf())

@Serializable
data class SearchResponse(val searchResult: SearchResult)

@Serializable
data class SearchResult(
    val artist: List<Artist> = listOf(),
    val album: List<Album> = listOf(),
    val song: List<Song> = listOf()
)
