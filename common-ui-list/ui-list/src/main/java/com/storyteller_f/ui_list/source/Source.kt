package com.storyteller_f.ui_list.source

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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

const val STARTING_PAGE_INDEX = 1

class SimpleSourceRepository<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
    service: suspend (Int, Int) -> CommonResponse<D, RK>,
    database: CommonRoomDatabase<D, RK, DT>,
    pagingSourceFactory: () -> PagingSource<Int, D>,

    ) {
    @OptIn(ExperimentalPagingApi::class)
    val resultStream: Flow<PagingData<D>> = Pager(
        config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = SimpleRemoteMediator(
            service,
            database,
        ),
        pagingSourceFactory = pagingSourceFactory
    ).flow

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}

class SimpleSourceViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey, DT : RoomDatabase>(
    sourceRepository: SimpleSourceRepository<D, RK, DT>,
    processFactory: (D, D?) -> Holder,
    interceptorFactory: ((Holder?, Holder?) -> DataItemHolder?)? = null
) : ViewModel() {

    val content: Flow<PagingData<DataItemHolder>>

    /**
     * 如果你想要使用 interceptor，那应observe 前者
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val content2: Flow<PagingData<Holder>>

    private var preDatum: D? = null

    init {
        val dataFlow = sourceRepository.resultStream
            .map {
                preDatum = null
                it.map { repo ->
                    val holder = processFactory(repo, preDatum)
                    preDatum = repo
                    holder
                }
            }
        if (interceptorFactory != null) {
            content = dataFlow.map {
                it.insertSeparators { before: Holder?, after: Holder? ->
                    interceptorFactory(before, after)
                }
            }.cachedIn(viewModelScope)
            content2 = emptyFlow()
        } else {
            content = emptyFlow()
            content2 = dataFlow.cachedIn(viewModelScope)
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

