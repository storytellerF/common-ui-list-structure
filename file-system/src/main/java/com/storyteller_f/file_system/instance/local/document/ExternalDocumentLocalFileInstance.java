package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;

import com.storyteller_f.file_system.FileSystemUriSaver;
import com.storyteller_f.file_system.Filter;


/**
 * 当前的文件夹只能够处理/storage/emulated/0/中的文件
 */
public class ExternalDocumentLocalFileInstance extends DocumentLocalFileInstance {
    private static final String TAG = "InternalDocumentLocalFi";
    public static final String Name = "externalDocumentFileInstance";
    public static final String STORAGE_URI = "storage-uri";

    public ExternalDocumentLocalFileInstance(Filter filter, Context context, String path) {
        super(filter, context, path, Name, STORAGE_URI);
        updateRoot();
        initCurrentFile();
    }

    public ExternalDocumentLocalFileInstance(String prefix, Context context) {
        super(context, Name, STORAGE_URI);
        updateRoot();
        this.prefix = prefix;
    }

    @Override
    protected DocumentLocalFileInstance getInstance() {
        return new ExternalDocumentLocalFileInstance(prefix, context);
    }
}
