package com.storyteller_f.file_system.model

class TorrentFileItemModel(name: String, fullPath: String, isHide: Boolean, time: Long) :
    FileItemModel(name, fullPath, isHide, time, "torrent") {
    var torrentName: String? = null
}