package com.storyteller_f.file_system_remote

import android.net.Uri

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