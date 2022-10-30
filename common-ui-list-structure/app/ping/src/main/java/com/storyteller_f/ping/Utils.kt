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
package com.storyteller_f.ping

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES20
import kotlin.math.roundToInt

/**
 * createVideoThumbnailFromUri
 * @param context Activity context or application context.
 * @param uri Video uri.
 * @return Bitmap thumbnail
 *
 * Hacked from ThumbnailUtils.createVideoThumbnail()'s code.
 */
fun createVideoThumbnailFromUri(
    context: Context,
    uri: Uri
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    val bitmap = try {
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(-1)
    } catch (e: RuntimeException) {
        e.printStackTrace()
        null
    } finally {
        try {
            retriever.release()
        } catch (e: RuntimeException) {
            // Ignore failures while cleaning up.
            e.printStackTrace()
        }
    } ?: return null
    // Scale down the bitmap if it's too large.
    val width = bitmap.width
    val height = bitmap.height
    val max = width.coerceAtLeast(height)
    return if (max > 512) {
        val scale = 512f / max
        val w = (scale * width).roundToInt()
        val h = (scale * height).roundToInt()
        Bitmap.createScaledBitmap(bitmap, w, h, true)
    } else
        bitmap
}

@Throws(RuntimeException::class)
fun compileShaderResourceGLES20(
    context: Context,
    shaderType: Int,
    shaderRes: Int
): Int {
    val shaderSource = context.resources.openRawResource(shaderRes).bufferedReader().use {
        it.readText()
    }
    val shader = GLES20.glCreateShader(shaderType)
    if (shader == 0) {
        throw RuntimeException("Failed to create shader")
    }
    GLES20.glShaderSource(shader, shaderSource)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
        val log = GLES20.glGetShaderInfoLog(shader)
        GLES20.glDeleteShader(shader)
        throw RuntimeException(log)
    }
    return shader
}

@Throws(RuntimeException::class)
fun linkProgramGLES20(
    vertShader: Int,
    fragShader: Int
): Int {
    val program = GLES20.glCreateProgram()
    if (program == 0) {
        throw RuntimeException("Failed to create program")
    }
    GLES20.glAttachShader(program, vertShader)
    GLES20.glAttachShader(program, fragShader)
    GLES20.glLinkProgram(program)
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (status[0] == 0) {
        val log = GLES20.glGetProgramInfoLog(program)
        GLES20.glDeleteProgram(program)
        throw RuntimeException(log)
    }
    return program
}
