package rip.sunrise.agent

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

const val SERVER_HOST = "localhost"
const val SERVER_PORT = 1337

fun premain(args: String?, inst: Instrumentation) {
    inst.addTransformer(Transformer())
}

fun ClassNode.toByteArray(flags: Int = ClassWriter.COMPUTE_MAXS): ByteArray =
    ClassWriter(flags).also { accept(it) }.toByteArray()

fun getConnectMethod(node: ClassNode): MethodNode? {
    return node.methods.firstOrNull { m ->
        m.instructions.filterIsInstance<MethodInsnNode>()
            .any { insn -> insn.name == "connect" && insn.owner == "io/netty/bootstrap/Bootstrap" }
    }
}

fun getAddLast(node: ClassNode): MethodNode? {
    return node.methods.firstOrNull { m ->
        m.instructions.filterIsInstance<MethodInsnNode>()
            .any { insn -> insn.name == "addLast" && insn.owner == "io/netty/channel/ChannelPipeline" }
    }
}

class Transformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        if (!className.startsWith("org/dreambot")) return classfileBuffer

        val node = ClassNode().also {
            ClassReader(classfileBuffer).accept(it, 0)
        }

        // Replace server host and port with our values
        getConnectMethod(node)?.let { m ->
            m.instructions.filterIsInstance<MethodInsnNode>().filter { it.name == "connect" }.forEach { insn ->
                println("Found connect call in ${node.name}.${m.name}")

                val list = InsnList().apply {
                    add(InsnNode(Opcodes.POP2))
                    add(LdcInsnNode(SERVER_HOST))
                    add(LdcInsnNode(SERVER_PORT))
                }
                m.instructions.insertBefore(insn, list)
            }

            return node.toByteArray()
        }

        // Remove SSL
        getAddLast(node)?.let { m ->
            m.instructions.filterIsInstance<MethodInsnNode>().filter { it.name == "addLast" }[0].also { insn ->
                println("Found SSL addLast call in ${node.name}.${m.name}")

                m.instructions.set(insn, InsnNode(Opcodes.POP))
            }

            return node.toByteArray()
        }

        // Always return true from both hasSDNScript and hasPremiumScript
        // NOTE: I think the list can't be shared.
        if (className == "org/dreambot/api/script/ScriptManager") {
            node.methods.first { it.name == "hasSDNScript" }.also {
                println("Hooked hasSDNScript")
                val list = InsnList().apply {
                    add(InsnNode(Opcodes.ICONST_1))
                    add(InsnNode(Opcodes.IRETURN))
                }
                it.instructions.clear()
                it.instructions.insert(list)
            }
            node.methods.first { it.name == "hasPremiumScript" }.also {
                println("Hooked hasPremiumScript")
                val list = InsnList().apply {
                    add(InsnNode(Opcodes.ICONST_1))
                    add(InsnNode(Opcodes.IRETURN))
                }
                it.instructions.clear()
                it.instructions.insert(list)
            }
            node.methods.first { it.name == "hasPurchasedScript" }.also {
                println("Hooked hasPurchasedScript")
                val list = InsnList().apply {
                    add(InsnNode(Opcodes.ICONST_1))
                    add(InsnNode(Opcodes.IRETURN))
                }
                it.instructions.clear()
                it.instructions.insert(list)
            }

            return node.toByteArray()
        }

        return classfileBuffer
    }
}