package com.storyteller_f.giant_explorer

import android.content.ContentResolver
import android.net.Uri

object PluginManager {
    val list = mutableListOf<Plugin>()
}

class Plugin(val name: String) {

}

object FileSystemProviderResolver {
    fun resolvePath(uri: Uri): String? {
        if (uri.authority == BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY) return null
        return uri.path
    }

    fun build(encrypted: Boolean, path: String): Uri? {
        val authority = if (encrypted) BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY
        else BuildConfig.FILE_SYSTEM_PROVIDER_AUTHORITY
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).path(path).build()
    }
}