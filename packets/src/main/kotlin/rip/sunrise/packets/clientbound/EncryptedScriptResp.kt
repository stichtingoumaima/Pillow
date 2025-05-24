package rip.sunrise.packets.clientbound

import java.io.Serializable

/**
 * Encrypted Script Response, unlike the old response, has encryption and caching. The IV is in the first 16 bytes of the encrypted jar.
 *
 * w -> url to the encrypted file
 * t -> cache file name on disk (in DreamBot/BotData/.cache/bin/)
 * l -> MD5 hash encrypted jar
 * z -> 256bit AES key
 * g -> trial time in seconds, <= 0 or above 1d is forever
 */
data class EncryptedScriptResp(val w: String, val t: String, val l: String, val z: String, val g: Int) :
    Serializable