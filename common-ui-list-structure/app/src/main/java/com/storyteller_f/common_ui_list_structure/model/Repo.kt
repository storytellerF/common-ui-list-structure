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

package com.storyteller_f.common_ui_list_structure.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.database.RemoteKey

/**
 * Immutable model class for a Github repo that holds all the information about a repository.
 * Objects of this type are received from the Github API, therefore all the fields are annotated
 * with the serialized name.
 * This class also defines the Room repos table, where the repo [id] is the primary key.
 */
@Entity(tableName = "repos")
data class Repo(
    @PrimaryKey @field:SerializedName("id") val id: Long,
    @field:SerializedName("name") val name: String,
    @field:SerializedName("full_name") val fullName: String,
    @field:SerializedName("description") val description: String?,
    @field:SerializedName("html_url") val url: String,
    @field:SerializedName("stargazers_count") val stars: Int,
    @field:SerializedName("forks_count") val forks: Int,
    @field:SerializedName("language") val language: String?
) : Datum<RepoRemoteKey> {
    override fun commonDatumId() = id.toString()
    override fun produceRemoteKey(prevKey: Int?, nextKey: Int?) =
        RepoRemoteKey(commonDatumId(), prevKey, nextKey)

    override fun remoteKeyId(): String = commonDatumId()
}

@Entity(tableName = "repo_remote_keys")
class RepoRemoteKey(
    itemId: String,
    prevKey: Int?,
    nextKey: Int?
) : RemoteKey(itemId, prevKey, nextKey)
