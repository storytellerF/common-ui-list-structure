package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util
import com.storyteller_f.ui_list.source.SimpleDataViewModel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持排序，需要搭配SimpleDataViewModel和SimpleDataRepository
 */
@Suppress("UNCHECKED_CAST")
class SimpleDataAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(group: String? = null) :
    ListAdapter<IH, VH>(common_diff_util as DiffUtil.ItemCallback<IH>) {

    private var fatData: SimpleDataViewModel.FatData<*, IH, *>? = null
    val d = DefaultAdapter<IH, VH>(group).apply {
        target = this@SimpleDataAdapter
    }
    private val skipNext = AtomicBoolean(false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = d.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = d.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) = d.onBindViewHolder(holder, position)

    fun submitData(fatData: SimpleDataViewModel.FatData<*, IH, *>) {
        if (skipNext.compareAndSet(true, false)) return
        this.fatData = fatData
        submitList(fatData.list)
    }

    fun swap(from: Int, to: Int) {
        skipNext.set(true)
        fatData?.swap(from, to)
        notifyItemMoved(from, to)
    }
}
