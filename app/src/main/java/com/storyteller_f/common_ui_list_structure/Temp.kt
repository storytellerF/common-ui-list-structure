package com.storyteller_f.common_ui_list_structure

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.storyteller_f.common_ui_list_structure.databinding.RepoViewItemBinding
import com.storyteller_f.ui_list.core.list
import com.storyteller_f.ui_list.core.registerCenter
import com.storyteller_f.ui_list.event.doWhenIs
import com.storyteller_f.ui_list.event.findActionReceiverOrNull

fun buildRepo2ViewHolder(view: ViewGroup): Repo2ViewHolder {
    val context = view.context
    val inflate = RepoViewItemBinding.inflate(LayoutInflater.from(context), view, false)

    val repo2ViewHolder = Repo2ViewHolder(inflate)
    inflate.root.setOnClickListener {
        doWhenIs<MainActivity>(it.context) { o ->
            o.clickRepo(it, repo2ViewHolder.itemHolder)
        }
        findActionReceiverOrNull(it, MainActivity::class.java)?.clickRepo(
            it,
            repo2ViewHolder.itemHolder
        )
    }
    return repo2ViewHolder
}

fun buildSeparatorViewHolder(view: ViewGroup): SeparatorViewHolder {
    val context = view.context
    val repo2ViewHolder = SeparatorViewHolder(ComposeView(context))
    return repo2ViewHolder
}

fun add() {
    registerCenter[RepoItemHolder::class.java] = 0
    list.add(::buildRepo2ViewHolder)
    registerCenter[SeparatorItemHolder::class.java] = 1
    list.add(::buildSeparatorViewHolder)
}