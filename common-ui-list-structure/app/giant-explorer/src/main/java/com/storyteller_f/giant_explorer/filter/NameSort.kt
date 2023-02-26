package com.storyteller_f.giant_explorer.filter

import android.view.View
import android.view.ViewGroup
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.adapter.SortItemContainer
import com.storyteller_f.sort_ui.adapter.SortItemViewHolder
import com.storyteller_f.sort_ui.adapter.SortViewHolderFactory

class NameSort(item: SortConfigItem?) : SortChain<FileSystemItemModel>("name sort", item) {
    override fun currentCompare(o1: FileSystemItemModel?, o2: FileSystemItemModel?): Int {
        return o1?.name.orEmpty().compareTo(o2?.name.orEmpty())
    }

    override fun copy(): Any {
        return NameSort(item.copy() as SortConfigItem?)
    }

    override fun getItemViewType(): Int {
        return 1
    }

    class ViewHolder(itemView: View) : SortItemViewHolder.Simple(itemView) {

    }

    class Item(sortDirection: Int = up) : SortConfigItem(sortDirection) {

        override fun copy(): Any {
            return Item(sortDirection)
        }

    }
}

class SortFactory : SortViewHolderFactory() {
    override fun create(parent: ViewGroup, viewType: Int, container: SortItemContainer): SortItemViewHolder {
        SortItemViewHolder.Simple.create(parent.context, container.frameLayout)
        return NameSort.ViewHolder(container.view)
    }

}