package com.storyteller_f.ui_list.data

import androidx.lifecycle.MutableLiveData
import com.storyteller_f.ui_list.core.Model

object ObjectPool {
    private val map = mutableMapOf<Class<*>, Map<String, Record<*>>>()

    /**
     * 更新对象，然后返回一个统一的数据
     */
    fun <T : Model> get(model: T, isFullVersion: Boolean = true): MutableLiveData<out Model> {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model.commonDatumId())) {
            objMap[model.commonDatumId()] =
                Record<T>(isFullVersion, MutableLiveData(model), System.currentTimeMillis())
        } else {
            objMap[model.commonDatumId()]?.let {
                it.isFullVersion = isFullVersion
                it.obj.value = model
                it.updatedTime = System.currentTimeMillis()
            }
        }
        return objMap[model.commonDatumId()]!!.obj
    }

    /**
     * 更新对象，然后返回一个统一的数据
     */
    fun <T : Model> update(model: T, isFullVersion: Boolean = true) {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model.commonDatumId())) {
            objMap[model.commonDatumId()] =
                Record<T>(isFullVersion, MutableLiveData(model), System.currentTimeMillis())
        } else {
            objMap[model.commonDatumId()]?.let {

            }
        }
    }

    /**
     * 更新对象，然后返回一个统一的数据
     */
    fun <T : Model> get(model: String, isFullVersion: Boolean = true): MutableLiveData<out Model> {
        val key = model.javaClass
        if (!map.containsKey(key)) {
            map[key] = mutableMapOf()
        }
        val objMap = map[key]?.toMutableMap()
        if (!objMap!!.containsKey(model)) {
            objMap[model] =
                Record(isFullVersion, MutableLiveData(), System.currentTimeMillis())
        }
        return objMap[model]?.obj!!
    }

    /**
     * 更新对象，然后返回一个统一的数据
     */
    fun <T : Model> getExisted(
        model: String,
        isFullVersion: Boolean = true
    ): MutableLiveData<out Model>? {
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
            else it.obj
        }
    }

    class Record<T : Model>(
        var isFullVersion: Boolean,
        val obj: MutableLiveData<T>,
        var updatedTime: Long
    )
}