package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter

class ManualAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) : ListAdapter<IH, VH>(SimpleSourceAdapter.common_diff_util as DiffUtil.ItemCallback<IH>) {
    val d = DefaultAdapter<IH, VH>(key).apply {
        target = this@ManualAdapter
    }

    var type: String
        get() = d.type
        set(value) {
            d.type = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = d.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = d.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) = d.onBindViewHolder(holder, position)

}