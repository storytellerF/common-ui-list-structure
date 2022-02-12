/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.storyteller_f.ui_list.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.room.RoomDatabase
import com.storyteller_f.ui_list.data.SimpleRepository
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SimpleSourceViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey, DT : RoomDatabase>(
    repository: SimpleRepository<D, RK, DT>,
    processFactory: (D, D?) -> Holder,
    interceptorFactory: ((Holder?, Holder?) -> DataItemHolder?)? = null
) : ViewModel() {

    var content: Flow<PagingData<DataItemHolder>>? = null
    var content2: Flow<PagingData<Holder>>? = null
    var last: D? = null

    init {
        val map = repository.resultStream()
            .map {
                it.map { repo ->
                    val processFactory1 = processFactory(repo, last)
                    last = repo
                    processFactory1
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

    override fun onCleared() {
        super.onCleared()
    }
}

fun <R, P1, P2> let(o1: P1?, o2: P2?, block: (P1, P2) -> R?): R? {
    return if (o1 != null && o2 != null) {
        block(o1, o2)
    } else null
}