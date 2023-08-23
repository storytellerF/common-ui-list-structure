package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance

data class ShareSpec(val server: String, val port: Int, val user: String, val password: String, val type: String, val share: String) {
    fun toUri(): Uri {
        return Uri.parse("$type://$user:$password@$server:$port:$share")!!
    }

    fun toRemote(): RemoteAccessSpec {
        return RemoteAccessSpec(server, port, user, password, share, type)
    }

    companion object {
        fun parse(parse: Uri): ShareSpec {
            val authority = parse.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (loc, port, share) = split.last().split(":")
            return ShareSpec(loc, port.toInt(), user, pass, parse.scheme!!, share)
        }

    }
}

data class RemoteSpec(val server: String, val port: Int, val user: String, val password: String, val type: String) {
    fun toUri(): Uri {
        val scheme = type
        return Uri.parse("$scheme://$user:$password@$server:$port/")!!
    }

    fun toRemote(): RemoteAccessSpec {
        return RemoteAccessSpec(server, port, user, password, type = type)
    }

    companion object {
        fun parse(parse: Uri): RemoteSpec {
            val scheme = parse.scheme!!
            val authority = parse.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (loc, port) = split.last().split(":")
            return RemoteSpec(loc, port.toInt(), user, pass, type = scheme)
        }

    }
}

val supportScheme = listOf("ftp", "smb", "sftp", "ftpes", "ftps", "webdav")

fun getRemoteInstance(uri: Uri): FileInstance {
    return when (uri.scheme) {
        "ftp" -> FtpFileInstance(RemoteSpec.parse(uri), uri)
        "smb" -> SmbFileInstance(ShareSpec.parse(uri), uri)
        "sftp" -> SFtpFileInstance(RemoteSpec.parse(uri), uri)
        "ftpes", "ftps" -> FtpsFileInstance(RemoteSpec.parse(uri), uri)
        "webdav" -> WebDavFileInstance(ShareSpec.parse(uri), uri)
        else -> throw Exception()
    }

}