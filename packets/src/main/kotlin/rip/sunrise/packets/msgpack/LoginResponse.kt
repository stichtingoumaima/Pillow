package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker

const val LOGIN_RESPONSE_PACKET_ID = 2

data class LoginResponse(val username: String, val accountSessionToken: String, val sessionId: String, val userId: Int, val ranks: Set<Int>) {
    fun pack(): ByteArray {
        val packer = MessagePack.newDefaultBufferPacker()

        packer.packInt(LOGIN_RESPONSE_PACKET_ID)
        packer.packInt(0)

        val innerPacker = MessagePack.newDefaultBufferPacker()
        innerPacker.packString(username)
        innerPacker.packString(accountSessionToken)
        innerPacker.packString(sessionId)
        innerPacker.packInt(userId)

        innerPacker.packArrayHeader(ranks.size)
        ranks.forEach { innerPacker.packInt(it) }

        innerPacker.close()

        val innerPackerBytes = innerPacker.toByteArray()

        packer.packBinaryHeader(innerPackerBytes.size)
        packer.writePayload(innerPackerBytes)

        packer.close()
        return packer.toByteArray()
    }
}

fun MessageUnpacker.unpackLoginResponse(): LoginResponse {
    val unk = unpackInt() // TODO: Also retry count?
    unpackBinaryHeader() // NOTE: Packet size

    val username = unpackString()
    val accountSessionToken = unpackString()
    val sessionId = unpackString()
    val userId = unpackInt()

    val ranks = buildSet {
        repeat(unpackArrayHeader()) {
            add(unpackInt())
        }
    }

    assert(!hasNext()) { "Failed to consume whole stream" }
    return LoginResponse(username, accountSessionToken, sessionId, userId, ranks)
}