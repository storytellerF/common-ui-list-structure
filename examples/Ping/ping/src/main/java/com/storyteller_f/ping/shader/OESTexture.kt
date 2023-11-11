package com.storyteller_f.ping.shader

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20

class OESTexture {
    /**
     * 设置外部纹理。
     */
    private val textures: IntArray by lazy {
        IntArray(1).apply {
            //生成之后会存储到数组中
            GLES20.glGenTextures(size, this, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, this[0])
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
            )
        }
    }

    fun build(): SurfaceTexture {
        return SurfaceTexture(textures[0])
    }
}