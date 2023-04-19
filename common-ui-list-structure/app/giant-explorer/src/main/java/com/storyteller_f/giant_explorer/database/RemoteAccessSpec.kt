package com.storyteller_f.giant_explorer.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.storyteller_f.giant_explorer.control.remote.RemoteAccessType
import kotlinx.coroutines.flow.Flow

data class ShareSpec(val server: String, val port: Int, val user: String, val password: String, val type: String, val share: String) {
    fun toUri(): String {
        return "$type://$user:$password@$server:$port:$share"
    }

    fun toRemote(): RemoteAccessSpec {
        return RemoteAccessSpec(server, port, user, password, share, type)
    }

    companion object {
        fun parse(url: String): ShareSpec {
            val parse = Uri.parse(url)
            val authority = parse.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (loc, port, share) = split.last().split(":")
            return ShareSpec(loc, port.toInt(), user, pass, parse.scheme!!, share)
        }

    }
}

data class RemoteSpec(val server: String, val port: Int, val user: String, val password: String, val type: String) {
    fun toUri(): String {
        val scheme = type
        return "$scheme://$user:$password@$server:$port/"
    }

    fun toRemote(): RemoteAccessSpec {
        return RemoteAccessSpec(server, port, user, password, type = type)
    }

    companion object {
        fun parse(url: String): RemoteSpec {
            val parse = Uri.parse(url)
            val scheme = parse.scheme!!
            val authority = parse.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (loc, port) = split.last().split(":")
            return RemoteSpec(loc, port.toInt(), user, pass, type = scheme)
        }

    }
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