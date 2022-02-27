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

import androidx.lifecycle.*
import androidx.paging.*
import androidx.room.RoomDatabase
import com.storyteller_f.ui_list.data.SimpleDataRepository
import com.storyteller_f.ui_list.data.SimpleSourceRepository
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SimpleSourceViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey, DT : RoomDatabase>(
    sourceRepository: SimpleSourceRepository<D, RK, DT>,
    processFactory: (D, D?) -> Holder,
    interceptorFactory: ((Holder?, Holder?) -> DataItemHolder?)? = null
) : ViewModel() {

    var content: Flow<PagingData<DataItemHolder>>? = null

    /**
     * 如果你想要使用 interceptor factory ，那应observe content2
     */
    var content2: Flow<PagingData<Holder>>? = null
    var last: D? = null

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

    override fun onCleared() {
        super.onCleared()
    }
}

class SimpleDataViewModel<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
    private val sourceRepository: SimpleDataRepository<D, RK>,
    processFactory: (D, D?) -> Holder,
) : ViewModel() {
    var last: D? = null

    val content: LiveData<List<Holder>> = liveData {
        val asLiveData = sourceRepository.requestSound().asLiveData(Dispatchers.Main)
        emitSource(asLiveData.map {
            it.map { repo ->
                val holder = processFactory(repo, last)
                last = repo
                holder
            }
        })
    }

    val loadState: LiveData<LoadState> = liveData {
        emitSource(sourceRepository.loadState.asLiveData())
    }

    fun requestMore() {
        viewModelScope.launch {
            sourceRepository.requestMore()
        }
    }

    fun retry() {
        viewModelScope.launch {
            sourceRepository.retry()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            sourceRepository.requestSound()
        }
    }
}

fun <R, P1, P2> let(o1: P1?, o2: P2?, block: (P1, P2) -> R?): R? {
    return if (o1 != null && o2 != null) {
        block(o1, o2)
    } else null
}