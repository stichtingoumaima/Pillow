package rip.sunrise.agent

import org.objectweb.asm.Opcodes
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.managers.HookManager
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.net.InetSocketAddress
import java.net.URL

const val SERVER_HOST = "localhost"
const val HTTP_HOST = "localhost"
const val SERVER_PORT = 1337

const val ANTISPOOF_PRODUCT_CONSTANT = 6925123
const val ANTISPOOF_PREMIUM_CONSTANT = 5136948
const val ANTISPOOF_SDN_CONSTANT = 2562934

fun premain(args: String?, inst: Instrumentation) {
    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            URL::class.java,
            TargetMethod("getAuthority", "()Ljava/lang/String;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0))
        ) { ctx: Context, instance: URL ->
            // TODO:
            //  Get the caller in a better way: 0 is lambda, 1 is hooked method,
            //  2 is caller (invoke handler), 3 is Method.invoke, 4 is actual caller
            val caller = Throwable().stackTrace[4].className
            if (instance.host == HTTP_HOST && caller.startsWith("org.dreambot")) {
                log("Spoofing script host!")
                ctx.setReturnValue("cloudflarestorage.com")
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            URL::class.java,
            TargetMethod("getProtocol", "()Ljava/lang/String;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0))
        ) { ctx: Context, instance: URL ->
            val caller = Throwable().stackTrace[2].className
            if (instance.host == HTTP_HOST && caller.startsWith("org.dreambot")) {
                log("Spoofing protocol!")
                ctx.setReturnValue("https")
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            InetSocketAddress::class.java,
            TargetMethod("getHostString", "()Ljava/lang/String;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0))
        ) { ctx: Context, instance: InetSocketAddress ->
            if (instance.hostString == SERVER_HOST) {
                log("Spoofing host!")
                ctx.setReturnValue("cdn.dreambot.org")
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.dreambot.api.script.ScriptManager"),
            TargetMethod("hasPurchasedScript", "(I)Z"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0), CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, instance: Any, productId: Int ->
            log("Checking whether we purchased product $productId")
            if (productId != ANTISPOOF_PRODUCT_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
                return@InjectHook
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.dreambot.api.script.ScriptManager"),
            TargetMethod("hasPremiumScript", "(I)Z"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0), CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, instance: Any, productId: Int ->
            log("Checking whether we have premium script $productId")
            if (productId != ANTISPOOF_PREMIUM_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
                return@InjectHook
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.dreambot.api.script.ScriptManager"),
            TargetMethod("hasSDNScript", "(I)Z"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0), CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, instance: Any, productId: Int ->
            log("Checking whether we have SDN script $productId")
            if (productId != ANTISPOOF_SDN_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
                return@InjectHook
            }
        }
    )

    // TODO: Hey, DreamBot owners - please bump the netty version to not use java 6.
    var fakeSslHandler: Any? = null
    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Method::class.java,
            TargetMethod("invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
            listOf(
                CapturedArgument(Opcodes.ALOAD, 0),
                CapturedArgument(Opcodes.ALOAD, 1),
                CapturedArgument(Opcodes.ALOAD, 2)
            )
        ) { ctx: Context, method: Method, instance: Any?, args: Array<Any>? ->
            // Don't set the SslHandler
            if (method.declaringClass.name == "io.netty.channel.ChannelPipeline" && method.name == "addLast") {
                // NOTE: For some reason it's an array, not a single handler.
                val handlers = args!![0]
                if (!handlers.javaClass.isArray) return@InjectHook

                val handler = (handlers as Array<*>)[0]!!
                if (!Class.forName("io.netty.handler.ssl.SslHandler").isInstance(handler)) return@InjectHook

                fakeSslHandler = handler
                ctx.setReturnValue(instance)
            }

            // Spoof SslHandler
            else if (method.declaringClass.name == "io.netty.channel.ChannelPipeline" && method.name == "get") {
                val handlerType = args!![0] as Class<*>
                if (handlerType.name != "io.netty.handler.ssl.SslHandler") return@InjectHook
                ctx.setReturnValue(fakeSslHandler)
            }

            // Redirect the connection
            else if (method.declaringClass.name == "io.netty.bootstrap.Bootstrap" && method.name == "connect") {
                val origHost = args!![0]
                val origPort = args[1]

                println("Redirecting netty from $origHost:$origPort to $SERVER_HOST:$SERVER_PORT")

                val future = Class.forName("io.netty.bootstrap.Bootstrap")
                    .getDeclaredMethod("connect", String::class.java, Int::class.java)
                    .invoke(instance, SERVER_HOST, SERVER_PORT)

                ctx.setReturnValue(future)
            }
        })

    InjectApi.transform(inst)
}

val DEBUG = System.getProperty("pillow.debug")?.isNotEmpty() == true
fun log(message: String) {
    if (DEBUG) {
        println(message)
    }
}