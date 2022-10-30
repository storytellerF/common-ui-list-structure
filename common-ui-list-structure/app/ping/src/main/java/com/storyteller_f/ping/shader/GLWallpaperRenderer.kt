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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.ExoPlayer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import kotlin.math.abs

interface GLWallpaperRender {
    fun setSourcePlayer(player: MediaPlayer)
    fun setSourcePlayer(exoPlayer: ExoPlayer)
    fun setScreenSize(width: Int, height: Int)
    fun setVideoSizeAndRotation(width: Int, height: Int, rotation: Int)
    fun setOffset(xOffset: Float, yOffset: Float)
}

abstract class GLWallpaperRenderer(protected val context: Context) : GLSurfaceView.Renderer, GLWallpaperRender {
    private var screenWidth = 0
    private var screenHeight = 0
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoRotation = 0
    private var xOffset = 0f
    private var yOffset = 0f
    private var maxXOffset = 0f
    private var maxYOffset = 0f
    protected val vertices: FloatBuffer
    protected val texCoordinationBuffer: FloatBuffer
    protected val indices: IntBuffer
    protected val textures: IntArray
    protected val buffers: IntArray
    protected val mvp: FloatArray
    protected var program = 0
    protected var mvpLocation = 0
    protected var surfaceTexture: SurfaceTexture? = null

    // Fix bug like https://stackoverflow.com/questions/14185661/surfacetexture-onframeavailablelistener-stops-being-called
    protected var updatedFrame: Long = 0
    protected var renderedFrame: Long = 0

    init {
        // Those replaced glGenBuffers() and glBufferData().
        val vertexArray = floatArrayOf( // x, y
            // bottom left
            -1.0f, -1.0f,  // top left
            -1.0f, 1.0f,  // bottom right
            1.0f, -1.0f,  // top right
            1.0f, 1.0f
        )
        vertices = ByteBuffer.allocateDirect(
            vertexArray.size * BYTES_PER_FLOAT
        ).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertices.put(vertexArray).position(0)
        val texCoordinationArray = floatArrayOf( // u, v
            // bottom left
            0.0f, 1.0f,  // top left
            0.0f, 0.0f,  // bottom right
            1.0f, 1.0f,  // top right
            1.0f, 0.0f
        )
        texCoordinationBuffer = ByteBuffer.allocateDirect(
            texCoordinationArray.size * BYTES_PER_FLOAT
        ).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordinationBuffer.put(texCoordinationArray).position(0)
        val indexArray = intArrayOf(0, 1, 2, 3, 2, 1)
        indices = ByteBuffer.allocateDirect(
            indexArray.size * BYTES_PER_INT
        ).order(ByteOrder.nativeOrder()).asIntBuffer()
        indices.put(indexArray).position(0)
        buffers = IntArray(3)
        textures = IntArray(1)
        mvp = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f
        )
    }

    override fun setScreenSize(width: Int, height: Int) {
        Log.d(TAG, "setScreenSize() called with: width = $width, height = $height")
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
            Log.i(TAG, "setScreenSize: success")
            updateOffset()
            updateMatrix()
        }
    }

    override fun setVideoSizeAndRotation(width: Int, height: Int, rotation: Int) {
        Log.d(TAG, "setVideoSizeAndRotation() called with: width = $width, height = $height, rotation = $rotation")
        // MediaMetadataRetriever always give us raw width and height and won't rotate them.
        // So we rotate them by ourselves.
        val (widthTemp, heightTemp) = if (rotation % 180 != 0) {
            height to width
        } else width to height
        if (videoWidth != widthTemp || videoHeight != heightTemp || videoRotation != rotation) {
            videoWidth = widthTemp
            videoHeight = heightTemp
            videoRotation = rotation
            updateOffset()
            updateMatrix()
        }
    }

    private fun updateOffset() {
        val screenWidthExpected = videoWidth.toFloat() * screenHeight / videoHeight
        val fl = 1.0f - screenWidth.toFloat() / screenWidthExpected
        maxXOffset = abs(fl) / 2
        val screenHeightExpected = videoHeight.toFloat() * screenWidth / videoWidth
        val fl1 = 1.0f - screenHeight.toFloat() / screenHeightExpected
        maxYOffset = abs(fl1) / 2
        Log.i(TAG, "setVideoSizeAndRotation: $fl $fl1")
    }

    override fun setOffset(xOffset: Float, yOffset: Float) {
        if (maxXOffset.equals(Float.NaN) || maxYOffset.equals(Float.NaN)) return
        val xOffsetTemp = xOffset.coerceIn(-maxXOffset..maxXOffset)
        val yOffsetTemp = yOffset.coerceIn(-maxYOffset..maxYOffset)
        if (this.xOffset != xOffsetTemp || this.yOffset != yOffsetTemp) {
            this.xOffset = xOffsetTemp
            this.yOffset = yOffsetTemp
            updateMatrix()
        }
    }

    override fun setSourcePlayer(exoPlayer: ExoPlayer) {
        // Re-create SurfaceTexture when getting a new player.
        // Because maybe a new video is loaded.
        createSurfaceTexture()
        exoPlayer.setVideoSurface(Surface(surfaceTexture))
    }

    override fun setSourcePlayer(player: MediaPlayer) {
        createSurfaceTexture()
        player.setSurface(Surface(surfaceTexture))
    }

    private fun createSurfaceTexture() {
        surfaceTexture?.release()
        updatedFrame = 0
        renderedFrame = 0
        surfaceTexture = SurfaceTexture(textures[0]).apply {
            setDefaultBufferSize(videoWidth, videoHeight)
            setOnFrameAvailableListener { ++updatedFrame }
        }
    }

    private fun updateMatrix() {
        // Players are buggy and unclear, so we do crop by ourselves.
        // Start with an identify matrix.
        for (i in 0..15) {
            mvp[i] = 0.0f
        }
        mvp[15] = 1.0f
        mvp[10] = mvp[15]
        mvp[5] = mvp[10]
        mvp[0] = mvp[5]
        // OpenGL model matrix: scaling, rotating, translating.
        val videoRatio = videoWidth.toFloat() / videoHeight
        val screenRatio = screenWidth.toFloat() / screenHeight
        if (videoRatio >= screenRatio) {
            Log.d(TAG, "updateMatrix: x crop")
            // Treat video and screen width as 1, and compare width to scale.
            val newVideoWidth = screenWidth.toFloat() * videoHeight / screenHeight
            val widthRatio = videoWidth / newVideoWidth
            Matrix.scaleM(mvp, 0, widthRatio, 1f, 1f)
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvp, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvp, 0, xOffset, 0f, 0f)
        } else {
            Log.d(TAG, "updateMatrix: y crop")
            // Treat video and screen height as 1, and compare height to scale.
            val newVideoHeight = screenHeight.toFloat() * videoWidth / screenWidth
            val heightRatio = videoHeight / newVideoHeight
            Matrix.scaleM(mvp, 0, 1f, heightRatio, 1f)
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvp, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvp, 0, 0f, yOffset, 0f)
        }
        // This is a 2D center crop, so we only need model matrix, no view and projection.
    }

    abstract fun buildProgram(): Int

    protected fun surfacePreProcess() {
        // No depth test for 2D video.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        program = buildProgram()
        mvpLocation = GLES20.glGetUniformLocation(program, "mvp")
        // Locations are NOT set in shader sources.

        GLES20.glGenBuffers(buffers.size, buffers, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER, vertices.capacity() * BYTES_PER_FLOAT,
            vertices, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER, texCoordinationBuffer.capacity() * BYTES_PER_FLOAT,
            texCoordinationBuffer, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2])
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * BYTES_PER_INT,
            indices, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    companion object {
        private const val TAG = "GLWallpaperRenderer"
        const val BYTES_PER_FLOAT = 4
        const val BYTES_PER_INT = 4
    }
}