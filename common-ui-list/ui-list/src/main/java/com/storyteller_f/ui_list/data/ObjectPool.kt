package com.storyteller_f.ui_list.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.storyteller_f.ui_list.core.Model

object ObjectPool {
    val map = mutableMapOf<Class<*>, Map<String, Record>>()

    /**
     * 更新对象，然后返回一个统一的数据
     */
    inline fun <reified T : Model> get(model: T, isFullVersion: Boolean = true): LiveData<out Model?> {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(produceKey(model))) {
            objMap[produceKey(model)] =
                Record(isFullVersion, MutableLiveData(model), System.currentTimeMillis())
        } else {
            objMap[produceKey(model)]?.let {
                it.isFullVersion = isFullVersion
                it.obj.value = model
                it.updatedTime = System.currentTimeMillis()
            }
        }
        return objMap[produceKey(model)]!!.obj.map {
            it as? T
        }
    }

    fun <T : Model> produceKey(model: T) = model.uniqueIdInOP()

    /**
     * 更新对象，然后返回一个统一的数据
     */
    fun <T : Model> update(model: T, isFullVersion: Boolean = true) {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model.commonId())) {
            objMap[model.commonId()] =
                Record(isFullVersion, MutableLiveData(model), System.currentTimeMillis())
        } else {
            objMap[model.commonId()]?.let {

            }
        }
    }

    /**
     * 更新对象，然后返回一个统一的数据
     */
    inline fun <reified T : Model> get(model: String, isFullVersion: Boolean = true): LiveData<out Model?> {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model)) {
            objMap[model] =
                Record(isFullVersion, MutableLiveData(), System.currentTimeMillis())
        }
        return objMap[model]?.obj!!.map {
            it as? T
        }
    }

    /**
     * 更新对象，然后返回一个统一的数据
     */
    inline fun <reified T : Model> getExisted(
        model: String,
        isFullVersion: Boolean = true
    ): LiveData<out Model?>? {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model)) {
            return null
        }
        return objMap[model]?.let {
            if (isFullVersion && !it.isFullVersion) null
            else it.obj.map { any ->
                any as? T
            }
        }
    }

    class Record(
        var isFullVersion: Boolean,
        val obj: MutableLiveData<Any>,
        var updatedTime: Long
    )
}