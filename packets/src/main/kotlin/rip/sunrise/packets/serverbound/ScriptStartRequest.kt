package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * Another script start request
 * b -> account session token
 * r -> nonce from a9
 * l -> ???
 * p -> script data (name, author, category, version, main class, ???, ???, ???)
 * t -> ???
 * q -> ???
 */
data class ScriptStartRequest(val b: String, val r: String, val l: String, val p: String, val t: String, val q: String) : Serializable
