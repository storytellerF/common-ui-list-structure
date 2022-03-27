package com.storyteller_f.file_system.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class FileUtility {
    private static final String TAG = "FileUtility";

    public static String getDetailStringByFile(FileSystemItemModel fileItemModel) {
        File file = new File(fileItemModel.getAbsolutePath());
        boolean w = file.canWrite();
        boolean e = file.canExecute();
        boolean r = file.canRead();
        return String.format(Locale.CHINA, "%c%c%c%c", (fileItemModel instanceof FileItemModel ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (e ? 'e' : '-'));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getDetailString(FileSystemItemModel fileItemModel) {
        Path path = new File(fileItemModel.getAbsolutePath()).toPath();
        boolean w = Files.isWritable(path);
        boolean e = Files.isExecutable(path);
        boolean r = Files.isReadable(path);
        return String.format(Locale.CHINA, "%c%c%c%c", (fileItemModel instanceof FileItemModel ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (e ? 'e' : '-'));
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String[] getStorageFileStrings(Context context) {
        File[] sdCard = getStorageFile(context);
        String[] strings = new String[sdCard.length];
        for (int i = 0; i < sdCard.length; i++) {
            strings[i] = sdCard[i].getAbsolutePath();
        }
        return strings;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static File[] getStorageFile(Context context) {
        File[] externalFilesDirs = context.getExternalCacheDirs();
        File[] files = new File[externalFilesDirs.length];
        for (int i = 0; i < externalFilesDirs.length; i++) {
            String absolutePath = externalFilesDirs[i].getAbsolutePath();
            String android = absolutePath.substring(0, absolutePath.indexOf("Android"));
            files[i] = new File(android);
        }
        return files;
    }

    public static String[] getStorageFileStringsSafe(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getStorageFileStrings(context);
        } else {
            File file = new File("/storage/");
            File[] files = file.listFiles((dir, name) -> {
                if (name.equals("emulated")) {
                    return false;
                } else return !name.equals("self");
            });
            if (files == null) {
                return null;
            }
            String[] strings = new String[files.length + 1];
            strings[0] = "/storage/emulated/0";
            for (int i = 0; i < files.length; i++) {
                strings[i + 1] = files[i].getAbsolutePath();
            }
            return strings;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String[] getUuid(Context context) {
        List<StorageVolume> storageVolume = getStorageVolume(context);
        String[] names = new String[storageVolume.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = storageVolume.get(i).getUuid();
        }
        return names;
    }
    /**
     * 请求SAF 权限，如果是/storage/emulated/0/目录，只在API 29有效，如果是SD 目录，直到API N
     * @param prefix
     * @param activity
     * @param requestCode
     * @return 如果本来就有权限，返回true
     */
    public static boolean requestSAF(String prefix, Activity activity, int requestCode) {
        Log.d(TAG, "requestSAF() called with: prefix = [" + prefix + "], activity = [" + activity + "], requestCode = [" + requestCode + "]");
        Intent intent = produceRequestSaf(prefix, activity);
        if (intent == null) return true;
        activity.startActivityForResult(intent, requestCode);
        return false;
    }

    @Nullable
    public static Intent produceRequestSaf(String prefix, Activity activity) {
        Intent intent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StorageManager sm = activity.getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(prefix));
            if (volume != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    intent = volume.createOpenDocumentTreeIntent();
                } else {
                    intent = volume.createAccessIntent(null);
                }
            }
        }
        if (intent == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            } else {
                //没有权限问题
                return null;
            }
        }
        return intent;
    }

}
