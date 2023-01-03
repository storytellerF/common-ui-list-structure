package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;

import com.storyteller_f.file_system.Filter;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MountedLocalFileInstance extends DocumentLocalFileInstance {
    public static final String Name = "mounted-DocumentLocalFileInstance";
    public static final String ROOT_URI = "external-extend-uri";

    public MountedLocalFileInstance(Filter filter, Context context, String path) {
        super(filter, context, path);
        updateRootKey(Name, ROOT_URI);
        initDocumentFile();
    }

    public MountedLocalFileInstance(Context context, String path) {
        super(context, path);
        updateRootKey(Name, ROOT_URI);
        initDocumentFile();
    }

    @Override
    protected DocumentLocalFileInstance getInstance() {
        return new MountedLocalFileInstance(filter, context, "");
    }

}
