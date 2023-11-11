package com.storyteller_f.ping.shader

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLES30

const val BYTES_PER_FLOAT = 4
const val BYTES_PER_INT = 4

fun Context.firstFrame(uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(this, uri)
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
    }
}

@Throws(RuntimeException::class)
fun compileShaderResourceGLES20(
    context: Context, shaderType: Int, shaderRes: Int
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
    vertShader: Int, fragShader: Int
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
        2 * BYTES_PER_FLOAT,//组成一个顶点所占用的数据长度
        0
    )
}