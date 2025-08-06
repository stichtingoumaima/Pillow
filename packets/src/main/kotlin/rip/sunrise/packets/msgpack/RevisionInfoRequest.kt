package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker

const val REVISION_INFO_REQUEST_PACKET_ID: Byte = 83

data class RevisionInfoRequest(
    val accountSession: String,
    val clientHash: String,
    val javaagentFlags: String,
) : Packet<RevisionInfoRequest>() {
    override val id = REVISION_INFO_REQUEST_PACKET_ID

    override fun packInner(packer: MessagePacker) {
        packer.packString(accountSession)
        packer.packString(clientHash)
        packer.packString(javaagentFlags)
    }

    companion object : PacketUnpacker<RevisionInfoRequest> {
        override fun unpack(unpacker: MessageUnpacker): RevisionInfoRequest {
            val hardwareId = unpacker.unpackString()
            val sharedSecret = unpacker.unpackString()
            val javaagentFlags = unpacker.unpackString()

            return RevisionInfoRequest(hardwareId, sharedSecret, javaagentFlags)
        }
    }
}

fun MessageUnpacker.unpackRevisionInfoRequest(): RevisionInfoRequest {
    return unpackWith(RevisionInfoRequest)
}