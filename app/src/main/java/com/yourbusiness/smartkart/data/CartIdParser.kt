package com.yourbusiness.smartkart.data

object CartIdParser {

    private val CART_ID_PATTERN = Regex("""CART_\d+""", RegexOption.IGNORE_CASE)

    /**
     * Extracts a cart ID from raw QR text.
     * Handles plain IDs like "CART_001" and URLs that contain the ID.
     */
    fun parse(rawQrValue: String): String? {
        val trimmed = rawQrValue.trim()
        if (trimmed.isBlank()) return null

        CART_ID_PATTERN.find(trimmed)?.value?.let { match ->
            return match.uppercase()
        }

        return trimmed
            .substringAfterLast('/')
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
