@file:Suppress("UNCHECKED_CAST")

package com.storyteller_f.ui_list.core

import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*

val list = mutableListOf<(ViewGroup, String) -> AbstractViewHolder<out DataItemHolder>>()

val registerCenter = mutableMapOf<Class<out DataItemHolder>, Int>()

interface DataItemHolder {
    /**
     * 可以直接进行强制类型转换，无需判断
     */
    fun areItemsTheSame(other: DataItemHolder): Boolean
    fun areContentsTheSame(other: DataItemHolder): Boolean = this == other
}

abstract class AbstractViewHolder<IH : DataItemHolder>(val view: View) :
    RecyclerView.ViewHolder(view) {
    private var _itemHolder: IH? = null
    lateinit var keyed: String
    val itemHolder get() = _itemHolder as IH
    fun onBind(itemHolder: IH) {
        this._itemHolder = itemHolder
        bindData(itemHolder)
    }

    abstract fun bindData(itemHolder: IH)

    fun getColor(@ColorRes id: Int) = ContextCompat.getColor(view.context, id)
}

abstract class AdapterViewHolder<IH : DataItemHolder>(binding: ViewBinding) :
    AbstractViewHolder<IH>(binding.root)

open class DefaultAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) : RecyclerView.Adapter<VH>() {
    var target: RecyclerView.Adapter<VH>? = null
    var type = ""
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = (list[viewType].invoke(parent, type) as VH).apply {
        keyed = key ?: "default"
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.onBind(getItemAbstract(position) as IH)
    }

    protected open fun getItemAbstract(position: Int): IH? {
        return if (target is ListAdapter<*, *>) {
            (target as ListAdapter<IH, VH>).currentList[position] as IH
        } else throw NotImplementedError("无法获取对应item holder")
    }

    override fun getItemCount() = 0

    override fun getItemViewType(position: Int): Int {
        val item = getItemAbstract(position) ?: return super.getItemViewType(position)
        return registerCenter[item::class.java] ?: throw Exception("${item::class.java.canonicalName} not found.registerCenter count: ${registerCenter.size}")
    }

}