package com.storyteller_f.ui_list.core

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

val list = mutableListOf<(ViewGroup) -> AbstractAdapterViewHolder<*>>()

val registerCenter = mutableMapOf<Class<out DataItemHolder>, Int>()

abstract class DataItemHolder {
    /**
     * 可以直接进行强制类型转换，无需判断
     */
    abstract fun areItemsTheSame(other: DataItemHolder): Boolean
    fun areContentsTheSame(other: DataItemHolder): Boolean = this == other
}

abstract class AbstractAdapterViewHolder<IH : DataItemHolder>(view: View) :
    RecyclerView.ViewHolder(view) {
    private var _itemHolder: IH? = null
    val itemHolder get() = _itemHolder as IH
    fun onBind(itemHolder: IH) {
        this._itemHolder = itemHolder
        bindData(itemHolder)
    }

    abstract fun bindData(itemHolder: IH)
}

abstract class AdapterViewHolder<IH : DataItemHolder>(binding: ViewBinding) :
    AbstractAdapterViewHolder<IH>(binding.root)

open class SimpleSourceAdapter<IH : DataItemHolder, VH : AbstractAdapterViewHolder<IH>> :
    PagingDataAdapter<IH, VH>(
        common_diff_util as DiffUtil.ItemCallback<IH>
    ) {
    override fun onBindViewHolder(holder: VH, position: Int) {
        val itemHolder = getItem(position) as IH
        holder.onBind(itemHolder)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return super.getItemViewType(position)
        val c = item::class.java
        return registerCenter[c]!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return list[viewType].invoke(parent) as VH
    }

    companion object {
        private val common_diff_util = object : DiffUtil.ItemCallback<DataItemHolder>() {
            override fun areItemsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean {
                return if (oldItem.javaClass == newItem.javaClass) {
                    oldItem.areItemsTheSame(newItem)
                } else false
            }

            override fun areContentsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean =
                oldItem.areContentsTheSame(newItem)
        }
    }
}