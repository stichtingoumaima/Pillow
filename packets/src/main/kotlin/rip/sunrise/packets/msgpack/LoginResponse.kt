package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker

const val LOGIN_RESPONSE_PACKET_ID: Byte = 2

data class LoginResponse(
    var username: String,
    var accountSessionToken: String,
    var sessionId: String,
    var userId: Int,
    var ranks: Set<Int>,
) : Packet<LoginResponse>() {
    override val id = LOGIN_RESPONSE_PACKET_ID

    override fun packInner(packer: MessagePacker) {
        packer.packString(username)
        packer.packString(accountSessionToken)
        packer.packString(sessionId)
        packer.packInt(userId)

        packer.packArrayHeader(ranks.size)
        ranks.forEach { packer.packInt(it) }
    }

    companion object : PacketUnpacker<LoginResponse> {
        override fun unpack(unpacker: MessageUnpacker): LoginResponse {
            val username = unpacker.unpackString()
            val accountSessionToken = unpacker.unpackString()
            val sessionId = unpacker.unpackString()
            val userId = unpacker.unpackInt()

            val ranks = buildSet {
                repeat(unpacker.unpackArrayHeader()) {
                    add(unpacker.unpackInt())
                }
            }

            return LoginResponse(username, accountSessionToken, sessionId, userId, ranks)
        }
    }
}

fun MessageUnpacker.unpackLoginResponse(): LoginResponse {
    return unpackWith(LoginResponse.Companion)
}