package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker

const val LOGIN_REQUEST_PACKET_ID: Byte = 1

data class LoginRequest(
    val username: String,
    val password: String,
    val sessionId: String,
    val sharedSecret: String,
    val hardwareId: String
) : Packet<LoginRequest>() {
    override val id = LOGIN_REQUEST_PACKET_ID

    override fun packInner(packer: MessagePacker) {
        packer.packString(username)
        packer.packString(password)
        packer.packString(sessionId)
        packer.packString(sharedSecret)
        packer.packString(hardwareId)
    }

    companion object : PacketUnpacker<LoginRequest> {
        override fun unpack(unpacker: MessageUnpacker): LoginRequest {
            val username = unpacker.unpackString()
            val password = unpacker.unpackString()
            val sessionId = unpacker.unpackString()
            val sharedSecret = unpacker.unpackString()
            val hardwareId = unpacker.unpackString()

            return LoginRequest(username, password, sessionId, sharedSecret, hardwareId)
        }
    }
}

fun MessageUnpacker.unpackLoginRequest(): LoginRequest {
    return unpackWith(LoginRequest.Companion)
}