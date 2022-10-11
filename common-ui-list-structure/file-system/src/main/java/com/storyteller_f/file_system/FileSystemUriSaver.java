package com.storyteller_f.file_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.HashMap;

public class FileSystemUriSaver {
    private static final FileSystemUriSaver fileSystemUriSaver = new FileSystemUriSaver();
    private HashMap<String, Uri> documentRootCache = new HashMap<>(2);

    public static FileSystemUriSaver getInstance() {
        return fileSystemUriSaver;
    }

    public void destroy() {
        documentRootCache.clear();
        documentRootCache = null;
    }

    public Uri getUri(String key) {
        return documentRootCache.get(key);
    }

    public String saveUri(String sharedPreferenceName, String sharedPreferenceKey, Context context) {
        String key = sharedPreferenceName + sharedPreferenceKey;
        if (!documentRootCache.containsKey(key)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE);
            String uriString = sharedPreferences.getString(sharedPreferenceKey, null);
            if (uriString != null) {
                Uri root = Uri.parse(uriString);
                documentRootCache.put(key, root);
            } else {
                return null;
            }
        }
        return key;
    }
}
