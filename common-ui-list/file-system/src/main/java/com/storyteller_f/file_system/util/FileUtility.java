package com.storyteller_f.file_system.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.storyteller_f.file_system.FileInstanceFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileUtility {
    private static final String TAG = "FileUtility";

    public static String getPermissionStringByFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPermissions(file.isFile(), file);
        }
        boolean w = file.canWrite();
        boolean e = file.canExecute();
        boolean r = file.canRead();
        return UtilityKt.permissions(r, w, e, file.isFile());
    }

    public static String getPermissions(DocumentFile file) {
        var w = file.canWrite();
        var r = file.canRead();
        return UtilityKt.permissions(r, w, false, file.isFile());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getPermissions(boolean b, File file) {
        Path path = file.toPath();
        boolean w = Files.isWritable(path);
        boolean e = Files.isExecutable(path);
        boolean r = Files.isReadable(path);
        return String.format(Locale.CHINA, "%c%c%c%c", (b ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (e ? 'e' : '-'));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static List<StorageVolume> getStorageVolume(Context context, StorageManager storageManager) {
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        for (StorageVolume volume : storageVolumes) {
            printStorageVolume(volume, context);
        }
        return storageVolumes;
    }

    public static void printStorageVolume(StorageVolume storageVolume, Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String uuid = storageVolume.getUuid();
            stringBuilder.append("Uuid:").append(uuid).append("\n");
            String description = storageVolume.getDescription(context);
            stringBuilder.append("Description:").append(description).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File directory = storageVolume.getDirectory();
            stringBuilder.append("directory:").append(directory).append("\n");
            String mediaStoreVolumeName = storageVolume.getMediaStoreVolumeName();
            stringBuilder.append("mediaStore:").append(mediaStoreVolumeName).append("\n");
            String state = storageVolume.getState();
            stringBuilder.append("state:").append(state).append("\n");
        }
        Log.i(TAG, "printStorageVolume: " + stringBuilder);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<StorageVolume> getStorageVolume(Context context) {
        StorageManager storageManager = context.getSystemService(StorageManager.class);
        return getStorageVolume(context, storageManager);
    }

    @NonNull
    public static File[] getStorageCompat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getStorageVolume(context).stream().map(storageVolume -> {
                String uuid = storageVolume.getUuid();
                return new File(FileInstanceFactory.storagePath, Objects.requireNonNullElse(uuid, "emulated"));
            }).toArray(File[]::new);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externalFilesDirs = context.getExternalCacheDirs();
            File[] files = new File[externalFilesDirs.length];
            for (int i = 0; i < externalFilesDirs.length; i++) {
                String absolutePath = externalFilesDirs[i].getAbsolutePath();
                String android = absolutePath.substring(0, absolutePath.indexOf("Android"));
                files[i] = new File(android);
            }
            return files;
        } else {
            File file = new File("/storage/");
            File[] files = file.listFiles();
            if (files == null) {
                return new File[]{};
            }
            return files;
        }
    }

    @Nullable
    public static Intent produceSafRequestIntent(Activity activity, String prefix) {
        Intent intent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StorageManager sm = activity.getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(prefix));
            if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = volume.createOpenDocumentTreeIntent();
            }
        }
        if (intent == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, prefix);
            }
        }
        return intent;
    }

    @Nullable
    public static String getExtension(String name) {
        String extension;
        int index = name.lastIndexOf('.');
        if (index != -1) {
            extension = name.substring(index + 1);
        } else {
            extension = null;
        }
        return extension;
    }

}
