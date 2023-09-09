package com.storyteller_f.view_holder_compose

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder

interface EdComposeViewEventEmitter {
    val isSelected: Boolean

    fun notifyClickEvent(viewName: String)

    fun notifyLongClickEvent(viewName: String)

    companion object {
        val default = object : EdComposeViewEventEmitter {
            override val isSelected: Boolean
                get() = false

            override fun notifyClickEvent(viewName: String) {
                TODO("Not yet implemented")
            }

            override fun notifyLongClickEvent(viewName: String) {
                TODO("Not yet implemented")
            }
        }
    }
}

class EDComposeView(
    context: Context,
) : EdComposeViewEventEmitter {
    override val isSelected: Boolean
        get() = composeView.isSelected
    val composeView: ComposeView = ComposeView(context = context)
    var clickListener: ((String) -> Unit)? = null
    var longClickListener: ((String) -> Unit)? = null

    override fun notifyClickEvent(viewName: String) {
        clickListener?.invoke(viewName)
    }

    override fun notifyLongClickEvent(viewName: String) {
        longClickListener?.invoke(viewName)
    }

    fun disposeComposition() {
        composeView.disposeComposition()
    }
}

abstract class ComposeViewHolder<IH : DataItemHolder>(val edComposeView: EDComposeView) :
    AbstractViewHolder<IH>(edComposeView.composeView) {
    init {
        edComposeView.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }
}
