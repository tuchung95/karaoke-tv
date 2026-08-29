package com.athr.karaoketv.util

import java.text.Normalizer

/**
 * Vietnamese-aware text folding. Karaoke on a TV remote means people type without
 * diacritics ("gan nhu la"), so everything searchable is stored pre-folded.
 */
object TextNormalizer {

    private val COMBINING = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    /** "Gần Như Là" -> "gan nhu la". Handles đ/Đ, which NFD does not decompose. */
    fun fold(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return COMBINING.replace(decomposed, "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
            .lowercase()
    }

    /** Folded text reduced to single-space-separated alphanumeric tokens. */
    fun key(input: String): String =
        NON_ALNUM.replace(fold(input), " ").trim()

    /** "Gần Như Là" -> "gnl", so a viewer can jump to a song with three keypresses. */
    fun initials(input: String): String =
        key(input).split(' ').mapNotNull { it.firstOrNull() }.joinToString("")

    /** Query as typed on the on-screen keyboard, folded the same way as the index. */
    fun query(input: String): String = key(input)
}
