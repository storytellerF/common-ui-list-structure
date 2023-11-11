/*
 * Copyright 2019 Alynx Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.storyteller_f.ping.shader

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RawRes
import com.storyteller_f.ping.compileShaderResourceGLES20
import com.storyteller_f.ping.linkProgramGLES20
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

data class VideoMatrix(val width: Int, val height: Int, val rotation: Int) {
    private val horizontalFlip: Boolean
        get() = rotation % 180 != 0

    val realHeight = if (horizontalFlip) width else height
    val realWidth = if (horizontalFlip) height else width
}

data class Offset(val xOffset: Float = 0f, val yOffset: Float = 0f) {
}

abstract class GLWallpaperRenderer(
    protected val context: Context,
    @RawRes val vertexRes: Int,
    @RawRes val fragmentRes: Int,
    val version: Int
) : GLSurfaceView.Renderer {
    private var screenSize: Size? = null
    private var videoMatrix: VideoMatrix? = null
    private var offset: Offset = Offset()

    private var maxOffset: Offset? = null

    private val mvpMatrix = floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,//
        0.0f, 1.0f, 0.0f, 0.0f,//
        0.0f, 0.0f, 1.0f, 0.0f,//
        0.0f, 0.0f, 0.0f, 1.0f//
    )

    protected val buffers by lazy {
        GLBuffer()
    }

    private val textures by lazy {
        OESTexture()
    }

    protected val program by lazy {
        linkProgramGLES20(
            compileShaderResourceGLES20(
                context, GLES30.GL_VERTEX_SHADER, vertexRes
            ), compileShaderResourceGLES20(
                context, GLES30.GL_FRAGMENT_SHADER, fragmentRes
            )
        )
    }
    private val mvpLocation by lazy { GLES20.glGetUniformLocation(program, "mvp") }

    private var surfaceTexture: SurfaceTexture? = null

    // Fix bug like https://stackoverflow.com/questions/14185661/surfacetexture-onframeavailablelistener-stops-being-called
    private var updatedFrame: Long = 0
    private var renderedFrame: Long = 0

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

    fun initGl() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        mvpLocation
        textures
    }
    //生命周期函数
    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (updatingMatrix) return
        val texture = surfaceTexture ?: return
        if (renderedFrame < updatedFrame) {
            texture.updateTexImage()
            ++renderedFrame
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvpMatrix, 0)

        drawImage()
    }

    abstract fun drawImage()
    //生命周期函数

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

    private var updatingMatrix: Boolean = false
    private fun updateMatrix() {
        Log.d(
            TAG,
            "updateMatrix() called $videoMatrix $screenSize $offset ${Thread.currentThread()}"
        )
        val matrix = videoMatrix ?: return
        val size = screenSize ?: return
        if (updatingMatrix) return
        updatingMatrix = true
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
        updatingMatrix = false
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

    companion object {
        private const val TAG = "GLWallpaperRenderer"
        const val BYTES_PER_FLOAT = 4
        const val BYTES_PER_INT = 4
    }
}

/**
 * 不会关闭对应的数组对象
 */
fun bindData(dataIndex: Int, targetIndex: Int) {
    //激活
    GLES20.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dataIndex)
    GLES20.glEnableVertexAttribArray(targetIndex)
    GLES20.glVertexAttribPointer(
        targetIndex,
        2,//组成一个顶点的数据个数
        GLES20.GL_FLOAT,//数据类型
        false,//是否需要gpu 归一化
        2 * GLWallpaperRenderer.BYTES_PER_FLOAT,//组成一个顶点所占用的数据长度
        0
    )
}