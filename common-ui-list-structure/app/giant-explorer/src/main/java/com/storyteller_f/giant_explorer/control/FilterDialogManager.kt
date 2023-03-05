package com.storyteller_f.giant_explorer.control

import android.content.Context
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.filter_core.config.FilterConfigItem
import com.storyteller_f.filter_ui.FilterDialog
import com.storyteller_f.giant_explorer.filter.FilterFactory
import com.storyteller_f.giant_explorer.filter.NameFilter
import com.storyteller_f.giant_explorer.filter.NameSort
import com.storyteller_f.giant_explorer.filter.SortFactory
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog

val factory: RuntimeTypeAdapterFactory<FilterConfigItem> = RuntimeTypeAdapterFactory.of(FilterConfigItem::class.java, "config-item-key").registerSubtype(NameFilter.Config::class.java, "name")!!

class FilterDialogManager {
    lateinit var filterDialog: FilterDialog<FileSystemItemModel>
    lateinit var sortDialog: SortDialog<FileSystemItemModel>

    fun init(
        context: Context, filters: (List<Filter<FileSystemItemModel>>) -> Unit,
        sort: (List<SortChain<FileSystemItemModel>>) -> Unit
    ) {
        filterDialog = FilterDialog(context, listOf(NameFilter(NameFilter.Config("^$"))), FilterFactory(), object : FilterDialog.Listener<FileSystemItemModel> {
            override fun onSaveState(filters: MutableList<Filter<FileSystemItemModel>>?): MutableList<FilterConfigItem> {
                return filters.orEmpty().map {
                    (it as NameFilter).item
                }.toMutableList()
            }

            override fun onActiveListSelected(dialog: FilterDialog<FileSystemItemModel>, configItems: MutableList<FilterConfigItem>?) = dialog.add(buildFilterActive(configItems.orEmpty()))

            override fun onActiveChanged(dialog: FilterDialog<FileSystemItemModel>) {
                filters(dialog.activeFilters)
            }

        }, "filter", factory)
        val adapterFactory =
            RuntimeTypeAdapterFactory.of(SortConfigItem::class.java, "sort-item-key")
                .registerSubtype(NameSort.Item::class.java, "name")
        sortDialog = SortDialog(context, listOf(NameSort(NameSort.Item())), SortFactory(), object : SortDialog.Listener<FileSystemItemModel> {
            override fun onSaveState(chains: MutableList<SortChain<FileSystemItemModel>>?): MutableList<SortConfigItem> {
                return chains.orEmpty().map {
                    NameSort.Item()
                }.toMutableList()
            }

            override fun onActiveSelected(sortDialog: SortDialog<FileSystemItemModel>, configItems: MutableList<SortConfigItem>?) = sortDialog.add(buildSortActive(configItems.orEmpty()))

            override fun onActiveChanged(sortDialog: SortDialog<FileSystemItemModel>) {
                sort(sortDialog.active)
            }

        }, adapterFactory)
    }

    fun showFilter() {
        filterDialog.show()
    }

    fun showSort() {
        sortDialog.show()
    }


}

fun buildFilterActive(configItems: List<FilterConfigItem>): List<Filter<FileSystemItemModel>> = configItems.map {
    NameFilter(it as NameFilter.Config)
}

fun buildSortActive(configItems: List<SortConfigItem>): List<SortChain<FileSystemItemModel>> = configItems.map {
    NameSort(NameSort.Item())
}