package com.storyteller_f.ui_list.core

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import com.storyteller_f.common_vm_ktx.sVM
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.data.SimpleDataRepository
import com.storyteller_f.ui_list.data.SimpleSourceRepository
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey

class SourceProducer<RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>>(
    val composite: () -> Composite,
    val service: suspend (Int, Int) -> CommonResponse<Data, RK>,
    val pagingSourceFactory: () -> PagingSource<Int, Data>,
    val processFactory: (Data, Data?) -> Holder,
    val interceptorFactory: (Holder?, Holder?) -> DataItemHolder? = { _, _ -> null }
)

class DataProducer<RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder>(
    val service: suspend (Int, Int) -> CommonResponse<Data, RK>,
    val processFactory: (Data, Data?) -> Holder,
)

class DetailProducer<D : Any>(
    val producer: suspend () -> D,
    val local: (suspend () -> D)? = null
)

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>, ARG1> ComponentActivity.source(
    arg1: () -> ARG1,
    sourceProducer: (ARG1) -> SourceProducer<RK, Data, Holder, Database, Composite>,
) = source(sourceProducer(arg1()))

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>, ARG1, ARG2> ComponentActivity.source(
    arg1: () -> ARG1,
    arg2: () -> ARG2,
    sourceProducer: (ARG1, ARG2) -> SourceProducer<RK, Data, Holder, Database, Composite>,
) = source(sourceProducer(arg1(), arg2()))

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>, ARG1, ARG2, ARG3> ComponentActivity.source(
    arg1: () -> ARG1,
    arg2: () -> ARG2,
    arg3: () -> ARG3,
    sourceProducer: (ARG1, ARG2, ARG3) -> SourceProducer<RK, Data, Holder, Database, Composite>,
) = source(sourceProducer(arg1(), arg2(), arg3()))

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>> ComponentActivity.source(
    sourceContent: SourceProducer<RK, Data, Holder, Database, Composite>,
): Lazy<SimpleSourceViewModel<Data, Holder, RK, Database>> {
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

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder> ComponentActivity.data(
    dataContent: DataProducer<RK, Data, Holder>,
): Lazy<SimpleDataViewModel<Data, Holder, RK>> {
    return sVM {
        SimpleDataViewModel(
            SimpleDataRepository(
                dataContent.service,
            ), dataContent.processFactory
        )
    }
}

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder> Fragment.data(
    dataContent: DataProducer<RK, Data, Holder>,
): Lazy<SimpleDataViewModel<Data, Holder, RK>> {
    return sVM {
        SimpleDataViewModel(
            SimpleDataRepository(
                dataContent.service,
            ), dataContent.processFactory
        )
    }
}

fun <Data : Any> Fragment.detail(
    detailContent: DetailProducer<Data>,
): Lazy<SimpleDetailViewModel<Data>> {
    return sVM {
        SimpleDetailViewModel(
            detailContent.producer,
            detailContent.local
        )
    }
}
