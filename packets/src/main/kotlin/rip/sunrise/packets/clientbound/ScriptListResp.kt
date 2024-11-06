package rip.sunrise.packets.clientbound

import java.io.Serializable

/**
 * Script list response
 * v -> scripts
 */
data class ScriptListResp(val v: Collection<ScriptWrapper>) : Serializable
