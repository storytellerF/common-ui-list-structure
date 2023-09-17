package com.storyteller_f.giant_explorer.filter

import android.view.View
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.filter_core.config.SimpleRegExpConfigItem
import com.storyteller_f.filter_core.filter.simple.SimpleRegExpFilter
import com.storyteller_f.filter_ui.adapter.FilterItemContainer
import com.storyteller_f.filter_ui.adapter.FilterItemViewHolder
import com.storyteller_f.filter_ui.adapter.FilterViewHolderFactory
import com.storyteller_f.filter_ui.filter.SimpleRegExpFilterViewHolder

class NameFilter(item: SimpleRegExpConfigItem) :
    SimpleRegExpFilter<FileSystemItemModel>("文件名", item) {
    override val itemViewType: Int
        get() {
            return 1
        }

    override fun dup(): Any {
        return NameFilter(item.dup() as SimpleRegExpConfigItem)
    }

    override fun getMatchString(t: FileSystemItemModel) = t.name

    class ViewHolder(itemView: View) : SimpleRegExpFilterViewHolder<FileSystemItemModel>(itemView)

    class Config(regexp: String) : SimpleRegExpConfigItem(regexp) {
        override fun dup(): Any {
            return Config(regexp)
        }

    }

}

class FilterFactory : FilterViewHolderFactory<FileSystemItemModel>() {
    override fun create(
        viewType: Int,
        container: FilterItemContainer
    ): FilterItemViewHolder<FileSystemItemModel> {
        SimpleRegExpFilterViewHolder.create(container)
        return NameFilter.ViewHolder(container.view)
    }

}