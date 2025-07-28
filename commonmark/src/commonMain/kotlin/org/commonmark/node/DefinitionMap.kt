package org.commonmark.node

import org.commonmark.ext.putIfAbsent
import org.commonmark.internal.util.Escaping
import kotlin.reflect.KClass

/**
 * A map that can be used to store and look up reference definitions by a label. The labels are case-insensitive and
 * normalized, the same way as for [LinkReferenceDefinition] nodes.
 *
 * @param <D> the type of value
</D> */
class DefinitionMap<D : Any>(private val type: KClass<D>) {

    // LinkedHashMap for determinism and to preserve document order
    private val definitions = linkedMapOf<String, D>()

    fun getType(): KClass<D> {
        return type
    }

    fun addAll(that: DefinitionMap<D>) {
        for (entry in that.definitions.entries) {
            // Note that keys are already normalized, so we can add them directly
            definitions.putIfAbsent(entry.key, entry.value)
        }
    }

    /**
     * Store a new definition unless one is already in the map. If there is no definition for that label yet, return null.
     * Otherwise, return the existing definition.
     *
     *
     * The label is normalized by the definition map before storing.
     */
    fun putIfAbsent(label: String, definition: D): D? {
        val normalizedLabel = Escaping.normalizeLabelContent(label)

        // spec: When there are multiple matching link reference definitions, the first is used
        return definitions.putIfAbsent(normalizedLabel, definition)
    }

    /**
     * Look up a definition by label. The label is normalized by the definition map before lookup.
     *
     * @return the value or null
     */
    fun get(label: String): D? {
        val normalizedLabel: String? = Escaping.normalizeLabelContent(label)
        return definitions[normalizedLabel]
    }

    fun keySet(): Set<String> {
        return definitions.keys
    }

    fun values(): Collection<D> {
        return definitions.values
    }
}
