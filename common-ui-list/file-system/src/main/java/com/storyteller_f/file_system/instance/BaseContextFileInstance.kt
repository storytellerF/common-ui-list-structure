package com.storyteller_f.file_system.instance

import android.content.Context
import android.net.Uri

abstract class BaseContextFileInstance
/**
 * @param uri   路径
 */(protected val context: Context, uri: Uri) : FileInstance(uri)
