package rip.sunrise.packets.clientbound

import java.io.Serializable

/**
 * Another script start response
 * r -> whether the script should stop
 */
data class ScriptStartResp(val r: Boolean) : Serializable
