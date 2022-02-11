package com.storyteller_f.view_holder_compose

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.storyteller_f.ui_list.core.AbstractAdapterViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder

abstract class ComposeViewHolder<IH : DataItemHolder>(val composeView: ComposeView) :
    AbstractAdapterViewHolder<IH>(composeView) {
    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }
}