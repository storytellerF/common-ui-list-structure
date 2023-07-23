package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.config_core.ConfigItem
import com.storyteller_f.config_edit.DefaultDialog
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.giant_explorer.filter.NameSort
import com.storyteller_f.giant_explorer.filter.SortFactory
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog
import java.io.IOException

val activeSortChains = MutableLiveData<List<SortChain<FileSystemItemModel>>>()

class SortDialogFragment : CommonDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?) = setupSortDialog().selfDialog

    @Throws(IOException::class)
    private fun setupSortDialog() = SortDialog(
        requireContext(),
        suffix,
        sortChains,
        listener,
        SortFactory(),
        adapterFactory
    )

    private val listener =
        object : DefaultDialog.Listener<SortChain<FileSystemItemModel>, SortConfigItem> {
            override fun onSaveState(oList: List<SortChain<FileSystemItemModel>>) =
                oList.map {
                    NameSort.Item()
                }.toMutableList()

            override fun onActiveListSelected(configItems: List<SortConfigItem>) =
                configItems.buildSorts()

            override fun onActiveChanged(activeList: List<SortChain<FileSystemItemModel>>) {
                activeSortChains.value = activeList
            }
        }

    private val sortChains: List<SortChain<FileSystemItemModel>>
        get() = buildList {
            add(NameSort(NameSort.Item()))
        }

    companion object {
        val suffix = "sort"
        val adapterFactory: RuntimeTypeAdapterFactory<ConfigItem> =
            RuntimeTypeAdapterFactory.of(
                ConfigItem::class.java,
                "sort-item-key"
            )
                .registerSubtype(NameSort.Item::class.java, "name")
    }
}

fun List<SortConfigItem>.buildSorts() =
    map {
        NameSort(NameSort.Item())
    }