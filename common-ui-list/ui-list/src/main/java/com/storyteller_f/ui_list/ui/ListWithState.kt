package com.storyteller_f.ui_list.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.SpannableString
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.adapter.SimpleDataAdapter
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.databinding.ListWithStateBinding
import com.storyteller_f.ui_list.source.SimpleDataViewModel
import com.storyteller_f.ui_list.source.isError
import com.storyteller_f.ui_list.source.isLoading
import com.storyteller_f.ui_list.source.isNotLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class ListWithState @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {

    val recyclerView get() = binding.list

    private val binding = ListWithStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.list.setHasFixedSize(true)
    }

    fun flash(uiState: UIState) {
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
        binding.errorPage.isVisible = uiState.showErrorPage
        binding.errorMsg.isVisible = uiState.error != null
        uiState.error?.let {
            binding.errorMsg.text = it
        }
    }

    fun <IH : DataItemHolder, VH : AbstractViewHolder<IH>> sourceUp(
        adapter: SimpleSourceAdapter<IH, VH>,
        lifecycleOwner: LifecycleOwner,
        plugLayoutManager: Boolean = true,
        refresh: () -> Unit = { },
        dampingSwipe: ((AbstractViewHolder<out DataItemHolder>, Int) -> Unit)? = null,
        flash: (CombinedLoadStates, Int) -> UIState = Companion::simple,
    ) {
        setAdapter(
            adapter.withLoadStateHeaderAndFooter(header = SimpleLoadStateAdapter { adapter.retry() },
                footer = SimpleLoadStateAdapter { adapter.retry() }), adapter, refresh, plugLayoutManager
        )
        val callbackFlow = callbackFlow {
            val listener: (CombinedLoadStates) -> Unit = {
                trySend(it)
            }
            adapter.addLoadStateListener(listener)
            awaitClose { adapter.removeLoadStateListener(listener) }
        }
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                autoScrollToTop(callbackFlow, lifecycleOwner)
                launch {
                    callbackFlow.map {
                        Log.d(TAG, "sourceUp: ${it.debugEmoji()}")
                        flash(it, adapter.itemCount)
                    }.stateIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed(), UIState.empty).collectLatest {
                        flash(it)
                    }
                }
            }
        }

        if (dampingSwipe != null) {
            setupDampingSwipeSupport(dampingSwipe)
        }
    }

    fun dataUp(
        adapter: SimpleDataAdapter<*, *>,
        lifecycleOwner: LifecycleOwner,
        vm: SimpleDataViewModel<*, *, *>,
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
        vm.loadState.map { simple(it.loadState, it.itemCount) }.observe(lifecycleOwner) {
            flash(it)
        }
        setupSwapSupport(adapter)
    }

    fun manualUp(adapter: ManualAdapter<*, *>, dampingSwipe: ((RecyclerView.ViewHolder, Int) -> Unit)? = null, refresh: (() -> Unit)? = null) {
        recyclerView.adapter = adapter
        setupLinearLayoutManager()
        if (dampingSwipe != null) setupDampingSwipeSupport(dampingSwipe)
        binding.refreshLayout.isEnabled = refresh != null
        binding.refreshLayout.setOnRefreshListener {
            refresh?.invoke()
        }
        binding.retryButton.setOnClickListener {
            refresh?.invoke()
        }
    }

    private fun CoroutineScope.autoScrollToTop(
        callbackFlow: Flow<CombinedLoadStates>,
        lifecycleOwner: LifecycleOwner
    ) {
        launch {
            callbackFlow.distinctUntilChanged { old, new ->
                old.mediator?.refresh.isLoading == new.mediator?.refresh.isLoading
            }.shareIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed())
                .collectLatest {
                    val notLoading = it.mediator?.refresh.isNotLoading
                    Log.d(TAG, "sourceUp: scroll to position if $notLoading")
                    if (notLoading) {
                        binding.list.smoothScrollToPosition(0)
                    }
                }
        }
    }

    /**
     * 仅data adapter 可用
     */
    private fun setupSwapSupport(adapter: SimpleDataAdapter<*, *>) {
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                binding.refreshLayout.isEnabled = viewHolder == null
            }

        }).attachToRecyclerView(binding.list)
    }

    private fun setupDampingSwipeSupport(block: (AbstractViewHolder<out DataItemHolder>, Int) -> Unit) {

        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            private var swipeEvent = false

            override fun getSwipeEscapeVelocity(defaultValue: Float) = 1000000F

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 10F


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                binding.refreshLayout.isEnabled = viewHolder == null
            }


            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val firstLine = 200
                    val secondLine = firstLine + 100
                    val isRight = dX > 0
                    val dx = abs(dX)
                    when {
                        dx < firstLine -> {
                            if (swipeEvent) {
                                swipeEvent = false
                            }
                            super.onChildDraw(c, recyclerView, viewHolder, dX / 2, dY, actionState, isCurrentlyActive)
                        }

                        dx < secondLine -> {
                            val firstMax = firstLine / 2
                            val x = firstMax + (dx - firstLine) / 4
                            super.onChildDraw(c, recyclerView, viewHolder, if (isRight) x else -x, dY, actionState, isCurrentlyActive)
                        }

                        dx >= secondLine -> {
                            if (!swipeEvent) {
                                block(viewHolder as AbstractViewHolder<out DataItemHolder>, if (isRight) ItemTouchHelper.RIGHT else ItemTouchHelper.LEFT)
                                swipeEvent = true
                            }
                        }
                    }
                }
            }

        }).attachToRecyclerView(binding.list)
    }

    fun setupClickSelectableSupport(editing: MutableLiveData<Boolean>, lifecycleOwner: LifecycleOwner, selectableDrawer: SelectableDrawer) {
        editing.observe(lifecycleOwner) {
            val adapter = binding.list.adapter
            binding.list.adapter = adapter
        }
        val value = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                if (editing.value == true) {
                    val childAdapterPosition = parent.getChildAdapterPosition(view)
                    val childViewHolder = parent.getChildViewHolder(view)
                    val absoluteAdapterPosition = (childViewHolder as AbstractViewHolder<*>).itemHolder
                    outRect.right = selectableDrawer.width(view, parent, state, childAdapterPosition, absoluteAdapterPosition)
                } else outRect.right = 0
            }

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                super.onDraw(c, parent, state)
                if (editing.value == true)
                    for (i in 0 until parent.childCount) {
                        val child = parent.getChildAt(i)
                        val top = child.top
                        val bottom = child.bottom
                        selectableDrawer.draw(c, top, bottom, child.width, child.height, parent.width, parent.height, child, parent, state)
                    }
            }

        }
        binding.list.addItemDecoration(value)
    }

    private fun setupLinearLayoutManager() {
        binding.list.layoutManager =
            LinearLayoutManager(binding.list.context, LinearLayoutManager.VERTICAL, false)
    }

    private fun setAdapter(
        concatAdapter: ConcatAdapter,
        adapter: SimpleSourceAdapter<out DataItemHolder, out AbstractViewHolder<*>>,
        refresh: () -> Unit,
        plugLayoutManager: Boolean = true,
    ) {
        if (plugLayoutManager)
            setupLinearLayoutManager()
        binding.list.adapter = concatAdapter
        setupRefresh(adapter, refresh)
    }

    private fun setupRefresh(adapter: SimpleSourceAdapter<*, *>, refresh: () -> Unit) {
        binding.refreshLayout.setOnRefreshListener {
            refresh()
            adapter.refresh()
        }
        binding.retryButton.setOnClickListener {
            refresh()
            adapter.refresh()
        }
    }

    class UIState(
        val retry: Boolean,
        val data: Boolean,
        val empty: Boolean,
        val progress: Boolean,
        val error: CharSequence?,
        val refresh: Boolean?
    ) {
        val showErrorPage get() = retry || error != null

        companion object {
            val data = UIState(retry = false, data = true, empty = false, progress = false, error = null, refresh = false)
            val empty = UIState(retry = false, data = false, empty = false, progress = false, error = null, refresh = null)
            val loading = UIState(retry = false, data = false, empty = false, progress = true, error = null, refresh = null)

        }
    }

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
                loadState.mediator?.refresh.isError,
                loadState.mediator?.refresh.isNotLoading && itemCount > 0,
                loadState.mediator?.refresh.isNotLoading && itemCount == 0,
                loadState.mediator?.refresh.isLoading,
                errorSpannable, refresh
            )
        }

        /**
         * 远程加载出错时，会显示数据
         */
        fun remote(loadState: CombinedLoadStates, itemCount: Int): UIState {
            val refresh = if (loadState.mediator?.refresh !is LoadState.Loading) false else null
            val error = loadState.source.append as? LoadState.Error
                ?: loadState.source.refresh as? LoadState.Error
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
                loadState.source.refresh.isError,
                loadState.source.refresh.isNotLoading && itemCount > 0,
                loadState.source.refresh.isNotLoading && itemCount == 0,
                loadState.source.refresh.isLoading,
                errorSpannable, refresh
            )
        }

        fun simple(loadState: LoadState, itemCount: Int): UIState {
            val refresh = if (!loadState.isLoading) false else null
            return UIState(
                loadState.isError,
                loadState.isNotLoading && itemCount != 0,
                loadState.isNotLoading && itemCount == 0,
                loadState.isLoading,
                null,
                refresh
            )
        }

        private const val TAG = "ListWithState"

    }

    interface SelectableDrawer {
        fun width(view: View, parent: RecyclerView, state: RecyclerView.State, childAdapterPosition: Int, absoluteAdapterPosition: DataItemHolder): Int
        fun draw(c: Canvas, top: Int, bottom: Int, childWidth: Int, childHeight: Int, parentWidth: Int, parentHeight: Int, child: View, parent: RecyclerView, state: RecyclerView.State)
    }
}

fun LoadState.debugEmoji() = when (this) {
    is LoadState.NotLoading -> if (endOfPaginationReached) "\uD83D\uDD1A" else "⏸️"
    is LoadState.Loading -> "⏳"
    is LoadState.Error -> "❌"
}

fun LoadStates?.debugEmoji() = if (this == null) "\uD83D\uDD72" else "${prepend.debugEmoji()} ${refresh.debugEmoji()} ${append.debugEmoji()}"

fun CombinedLoadStates.debugEmoji() =
    "source: ${source.debugEmoji()}  mediator: ${mediator.debugEmoji()} prepend: ${prepend.debugEmoji()} refresh: ${refresh.debugEmoji()} append: ${append.debugEmoji()}"

/**
 * 反选。pair 的first 作为key。
 */
fun List<Pair<DataItemHolder, Int>>?.toggle(pair: Pair<DataItemHolder, Int>): Pair<List<Pair<DataItemHolder, Int>>, Boolean> {
    val oldSelectedHolders = this ?: mutableListOf()
    val otherHolders = oldSelectedHolders.filter {
        !it.first.areItemsTheSame(pair.first)
    }
    val stateSelected = otherHolders.size == oldSelectedHolders.size
    val selected = if (stateSelected) otherHolders + pair else otherHolders
    return selected to stateSelected
}

fun List<Pair<DataItemHolder, Int>>.valueContains(pair: Pair<DataItemHolder, Int>): Boolean {
    val firstOrNull = firstOrNull {
        it.first.areItemsTheSame(pair.first)
    }
    return firstOrNull != null
}

fun MutableLiveData<List<Pair<DataItemHolder, Int>>>.toggle(viewHolder: RecyclerView.ViewHolder) {
    val adapterViewHolder = viewHolder as AbstractViewHolder<out DataItemHolder>
    val (selectedHolders, currentSelected) =
        value.toggle(adapterViewHolder.itemHolder to viewHolder.absoluteAdapterPosition)
    viewHolder.view.isSelected = currentSelected
    value = selectedHolders
}
