package rip.sunrise.client

import rip.sunrise.client.utils.extensions.sha256
import java.io.File
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlin.random.Random

object UserAnonymization {
    private val secretFile = File(".anonymize_secret")

    @OptIn(ExperimentalStdlibApi::class)
    fun getHwid(username: String): String {
        val rand = getPrng(username)
        return rand.nextBytes(16).toHexString()
    }

    private fun getPrng(username: String): Random {
        val hash = (username.toByteArray() + getSecret()).sha256()
        val buf = ByteBuffer.wrap(hash)

        var seed = 0L
        repeat(4) {
            seed = seed xor buf.long
        }

        return Random(seed)
    }

    /**
     * Looks up a stored secret. Meant to be unique between users to avoid fingerprinting of the deterministic PRNG.
     */
    private fun getSecret(): ByteArray {
        if (!secretFile.exists()) {
            secretFile.writeBytes(SecureRandom.getInstanceStrong().generateSeed(32))
        }

        return secretFile.readBytes()
    }
}