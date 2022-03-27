package com.storyteller_f.file_system;

import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;


import com.storyteller_f.file_system.instance.local.EmulatedLocalFileInstance;
import com.storyteller_f.file_system.instance.local.ExternalLocalFileInstance;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.instance.local.document.ExternalDocumentLocalFileInstance;
import com.storyteller_f.file_system.instance.local.document.MountedLocalFileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;

import java.util.ArrayList;
import java.util.List;

public class FileInstanceFactory {
    private static final String TAG = "FileInstanceFactory";
    static final String rootUserEmulatedPath = "/storage/emulated/0/";
    static final String emulatedRootPath = "/storage/emulated/";
    static final String storagePath = "/storage/";
    private static final Filter filter = new Filter() {
        @Override
        public boolean onPath(String parent, String absolutePath, boolean isFile) {
            return true;
        }

        @Override
        public List<FileItemModel> onFile(String parent) {
            return new ArrayList<>();
        }

        @Override
        public List<DirectoryItemModel> onDirectory(String parent) {
            return new ArrayList<>();
        }
    };

    public static LocalFileInstance getFileInstance(String path, Context context) {
        return getFileInstance(filter, path, context);
    }

    public static LocalFileInstance getFileInstance(Filter filter, String path, Context context) {
//        if (path.endsWith("//")) {
//            Log.e(TAG, "getFileInstance: path末尾是//:"+path);
//        }
        if (path.startsWith(rootUserEmulatedPath)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                return new ExternalDocumentLocalFileInstance(filter, context, path);
            } else {
                return new ExternalLocalFileInstance(context, filter, path);
            }
        } else if (path.startsWith(emulatedRootPath)) {
            return new EmulatedLocalFileInstance(context, filter);
        } else if (path.startsWith("/storage/self/")) {
            return new ExternalLocalFileInstance(context, filter, path);
        } else if (path.equals(storagePath)) {
            return new ExternalLocalFileInstance(context, filter, path);
        } else if (path.startsWith(storagePath)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return new ExternalLocalFileInstance(context, filter, path);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new MountedLocalFileInstance(filter, context, path);
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.e(TAG, "getFileInstance: 当前版本不支持formTreeUri，测试性的返回");
                return new ExternalLocalFileInstance(context, filter, path);
            } else {
                return new ExternalLocalFileInstance(context, filter, path);
            }

        } else {
            return new ExternalLocalFileInstance(context, filter, path);
        }
    }

    public static String getPrefix(String path) {
        if (path.startsWith(rootUserEmulatedPath)) {
            return rootUserEmulatedPath;
        } else if (path.startsWith(emulatedRootPath)) {
            return emulatedRootPath;
        } else if (path.equals(storagePath)) {
            return "/";
        } else if (path.startsWith(storagePath)) {
            int endIndex = path.indexOf("/", storagePath.length());
            if (endIndex == -1) endIndex = path.length();
            return path.substring(0, endIndex + 1);
        } else {
            return "/";
        }
    }

    public static long getSpace(String prefix) {
        StatFs stat = new StatFs(prefix);
        long blockSize;
        long availableBlocks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
        }
        return blockSize * availableBlocks;
    }
}
