package org.commonmark.ext

actual fun <K, V> MutableMap<K, V>.computeIfAbsent2(
    key: K,
    mappingFunction: (K) -> V
): V {
    return computeIfAbsent(key, mappingFunction)
}