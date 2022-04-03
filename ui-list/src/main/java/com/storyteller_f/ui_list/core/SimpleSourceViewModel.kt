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

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.paging.*
import androidx.room.RoomDatabase
import com.storyteller_f.ui_list.data.*
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var last: D? = null

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

    val content: MediatorLiveData<DataHook<D, Holder, RK>> = liveData {
        val asLiveData = sourceRepository.request().asLiveData(Dispatchers.Main)
        val source = asLiveData.map {
            DataHook<D, Holder, RK>(this@SimpleDataViewModel, it.map { repo ->
                val holder = processFactory(repo, last)
                last = repo
                holder
            })
        }
        emitSource(source)
    } as MediatorLiveData<DataHook<D, Holder, RK>>

    val loadState: LiveData<MoreInfoLoadState> = liveData {
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
            sourceRepository.refresh()
        }
    }

    fun <IH> reset(last: MutableList<IH>) {
        content.value = DataHook(this, last as List<Holder>)
    }

    class DataHook<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
        val viewModel: SimpleDataViewModel<*, *, *>,
        val list: List<Holder>
    ) {
        fun swap(from: Int, to: Int) {
            viewModel.sourceRepository.swap(from, to)
        }
    }
}

class SimpleDetailViewModel<D : Any>(
    private val producer: suspend () -> D,
    local: (suspend () -> D?)? = null
) : ViewModel() {
    val content = MutableLiveData<D>()
    val loadState = MutableLiveData<LoadState>()

    init {
        refresh(local, producer)
    }

    private fun refresh(local: (suspend () -> D?)?, producer: suspend () -> D) {
        viewModelScope.launch {
            try {
                loadState.value = LoadState.Loading
                val value = withContext(Dispatchers.IO) {
                    if (local != null) {
                        local() ?: producer()
                    } else producer()
                }
                content.value = value
                loadState.value = LoadState.NotLoading(true)
            } catch (e: Exception) {
                loadState.value = LoadState.Error(e)
            }
        }
    }

    fun refresh() {
        refresh(null, producer)
    }
}

class SimpleSearchViewModel<D : Model, SQ : Any, Holder : DataItemHolder>(
    private val repository: SimpleSearchRepository<D, SQ>,
    val processFactory: (D, D?) -> Holder,
) : ViewModel() {
    private var currentQueryValue: SQ? = null
    private var last: D? = null
    var lastJob: Job? = null

    private var currentSearchResult: Flow<PagingData<Holder>>? = null
    fun search(sq: SQ): Flow<PagingData<Holder>> {
        val lastResult = currentSearchResult
        if (sq == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = sq
        val newResult: Flow<PagingData<Holder>> = repository.search(sq)
            .map {
                it.map {
                    processFactory(it, last).apply {
                        last = it
                    }
                }
            }
            .cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }
}

fun <SQ : Any, Holder : DataItemHolder> SimpleSearchViewModel<*, SQ, Holder>.observer(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    search: SQ,
    block: suspend (PagingData<Holder>) -> Unit
) {
    lastJob?.cancel()
    lastJob = lifecycleCoroutineScope.launch {
        search(search).collectLatest {
            block(it)
        }
    }
}

fun <R, P1, P2> let(o1: P1?, o2: P2?, block: (P1, P2) -> R?): R? {
    return if (o1 != null && o2 != null) {
        block(o1, o2)
    } else null
}