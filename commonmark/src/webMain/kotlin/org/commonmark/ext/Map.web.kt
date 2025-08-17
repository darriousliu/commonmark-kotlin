package org.commonmark.ext

actual fun <K, V> MutableMap<K, V>.computeIfAbsent2(
    key: K,
    mappingFunction: (K) -> V
): V {
    return get(key) ?: mappingFunction(key).also { put(key, it) }
}