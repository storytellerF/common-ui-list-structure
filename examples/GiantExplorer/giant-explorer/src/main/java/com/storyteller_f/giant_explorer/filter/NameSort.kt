package com.storyteller_f.giant_explorer.filter

import android.view.View
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.sort_core.config.SortChain
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.adapter.SortItemContainer
import com.storyteller_f.sort_ui.adapter.SortItemViewHolder
import com.storyteller_f.sort_ui.adapter.SortViewHolderFactory

class NameSort(item: SortConfigItem) : SortChain<FileSystemItemModel>("name sort", item) {
    override fun compare(o1: FileSystemItemModel, o2: FileSystemItemModel): Int {
        return o1.name.compareTo(o2.name)
    }

    override fun dup(): Any {
        return NameSort(item.dup() as SortConfigItem)
    }

    override val itemViewType: Int
        get() {
            return 1
        }

    class ViewHolder(itemView: View) : SortItemViewHolder.Simple<FileSystemItemModel>(itemView)

    class Item(sortDirection: Int = up) : SortConfigItem(sortDirection) {

        override fun dup(): Any {
            return Item(sortDirection)
        }

    }
}

class SortFactory : SortViewHolderFactory<FileSystemItemModel>() {
    override fun create(
        viewType: Int,
        container: SortItemContainer
    ): SortItemViewHolder<FileSystemItemModel> {
        SortItemViewHolder.Simple.create(container)
        return NameSort.ViewHolder(container.view)
    }

}