package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util
import com.storyteller_f.ui_list.source.SimpleDataViewModel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持排序，需要搭配SimpleDataViewModel和SimpleDataRepository
 */
@Suppress("UNCHECKED_CAST")
class SimpleDataAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) :
    ListAdapter<IH, VH>(common_diff_util as DiffUtil.ItemCallback<IH>) {

    /**
     * 决定下一次的live 出发的observe 是否处理
     */
    private val receiveDataChange: AtomicBoolean = AtomicBoolean(true)

    private var fatData: SimpleDataViewModel.FatData<*, IH, *>? = null
    val d = DefaultAdapter<IH, VH>(key).apply {
        target = this@SimpleDataAdapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = d.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = d.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) = d.onBindViewHolder(holder, position)

    fun submitData(fatData: SimpleDataViewModel.FatData<*, IH, *>) {
        this.fatData = fatData
        submitList(fatData.list)
    }

    fun swap(from: Int, to: Int) {
        fatData?.swap(from, to)
        notifyItemMoved(from, to)
    }

}