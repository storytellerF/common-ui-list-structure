package com.storyteller_f.common_ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.*
import androidx.databinding.BindingAdapter

/**
 * 增加额外操作，暂停开发
 */
class InsetLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //保存view 原始padding margin
        getInsetBlock()
    }
}

object InsetBlockDirection {
    const val top = 4
    const val start = 8
    const val end = 16
    const val bottom = 32
}

class InsetBlock(val padding: Direction, val margin: Direction)

@BindingAdapter(
    "status_padding",
    "status_margin",
    "navigator_padding",
    "navigator_margin",
    requireAll = false
)
fun View.inset(
    statusPadding: Int = 0,
    statusMargin: Int = 0,
    navigatorPadding: Int = 0,
    navigatorMargin: Int = 0
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val block = getInsetBlock()
        val v1 = insets.navigator.bottom
        val v2 = insets.status.top
        v.updatePadding(
            block.padding + statusPadding.insetBlock(v2) + navigatorPadding.insetBlock(v1)
        )
        v.updateMargin(
            block.margin + statusMargin.insetBlock(v2) + navigatorMargin.insetBlock(v1)
        )
        insets
    }
}

fun Int.insetBlock(v: Int) =
    Direction(
        if (this and InsetBlockDirection.start == InsetBlockDirection.start)
            v
        else 0, if (this and InsetBlockDirection.top == InsetBlockDirection.top)
            v
        else 0, if (this and InsetBlockDirection.end == InsetBlockDirection.end)
            v
        else 0, if (this and InsetBlockDirection.bottom == InsetBlockDirection.bottom)
            v
        else 0
    )

fun View.updatePadding(block: Direction) {
    val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
    updatePadding(
        if (isRtl) block.end else block.start,
        block.top,
        if (isRtl) block.start else block.end,
        block.bottom
    )
}

fun <T : Any> getOrCreate(retrieve: () -> T?, produce: () -> T): T {
    return retrieve() ?: produce()
}

fun View.getInsetBlock() =
    getOrCreate(
        { getTag(R.id.inset_block) as? InsetBlock },
        {
            InsetBlock(
                Direction(paddingStart, paddingTop, paddingEnd, paddingBottom),
                Direction(marginStart, marginTop, marginEnd, marginBottom)
            ).also {
                setTag(
                    R.id.inset_block,
                    it
                )
            }
        })

class Direction(val start: Int, val top: Int, val end: Int, val bottom: Int) {
    operator fun plus(r: Direction): Direction {
        return Direction(start + r.start, top + r.top, end + r.end, bottom + r.bottom)
    }

    override fun toString(): String {
        return "Direction(start=$start, top=$top, end=$end, bottom=$bottom)"
    }

}
