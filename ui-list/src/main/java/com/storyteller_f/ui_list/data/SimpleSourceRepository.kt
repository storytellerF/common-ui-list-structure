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

package com.storyteller_f.ui_list.data

import androidx.paging.*
import androidx.room.RoomDatabase
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleSourceRepository.Companion.NETWORK_PAGE_SIZE
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.database.SimpleRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException
import java.io.IOException
import java.util.*

const val STARTING_PAGE_INDEX = 1

class SimpleSourceRepository<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    private val database: CommonRoomDatabase<D, RK, DT>,
    private val pagingSourceFactory: () -> PagingSource<Int, D>,

    ) {
    @OptIn(ExperimentalPagingApi::class)
    fun resultStream(): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = SimpleRemoteMediator(
                service,
                database,
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}

sealed class ReloadCause {
    object CacheExpired : ReloadCause()
    object PullToRefresh : ReloadCause()
}

val LoadState?.isError get() = this is LoadState.Error
val LoadState?.isLoading get() = this is LoadState.Loading
val LoadState?.isNotLoading get() = this is LoadState.NotLoading

class MoreInfoLoadState(val loadState: LoadState, val itemCount: Int, val cause: ReloadCause?)

class SimpleDataRepository<D : Datum<RK>, RK : RemoteKey>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
) {
    // 保存所有接收到的结果
    private val inMemoryCache = mutableListOf<D>()

    // shared flow of results, which allows us to broadcast updates so
    // the subscriber will have the latest data
    private val results = MutableSharedFlow<List<D>>(replay = 1)
    val loadState = MutableSharedFlow<MoreInfoLoadState>(replay = 1)

    // 保存上一次请求的页数，如果成功，自增
    private var lastRequestedPage = STARTING_PAGE_INDEX
    private var causeTempSaved: ReloadCause? = null

    // 避免同一时刻进行多个请求
    private var isRequestInProgress = false

    suspend fun request(): Flow<List<D>> {
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(lastRequestedPage)
        return results
    }

    suspend fun requestMore() {
        if (isRequestInProgress) return
        val successful = requestAndSaveData(lastRequestedPage + 1)
        if (successful) {
            lastRequestedPage++
        }
    }

    suspend fun retry() {
        if (isRequestInProgress) return
        requestAndSaveData(lastRequestedPage + 1)
    }

    suspend fun refresh(cause: ReloadCause?) {
        causeTempSaved = cause
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(lastRequestedPage)
    }

    private suspend fun requestAndSaveData(pages: Int): Boolean {
        isRequestInProgress = true
        loadState.emit(MoreInfoLoadState(LoadState.Loading, 0, null))
        var successful = false
        try {
            val response = service.invoke(pages, 30)
            val elements = response.items
            inMemoryCache.addAll(elements)
            results.emit(inMemoryCache)
            loadState.emit(
                MoreInfoLoadState(
                    LoadState.NotLoading(elements.isEmpty()),
                    inMemoryCache.size,
                    null
                )
            )
            successful = true
        } catch (exception: IOException) {
            loadState.emit(MoreInfoLoadState(LoadState.Error(exception), inMemoryCache.size, null))
        } catch (exception: HttpException) {
            loadState.emit(MoreInfoLoadState(LoadState.Error(exception), inMemoryCache.size, null))
        } finally {
            causeTempSaved = null
        }
        isRequestInProgress = false
        return successful
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(inMemoryCache, from, to)
    }
}

class SimpleSearchSourceFactory<D : Model, SQ : Any>(
    val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
    private val sq: SQ
) : PagingSource<Int, D>() {
    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, D> {
        val position = params.key ?: STARTING_PAGE_INDEX
        return try {
            val response = service(sq, position, params.loadSize)
            val items = response.items
            val nextKey = if (items.isEmpty() || items.size <= params.loadSize) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = items,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}

class SimpleSearchRepository<D : Model, SQ : Any>(
    private val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
) {
    fun search(sq: SQ): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SimpleSearchSourceFactory(service, sq) }
        ).flow
    }
}
