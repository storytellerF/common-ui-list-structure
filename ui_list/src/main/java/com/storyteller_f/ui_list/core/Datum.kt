package com.storyteller_f.ui_list.core

import com.storyteller_f.ui_list.database.RemoteKey

interface Datum<RK : RemoteKey> {
    fun commonDatumId(): String
    fun produceRemoteKey(prevKey: Int?, nextKey: Int?): RK
}