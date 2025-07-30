package rip.sunrise.client.config

data class ScriptConfig(
    val name: String,
    val description: String,
    val version: Double,
    val author: String,
    val imageUrl: String,
    val threadUrl: String,

    val jarFile: String,
    val optionFile: String
)