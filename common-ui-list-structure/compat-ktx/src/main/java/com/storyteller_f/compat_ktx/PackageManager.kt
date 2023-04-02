package com.storyteller_f.compat_ktx

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.packageInfoCompat(absolutePath: String): PackageInfo? {
    val packageArchiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageArchiveInfo(absolutePath, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        (getPackageArchiveInfo(absolutePath, 0))
    }
    return packageArchiveInfo
}