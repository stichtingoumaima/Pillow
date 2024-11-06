package rip.sunrise.packets.serialization

import io.netty.handler.codec.serialization.ClassResolver
import io.netty.handler.codec.serialization.ClassResolvers
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.serverbound.*

object ObfuscatedClassResolver : ClassResolver {
    val defaultResolver = ClassResolvers.cacheDisabled(null)

    val map = mapOf(
        "aI" to LoginRequest::class,
        "aQ" to RevisionInfoRequest::class,
        "b1" to FreeScriptListRequest::class,
        "aR" to PaidScriptListRequest::class,
        "ba" to ScriptSessionRequest::class,
        "bB" to ScriptURLRequest::class,
        "bq" to ScriptStartRequest::class,
        "a1" to ScriptOptionsRequest::class,

        "a9" to ScriptSessionResp::class,
        "a6" to RevisionInfoResp::class,
        "Ad" to ScriptStartResp::class,
        "a3" to ScriptListResp::class,
        "b2" to ScriptURLResp::class,
        "b3" to ScriptWrapper::class,
        "ap" to LoginResp::class,
        "bd" to ScriptOptionsResp::class,
    ).map { (k, v) -> "org.dreambot.$k" to v.java }.toMap()

    override fun resolve(className: String?): Class<*> {
        return map[className] ?: defaultResolver.resolve(className)
    }
}