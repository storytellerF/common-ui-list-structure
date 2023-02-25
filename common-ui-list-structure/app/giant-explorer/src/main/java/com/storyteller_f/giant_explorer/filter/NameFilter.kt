package com.storyteller_f.giant_explorer.filter

import android.view.View
import android.view.ViewGroup
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.filter_core.config.SimpleRegExpConfigItem
import com.storyteller_f.filter_ui.adapter.FilterItemContainer
import com.storyteller_f.filter_ui.adapter.FilterItemViewHolder
import com.storyteller_f.filter_ui.adapter.FilterViewHolderFactory
import com.storyteller_f.filter_ui.filter.SimpleRegexpFilter

class NameFilterConfig(regexp: String) : SimpleRegExpConfigItem(regexp) {

}

class NameFilter(item: SimpleRegExpConfigItem?): SimpleRegexpFilter<FileSystemItemModel>("文件名", item) {
    override fun getItemViewType(): Int {
        return 1
    }

    override fun getMatchString(t: FileSystemItemModel?): CharSequence {
        return t?.name.orEmpty()
    }

    class ViewHolder(itemView: View): SimpleRegexpFilter.ViewHolder(itemView) {

    }

}

class FilterFactory: FilterViewHolderFactory() {
    override fun create(parent: ViewGroup, viewType: Int, container: FilterItemContainer): FilterItemViewHolder {
        SimpleRegexpFilter.ViewHolder.create(parent.context, container.frameLayout)
        return NameFilter.ViewHolder(container.view)
    }

}