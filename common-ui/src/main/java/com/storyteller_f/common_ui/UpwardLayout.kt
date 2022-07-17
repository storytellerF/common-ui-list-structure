package com.storyteller_f.common_ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class UpwardLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxOf = maxOf(minimumHeight, measuredHeight)
        val maxOf1 = maxOf(minimumWidth, measuredWidth)
        if (maxOf != minimumHeight || maxOf1 != minimumWidth) {
            minimumWidth = maxOf1
            minimumHeight = maxOf
            invalidate()
        }


    }
}