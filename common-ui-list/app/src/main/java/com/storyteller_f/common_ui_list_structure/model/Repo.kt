package com.storyteller_f.common_ui_list_structure.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.database.RemoteKey

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
    override fun commonId() = id.toString()
    override fun produceRemoteKey(prevKey: Int?, nextKey: Int?) =
        RepoRemoteKey(commonId(), prevKey, nextKey)

    override fun remoteKeyId(): String = commonId()
}

@Entity(tableName = "repo_remote_keys")
class RepoRemoteKey(
    itemId: String,
    prevKey: Int?,
    nextKey: Int?
) : RemoteKey(itemId, prevKey, nextKey)
