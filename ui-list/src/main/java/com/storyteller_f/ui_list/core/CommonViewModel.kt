package com.storyteller_f.ui_list.core

import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.data.*
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey

class SourceProducer<RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>>(
    val composite: () -> Composite,
    val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    val pagingSourceFactory: () -> PagingSource<Int, D>,
    val processFactory: (D, D?) -> Holder,
    val interceptorFactory: (Holder?, Holder?) -> DataItemHolder? = { _, _ -> null }
)

class DataProducer<RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder>(
    val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    val processFactory: (D, D?) -> Holder,
)

class DetailProducer<D : Any>(
    val producer: suspend () -> D,
    val local: (suspend () -> D)? = null
)

class SearchProducer<D : Model, SQ : Any, Holder : DataItemHolder>(
    val service: suspend (SQ, start: Int, count: Int) -> SimpleResponse<D>,
    val processFactory: (D, list: D?) -> Holder,
)

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>, ARG, T> T.source(
    arg: () -> ARG,
    sourceContentProducer: (ARG) -> SourceProducer<RK, D, Holder, Database, Composite>,
) : Lazy<SimpleSourceViewModel<D, Holder, RK, Database>> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return vm {
        val sourceContent = sourceContentProducer(arg())
        SimpleSourceViewModel(
            SimpleSourceRepository(
                sourceContent.service,
                sourceContent.composite(),
                sourceContent.pagingSourceFactory,
            ), sourceContent.processFactory, sourceContent.interceptorFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>, T> T.source(
    sourceContent: SourceProducer<RK, D, Holder, Database, Composite>,
): Lazy<SimpleSourceViewModel<D, Holder, RK, Database>> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return vm {
        SimpleSourceViewModel(
            SimpleSourceRepository(
                sourceContent.service,
                sourceContent.composite(),
                sourceContent.pagingSourceFactory,
            ), sourceContent.processFactory, sourceContent.interceptorFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, T> T.data(
    dataContent: DataProducer<RK, D, Holder>,
): Lazy<SimpleDataViewModel<D, Holder, RK>> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return vm {
        SimpleDataViewModel(
            SimpleDataRepository(
                dataContent.service,
            ), dataContent.processFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, ARG, T> T.data(
    arg: () -> ARG,
    dataContentProducer: (ARG) -> DataProducer<RK, D, Holder>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = data(dataContentProducer(arg()))

fun <D : Any, T> T.detail(
    detailContent: DetailProducer<D>,
): Lazy<SimpleDetailViewModel<D>> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return vm {
        SimpleDetailViewModel(
            detailContent.producer,
            detailContent.local
        )
    }
}

fun <D : Any, ARG, T> T.detail(
    arg: () -> ARG,
    detailContentProducer: (ARG) -> DetailProducer<D>,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = detail(detailContentProducer(arg()))

fun <D : Model, SQ : Any, Holder : DataItemHolder, T> T.search(
    searchProducer: SearchProducer<D, SQ, Holder>
): Lazy<SimpleSearchViewModel<D, SQ, Holder>> where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    return vm {
        SimpleSearchViewModel(
            SimpleSearchRepository(searchProducer.service),
            searchProducer.processFactory,
        )
    }
}

fun <D : Model, SQ : Any, Holder : DataItemHolder, T, ARG> T.search(
    arg: () -> ARG,
    searchContentProducer: (ARG) -> SearchProducer<D, SQ, Holder>
): Lazy<SimpleSearchViewModel<D, SQ, Holder>> where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    return vm {
        val searchProducer = searchContentProducer(arg())
        SimpleSearchViewModel(
            SimpleSearchRepository(searchProducer.service),
            searchProducer.processFactory,
        )
    }
}