package com.storyteller_f.view_holder_compose

import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.SimpleSourceAdapter

class ComposeSourceAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>> :
    SimpleSourceAdapter<IH, VH>() {
    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        if (holder is ComposeViewHolder<*>) {
            holder.edComposeView.disposeComposition()
        }
    }
}