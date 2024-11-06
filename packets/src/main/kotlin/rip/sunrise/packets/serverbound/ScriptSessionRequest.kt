package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * Script start
 * g -> account session token:MID:some sort of id
 */
data class ScriptSessionRequest(val g: String) : Serializable
