package com.storyteller_f.file_system_remote

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

object RemoteAccessType {
    const val ftp = "ftp"
    const val sftp = "sftp"
    const val smb = "smb"
    const val ftpes = "ftpes"
    const val ftps = "ftps"
    const val webDav = "webdav"

    val list = listOf("", smb, sftp, ftp, ftpes, ftps, webDav)
}


@Entity(tableName = "remote-access", primaryKeys = ["server", "port", "user", "password", "share"])
class RemoteAccessSpec(
    val server: String = "",
    val port: Int = 0,
    val user: String = "",
    val password: String = "",
    @ColumnInfo(defaultValue = "") val share: String = "",//smb 专用
    @ColumnInfo(defaultValue = RemoteAccessType.ftp) val type: String
) {
    fun toFtpSpec(): RemoteSpec {
        return RemoteSpec(server, port, user, password, type)
    }

    fun toShareSpec(): ShareSpec {
        return ShareSpec(server, port, user, password, type, share)
    }

}

@Dao
interface RemoteAccessDao {
    @Query("select * from `remote-access`")
    fun list(): Flow<List<RemoteAccessSpec>>

    @Query("select * from `remote-access`")
    suspend fun listAsync(): List<RemoteAccessSpec>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(spec: RemoteAccessSpec)

    @Delete
    fun remove(spec: RemoteAccessSpec)
}