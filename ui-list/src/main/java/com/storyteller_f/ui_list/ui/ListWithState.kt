package com.storyteller_f.ui_list.ui

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.map
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.core.SimpleDataAdapter
import com.storyteller_f.ui_list.core.SimpleDataViewModel
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.databinding.ListWithStateBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ListWithState @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet) {
    private fun flash(uiState: UIState) {
        // Only show the list if refresh succeeds.
        binding.list.isVisible = uiState.data
        binding.emptyList.isVisible = uiState.empty
        // Show loading spinner during initial load or refresh.
        binding.progressBar.isVisible = uiState.progress
        // Show the retry state if initial load or refresh fails.
        binding.retryButton.isVisible = uiState.retry
        uiState.refresh?.let {
            binding.refreshLayout.isRefreshing = it
        }
        binding.errorMsg.isVisible = uiState.error != null
        uiState.error?.let {
            binding.errorMsg.text = it
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
        }.map {
            simple(it, adapter.itemCount)
        }.onEach {
            flash(it)
        }.launchIn(lifecycleCoroutineScope)
    }


    private fun setAdapter(concatAdapter: ConcatAdapter) {
        setupLinearLayoutManager()
        binding.list.adapter = concatAdapter
    }

    fun setAdapter(adapter: SimpleSourceAdapter<*, *>) {
        setupLinearLayoutManager()
        binding.list.adapter = adapter
    }

    private fun setAdapter(
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

    private fun setupSwapSupport(adapter: SimpleDataAdapter<*, *>) {
        setupLinearLayoutManager()
        binding.list.adapter = adapter
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.absoluteAdapterPosition
                val to = target.absoluteAdapterPosition
                adapter.swap(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                TODO("Not yet implemented")
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                binding.refreshLayout.isEnabled = viewHolder == null
            }

        }).attachToRecyclerView(binding.list)
    }

    private fun setupLinearLayoutManager() {
        binding.list.layoutManager =
            LinearLayoutManager(binding.list.context, LinearLayoutManager.VERTICAL, false)
    }

    fun up(
        adapter: SimpleDataAdapter<*, *>,
        lifecycleOwner: LifecycleOwner,
        vm: SimpleDataViewModel<*, *, *>
    ) {
        setupLinearLayoutManager()
        val layoutManager = binding.list.layoutManager as LinearLayoutManager
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val visibleItemCount = layoutManager.childCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (visibleItemCount + lastVisibleItem + visibleItemCount >= totalItemCount) {
                    vm.requestMore()
                }
            }
        })
        binding.list.adapter = adapter
        binding.refreshLayout.setOnRefreshListener {
            vm.refresh()
        }
        binding.retryButton.setOnClickListener {
            vm.retry()
        }
        vm.loadState.map { simple(it, adapter.itemCount) }.observe(lifecycleOwner) {
            flash(it)
        }
        setupSwapSupport(adapter)
    }

    private val binding = ListWithStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.list.setHasFixedSize(true)
    }

    class UIState(
        val retry: Boolean,
        val data: Boolean,
        val empty: Boolean,
        val progress: Boolean,
        val error: Spannable?,
        val refresh: Boolean?
    )

    companion object {
        /**
         * 远程加载出错时，不会显示数据
         */
        fun simple(loadState: CombinedLoadStates, itemCount: Int): UIState {
            val refresh = if (loadState.mediator?.refresh !is LoadState.Loading) false else null
            val error = loadState.source.append as? LoadState.Error
                ?: loadState.source.prepend as? LoadState.Error
                ?: loadState.append as? LoadState.Error
                ?: loadState.prepend as? LoadState.Error
                ?: loadState.mediator?.append as? LoadState.Error
                ?: loadState.mediator?.prepend as? LoadState.Error
                ?: loadState.mediator?.refresh as? LoadState.Error
            val errorSpannable = error?.error?.localizedMessage?.let {
                SpannableString(it)
            }
            return UIState(
                loadState.mediator?.refresh is LoadState.Error,
                loadState.mediator?.refresh is LoadState.NotLoading && itemCount > 0,
                loadState.mediator?.refresh is LoadState.NotLoading && itemCount == 0,
                loadState.mediator?.refresh is LoadState.Loading,
                errorSpannable, refresh
            )
        }

        fun simple(loadState: LoadState, itemCount: Int): UIState {
            val refresh = if (loadState !is LoadState.Loading) false else null
            return UIState(
                loadState is LoadState.Error,
                loadState is LoadState.NotLoading && itemCount != 0,
                loadState is LoadState.NotLoading && itemCount == 0,
                loadState is LoadState.Loading,
                null,
                refresh
            )
        }

    }
}