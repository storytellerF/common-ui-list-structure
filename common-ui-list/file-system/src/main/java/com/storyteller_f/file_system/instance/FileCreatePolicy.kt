package com.storyteller_f.file_system.instance

interface FileCreatePolicy

object NotCreate: FileCreatePolicy

class Create(val isFile: Boolean) : FileCreatePolicy