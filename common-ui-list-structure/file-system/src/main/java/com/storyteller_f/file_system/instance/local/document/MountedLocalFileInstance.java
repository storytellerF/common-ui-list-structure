package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;

import com.storyteller_f.file_system.Filter;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MountedLocalFileInstance extends DocumentLocalFileInstance {
    public static final String NAME = "mounted-DocumentLocalFileInstance";
    public static final String ROOT_URI = "external-extend-uri";

    public MountedLocalFileInstance(Filter filter, Context context, String path) {
        super(filter, context, path, NAME, ROOT_URI);
    }

    public MountedLocalFileInstance(Context context, String path) {
        super(context, path, NAME, ROOT_URI);
    }

    @Override
    protected DocumentLocalFileInstance getInstance() {
        return new MountedLocalFileInstance(filter, context, "");
    }

}
