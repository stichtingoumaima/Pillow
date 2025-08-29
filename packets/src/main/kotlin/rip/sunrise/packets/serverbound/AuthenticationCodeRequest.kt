package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * r -> account session token
 */
data class AuthenticationCodeRequest(val r: String) : Serializable