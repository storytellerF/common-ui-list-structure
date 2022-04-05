package com.storyteller_f.giant_explorer.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

public class FileOperateService extends Service {
    private static final String TAG = "FileOperateService";

    public FileOperateService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind() called with: intent = [" + intent + "]");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");
        return super.onUnbind(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged() called with: newConfig = [" + newConfig + "]");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FileOperateBinder(getApplicationContext());
    }

    public interface FileOperateResult {

        /**
         * @param dest 去路径
         * @param origin 来路径
         */
        void onSuccess(String dest,String origin);

        void onError(String string);

        void onCancel();

    }
}
