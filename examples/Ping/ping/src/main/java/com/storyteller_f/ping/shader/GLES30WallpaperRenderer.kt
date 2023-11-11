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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class GLES30WallpaperRenderer(context: Context) :
    GLWallpaperRenderer(context, R.raw.vertex_30, R.raw.fragment_30, 3) {
    private val positionLocation = 0
    private val texCoordinationLocation = 1
    private val vertexArrays = IntArray(1)

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        initGl()

        //VAO 顶点数组对象
        GLES30.glGenVertexArrays(vertexArrays.size, vertexArrays, 0)
        GLES30.glBindVertexArray(vertexArrays[0])
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[2])

        bindData(buffers[0], positionLocation)
        bindData(buffers[1], texCoordinationLocation)

        GLES30.glBindVertexArray(0)
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun drawImage() {
        GLES30.glBindVertexArray(vertexArrays[0])
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            6,//组成一个三角形的数据个数
            GLES30.GL_UNSIGNED_INT,
            0
        )
        GLES30.glBindVertexArray(0)

        GLES30.glUseProgram(0)
    }

    companion object
}
