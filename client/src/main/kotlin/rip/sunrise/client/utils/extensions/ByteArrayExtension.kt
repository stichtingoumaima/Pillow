package rip.sunrise.client.utils.extensions

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun ByteArray.decryptScript(key: ByteArray): ByteArray {
    require(key.size == 32) { "AES Key must be 32 bytes long" }

    val iv = this.take(16).toByteArray()

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))

    return cipher.doFinal(this.drop(16).toByteArray())
}

fun ByteArray.sha256(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}