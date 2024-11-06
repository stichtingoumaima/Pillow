package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * Script list request
 * c -> account session token
 */
data class PaidScriptListRequest(val a: String) : Serializable
