package com.storyteller_f.ui_list.source

import androidx.lifecycle.*
import androidx.paging.*
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SimpleSearchSource<D : Model, SQ : Any>(
    val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
    private val sq: SQ
) : PagingSource<Int, D>() {
    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, D> {
        val position = params.key ?: STARTING_PAGE_INDEX
        return try {
            val response = service(sq, position, params.loadSize)
            val items = response.items
            val nextKey = if (items.isEmpty() || items.size <= params.loadSize) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / SimpleSourceRepository.NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = items,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}

class SimpleSearchRepository<D : Model, SQ : Any>(
    private val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
) {
    fun search(sq: SQ): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(
                pageSize = SimpleSourceRepository.NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SimpleSearchSource(service, sq) }
        ).flow
    }
}

class SimpleSearchViewModel<D : Model, SQ : Any, Holder : DataItemHolder>(
    private val repository: SimpleSearchRepository<D, SQ>,
    val processFactory: (D, D?) -> Holder,
) : ViewModel() {
    private var currentQueryValue: SQ? = null
    private var last: D? = null
    var lastJob: Job? = null

    private var currentSearchResult: Flow<PagingData<Holder>>? = null
    fun search(sq: SQ): Flow<PagingData<Holder>> {
        val lastResult = currentSearchResult
        if (sq == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = sq
        val newResult: Flow<PagingData<Holder>> = repository.search(sq)
            .map { pagingData ->
                pagingData.map {
                    processFactory(it, last).apply {
                        last = it
                    }
                }
            }
            .cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }
}

fun <SQ : Any, Holder : DataItemHolder> SimpleSearchViewModel<*, SQ, Holder>.observerInScope(
    lifecycleOwner: LifecycleOwner,
    search: SQ,
    block: suspend (PagingData<Holder>) -> Unit
) {
    lastJob?.cancel()
    lastJob = lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            search(search).collectLatest {
                block(it)
            }
        }
    }
}

class SearchProducer<D : Model, SQ : Any, Holder : DataItemHolder>(
    val service: suspend (SQ, start: Int, count: Int) -> SimpleResponse<D>,
    val processFactory: (D, list: D?) -> Holder,
)

fun <D : Model, SQ : Any, Holder : DataItemHolder, T> T.search(
    searchProducer: SearchProducer<D, SQ, Holder>
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner = vm({}) {
    SimpleSearchViewModel(
        SimpleSearchRepository(searchProducer.service),
        searchProducer.processFactory,
    )
}

fun <D : Model, SQ : Any, Holder : DataItemHolder, T, ARG> T.search(
    arg: () -> ARG,
    searchContentProducer: (ARG) -> SearchProducer<D, SQ, Holder>
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner = vm({}) {
    val searchProducer = searchContentProducer(arg())
    SimpleSearchViewModel(
        SimpleSearchRepository(searchProducer.service),
        searchProducer.processFactory,
    )
}