package com.navidrome.auth

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import com.navidrome.api.NavidromeClient

/**
 * Gestor de autenticación para Navidrome
 */
class AuthManager(
    private var baseUrl: String = "",
    private var username: String = "",
    private var password: String = ""
) {
    private var client: NavidromeClient? = null
    
    var isConfigured: Boolean
        get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        private set(_) {}

    /**
     * Intentar conectar con las credenciales proporcionadas
     */
    suspend fun login(baseUrl: String, username: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                // Quitar trailing slash si lo hay
                this@AuthManager.baseUrl = baseUrl.trimEnd('/')
                this@AuthManager.username = username
                this@AuthManager.password = password
                
                client = NavidromeClient(this@AuthManager.baseUrl, username, password)
                client!!.ping().getOrThrow()
            }
        }
    }

    /**
     * Obtener cliente conectado
     */
    fun getClient(): NavidromeClient? = client

    /**
     * Cerrar sesión
     */
    fun logout() {
        client?.close()
        client = null
    }

    /**
     * Obtener URL guardada
     */
    fun getBaseUrl(): String = baseUrl

    /**
     * Obtener usuario guardado
     */
    fun getUsername(): String = username
}

/**
 * Credenciales (sin password, para mostrar en UI)
 */
@Serializable
data class Credentials(
    val baseUrl: String = "",
    val username: String = ""
)
