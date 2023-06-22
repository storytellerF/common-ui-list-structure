package com.storyteller_f.file_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileSystemUriSaver {
    private static final FileSystemUriSaver fileSystemUriSaver = new FileSystemUriSaver();

    private final HashMap<String, Uri> documentRootCache = new HashMap<>(2);

    public static FileSystemUriSaver getInstance() {
        return fileSystemUriSaver;
    }

    @Nullable
    public Uri savedUri(Context context, String sharedPreferenceKey) {
        if (!documentRootCache.containsKey(sharedPreferenceKey)) {
            var sharedPreferences = getSharedPreferences(context);
            String uriString = sharedPreferences.getString(sharedPreferenceKey, null);
            if (uriString == null) return null;
            Uri root = Uri.parse(uriString);
            documentRootCache.put(sharedPreferenceKey, root);
        }
        return documentRootCache.get(sharedPreferenceKey);
    }

    public void saveUri(Context context, String key, Uri uri) {
        var sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putString(key, uri.toString()).apply();
        documentRootCache.put(key, uri);
    }

    public List<String> savedUris(Context context) {
        if (documentRootCache.isEmpty()) {
            var sharedPreferences = getSharedPreferences(context);
            for (String k : sharedPreferences.getAll().keySet()) {
                String uriString = sharedPreferences.getString(k, null);
                if (uriString != null)
                    documentRootCache.put(k, Uri.parse(uriString));
            }
        }
        return new ArrayList<>(documentRootCache.keySet());
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("file-system-uri", Context.MODE_PRIVATE);
    }
}
