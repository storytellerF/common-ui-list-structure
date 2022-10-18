package com.storyteller_f.ui_list.source

import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import androidx.room.RoomDatabase
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.database.SimpleRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val STARTING_PAGE_INDEX = 1

class SimpleSourceRepository<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    private val database: CommonRoomDatabase<D, RK, DT>,
    private val pagingSourceFactory: () -> PagingSource<Int, D>,

    ) {
    @OptIn(ExperimentalPagingApi::class)
    fun resultStream(): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = SimpleRemoteMediator(
                service,
                database,
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}

class SimpleSourceViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey, DT : RoomDatabase>(
    sourceRepository: SimpleSourceRepository<D, RK, DT>,
    processFactory: (D, D?) -> Holder,
    interceptorFactory: ((Holder?, Holder?) -> DataItemHolder?)? = null
) : ViewModel() {

    var content: Flow<PagingData<DataItemHolder>>? = null

    /**
     * 如果你想要使用 interceptor factory ，那应observe content
     */
    var content2: Flow<PagingData<Holder>>? = null
    private var last: D? = null

    init {
        val map = sourceRepository.resultStream()
            .map {
                it.map { repo ->
                    val holder = processFactory(repo, last)
                    last = repo
                    holder
                }
            }
        if (interceptorFactory != null) {
            content = map.map {
                it.insertSeparators { before: Holder?, after: Holder? ->
                    interceptorFactory(before, after)
                }
            }.cachedIn(viewModelScope)
        } else {
            content2 = map.cachedIn(viewModelScope)
        }
    }
}

class SourceProducer<RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>>(
    val composite: () -> Composite,
    val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    val pagingSourceFactory: () -> PagingSource<Int, D>,
    val processFactory: (D, D?) -> Holder,
    val interceptorFactory: (Holder?, Holder?) -> DataItemHolder? = { _, _ -> null }
)




fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>, ARG, T> T.source(
    arg: () -> ARG,
    sourceContentProducer: (ARG) -> SourceProducer<RK, D, Holder, Database, Composite>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = vm({}) {
    val sourceContent = sourceContentProducer(arg())
    SimpleSourceViewModel(
        SimpleSourceRepository(
            sourceContent.service,
            sourceContent.composite(),
            sourceContent.pagingSourceFactory,
        ), sourceContent.processFactory, sourceContent.interceptorFactory
    )
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>, T> T.source(
    sourceContent: SourceProducer<RK, D, Holder, Database, Composite>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = vm({}) {
    SimpleSourceViewModel(
        SimpleSourceRepository(
            sourceContent.service,
            sourceContent.composite(),
            sourceContent.pagingSourceFactory,
        ), sourceContent.processFactory, sourceContent.interceptorFactory
    )
}

