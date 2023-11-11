package com.storyteller_f.ping.shader

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

open class GLBuffer {

    private val vertices: FloatBuffer = run {
        // Those replaced glGenBuffers() and glBufferData().
        val vertexArray = floatArrayOf(
            -1.0f, -1.0f, // bottom left
            -1.0f, 1.0f,  // top left
            1.0f, -1.0f,  // bottom right
            1.0f, 1.0f    // top right
        )
        ByteBuffer.allocateDirect(
            vertexArray.size * BYTES_PER_FLOAT
        ).run {
            order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(vertexArray).position(0)
            }
        }
    }
    private val texCoordinationBuffer: FloatBuffer = run {
        val texCoordinationArray = floatArrayOf(
            0.0f, 1.0f,  // bottom left
            0.0f, 0.0f,  // top left
            1.0f, 1.0f,  // bottom right
            1.0f, 0.0f   // top right
        )
        ByteBuffer.allocateDirect(
            texCoordinationArray.size * BYTES_PER_FLOAT
        ).run {
            order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(texCoordinationArray).position(0)
            }
        }
    }

    /**
     * 用于EBO
     */
    private val indicesBuffer: IntBuffer = run {
        val indexArray = intArrayOf(0, 1, 2, 3, 2, 1)
        ByteBuffer.allocateDirect(
            indexArray.size * BYTES_PER_INT
        ).run {
            order(ByteOrder.nativeOrder()).asIntBuffer().apply {
                put(indexArray).position(0)
            }
        }
    }

    private val buffers: IntArray by lazy {
        IntArray(3).apply {
            //获取指定的缓冲区
            GLES20.glGenBuffers(size, this, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                vertices.capacity() * BYTES_PER_FLOAT,
                vertices,
                GLES20.GL_STATIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this[1])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                texCoordinationBuffer.capacity() * BYTES_PER_FLOAT,
                texCoordinationBuffer,
                GLES20.GL_STATIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            //绑定EBO
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, this[2])
            GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indicesBuffer.capacity() * BYTES_PER_INT,
                indicesBuffer,
                GLES20.GL_STATIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
    }

    operator fun get(index: Int): Int {
        return buffers[index]
    }
}