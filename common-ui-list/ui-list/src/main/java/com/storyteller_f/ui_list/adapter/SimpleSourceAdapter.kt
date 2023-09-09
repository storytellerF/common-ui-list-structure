package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util

@Suppress("UNCHECKED_CAST")
open class SimpleSourceAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(group: String? = null) :
    PagingDataAdapter<IH, VH>(
        common_diff_util as DiffUtil.ItemCallback<IH>
    ) {
    private val proxy = object : DefaultAdapter<IH, VH>(group) {
        override fun getItemAbstract(position: Int) = getItem(position)
    }.apply {
        target = this@SimpleSourceAdapter
    }

    override fun onBindViewHolder(holder: VH, position: Int) = proxy.onBindViewHolder(holder, position)

    override fun getItemViewType(position: Int) = proxy.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = proxy.onCreateViewHolder(parent, viewType)
}
