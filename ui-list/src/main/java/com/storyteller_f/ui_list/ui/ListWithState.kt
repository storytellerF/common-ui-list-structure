package com.storyteller_f.ui_list.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.databinding.ListWithStateBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ListWithState @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet) {
    private fun flash(loadState: CombinedLoadStates, itemCount: Int) {
        // Only show the list if refresh succeeds.
        binding.list.isVisible =
            loadState.mediator?.refresh is LoadState.NotLoading && itemCount > 0
        binding.emptyList.isVisible =
            loadState.mediator?.refresh is LoadState.NotLoading && itemCount == 0
        // Show loading spinner during initial load or refresh.
        binding.progressBar.isVisible = loadState.mediator?.refresh is LoadState.Loading
        // Show the retry state if initial load or refresh fails.
        binding.retryButton.isVisible = loadState.mediator?.refresh is LoadState.Error

        if (loadState.mediator?.refresh !is LoadState.Loading) binding.refreshLayout.isRefreshing = false

        // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
        val errorState = loadState.source.append as? LoadState.Error
            ?: loadState.source.prepend as? LoadState.Error
            ?: loadState.append as? LoadState.Error
            ?: loadState.prepend as? LoadState.Error
        errorState?.let {
            Toast.makeText(
                context,
                "\uD83D\uDE28 Wooops ${it.error}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun up(adapter: SimpleSourceAdapter<*, *>, lifecycleCoroutineScope: LifecycleCoroutineScope) {
        setAdapter(
            adapter.withLoadStateHeaderAndFooter(header = SimpleLoadStateAdapter { adapter.retry() },
                footer = SimpleLoadStateAdapter { adapter.retry() }), adapter
        )
        callbackFlow {
            val listener: (CombinedLoadStates) -> Unit = {
                trySend(it)
            }
            adapter.addLoadStateListener(listener)
            awaitClose { adapter.removeLoadStateListener(listener) }
        }.onEach {
            flash(it, adapter.itemCount)
        }.launchIn(lifecycleCoroutineScope)
    }


    fun setAdapter(concatAdapter: ConcatAdapter) {
        binding.list.layoutManager =
            LinearLayoutManager(binding.list.context, LinearLayoutManager.VERTICAL, false)
        binding.list.adapter = concatAdapter
    }

    fun setAdapter(adapter: SimpleSourceAdapter<*, *>) {
        binding.list.layoutManager =
            LinearLayoutManager(binding.list.context, LinearLayoutManager.VERTICAL, false)
        binding.list.adapter = adapter
    }

    fun setAdapter(
        concatAdapter: ConcatAdapter,
        adapter: SimpleSourceAdapter<*, *>
    ) {
        setAdapter(concatAdapter)
        setupRefresh(adapter)
    }

    private fun setupRefresh(adapter: SimpleSourceAdapter<*, *>) {
        binding.refreshLayout.setOnRefreshListener {
            adapter.refresh()
        }
        binding.retryButton.setOnClickListener {
            adapter.refresh()
        }
    }

    private val binding = ListWithStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.list.setHasFixedSize(true)
    }
}