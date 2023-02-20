package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;

import com.storyteller_f.file_system.Filter;


/**
 * 当前的文件夹只能够处理/storage/emulated/0中的文件
 */
public class ExternalDocumentLocalFileInstance extends DocumentLocalFileInstance {
    public static final String NAME = "external-DocumentLocalFileInstance";
    public static final String STORAGE_URI = "external-storage-uri";

    public ExternalDocumentLocalFileInstance(Filter filter, Context context, String path) {
        super(filter, context, path, NAME, STORAGE_URI);
    }

    public ExternalDocumentLocalFileInstance(Context context, String path) {
        super(context, path, NAME, STORAGE_URI);
    }

    @Override
    protected DocumentLocalFileInstance getInstance() {
        return new ExternalDocumentLocalFileInstance(filter, context, "");
    }
}
