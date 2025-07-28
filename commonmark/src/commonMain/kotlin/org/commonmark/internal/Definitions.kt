package org.commonmark.internal

import org.commonmark.node.DefinitionMap
import kotlin.reflect.KClass

class Definitions {
    private val definitionsByType = hashMapOf<KClass<*>, DefinitionMap<*>>()

    fun <D : Any> addDefinitions(definitionMap: DefinitionMap<D>) {
        val existingMap = getMap(definitionMap.getType())
        if (existingMap == null) {
            definitionsByType.put(definitionMap.getType(), definitionMap)
        } else {
            existingMap.addAll(definitionMap)
        }
    }

    fun <V : Any> getDefinition(type: KClass<V>, label: String): V? {
        val definitionMap = getMap(type)
        if (definitionMap == null) {
            return null
        }
        return definitionMap.get(label)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> getMap(type: KClass<V>): DefinitionMap<V>? {
        return definitionsByType[type] as DefinitionMap<V>?
    }
}
