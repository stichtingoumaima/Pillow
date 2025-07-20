package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * u -> username
 * q -> password
 * y -> session token, or empty for initial login
 * r -> shared secret
 */
data class LoginRequest(val u: String, val q: String, val y: String, val r: String) : Serializable
