package com.storyteller_f.ping

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
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
import java.io.File
import kotlin.coroutines.CoroutineContext

class PingPagerService : WallpaperService() {
    val job = Job()
    val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main

    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine() called")
        return PingEngine(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "onDestroy() called")
    }

    inner class PingEngine(val inContext: Context) : WallpaperService.Engine() {
        private var player: MediaPlayer? = null
        private var render: GLWallpaperRenderer? = null
        private val surfaceView: GLPingSurfaceView = GLPingSurfaceView(inContext)

        inner class GLPingSurfaceView(context: Context) : GLSurfaceView(context) {
            override fun getHolder(): SurfaceHolder = surfaceHolder
            fun destroy() {
                super.onDetachedFromWindow()
            }
        }

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            Log.d(TAG, "onCommand() called with: action = $action, x = $x, y = $y, z = $z, extras = $extras, resultRequested = $resultRequested")
            if (action == "android.wallpaper.reapply") {
                play()
            }
            return null
        }

        private fun play() {
            initPlayer()
            scope.launch {
                flash()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            Log.d(TAG, "onCreate() called with: surfaceHolder = $surfaceHolder")
            super.onCreate(surfaceHolder)

            val systemService = ContextCompat.getSystemService(inContext, ActivityManager::class.java) ?: throw Exception("无法获得activity manager")
            val deviceConfigurationInfo = systemService.deviceConfigurationInfo
            val (ver, glWallpaperRenderer) = when {
                deviceConfigurationInfo.reqGlEsVersion >= 0x30000 -> {
                    3 to GLES30WallpaperRenderer(inContext)
                }
                deviceConfigurationInfo.reqGlEsVersion >= 0x20000 -> {
                    2 to GLES20WallpaperRenderer(inContext)
                }
                else -> throw RuntimeException("can not get gl version")
            }
            Log.i(TAG, "onCreate: version: $ver")
            surfaceView.setEGLContextClientVersion(ver)
            render = glWallpaperRenderer
            surfaceView.preserveEGLContextOnPause = true
            surfaceView.setRenderer(render)
            surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            play()
        }

        private fun initPlayer() {
            player?.stop()
            player?.release()
            player = MediaPlayer().apply {
                isLooping = true
            }
        }

        private fun flash() {
            scope.launch {
                val wallpaper = wallpaperUri() ?: return@launch
                Log.i(TAG, "flash: wallpaper $wallpaper")
                player?.setDataSource(inContext, Uri.fromFile(File(wallpaper)))
                player?.prepareAsync()
                player?.setOnVideoSizeChangedListener { _, width, height ->
                    Log.i(TAG, "flash onVideoSizeChangedListener: width $width height $height")
                    render?.setVideoSizeAndRotation(width, height, 0)
                }
                player?.setOnPreparedListener {
                    Log.i(TAG, "flash: OnPreparedListener")
                    render?.setSourcePlayer(it)
                    it.start()
                }
            }
        }

        private suspend fun wallpaperUri(): String? {
            val exampleCounterFlow = inContext.dataStore.data.mapNotNull { preferences ->
                // No type safety.
                preferences[preview].takeIf { it?.isNotEmpty() == true } ?: preferences[selected]
            }
            return exampleCounterFlow.first().takeIf { it.isNotEmpty() }
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
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceDestroyed() called with: holder = $holder")
            super.onSurfaceDestroyed(holder)
        }
    }

    companion object {
        private const val TAG = "PingPagerService"
    }
}
