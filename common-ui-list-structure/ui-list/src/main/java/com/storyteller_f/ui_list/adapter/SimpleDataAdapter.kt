package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.source.SimpleDataViewModel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持排序，需要搭配SimpleDataViewModel和SimpleDataRepository
 */
@Suppress("UNCHECKED_CAST")
class SimpleDataAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(val key: String? = null) :
    ListAdapter<IH, VH>(SimpleSourceAdapter.common_diff_util as DiffUtil.ItemCallback<IH>) {
    var last = mutableListOf<IH>()

    /**
     * 下一次的observe 不处理
     */
    private val mPending: AtomicBoolean = AtomicBoolean(true)

    var dataHook: SimpleDataViewModel.DataHook<*, IH, *>? = null
    val d = DefaultAdapter<IH, VH>(key).apply {
        target = this@SimpleDataAdapter
    }

    var type: String
        get() = d.type
        set(value) {
            d.type = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = d.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = d.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) = d.onBindViewHolder(holder, position)

    override fun submitList(list: MutableList<IH>?) {
        super.submitList(list)
        if (list != null) {
            last = list
        }
    }

    fun submitData(dataHook: SimpleDataViewModel.DataHook<*, IH, *>) {
        if (mPending.get()) {
            this.dataHook = dataHook
            submitList(dataHook.list.toMutableList())
        }
        mPending.compareAndSet(false, true)
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(last, from, to)
        dataHook?.swap(from, to)
        mPending.set(false)
        dataHook?.viewModel?.reset(last)
        notifyItemMoved(from, to)
    }

}