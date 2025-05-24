package rip.sunrise.packets.serialization

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.serverbound.*
import java.io.*

@Sharable
object ObfuscatedEncoder : MessageToByteEncoder<Serializable>() {
    private const val TYPE_FAT_DESCRIPTOR: Int = 0
    private const val TYPE_THIN_DESCRIPTOR: Int = 1

    val map = mapOf(
        ScriptSessionResp::class   to "a9",
        RevisionInfoResp::class    to "a6",
        ScriptStartResp::class     to "Ad",
        ScriptListResp::class      to "a3",
        ScriptURLResp::class       to "b2",
        ScriptWrapper::class       to "b3",
        LoginResp::class           to "ap",
        ScriptOptionsResp::class   to "bd",
        EncryptedScriptResp::class to "aJ",

        LoginRequest::class to "aI",
        RevisionInfoRequest::class to "aQ",
        FreeScriptListRequest::class to "b1",
        PaidScriptListRequest::class to "aR",
        ScriptSessionRequest::class to "ba",
        ScriptURLRequest::class to "bB",
        ScriptOptionsRequest::class to "a1",
        EncryptedScriptRequest::class to "B5",
    ).map { (k, v) -> k.java.name to "org.dreambot.$v" }.toMap()

    override fun encode(ctx: ChannelHandlerContext, msg: Serializable, out: ByteBuf) {
        val startIdx = out.writerIndex()

        val byteOut = ByteBufOutputStream(out).also {
            it.write(ByteArray(4))
        }
        val objectOut = CustomObjectOutputStream(byteOut).also {
            it.writeObject(msg)
            it.flush()
            it.close()
        }

        byteOut.close()
        objectOut.close()

        val endIdx = out.writerIndex()
        out.setInt(startIdx, endIdx - startIdx - 4)
    }

    class CustomObjectOutputStream(out: OutputStream) : ObjectOutputStream(out) {
        override fun writeStreamHeader() {
            writeByte(STREAM_VERSION.toInt())
        }

        override fun writeClassDescriptor(desc: ObjectStreamClass) {
            val clazz = desc.forClass()

            if (clazz.isPrimitive || clazz.isArray || clazz.isInterface || desc.serialVersionUID == 0L) {
                write(TYPE_FAT_DESCRIPTOR)
                super.writeClassDescriptor(desc)
            } else {
                write(TYPE_THIN_DESCRIPTOR)
                val obfuscatedName = map[desc.name] ?: desc.name
                writeUTF(obfuscatedName)
            }
        }
    }
}
