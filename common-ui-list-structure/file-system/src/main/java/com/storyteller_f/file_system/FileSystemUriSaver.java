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
    public Uri savedUri(String sharedPreferenceKey, Context context) {
        if (!documentRootCache.containsKey(sharedPreferenceKey)) {
            var sharedPreferences = getSharedPreferences(context);
            String uriString = sharedPreferences.getString(sharedPreferenceKey, null);
            if (uriString == null) return null;
            Uri root = Uri.parse(uriString);
            documentRootCache.put(sharedPreferenceKey, root);
        }
        return documentRootCache.get(sharedPreferenceKey);
    }

    public void saveUri(String key, Context context, Uri uri) {
        var sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putString(key, uri.toString()).apply();
        documentRootCache.put(key, uri);
    }

    public List<String> savedUris(Context context) {
        if (documentRootCache.isEmpty()) {
            var sharedPreferences = getSharedPreferences(context);
            for (String k : sharedPreferences.getAll().keySet()) {
                String string = sharedPreferences.getString(k, "");
                documentRootCache.put(k, Uri.parse(string));
            }
        }
        return new ArrayList<>(documentRootCache.keySet());
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("file-system-uri", Context.MODE_PRIVATE);
    }
}
