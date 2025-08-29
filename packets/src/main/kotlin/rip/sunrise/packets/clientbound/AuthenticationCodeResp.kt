package rip.sunrise.packets.clientbound

import java.io.Serializable

/**
 * Seems to have pretty little usage currently.
 *
 * j -> authentication code
 */
data class AuthenticationCodeResp(val j: String) : Serializable