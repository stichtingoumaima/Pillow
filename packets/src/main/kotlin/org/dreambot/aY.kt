package org.dreambot

import java.io.Serializable

/**
 * UserIdRequest.
 * Left obfuscated because it makes no sense for this to exist - it might evolve. (see [rip.sunrise.packets.clientbound.LoginResp])
 *
 * e -> account session token
 */
data class aY(val e: String) : Serializable