package rip.sunrise.packets.clientbound

import java.io.Serializable

/**
 * i -> username
 * d -> account session token
 * s -> session token (for re-logging)
 * a -> forum ranks
 * k -> uid
 */
data class LoginResp(val i: String, val p: String, val s: String, val a: HashSet<Int>, val k: Int) : Serializable
