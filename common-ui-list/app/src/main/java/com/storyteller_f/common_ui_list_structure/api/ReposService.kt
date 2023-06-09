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

package com.storyteller_f.common_ui_list_structure.api

import android.app.Activity
import androidx.fragment.app.Fragment
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_ui_list_structure.model.RepoRemoteKey
import com.storyteller_f.ui_list.data.CommonResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Github API communication setup via Retrofit.
 */
interface ReposService {
    /**
     * Get repos ordered by stars.
     */
    @GET("search/repositories?sort=stars&q=Android")
    suspend fun searchRepos(
        @Query("page") page: Int,
        @Query("per_page") itemsPerPage: Int
    ): CommonResponse<Repo, RepoRemoteKey>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): ReposService {
            val logger = HttpLoggingInterceptor()
            logger.level = Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ReposService::class.java)
        }
    }
}

val Activity.requireReposService by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    ReposService.create()
}

val Fragment.requireReposService get() = requireActivity().requireReposService
