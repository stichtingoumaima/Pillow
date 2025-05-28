package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * h -> ???
 * q -> ???
 */
data class GetActiveInstancesRequest(val h: String, val q: Int) : Serializable