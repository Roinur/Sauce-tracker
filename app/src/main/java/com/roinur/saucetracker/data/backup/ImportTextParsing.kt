package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.feature.library.creators.*
import com.roinur.saucetracker.feature.library.detail.*
import com.roinur.saucetracker.feature.library.history.*
import com.roinur.saucetracker.feature.library.tags.*
import com.roinur.saucetracker.feature.settings.*
import com.roinur.saucetracker.feature.subscriptions.*
import com.roinur.saucetracker.feature.suggestions.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

internal fun decodeStrict(bytes: ByteArray, charset: java.nio.charset.Charset): String? {
    return try {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

internal fun spansOverlap(aStart: Int, aEndExclusive: Int, bStart: Int, bEndExclusive: Int): Boolean {
    return aStart < bEndExclusive && aEndExclusive > bStart
}

internal fun findSplitCodeSequences(text: String): List<SplitSequence> {
    val sequences = mutableListOf<SplitSequence>()
    SPLIT_CODE_PATTERN.findAll(text).forEach { match ->
        val raw = match.groupValues.getOrNull(1).orEmpty()
        val merged = raw.replace(Regex("[ \\t]+"), "")
        if (merged.isBlank() || merged.length > 8) return@forEach
        sequences += SplitSequence(
            start = match.range.first,
            endExclusive = match.range.last + 1,
            raw = raw,
            merged = merged
        )
    }
    return sequences
}

internal fun extractCandidates(
    text: String,
    splitSequences: List<SplitSequence>,
    combineSplitCodes: Boolean
): List<Pair<Int, Int>> {
    val blocked = splitSequences.map { it.start to it.endExclusive }
    val candidates = mutableListOf<Pair<Int, Int>>()

    CODE_PATTERN.findAll(text).forEach { match ->
        val spanStart = match.range.first
        val spanEnd = match.range.last + 1
        if (blocked.any { spansOverlap(spanStart, spanEnd, it.first, it.second) }) {
            return@forEach
        }

        val digits = match.groupValues.getOrNull(1).orEmpty()
        if (digits.length > 8) return@forEach
        val code = digits.toIntOrNull() ?: return@forEach
        if (code > 0) {
            candidates += code to digits.length
        }
    }

    if (combineSplitCodes) {
        splitSequences.forEach { seq ->
            val code = seq.merged.toIntOrNull() ?: return@forEach
            if (code > 0) {
                candidates += code to seq.merged.length
            }
        }
    }

    val deduped = LinkedHashMap<Int, Int>()
    candidates.forEach { (code, len) ->
        if (!deduped.containsKey(code)) {
            deduped[code] = len
        }
    }

    return deduped.entries.map { it.key to it.value }
}

internal val LocalCunnyModeEnabled = compositionLocalOf { false }

private val CUNNY_MORSE_MAP = mapOf(
    'a' to ".-",
    'b' to "-...",
    'c' to "-.-.",
    'd' to "-..",
    'e' to ".",
    'f' to "..-.",
    'g' to "--.",
    'h' to "....",
    'i' to "..",
    'j' to ".---",
    'k' to "-.-",
    'l' to ".-..",
    'm' to "--",
    'n' to "-.",
    'o' to "---",
    'p' to ".--.",
    'q' to "--.-",
    'r' to ".-.",
    's' to "...",
    't' to "-",
    'u' to "..-",
    'v' to "...-",
    'w' to ".--",
    'x' to "-..-",
    'y' to "-.--",
    'z' to "--..",
    '0' to "-----",
    '1' to ".----",
    '2' to "..---",
    '3' to "...--",
    '4' to "....-",
    '5' to ".....",
    '6' to "-....",
    '7' to "--...",
    '8' to "---..",
    '9' to "----."
)

internal fun toCunnyMorse(text: String): String {
    if (text.isBlank()) return text
    return buildString {
        text.forEachIndexed { index, raw ->
            when {
                raw.isWhitespace() -> {
                    if (isNotEmpty() && last() != ' ') append(' ')
                }
                else -> {
                    val pattern = CUNNY_MORSE_MAP[raw.lowercaseChar()]
                    if (pattern != null) {
                        if (isNotEmpty() && last() != ' ') append(' ')
                        pattern.forEach { symbol ->
                            append(
                                when (symbol) {
                                    '.' -> "\uD83D\uDE2D"
                                    '-' -> "\uD83D\uDCA2"
                                    else -> ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }.trim()
}
