package rip.sunrise.packets.msgpack

import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker

abstract class Packet<T : Packet<T>> {
    abstract val id: Byte
    var counter: Int = -1

    fun pack(counter: Int): ByteArray {
        return MessagePack.newDefaultBufferPacker().use { packer ->
            packer.packByte(id) // packet id
            packer.packInt(counter) // counter

            val innerBytes = MessagePack.newDefaultBufferPacker().use {
                packInner(it); it
            }.toByteArray()

            packer.packBinaryHeader(innerBytes.size) // data size
            packer.writePayload(innerBytes) // data

            packer.toByteArray()
        }
    }

    protected abstract fun packInner(packer: MessagePacker)
}

interface PacketUnpacker<T : Packet<T>> {
    fun unpack(unpacker: MessageUnpacker): T
}

fun <T : Packet<T>> MessageUnpacker.unpackWith(packetUnpacker: PacketUnpacker<T>): T {
    val counter = unpackInt()
    unpackBinaryHeader() // NOTE: Packet size

    val packet = packetUnpacker.unpack(this)
    packet.counter = counter

    assert(!hasNext()) { "Failed to consume whole stream" }
    return packet
}