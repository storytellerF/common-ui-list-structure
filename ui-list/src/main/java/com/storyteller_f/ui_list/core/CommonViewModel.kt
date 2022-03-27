package com.storyteller_f.ui_list.core

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import com.storyteller_f.common_vm_ktx.sVM
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

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>, ARG> ComponentActivity.source(
    arg: () -> ARG,
    sourceProducer: (ARG) -> SourceProducer<RK, D, Holder, Database, Composite>,
) = source(sourceProducer(arg()))

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<D, RK, Database>> ComponentActivity.source(
    sourceContent: SourceProducer<RK, D, Holder, Database, Composite>,
): Lazy<SimpleSourceViewModel<D, Holder, RK, Database>> {
    return sVM {
        SimpleSourceViewModel(
            SimpleSourceRepository(
                sourceContent.service,
                sourceContent.composite(),
                sourceContent.pagingSourceFactory,
            ), sourceContent.processFactory, sourceContent.interceptorFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder> ComponentActivity.data(
    dataContent: DataProducer<RK, D, Holder>,
): Lazy<SimpleDataViewModel<D, Holder, RK>> {
    return sVM {
        SimpleDataViewModel(
            SimpleDataRepository(
                dataContent.service,
            ), dataContent.processFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, ARG> ComponentActivity.data(
    arg: () -> ARG,
    dataContentProducer: (ARG) -> DataProducer<RK, D, Holder>,
) = data(dataContentProducer(arg()))

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder> Fragment.data(
    dataContent: DataProducer<RK, D, Holder>,
): Lazy<SimpleDataViewModel<D, Holder, RK>> {
    return sVM {
        SimpleDataViewModel(
            SimpleDataRepository(
                dataContent.service,
            ), dataContent.processFactory
        )
    }
}

fun <RK : RemoteKey, D : Datum<RK>, Holder : DataItemHolder, ARG> Fragment.data(
    arg: () -> ARG,
    dataContentProducer: (ARG) -> DataProducer<RK, D, Holder>,
) = data(dataContentProducer(arg()))

fun <D : Any> Fragment.detail(
    detailContent: DetailProducer<D>,
): Lazy<SimpleDetailViewModel<D>> {
    return sVM {
        SimpleDetailViewModel(
            detailContent.producer,
            detailContent.local
        )
    }
}

fun <D : Any, ARG> Fragment.detail(
    arg: () -> ARG,
    detailContentProducer: (ARG) -> DetailProducer<D>,
) = detail(detailContentProducer(arg()))

fun <D : Model, SQ : Any, Holder : DataItemHolder> ComponentActivity.search(
    searchProducer: SearchProducer<D, SQ, Holder>
): Lazy<SimpleSearchViewModel<D, SQ, Holder>> {
    return sVM {
        SimpleSearchViewModel(
            SimpleSearchRepository(searchProducer.service),
            searchProducer.processFactory,
        )
    }
}