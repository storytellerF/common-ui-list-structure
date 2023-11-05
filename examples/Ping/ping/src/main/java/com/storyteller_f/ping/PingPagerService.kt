package com.storyteller_f.ping

import android.app.ActivityManager
import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.storyteller_f.ping.shader.GLES20WallpaperRenderer
import com.storyteller_f.ping.shader.GLES30WallpaperRenderer
import com.storyteller_f.ping.shader.GLWallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class PingPagerService : WallpaperService() {
    val job = Job()
    val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main

    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind() called with: intent = $intent")
        return super.onUnbind(intent)
    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine() called")
        return PingEngine(this)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        job.cancel()
    }

    inner class PingEngine(private val inContext: Context) : WallpaperService.Engine() {
        private var currentThumbnail: Bitmap? = null
        private val player: MediaPlayer by lazy {
            MediaPlayer().apply {
                isLooping = true
                setOnVideoSizeChangedListener { _, width, height ->
                    Log.i(TAG, "onVideoSizeChangedListener: width $width height $height")
                    renderer.setVideoSizeAndRotation(width, height, 0)
                }
                setOnPreparedListener {
                    Log.i(TAG, "OnPreparedListener")
                    renderer.setSourcePlayer(it)
                    it.start()
                }
            }
        }
        private val renderer: GLWallpaperRenderer by lazy {
            val systemService =
                ContextCompat.getSystemService(inContext, ActivityManager::class.java)
                    ?: throw Exception("无法获得activity manager")
            val deviceConfigurationInfo = systemService.deviceConfigurationInfo
            val glWallpaperRenderer = when {
                deviceConfigurationInfo.reqGlEsVersion >= 0x30000 -> {
                    GLES30WallpaperRenderer(inContext)
                }

                deviceConfigurationInfo.reqGlEsVersion >= 0x20000 -> {
                    GLES20WallpaperRenderer(inContext)
                }

                else -> throw RuntimeException("can not get gl version")
            }
            glWallpaperRenderer
        }
        private val surfaceView: GLPingSurfaceView by lazy {
            GLPingSurfaceView(inContext).apply {
                setEGLContextClientVersion(renderer.version)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }

        inner class GLPingSurfaceView(context: Context) : GLSurfaceView(context) {
            override fun getHolder(): SurfaceHolder = surfaceHolder
            fun destroy() = super.onDetachedFromWindow()
        }

        private fun observeLatestUri() {
            scope.launch {
                inContext.wallpaperUri().distinctUntilChanged().collectLatest {
                    val file = File(it)
                    val thumbnail = File(file.parentFile, "thumbnail.jpg")
                    Log.i(
                        TAG,
                        "wallpaper $it thumb${thumbnail.absolutePath} ${thumbnail.exists()}"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        currentThumbnail = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeFile(thumbnail.absolutePath)
                        }
                        notifyColorsChanged()
                    }
                    player.run {
                        stop()
                        setDataSource(inContext, Uri.fromFile(file))
                        prepareAsync()
                    }
                }
            }
        }

        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean
        ): Bundle? {
            Log.d(
                TAG,
                "onCommand() called with: action = $action, x = $x, y = $y, z = $z, extras = $extras, resultRequested = $resultRequested"
            )
            return null
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            Log.d(TAG, "onCreate() called with: surfaceHolder = $surfaceHolder")
            super.onCreate(surfaceHolder)
            surfaceView//init
            observeLatestUri()
        }

        override fun onDestroy() {
            Log.d(TAG, "onDestroy() called")
            super.onDestroy()
            player.release()
            surfaceView.destroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            val playing = player.isPlaying
            Log.d(TAG, "onVisibilityChanged() called with: visible = $visible $playing")
            if (visible) {
                if (!playing) {
                    player.start()
                }
            } else {
                if (playing) player.pause()
            }
            super.onVisibilityChanged(visible)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            Log.d(
                TAG,
                "onOffsetsChanged() called with: xOffset = $xOffset, yOffset = $yOffset, xOffsetStep = $xOffsetStep, yOffsetStep = $yOffsetStep, xPixelOffset = $xPixelOffset, yPixelOffset = $yPixelOffset"
            )
            super.onOffsetsChanged(
                xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset
            )
            renderer.setOffset(xOffset, yOffset)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            Log.d(
                TAG,
                "onSurfaceChanged() called with: holder = $holder, format = $format, width = $width, height = $height"
            )
            super.onSurfaceChanged(holder, format, width, height)
            renderer.setScreenSize(width, height)
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
            renderer.setScreenSize(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceDestroyed() called with: holder = $holder")
            super.onSurfaceDestroyed(holder)
        }

        override fun onComputeColors() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            currentThumbnail?.let {
                WallpaperColors.fromBitmap(it).apply {
                    Log.i(TAG, "onComputeColors: ${this.primaryColor}")
                }
            }
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "PingPagerService"
    }
}

private fun Context.wallpaperUri() = dataStore.data.mapNotNull { preferences ->
    // No type safety.
    preferences[preview].takeIf { it?.isNotEmpty() == true } ?: preferences[selected]
}