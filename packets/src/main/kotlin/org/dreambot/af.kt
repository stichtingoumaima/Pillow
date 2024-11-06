package org.dreambot

import java.io.Serializable

/**
 * Some sort of keepalive request, maybe for paid scripts
 * v -> token
 * n -> nonce from a9
 * g -> incremented every packet, starts at 0
 * s -> last ran script id? seems to always be -1
 */
data class af(val v: String, val n: String, val g: Int, val s: Int) : Serializable