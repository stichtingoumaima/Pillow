package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * v -> account session id
 * b -> script instance session id
 */
data class ScriptOptionsRequest(val v: String, val b: String) : Serializable
