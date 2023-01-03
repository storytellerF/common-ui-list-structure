package com.example.ui_list_annotation_common

import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class UIListHolderZoom<T> {
    val setTemp = mutableMapOf<TypeElement, PackageElement>()
    val holderEntryTemp = mutableListOf<Entry<T>>()
    val clickEventMapTemp = mutableMapOf<String?, Map<String, List<Event<T>>>>()
    val longClickEventMapTemp = mutableMapOf<String?, Map<String, List<Event<T>>>>()

    fun debugState(): String {
        return "click: ${clickEventMapTemp.size} long:${longClickEventMapTemp.size}  holder:${holderEntryTemp.size} set:${setTemp.size}"
    }

    fun addHolderEntry(list: List<Entry<T>>) {
        holderEntryTemp.addAll(getGroupedHolders(holderEntryTemp + list))
    }

    fun logPackageInfo(first: TypeElement, second: PackageElement) {
        setTemp[first] = second
    }

    fun addClickEvent(map: Map<String, Map<String, List<Event<T>>>>) {
        clickEventMapTemp.putAll(map)
    }

    fun addLongClick(map: Map<String, Map<String, List<Event<T>>>>) {
        longClickEventMapTemp.putAll(map)
    }

    /**
     * 合并相同itemHolder的entry
     */
    fun getGroupedHolders(mutableList: List<Entry<T>>): List<Entry<T>> {
        return mutableList.groupBy {
            it.itemHolderFullName
        }.map { entry ->
            val first = entry.value.first()
            entry.value.subList(1, entry.value.size).map {
                first.viewHolders.putAll(it.viewHolders)
            }
            first
        }
    }

    fun getAllSource(): List<T> {
        return clickEventMapTemp.flatMap { entry ->
            entry.value.flatMap { it.value }.map { it.origin }
        }.plus(longClickEventMapTemp.flatMap { entry ->
            entry.value.flatMap { it.value }.map { it.origin }
        }).plus(holderEntryTemp.map { it.origin })
    }

    fun importHolders(): String {
        return holderEntryTemp.joinToString("\n") { entry ->
            val bindings = entry.viewHolders.values.map {
                it.bindingFullName
            }.distinct().joinToString("\n") {
                "import ${it};"
            }
            val viewHolders = entry.viewHolders.values.joinToString("\n") {
                "import ${it.viewHolderFullName};"
            }
            bindings + "\n" + viewHolders + "\nimport ${entry.itemHolderFullName};//item holder end\n"
        }
    }

    val hasComposeView: Boolean
        get() {
            return holderEntryTemp.any { entry ->
                entry.viewHolders.any {
                    !it.value.bindingName.endsWith("Binding")
                }
            }
        }

    private fun receiverList(longClickEvent: Map<String?, Map<String, List<Event<T>>>>) =
        longClickEvent.flatMap { it.value.flatMap { entry -> entry.value } }
            .map { it.receiverFullName }
            .distinct()

    fun importReceiverClass(): String {
        val flatMap = receiverList(this.clickEventMapTemp)
        val flatMap2 = receiverList(longClickEventMapTemp)
        return flatMap.plus(flatMap2).joinToString("\n") {
            "import $it;\n"
        }
    }
}

inline fun <T, K1 : Any, K2 : Any, V> Iterable<T>.doubleLayerGroupBy(doubleKeySelector: (T) -> Pair<K1, K2>?, valueTransform: (T) -> V): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = doubleKeySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }

    }
    return destination
}

inline fun <T, K1, K2, V> Sequence<T>.doubleLayerGroupBy(doubleKeySelector: (T) -> Pair<K1, K2>?, valueTransform: (T) -> V): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = doubleKeySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }

    }
    return destination
}