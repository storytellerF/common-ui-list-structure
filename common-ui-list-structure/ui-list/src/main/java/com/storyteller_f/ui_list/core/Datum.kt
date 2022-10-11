package com.storyteller_f.ui_list.core

import com.storyteller_f.ui_list.database.RemoteKey

interface Datum<RK : RemoteKey> : Model {
    fun produceRemoteKey(prevKey: Int?, nextKey: Int?): RK
    fun remoteKeyId(): String
}

interface Model {
    fun commonDatumId(): String
    fun uniqueIdInOP() = commonDatumId()
}