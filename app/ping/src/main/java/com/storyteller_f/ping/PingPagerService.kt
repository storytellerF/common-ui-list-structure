package com.storyteller_f.ping

import android.app.WallpaperColors
import android.content.ContentResolver
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowInsets
import java.io.FileDescriptor
import java.io.PrintWriter

class PingPagerService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine() called")
        return PingEngine()
    }

    inner class PingEngine : WallpaperService.Engine() {
        override fun getSurfaceHolder(): SurfaceHolder {
            Log.d(TAG, "getSurfaceHolder() called")
            return super.getSurfaceHolder()
        }

        var player: MediaPlayer? = null

        override fun getDesiredMinimumWidth(): Int {
            Log.d(TAG, "getDesiredMinimumWidth() called")
            return super.getDesiredMinimumWidth()
        }

        override fun getDesiredMinimumHeight(): Int {
            Log.d(TAG, "getDesiredMinimumHeight() called")
            return super.getDesiredMinimumHeight()
        }

        override fun isVisible(): Boolean {
            Log.d(TAG, "isVisible() called")
            return super.isVisible()
        }

        override fun isPreview(): Boolean {
            Log.d(TAG, "isPreview() called")
            return super.isPreview()
        }

        override fun setTouchEventsEnabled(enabled: Boolean) {
            Log.d(TAG, "setTouchEventsEnabled() called with: enabled = $enabled")
            super.setTouchEventsEnabled(enabled)
        }

        override fun setOffsetNotificationsEnabled(enabled: Boolean) {
            Log.d(TAG, "setOffsetNotificationsEnabled() called with: enabled = $enabled")
            super.setOffsetNotificationsEnabled(enabled)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            Log.d(TAG, "onCreate() called with: surfaceHolder = $surfaceHolder")
            if (player == null) player = MediaPlayer()
            val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(packageName)
                .appendPath("${R.raw.ani}")
                .build()
            player?.setDataSource(this@PingPagerService, uri)
            player?.isLooping = true
            player?.prepareAsync()
            player?.setOnPreparedListener {
                it.start()
            }
            super.onCreate(surfaceHolder)
        }

        override fun onDestroy() {
            Log.d(TAG, "onDestroy() called")
            player?.release()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Log.d(TAG, "onVisibilityChanged() called with: visible = $visible")
            if (visible) {
                if (player?.isPlaying == true) {

                }
            }
            super.onVisibilityChanged(visible)
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            Log.d(TAG, "onApplyWindowInsets() called with: insets = $insets")
            super.onApplyWindowInsets(insets)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            Log.d(TAG, "onTouchEvent() called with: event = $event")
            super.onTouchEvent(event)
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            Log.d(
                TAG,
                "onOffsetsChanged() called with: xOffset = $xOffset, yOffset = $yOffset, xOffsetStep = $xOffsetStep, yOffsetStep = $yOffsetStep, xPixelOffset = $xPixelOffset, yPixelOffset = $yPixelOffset"
            )
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
        }

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle {
            Log.d(TAG, "onCommand() called with: action = $action, x = $x, y = $y, z = $z, extras = $extras, resultRequested = $resultRequested")
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onDesiredSizeChanged(desiredWidth: Int, desiredHeight: Int) {
            Log.d(TAG, "onDesiredSizeChanged() called with: desiredWidth = $desiredWidth, desiredHeight = $desiredHeight")
            super.onDesiredSizeChanged(desiredWidth, desiredHeight)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceChanged() called with: holder = $holder, format = $format, width = $width, height = $height")
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceRedrawNeeded() called with: holder = $holder")
            super.onSurfaceRedrawNeeded(holder)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceCreated() called with: holder = $holder")
            player?.setSurface(holder?.surface)
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceDestroyed() called with: holder = $holder")
            super.onSurfaceDestroyed(holder)
//            player?.release()
        }

        override fun onZoomChanged(zoom: Float) {
            Log.d(TAG, "onZoomChanged() called with: zoom = $zoom")
            super.onZoomChanged(zoom)
        }

        override fun notifyColorsChanged() {
            Log.d(TAG, "notifyColorsChanged() called")
            super.notifyColorsChanged()
        }

        override fun onComputeColors(): WallpaperColors? {
            Log.d(TAG, "onComputeColors() called")
            return super.onComputeColors()
        }

        override fun dump(prefix: String?, fd: FileDescriptor?, out: PrintWriter?, args: Array<out String>?) {
            Log.d(TAG, "dump() called with: prefix = $prefix, fd = $fd, out = $out, args = $args")
            super.dump(prefix, fd, out, args)
        }

        override fun getDisplayContext(): Context? {
            Log.d(TAG, "getDisplayContext() called")
            return super.getDisplayContext()
        }
    }

    companion object {
        private const val TAG = "PingPagerService"
    }
}
