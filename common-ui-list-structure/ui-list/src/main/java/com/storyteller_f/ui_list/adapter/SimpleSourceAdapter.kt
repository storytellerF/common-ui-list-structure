package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter

open class SimpleSourceAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) :
    PagingDataAdapter<IH, VH>(
        common_diff_util as DiffUtil.ItemCallback<IH>
    ) {
    val d = DefaultAdapter<IH, VH>(key).apply {
        target = this@SimpleSourceAdapter
    }

    var type: String
        get() = d.type
        set(value) {
            d.type = value
        }

    override fun onBindViewHolder(holder: VH, position: Int) = d.onBindViewHolder(holder, position)

    override fun getItemViewType(position: Int) = d.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = d.onCreateViewHolder(parent, viewType)

    companion object {
        val common_diff_util = object : DiffUtil.ItemCallback<DataItemHolder>() {
            override fun areItemsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean {
                return when {
                    oldItem === newItem -> true
                    oldItem.javaClass == newItem.javaClass -> {
                        oldItem.areItemsTheSame(newItem)
                    }
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean =
                oldItem.areContentsTheSame(newItem)
        }
    }
}