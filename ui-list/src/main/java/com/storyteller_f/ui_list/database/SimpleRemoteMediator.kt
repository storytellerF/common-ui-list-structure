package com.storyteller_f.ui_list.database

import android.util.Log
import androidx.paging.*
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.data.GITHUB_STARTING_PAGE_INDEX
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class SimpleRemoteMediator<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    private val commonRoomDatabase: CommonRoomDatabase<D, RK, DT>,
) : RemoteMediator<Int, D>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, D>): MediatorResult {
        Log.d(
            TAG,
            "load() called with: loadType = $loadType, state = ${state.anchorPosition} ${state.config.selfPrint()}"
        )
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with `endOfPaginationReached = false` because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its prevKey is null, that means we've reached
                // the end of pagination for prepend.
                remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with endOfPaginationReached = false because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its prevKey is null, that means we've reached
                // the end of pagination for append.
                remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
            }
        }
        Log.i(TAG, "load: $page")
        try {
            val apiResponse = service(page, state.config.pageSize)

            val items = apiResponse.items
            val endOfPaginationReached = items.isEmpty()
            commonRoomDatabase.database.withTransaction {
                // clear all tables in the database
                if (loadType == LoadType.REFRESH) {
                    commonRoomDatabase.clearOld()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = items.map {
                    it.produceRemoteKey(prevKey, nextKey)
                }
                commonRoomDatabase.insertRemoteKey(keys)
                commonRoomDatabase.insertAllData(items)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, D>): RemoteKey? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { item ->
                // Get the remote keys of the last item retrieved
                commonRoomDatabase.getRemoteKey(getSpecialId(item))
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, D>): RemoteKey? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { item ->
                // Get the remote keys of the first items retrieved
                commonRoomDatabase.getRemoteKey(getSpecialId(item))
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, D>
    ): RemoteKey? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { item ->
                commonRoomDatabase.getRemoteKey(getSpecialId(item))
            }
        }
    }

    private fun getSpecialId(d: D) = d.commonDatumId()

    companion object {
        private const val TAG = "SimpleRemoteMediator"
    }
}

fun PagingConfig.selfPrint(): String =
    "$prefetchDistance $initialLoadSize $pageSize $enablePlaceholders $maxSize $jumpThreshold"