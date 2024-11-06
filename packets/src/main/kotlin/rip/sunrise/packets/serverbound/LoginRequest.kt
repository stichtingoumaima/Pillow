package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * Login request
 * l -> username
 * x -> password
 * f -> constant, probably client version, or hwid
 */
data class LoginRequest(val l: String, val x: String, val f: String) : Serializable
