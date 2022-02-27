@file:Suppress("UNCHECKED_CAST")

package com.storyteller_f.ui_list.core

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*

val list = mutableListOf<(ViewGroup) -> AbstractAdapterViewHolder<out DataItemHolder>>()

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
    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.onBind(getItem(position) as IH)

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return super.getItemViewType(position)
        val c = item::class.java
        return registerCenter[c]!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        list[viewType].invoke(parent) as VH

    companion object {
        val common_diff_util = object : DiffUtil.ItemCallback<DataItemHolder>() {
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

class SimpleDataAdapter<IH : DataItemHolder, VH : AbstractAdapterViewHolder<IH>> :
    ListAdapter<IH, VH>(SimpleSourceAdapter.common_diff_util as DiffUtil.ItemCallback<IH>) {
    var last = mutableListOf<IH>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH = list[viewType].invoke(parent) as VH

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return super.getItemViewType(position)
        return registerCenter[item::class.java]!!
    }


    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.onBind(getItem(position) as IH)

    override fun submitList(list: MutableList<IH>?) {
        super.submitList(list)
        if (list != null) {
            last = list
        }
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(last, from, to)
        notifyItemMoved(from, to)
    }

}