package com.storyteller_f.file_system.model

class TorrentFileItemModel(name: String, fullPath: String, isHide: Boolean, time: Long, isSymLink: Boolean) :
    FileItemModel(name, fullPath, isHide, time, isSymLink, "torrent") {
    var torrentName: String? = null
}