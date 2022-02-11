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
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.database.SimpleRemoteMediator
import kotlinx.coroutines.flow.Flow

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
const val GITHUB_STARTING_PAGE_INDEX = 1

class SimpleRepository<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
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
