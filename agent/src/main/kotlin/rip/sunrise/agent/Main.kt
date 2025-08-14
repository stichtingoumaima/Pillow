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
            listOf(CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, productId: Int ->
            log("Checking whether we purchased product $productId")
            if (productId != ANTISPOOF_PRODUCT_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.dreambot.api.script.ScriptManager"),
            TargetMethod("hasPremiumScript", "(I)Z"),
            listOf(CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, productId: Int ->
            log("Checking whether we have premium script $productId")
            if (productId != ANTISPOOF_PREMIUM_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
            }
        }
    )

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.dreambot.api.script.ScriptManager"),
            TargetMethod("hasSDNScript", "(I)Z"),
            listOf(CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, productId: Int ->
            log("Checking whether we have SDN script $productId")
            if (productId != ANTISPOOF_SDN_CONSTANT && productId > 0) {
                ctx.setReturnValue(true)
            }
        }
    )
    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            InetSocketAddress::class.java,
            TargetMethod("createUnresolved", "(Ljava/lang/String;I)Ljava/net/InetSocketAddress;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0), CapturedArgument(Opcodes.ILOAD, 1))
        ) { ctx: Context, host: String, port: Int ->
            if (host == "cdn.dreambot.org" && port == 43831) {
                println("Redirecting netty from $host:$port to $SERVER_HOST:$SERVER_PORT")
                ctx.setReturnValue(InetSocketAddress.createUnresolved(SERVER_HOST, SERVER_PORT))
            }
        })

    var fakeSslHandler: Any? = null
    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.5C"),
            TargetMethod("14", "([Ljava/lang/Object;)Lorg/7A;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 0), CapturedArgument(Opcodes.ALOAD, 1))
        ) { ctx: Context, instance: Any, args: Array<Any> ->
            val handlers = args[0] as Array<*>
            val handler = handlers[0] as Any

            println("addLast call for handler!")
            println(handler.javaClass)

            if (handler.javaClass.name == "org.0T") {
                println("Found SSL Handler")
                fakeSslHandler = handler
                ctx.setReturnValue(instance)
            }
        })

    HookManager.addHook(
        InjectHook(
            HeadInjection(),
            Class.forName("org.5C"),
            TargetMethod("9D", "([Ljava/lang/Object;)Lorg/6V;"),
            listOf(CapturedArgument(Opcodes.ALOAD, 1))
        ) { ctx: Context, args: Array<Any> ->
            val clazz = args[0] as Class<*>

            if (clazz.name == "org.0T") {
                println("Get for SSL Handler")
                ctx.setReturnValue(fakeSslHandler)
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