package org.commonmark.ext

fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? {
    val v = get(key)
    if (v == null) {
        put(key, value)
    }
    return v
}

expect fun <K, V> MutableMap<K, V>.computeIfAbsent2(key: K, mappingFunction: (K) -> V): V