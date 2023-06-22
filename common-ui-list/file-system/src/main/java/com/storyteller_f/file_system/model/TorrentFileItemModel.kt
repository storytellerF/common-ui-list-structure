package com.storyteller_f.file_system.model

import android.net.Uri

class TorrentFileItemModel(name: String, uri: Uri, isHide: Boolean, time: Long, isSymLink: Boolean) :
    FileItemModel(name, uri, isHide, time, isSymLink, "torrent") {
    var torrentName: String? = null
}