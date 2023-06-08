package com.storyteller_f.ui_list.source

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.common_vm_ktx.vm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleDetailViewModel<D : Any>(
    private val producer: suspend () -> D,
    local: (suspend () -> D?)? = null
) : ViewModel() {
    val content = MutableLiveData<D>()
    val loadState = MutableLiveData<LoadState>()

    init {
        refresh(local, producer)
    }

    private fun refresh(local: (suspend () -> D?)?, producer: suspend () -> D) {
        viewModelScope.launch {
            try {
                loadState.value = LoadState.Loading
                val value = withContext(Dispatchers.IO) {
                    if (local != null) {
                        local() ?: producer()
                    } else producer()
                }
                content.value = value
                loadState.value = LoadState.NotLoading(true)
            } catch (e: Exception) {
                loadState.value = LoadState.Error(e)
            }
        }
    }

    fun refresh() {
        refresh(null, producer)
    }
}

class DetailProducer<D : Any>(
    val producer: suspend () -> D,
    val local: (suspend () -> D)? = null
)


fun <D : Any, T> T.detail(
    detailContent: DetailProducer<D>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = vm({}) {
    SimpleDetailViewModel(
        detailContent.producer,
        detailContent.local
    )
}

fun <D : Any, ARG, T> T.detail(
    arg: () -> ARG,
    detailContentProducer: (ARG) -> DetailProducer<D>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = detail(detailContentProducer(arg()))

