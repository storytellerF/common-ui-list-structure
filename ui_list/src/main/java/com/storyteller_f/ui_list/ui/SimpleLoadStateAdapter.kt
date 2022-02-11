package com.storyteller_f.ui_list.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.R
import com.storyteller_f.ui_list.databinding.SimpleLoadStateFooterViewItemBinding

class SimpleLoadStateAdapter(private val retry: () -> Unit) :
    LoadStateAdapter<SimpleLoadStateViewHolder>() {
    override fun onBindViewHolder(holder: SimpleLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): SimpleLoadStateViewHolder {
        return SimpleLoadStateViewHolder.create(parent, retry)
    }
}

class SimpleLoadStateViewHolder(
    private val binding: SimpleLoadStateFooterViewItemBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
        if (loadState is LoadState.Error) {
            binding.errorMsg.text = loadState.error.localizedMessage
        }
        binding.progressBar.isVisible = loadState is LoadState.Loading
        binding.retryButton.isVisible = loadState is LoadState.Error
        binding.errorMsg.isVisible = loadState is LoadState.Error
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): SimpleLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.simple_load_state_footer_view_item, parent, false)
            val binding = SimpleLoadStateFooterViewItemBinding.bind(view)
            return SimpleLoadStateViewHolder(binding, retry)
        }
    }
}