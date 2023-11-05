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
import android.opengl.GLES30
import com.storyteller_f.ping.R
import com.storyteller_f.ping.compileShaderResourceGLES20
import com.storyteller_f.ping.linkProgramGLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class GLES30WallpaperRenderer(context: Context) : GLWallpaperRenderer(context) {
    private val positionLocation = 0
    private val texCoordinationLocation = 1
    private val vertexArrays = IntArray(1)

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        surfacePreProcess()
        // Locations are set in shader sources.
        GLES30.glGenVertexArrays(vertexArrays.size, vertexArrays, 0)
        GLES30.glBindVertexArray(vertexArrays[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0])
        GLES30.glEnableVertexAttribArray(positionLocation)
        GLES30.glVertexAttribPointer(
            positionLocation, 2, GLES30.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1])
        GLES30.glEnableVertexAttribArray(texCoordinationLocation)
        GLES30.glVertexAttribPointer(
            texCoordinationLocation, 2, GLES30.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[2])
        GLES30.glBindVertexArray(0)
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override val version: Int
        get() = 3

    override fun buildProgram() = linkProgramGLES20(
        compileShaderResourceGLES20(
            context, GLES30.GL_VERTEX_SHADER, R.raw.vertex_30
        ), compileShaderResourceGLES20(
            context, GLES30.GL_FRAGMENT_SHADER, R.raw.fragment_30
        )
    )

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        val local = surfaceTexture ?: return
        if (renderedFrame < updatedFrame) {
            local.updateTexImage()
            ++renderedFrame
        }
        drawFramePrepare()

        GLES30.glBindVertexArray(vertexArrays[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)

        GLES30.glUseProgram(0)
    }

    companion object
}