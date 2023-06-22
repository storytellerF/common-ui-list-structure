package com.storyteller_f.file_system.instance;

import android.content.Context;
import android.net.Uri;

public abstract class BaseContextFileInstance extends FileInstance {
    protected Context context;

    /**
     * @param uri   路径
     */
    public BaseContextFileInstance(Context context, Uri uri) {
        super(uri);
        this.context = context;
    }
}
