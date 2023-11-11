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
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.annotation.RawRes
import javax.microedition.khronos.opengles.GL10

abstract class GLWallpaperRenderer(
    protected val context: Context,
    @RawRes val vertexRes: Int,
    @RawRes val fragmentRes: Int,
    val version: Int
) : GLSurfaceView.Renderer {

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

    val binding by lazy {
        SurfaceTextureBinding(mvpMatrix, textures)
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

    fun initGl() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        mvpLocation
        binding
    }
    //生命周期函数
    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) =
        GLES20.glViewport(0, 0, width, height)

    override fun onDrawFrame(gl: GL10?) {
        if (!binding.isReady) return
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvpMatrix, 0)

        drawImage()
    }

    abstract fun drawImage()
    //生命周期函数
}
