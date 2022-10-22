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
import com.storyteller_f.ping.R
import com.storyteller_f.ping.Utils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class GLES20WallpaperRenderer(context: Context) : GLWallpaperRenderer(context) {
    private var positionLocation = 0
    private var texCoordinationLocation = 0

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        surfaceCreatedPrepare()
        positionLocation = GLES20.glGetAttribLocation(program, "in_position")
        texCoordinationLocation = GLES20.glGetAttribLocation(program, "in_tex_coord")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }
    override fun buildProgram() = Utils.linkProgramGLES20(
        Utils.compileShaderResourceGLES20(
            context, GLES20.GL_VERTEX_SHADER, R.raw.vertex_20
        ),
        Utils.compileShaderResourceGLES20(
            context, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_20
        )
    )

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        val local = surfaceTexture ?: return
        if (renderedFrame < updatedFrame) {
            local.updateTexImage()
            ++renderedFrame
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0)
        // No vertex array in OpenGL ES 2.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
        GLES20.glEnableVertexAttribArray(texCoordinationLocation)
        GLES20.glVertexAttribPointer(
            texCoordinationLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2])
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(texCoordinationLocation)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glUseProgram(0)
    }

    companion object
}