package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker

const val LOGIN_REQUEST_PACKET_ID = 1

data class LoginRequest(
    val retryCount: Int,
    val username: String,
    val password: String,
    val sessionId: String,
    val sharedSecret: String,
    val hardwareId: String
) {
    fun pack(): ByteArray {
        val packer = MessagePack.newDefaultBufferPacker()

        packer.packInt(LOGIN_REQUEST_PACKET_ID)
        packer.packInt(retryCount)

        val innerPacker = MessagePack.newDefaultBufferPacker()
        innerPacker.packString(username)
        innerPacker.packString(password)
        innerPacker.packString(sessionId)
        innerPacker.packString(sharedSecret)
        innerPacker.packString(hardwareId)
        innerPacker.close()

        val innerPackerBytes = innerPacker.toByteArray()

        packer.packBinaryHeader(innerPackerBytes.size)
        packer.writePayload(innerPackerBytes)

        packer.close()
        return packer.toByteArray()
    }
}

fun MessageUnpacker.unpackLoginRequest(): LoginRequest {
    val retryCount = unpackInt()
    println("Got retry: $retryCount")

    val dataLen = unpackBinaryHeader()
    println("Got data: $dataLen")

    val username = unpackString()
    val password = unpackString()
    val sessionId = unpackString()
    val sharedSecret = unpackString()
    val hardwareId = unpackString()

    assert(!hasNext()) { "Failed to consume whole stream" }
    return LoginRequest(retryCount, username, password, sessionId, sharedSecret, hardwareId)
}