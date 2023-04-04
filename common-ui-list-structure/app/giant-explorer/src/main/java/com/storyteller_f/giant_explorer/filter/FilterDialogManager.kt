package com.storyteller_f.giant_explorer.filter

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.storyteller_f.compat_ktx.getSerializableCompat
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.filter_core.config.FilterConfigItem
import com.storyteller_f.filter_ui.FilterDialog
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog
import java.io.Serializable

val factory: RuntimeTypeAdapterFactory<FilterConfigItem> = RuntimeTypeAdapterFactory.of(FilterConfigItem::class.java, "config-item-key").registerSubtype(NameFilter.Config::class.java, "name")!!

class FilterDialogManager {
    lateinit var filterDialog: FilterDialog<FileSystemItemModel>
    lateinit var sortDialog: SortDialog<FileSystemItemModel>

    fun init(
        context: Context, filters: (List<Filter<FileSystemItemModel>>) -> Unit,
        sort: (List<SortChain<FileSystemItemModel>>) -> Unit
    ) {
        filterDialog = FilterDialog(context, listOf(NameFilter(NameFilter.Config("^$"))), FilterFactory(), object : FilterDialog.Listener<FileSystemItemModel> {
            override fun onSaveState(filters: List<Filter<FileSystemItemModel>>?): List<FilterConfigItem> {
                return filters.saveFilterItem()
            }

            override fun onActiveListSelected(dialog: FilterDialog<FileSystemItemModel>, configItems: MutableList<FilterConfigItem>?) = dialog.add(configItems.orEmpty().buildFilterActive())

            override fun onActiveChanged(dialog: FilterDialog<FileSystemItemModel>) {
                filters(dialog.activeFilters)
            }

        }, "filter", factory)
        val adapterFactory =
            RuntimeTypeAdapterFactory.of(SortConfigItem::class.java, "sort-item-key")
                .registerSubtype(NameSort.Item::class.java, "name")
        sortDialog = SortDialog(context, listOf(NameSort(NameSort.Item())), SortFactory(), object : SortDialog.Listener<FileSystemItemModel> {
            override fun onSaveState(chains: MutableList<SortChain<FileSystemItemModel>>?): List<SortConfigItem> {
                return chains.saveSortItem()
            }

            override fun onActiveSelected(sortDialog: SortDialog<FileSystemItemModel>, configItems: MutableList<SortConfigItem>?) = sortDialog.add(configItems.orEmpty().buildSortActive())

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

private fun List<SortChain<FileSystemItemModel>>?.saveSortItem(): List<SortConfigItem> =
    orEmpty().map {
        NameSort.Item()
    }.toMutableList()

private fun List<Filter<FileSystemItemModel>>?.saveFilterItem(): List<FilterConfigItem> =
    orEmpty().map {
        (it as NameFilter).item
    }.toMutableList()

fun List<FilterConfigItem>?.buildFilterActive(): List<Filter<FileSystemItemModel>> = this?.map {
    NameFilter(it as NameFilter.Config)
}.orEmpty()

fun List<SortConfigItem>?.buildSortActive(): List<SortChain<FileSystemItemModel>> = this?.map {
    NameSort(NameSort.Item())
}.orEmpty()

class FilterDataWrapper(val list: List<FilterConfigItem>) : Serializable
class SortDataWrapper(val list: List<SortConfigItem>) : Serializable

class FilterViewModel(handle: SavedStateHandle, defaultValue: List<Filter<FileSystemItemModel>>) : ViewModel() {
    val data = MutableLiveData<List<Filter<FileSystemItemModel>>>()

    init {
        val bundle = handle.get<Bundle>("filter")
        val cache = bundle?.getSerializableCompat("data", FilterDataWrapper::class.java)
        data.value = cache?.list?.buildFilterActive() ?: defaultValue
        handle.setSavedStateProvider("filter") {
            bundleOf("data" to FilterDataWrapper(data.value.saveFilterItem()))
        }
    }
}

class SortViewModel(handle: SavedStateHandle, defaultValue: List<SortChain<FileSystemItemModel>>) : ViewModel() {
    val data = MutableLiveData<List<SortChain<FileSystemItemModel>>>()

    init {
        val cache = handle.get<Bundle>("sort")?.getSerializableCompat("data", SortDataWrapper::class.java)
        data.value = cache?.list?.buildSortActive() ?: defaultValue
        handle.setSavedStateProvider("sort") {
            bundleOf("data" to SortDataWrapper(data.value.saveSortItem()))
        }
    }
}

val buildFilterDialogState: (SavedStateHandle, FilterDialog<FileSystemItemModel>) -> FilterViewModel = { handle, dialog ->
    FilterViewModel(handle, dialog.currentConfig()?.configItems.buildFilterActive())
}
val buildSortDialogState: (SavedStateHandle, SortDialog<FileSystemItemModel>) -> SortViewModel = { handle, dialog ->
    SortViewModel(handle, dialog.current()?.configItems.buildSortActive())
}