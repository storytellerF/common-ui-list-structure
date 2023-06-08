package com.storyteller_f.file_system.util

import java.util.Locale

fun permissions(r: Boolean, w: Boolean, e: Boolean, isFile: Boolean): String {
    return String.format(Locale.CHINA, "%c%c%c%c", if (isFile) '-' else 'd', if (r) 'r' else '-', if (w) 'w' else '-', if (e) 'e' else '-')
}