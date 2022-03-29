package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;

import com.storyteller_f.file_system.FileSystemUriSaver;
import com.storyteller_f.file_system.Filter;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MountedLocalFileInstance extends DocumentLocalFileInstance {
    public static final String Name = "mountedFileInstance";
    public static final String ROOT_URI = "external-extend-uri";

    public MountedLocalFileInstance(Filter filter, Context context, String path) {
        super(filter, context, path, Name, ROOT_URI);
        updateRoot();
        initCurrentFile();
    }

    public MountedLocalFileInstance(Context context, String path) {
        super(context, path, Name, ROOT_URI);
        updateRoot();
        initCurrentFile();
    }

    public MountedLocalFileInstance(String prefix, Context context) {
        super(context, Name, ROOT_URI);
        updateRoot();
        this.prefix = prefix;
    }

    @Override
    protected DocumentLocalFileInstance getInstance() {
        return new MountedLocalFileInstance(prefix, context);
    }

}
