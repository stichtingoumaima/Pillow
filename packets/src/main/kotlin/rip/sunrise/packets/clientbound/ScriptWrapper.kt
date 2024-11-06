package rip.sunrise.packets.clientbound

import java.io.Serializable

data class ScriptWrapper(
    val c: Int,    // unused
    val w: String, // description
    val m: String, // script name
    val i: Int,    // something for hashcode
    val e: Double, // version
    val u: String, // unused
    val p: String, // unused
    val l: String, // author
    val v: String, // thread url
    val q: String, // image url
    val d: Int,    // storeId
    val x: Int,    // scriptId
    val t: Boolean // trial? not sure
) : Serializable
