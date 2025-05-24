package rip.sunrise.server.utils.extensions

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

fun ByteArray.md5sum(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this)
}

fun ByteArray.encryptScriptAES(key: ByteArray, iv: ByteArray): ByteArray {
    require(key.size == 32) { "AES Key must be 32 bytes long" }

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))

    return iv + cipher.doFinal(this)
}
