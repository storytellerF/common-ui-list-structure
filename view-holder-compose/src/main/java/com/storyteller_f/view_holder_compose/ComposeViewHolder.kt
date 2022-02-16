package com.storyteller_f.view_holder_compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.storyteller_f.ui_list.core.AbstractAdapterViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder

class EDComposeView(
    context: Context,
) {
    val composeView: ComposeView = ComposeView(context = context)
    var clickListener: ((String) -> Unit)? = null
    var longClickListener: ((String) -> Unit)? = null
    fun notifyClickEvent(viewName: String) {
        clickListener?.invoke(viewName)
    }

    fun notifyLongClickEvent(viewName: String) {
        longClickListener?.invoke(viewName)
    }

    fun disposeComposition() {
        composeView.disposeComposition()
    }
}

abstract class ComposeViewHolder<IH : DataItemHolder>(val edComposeView: EDComposeView) :
    AbstractAdapterViewHolder<IH>(edComposeView.composeView) {
    init {
        edComposeView.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }
}