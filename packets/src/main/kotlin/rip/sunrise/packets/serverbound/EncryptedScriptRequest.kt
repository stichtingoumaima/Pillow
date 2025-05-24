package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * f -> script id
 * o -> script session id
 * e -> account session id
 */
data class EncryptedScriptRequest(val f: Int, val o: String, val e: String) : Serializable