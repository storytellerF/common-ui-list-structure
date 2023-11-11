package com.storyteller_f.ping.shader

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class SurfaceTextureBinding(private val mvpMatrix: FloatArray, private val textures: OESTexture) {
    private var surfaceTexture: SurfaceTexture? = null

    // Fix bug like https://stackoverflow.com/questions/14185661/surfacetexture-onframeavailablelistener-stops-being-called
    private var updatedFrame: Long = 0
    private var renderedFrame: Long = 0

    private var screenSize: Size? = null
    private var videoMatrix: VideoMatrix? = null
    private var offset: Offset = Offset()

    private var maxOffset: Offset? = null

    fun setScreenSize(size: Size) {
        Log.d(TAG, "setScreenSize() called with: size = $size")
        val oldSize = this.screenSize
        if (oldSize != size) {
            this.screenSize = size
            updateMaxOffset()
            updateMatrix()
        }
    }

    fun setVideoMatrix(videoMatrix: VideoMatrix, player: MediaPlayer) {
        Log.d(TAG, "setVideoMatrix() called with: matrix = $videoMatrix")
        // MediaMetadataRetriever always give us raw width and height and won't rotate them.
        // So we rotate them by ourselves.
        val oldVideoMatrix = this.videoMatrix
        if (oldVideoMatrix != videoMatrix) {
            this.videoMatrix = videoMatrix
            Log.i(TAG, "setVideoMatrix: new matrix $videoMatrix")
            updateMaxOffset()
            updateMatrix()
            setSourcePlayer(player)
        }
    }

    fun setOffset(offset: Offset) {
        Log.d(TAG, "setOffset() called with: offset = $offset $maxOffset")
        val maxOffset1 = maxOffset ?: return
        val oldXOffset = this.offset
        val rectified = Offset((offset.xOffset * maxOffset1.xOffset).let {
            if (it < 0.001) 0f else it
        }, (offset.yOffset * maxOffset1.yOffset).let {
            if (it < 0.001) 0f else it
        })
        if (rectified != oldXOffset) {
            this.offset = rectified
            Log.i(TAG, "setOffset: new offset $rectified")
            updateMatrix()
        }
    }

    private fun setSourcePlayer(player: MediaPlayer) {
        createSurfaceTexture()
        player.setSurface(Surface(surfaceTexture))
    }

    private fun createSurfaceTexture() {
        surfaceTexture?.release()
        updatedFrame = 0
        renderedFrame = 0
        surfaceTexture = textures.build().apply {
            val videoMatrix1 = videoMatrix!!
            setDefaultBufferSize(videoMatrix1.realWidth, videoMatrix1.realWidth)
            setOnFrameAvailableListener { ++updatedFrame }
        }
    }

    private fun updateMaxOffset() {
        Log.d(TAG, "updateMaxOffset() called $videoMatrix $screenSize ${Thread.currentThread()}")
        val matrix = videoMatrix ?: return
        val size = screenSize ?: return
        val videoMoreWidth = getVideoMoreWidth(matrix, size)
        maxOffset = if (videoMoreWidth) {
            val screenWidthExpected = matrix.realWidth.toFloat() * size.height / matrix.realHeight
            val widthOffset = 1.0f - size.width.toFloat() / screenWidthExpected
            Offset(abs(widthOffset) / 2)
        } else {
            val screenHeightExpected = matrix.realHeight.toFloat() * size.width / matrix.realWidth
            val heightOffset = 1.0f - size.height.toFloat() / screenHeightExpected
            Offset(yOffset = abs(heightOffset) / 2)
        }

        Log.i(TAG, "updateMaxOffset: $maxOffset")
    }

    private var updatingMatrix = AtomicBoolean(false)
    private fun updateMatrix() {
        Log.d(
            TAG,
            "updateMatrix() called $videoMatrix $screenSize $offset ${Thread.currentThread()}"
        )
        val matrix = videoMatrix ?: return
        val size = screenSize ?: return
        if (!updatingMatrix.compareAndSet(false, true)) {
            return
        }
        val offset1 = offset
        val videoMoreWidth = getVideoMoreWidth(matrix, size)
        val videoRotation = matrix.rotation
        // Players are buggy and unclear, so we do crop by ourselves.
        // Start with an identify matrix.
        for (i in 0..15) mvpMatrix[i] = 0.0f
        mvpMatrix[15] = 1.0f
        mvpMatrix[10] = mvpMatrix[15]
        mvpMatrix[5] = mvpMatrix[10]
        mvpMatrix[0] = mvpMatrix[5]
        // OpenGL model matrix: scaling, rotating, translating.
        if (videoMoreWidth) {
            Log.d(TAG, "updateMatrix: crop x")
            // Treat video and screen width as 1, and compare width to scale.
            val widthRatio = matrix.realWidth / getFitVideoWidth(size, matrix)
            Matrix.scaleM(mvpMatrix, 0, widthRatio, 1f, 1f)
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvpMatrix, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvpMatrix, 0, offset1.xOffset, 0f, 0f)
        } else {
            Log.d(TAG, "updateMatrix: crop y")
            // Treat video and screen height as 1, and compare height to scale.
            val heightRatio = matrix.realHeight / getFitVideoHeight(size, matrix)
            Matrix.scaleM(mvpMatrix, 0, 1f, heightRatio, 1f)
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvpMatrix, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvpMatrix, 0, 0f, offset1.yOffset, 0f)
        }
        updatingMatrix.set(false)
    }

    /**
     * 视频比屏幕更宽一些，所以根据屏幕尺寸和视频高度重新确定视频宽度。
     */
    private fun getFitVideoWidth(size: Size, matrix: VideoMatrix) =
        size.width.toFloat() * matrix.realHeight / size.height

    /**
     * 视频比屏幕更高一些，所以根据屏幕尺寸和视频宽度重新确定通过视频高度。
     */
    private fun getFitVideoHeight(size: Size, matrix: VideoMatrix) =
        size.height.toFloat() * matrix.realWidth / size.width

    /**
     * @return 如果为true，视频比屏幕更加宽。完全覆盖屏幕之后左右两边会有空余，这部分空余用于
     * 桌面滚动。反之亦然。
     */
    private fun getVideoMoreWidth(videoMatrix1: VideoMatrix, size: Size): Boolean {
        val videoRatio1 = videoMatrix1.let {
            it.realWidth.toFloat() / it.realHeight
        }
        val screenRatio1 = size.let {
            it.width.toFloat() / it.height
        }
        return videoRatio1 >= screenRatio1
    }

    val isReady: Boolean get() {
        if (updatingMatrix.get()) return false
        val texture = surfaceTexture ?: return false
        if (renderedFrame < updatedFrame) {
            texture.updateTexImage()
            ++renderedFrame
        }
        return true
    }
    
    companion object {
        private const val TAG = "SurfaceTextureBinding"
    }
}