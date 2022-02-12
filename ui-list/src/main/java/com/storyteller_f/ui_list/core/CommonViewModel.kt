package com.storyteller_f.ui_list.core

import androidx.activity.ComponentActivity
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import com.storyteller_f.common_vm_ktx.cVM
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.data.SimpleRepository
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey

class SourceProducer<RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>>(
    val composite: () -> Composite,
    val service: suspend (Int, Int) -> CommonResponse<Data, RK>,
    val pagingSourceFactory: () -> PagingSource<Int, Data>,
    val processFactory: (Data, Data?) -> Holder,
    val interceptorFactory: (Holder?, Holder?) -> DataItemHolder? = { _, _ -> null }
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
    return cVM {
        SimpleSourceViewModel(
            SimpleRepository(
                sourceContent.service,
                sourceContent.composite(),
                sourceContent.pagingSourceFactory,
            ), sourceContent.processFactory, sourceContent.interceptorFactory
        )
    }
}
