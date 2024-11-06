package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * Script list request
 * c -> account session token
 */
data class FreeScriptListRequest(val a: String) : Serializable
