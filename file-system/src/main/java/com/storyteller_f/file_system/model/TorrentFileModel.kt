package com.storyteller_f.file_system.model

class TorrentFileModel(name: String, absolutePath: String, isHide: Boolean, time: Long) :
    FileItemModel(name, absolutePath, isHide, time, "torrent") {
    var torrentName: String? = null
}