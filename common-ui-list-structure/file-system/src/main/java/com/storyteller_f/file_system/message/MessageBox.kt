package com.storyteller_f.file_system.message

class MessageBox(private val name: String) {
    fun setSuccess(success: Boolean): Boolean {
        isSuccess = success
        return success
    }

    var isSuccess = true

}