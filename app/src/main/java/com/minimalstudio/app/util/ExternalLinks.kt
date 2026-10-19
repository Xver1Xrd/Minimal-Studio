package com.minimalstudio.app.util

object ExternalLinks {
    /** Only ordinary web URLs can leave the app. Do not accept javascript:, file:, intent:, etc. */
    fun safeHttpUrl(value: String): String? {
        val input = value.trim()
        if (input.isBlank()) return null
        val normalized = when {
            input.startsWith("https://", true) || input.startsWith("http://", true) -> input
            "://" in input || input.contains(':') -> return null
            else -> "https://$input"
        }
        val host = normalized.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        if (host.isBlank() || host.any { it.isWhitespace() }) return null
        return normalized
    }
}
