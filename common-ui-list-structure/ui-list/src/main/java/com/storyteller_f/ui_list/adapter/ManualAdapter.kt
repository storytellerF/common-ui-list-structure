package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter

@Suppress("UNCHECKED_CAST")
class ManualAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) : ListAdapter<IH, VH>(SimpleSourceAdapter.common_diff_util as DiffUtil.ItemCallback<IH>) {
    private val proxy = DefaultAdapter<IH, VH>(key).apply {
        target = this@ManualAdapter
    }

    var type: String
        get() = proxy.type
        set(value) {
            proxy.type = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = proxy.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = proxy.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) = proxy.onBindViewHolder(holder, position)

}