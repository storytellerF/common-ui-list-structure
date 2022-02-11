package com.storyteller_f.ui_list.core

import androidx.activity.ComponentActivity
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import com.storyteller_f.common_vm_ktx.cVM
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.data.SimpleRepository

fun <RK : RemoteKey, Data : Datum<RK>, Holder : DataItemHolder, Database : RoomDatabase, Composite : CommonRoomDatabase<Data, RK, Database>> ComponentActivity.source(
    composite: () -> Composite,
    service: suspend (Int, Int) -> CommonResponse<Data, RK>,
    pagingSourceFactory: () -> PagingSource<Int, Data>,
    processFactory: (Data) -> Holder,
    interceptorFactory: (Holder?, Holder?) -> DataItemHolder? = { _, _ -> null }
): Lazy<SimpleSourceViewModel<Data, Holder, RK, Database>> {
    return cVM {
        SimpleSourceViewModel(
            SimpleRepository(
                service,
                composite(),
                pagingSourceFactory,
            ), processFactory, interceptorFactory
        )
    }
}
