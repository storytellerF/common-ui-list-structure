package com.storyteller_f.ping

import android.app.ActivityManager
import android.app.WallpaperColors
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.storyteller_f.ping.shader.GLES20WallpaperRenderer
import com.storyteller_f.ping.shader.GLES30WallpaperRenderer
import com.storyteller_f.ping.shader.GLWallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.PrintWriter
import kotlin.coroutines.CoroutineContext

class PingPagerService : WallpaperService() {
    val job = Job()
    val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main

    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine() called")
        return PingEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "onDestroy() called")
    }

    inner class PingEngine : WallpaperService.Engine() {
        private var player: MediaPlayer? = null
        private var render: GLWallpaperRenderer? = null
        private val surfaceView: GLPingSurfaceView = GLPingSurfaceView(this@PingPagerService)

        inner class GLPingSurfaceView(context: Context) : GLSurfaceView(context) {
            override fun getHolder(): SurfaceHolder = surfaceHolder
            fun destroy() {
                super.onDetachedFromWindow()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            Log.d(TAG, "onCreate() called with: surfaceHolder = $surfaceHolder")
            super.onCreate(surfaceHolder)
            if (player == null) player = MediaPlayer().apply {
                isLooping = true
            }

            val systemService = ContextCompat.getSystemService(this@PingPagerService, ActivityManager::class.java) ?: throw Exception("无法获得activity manager")

            val deviceConfigurationInfo = systemService.deviceConfigurationInfo
            val (ver, glWallpaperRenderer) = when {
                deviceConfigurationInfo.reqGlEsVersion >= 0x30000 -> {
                    3 to GLES30WallpaperRenderer(this@PingPagerService)
                }
                deviceConfigurationInfo.reqGlEsVersion >= 0x20000 -> {
                    2 to GLES20WallpaperRenderer(this@PingPagerService)
                }
                else -> throw RuntimeException("can not get gl version")
            }
            Log.i(TAG, "onCreate: version: $ver")
            surfaceView.setEGLContextClientVersion(ver)
            render = glWallpaperRenderer
            surfaceView.preserveEGLContextOnPause = true
            surfaceView.setRenderer(render)
            surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            scope.launch {
                val wallpaper = wallpaperUri() ?: return@launch
                Log.i(TAG, "onCreate: wallpaper $wallpaper")
                player?.setDataSource(this@PingPagerService, wallpaper)
                player?.prepareAsync()
                player?.setOnVideoSizeChangedListener { _, width, height ->
                    Log.i(TAG, "onVideoSizeChangedListener: width $width height $height")
                    render?.setVideoSizeAndRotation(width, height, 0)
                }
            }

        }

        private suspend fun wallpaperUri(): Uri? {
            val exampleCounterFlow = this@PingPagerService.dataStore.data.mapNotNull { preferences ->
                // No type safety.
                (if (isPreview) preferences[preview]
                else preferences[selected]) ?: preferences[preview]
            }
            return exampleCounterFlow.first().takeIf { it.isNotEmpty() }?.toUri()
        }

        override fun onDestroy() {
            Log.d(TAG, "onDestroy() called")
            player?.release()
            render = null
            surfaceView.destroy()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Log.d(TAG, "onVisibilityChanged() called with: visible = $visible")
            if (visible) {
                if (player?.isPlaying != true) {
                    player?.start()
                }
            } else {
                if (player?.isPlaying == true) player?.pause()
            }
            super.onVisibilityChanged(visible)
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            Log.d(TAG, "onApplyWindowInsets() called with: insets = $insets")
            super.onApplyWindowInsets(insets)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            Log.d(
                TAG,
                "onOffsetsChanged() called with: xOffset = $xOffset, yOffset = $yOffset, xOffsetStep = $xOffsetStep, yOffsetStep = $yOffsetStep, xPixelOffset = $xPixelOffset, yPixelOffset = $yPixelOffset"
            )
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            render?.setOffset(xOffset, yOffset)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceChanged() called with: holder = $holder, format = $format, width = $width, height = $height")
            super.onSurfaceChanged(holder, format, width, height)
            render?.setScreenSize(width, height)
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceRedrawNeeded() called with: holder = $holder")
            super.onSurfaceRedrawNeeded(holder)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceCreated() called with: holder = $holder")
            super.onSurfaceCreated(holder)
            val width = holder?.surfaceFrame?.width() ?: return
            val height = holder.surfaceFrame.height()
            render?.setScreenSize(width, height)
            player?.setOnPreparedListener {
                Log.i(TAG, "onSurfaceCreated: OnPreparedListener")
                render?.setSourcePlayer(it)
                it.start()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceDestroyed() called with: holder = $holder")
            super.onSurfaceDestroyed(holder)
        }

        override fun onZoomChanged(zoom: Float) {
            Log.d(TAG, "onZoomChanged() called with: zoom = $zoom")
            super.onZoomChanged(zoom)
        }

        override fun onComputeColors(): WallpaperColors? {
            Log.d(TAG, "onComputeColors() called")
            return super.onComputeColors()
        }

        override fun dump(prefix: String?, fd: FileDescriptor?, out: PrintWriter?, args: Array<out String>?) {
            Log.d(TAG, "dump() called with: prefix = $prefix, fd = $fd, out = $out, args = $args")
            super.dump(prefix, fd, out, args)
        }
    }

    companion object {
        private const val TAG = "PingPagerService"
    }
}
