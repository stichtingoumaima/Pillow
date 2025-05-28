package rip.sunrise.packets.serverbound

import java.io.Serializable

/**
 * c -> ???
 * w -> ???
 */
data class GetTotalInstancesRequest(val c: String, val w: Int) : Serializable