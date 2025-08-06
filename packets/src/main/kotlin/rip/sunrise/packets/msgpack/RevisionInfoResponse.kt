package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

const val REVISION_INFO_RESPONSE_PACKET_ID: Byte = 84

data class RevisionInfoResponse(
    val revisionData: String,
    val javaagentChecksum: Int
) : Packet<RevisionInfoResponse>() {
    override val id = REVISION_INFO_RESPONSE_PACKET_ID

    override fun packInner(packer: MessagePacker) {
        val gzipped = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use {
                it.write(revisionData.toByteArray())
            }

            baos.toByteArray()
        }

        packer.packBinaryHeader(gzipped.size)
        packer.writePayload(gzipped)
        packer.packInt(javaagentChecksum)
    }

    companion object : PacketUnpacker<RevisionInfoResponse> {
        override fun unpack(unpacker: MessageUnpacker): RevisionInfoResponse {
            val length = unpacker.unpackBinaryHeader()
            val gzip = unpacker.readPayload(length)
            val javaagentChecksum = unpacker.unpackInt()

            val revisionData = String(GZIPInputStream(ByteArrayInputStream(gzip)).readAllBytes())

            return RevisionInfoResponse(revisionData, javaagentChecksum)
        }
    }
}

fun MessageUnpacker.unpackRevisionInfoResponse(): RevisionInfoResponse {
    return unpackWith(RevisionInfoResponse.Companion)
}