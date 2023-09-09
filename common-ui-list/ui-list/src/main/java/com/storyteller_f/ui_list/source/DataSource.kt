package com.storyteller_f.ui_list.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Collections

val LoadState?.isError get() = this is LoadState.Error
val LoadState?.isLoading get() = this is LoadState.Loading
val LoadState?.isNotLoading get() = this is LoadState.NotLoading

class MoreInfoLoadState(val loadState: LoadState, val itemCount: Int)

class SimpleDataRepository<D : Datum<RK>, RK : RemoteKey>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
) {
    // 保存所有接收到的结果
    private val inMemoryCache = mutableListOf<D>()

    // shared flow of results, which allows us to broadcast updates so
    // the subscriber will have the latest data
    private val results = MutableSharedFlow<List<D>>(replay = 1)
    val loadState = MutableSharedFlow<MoreInfoLoadState>(replay = 1)

    // 保存上一次请求的页数，如果成功，自增
    private var lastRequestedPage = STARTING_PAGE_INDEX
    private var tryToIntercept = false

    // 避免同一时刻进行多个请求
    private var isRequestInProgress = false

    suspend fun request(): Flow<List<D>> {
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(lastRequestedPage)
        return results
    }

    suspend fun requestMore() {
        if (isRequestInProgress) return
        val successful = requestAndSaveData(lastRequestedPage + 1)
        if (successful) {
            lastRequestedPage++
        }
    }

    suspend fun retry() {
        if (isRequestInProgress) return
        requestAndSaveData(lastRequestedPage + 1)
    }

    suspend fun refresh() {
        tryToIntercept = true
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(lastRequestedPage)
    }

    private suspend fun requestAndSaveData(pages: Int): Boolean {
        isRequestInProgress = true
        tryToIntercept = false
        loadState.emit(MoreInfoLoadState(LoadState.Loading, 0))
        try {
            val response = service(pages, 30)
            if (tryToIntercept) return false
            val elements = response.items
            inMemoryCache.addAll(elements)
            results.emit(inMemoryCache)
            loadState.emit(
                MoreInfoLoadState(
                    LoadState.NotLoading(elements.isEmpty()),
                    inMemoryCache.size,
                )
            )
            return true
        } catch (exception: IOException) {
            loadState.emit(MoreInfoLoadState(LoadState.Error(exception), inMemoryCache.size))
        } catch (exception: HttpException) {
            loadState.emit(MoreInfoLoadState(LoadState.Error(exception), inMemoryCache.size))
        } finally {
            isRequestInProgress = false
        }
        return false
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(inMemoryCache, from, to)
    }
}


/**
 * 与 SimpleSourceViewModel 类似，不过支持排序
 */
class SimpleDataViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
    private val sourceRepository: SimpleDataRepository<D, RK>,
    processFactory: (D, D?) -> Holder,
) : ViewModel() {

    private var preDatum: D? = null

    val content: LiveData<FatData<D, Holder, RK>> = liveData {
        val asLiveData = sourceRepository.request().asLiveData(Dispatchers.Main)
        val source = asLiveData.map {
            preDatum = null
            FatData(this@SimpleDataViewModel, it.map { datum ->
                val holder = processFactory(datum, preDatum)
                preDatum = datum
                holder
            }.toMutableList())
        }
        emitSource(source)
    }

    val loadState: LiveData<MoreInfoLoadState> = liveData {
        emitSource(sourceRepository.loadState.asLiveData())
    }

    fun requestMore() {
        viewModelScope.launch {
            sourceRepository.requestMore()
        }
    }

    fun retry() {
        viewModelScope.launch {
            sourceRepository.retry()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            sourceRepository.refresh()
        }
    }

    class FatData<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
        val viewModel: SimpleDataViewModel<D, Holder, RK>,
        val list: MutableList<Holder>
    ) {
        fun swap(from: Int, to: Int) {
            Collections.swap(list, from, to)
            viewModel.sourceRepository.swap(from, to)
        }
    }
}


class DataProducer<RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder>(
    val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    val processFactory: (D, D?) -> Holder,
)


fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, T> T.data(
    dataContent: DataProducer<RK, D, Holder>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = vm({}) {
    SimpleDataViewModel(
        SimpleDataRepository(
            dataContent.service,
        ), dataContent.processFactory
    )
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, ARG, T> T.data(
    arg: () -> ARG,
    dataContentProducer: (ARG) -> DataProducer<RK, D, Holder>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = data(dataContentProducer(arg()))

