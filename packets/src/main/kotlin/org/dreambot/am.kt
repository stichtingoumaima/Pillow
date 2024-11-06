package org.dreambot

import java.io.Serializable

/**
 * Response to af
 * d -> some kind of switch value
 * 1 stops the script because there was a problem connecting to DreamBot servers
 * 2 or 3 disconnects from socket for some reason
 * 4 stops the script because you no longer have access
 * 5 stops the script because of too many instances running
 * anything else sets nano time
 */
data class am(val d: Byte) : Serializable