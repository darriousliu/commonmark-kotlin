package org.commonmark.internal.util

import io.github.mrl.commonmark.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import org.commonmark.ext.toChars

object Html5Entities {
    private val NAMED_CHARACTER_REFERENCES: Map<String, String> = readEntities()
    private const val ENTITY_PATH = "files/org/commonmark/internal/util/entities.txt"

    fun entityToString(input: String): String {
        if (!input.startsWith("&") || !input.endsWith(";")) {
            return input
        }

        var value = input.substring(1, input.length - 1)
        if (value.startsWith("#")) {
            value = value.substring(1)
            var base = 10
            if (value.startsWith("x") || value.startsWith("X")) {
                value = value.substring(1)
                base = 16
            }

            try {
                val codePoint = value.toInt(base)
                if (codePoint == 0) {
                    return "\uFFFD"
                }
                return codePoint.toChars().concatToString()
            } catch (_: IllegalArgumentException) {
                return "\uFFFD"
            }
        } else {
            val s = NAMED_CHARACTER_REFERENCES[value]
            return s ?: input
        }
    }

    private fun readEntities(): Map<String, String> {
        val entities = hashMapOf<String, String>()
        val lines =
            runBlocking(Dispatchers.IO) { Res.readBytes(ENTITY_PATH).decodeToString().lines() }
        lines.forEach { line ->
            if (line.isEmpty()) return@forEach
            val equal = line.indexOf("=")
            if (equal == -1) return@forEach // 跳过格式不对的行
            val key = line.substring(0, equal)
            val value = line.substring(equal + 1)
            entities[key] = value
        }
        entities["NewLine"] = "\n"
        return entities
    }
}
