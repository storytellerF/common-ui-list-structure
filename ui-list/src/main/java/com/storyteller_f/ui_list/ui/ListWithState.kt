package com.storyteller_f.ui_list.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.isError
import com.storyteller_f.ui_list.data.isLoading
import com.storyteller_f.ui_list.data.isNotLoading
import com.storyteller_f.ui_list.databinding.ListWithStateBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ListWithState @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {
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

    fun<IH: DataItemHolder, VH: AbstractViewHolder<IH>> sourceUp(
        adapter: SimpleSourceAdapter<IH, VH>,
        lifecycleOwner: LifecycleOwner,
        selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>? = null,
        flash: ((CombinedLoadStates, Int) -> UIState) = Companion::simple
    ) {
        setAdapter(
            adapter.withLoadStateHeaderAndFooter(header = SimpleLoadStateAdapter { adapter.retry() },
                footer = SimpleLoadStateAdapter { adapter.retry() }), adapter
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
                launch {
                    callbackFlow.distinctUntilChanged { old, new ->
                        old.mediator?.refresh.isLoading == new.mediator?.refresh.isLoading
                    }.shareIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed()).collectLatest {
                        if (it.mediator?.refresh.isNotLoading) {
                            binding.list.smoothScrollToPosition(0)
                        }
                    }
                }
                launch {
                    callbackFlow.map {
                        flash(it, adapter.itemCount)
                    }.stateIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed(), UIState.empty).collectLatest {
                        flash(it)
                    }
                }
            }
        }

        if (selected != null) {
            setupSwipeSelectableSupport(selected)
        }
    }


    private fun setAdapter(
        concatAdapter: ConcatAdapter,
        adapter: SimpleSourceAdapter<out DataItemHolder, out AbstractViewHolder<*>>
    ) {
        setupLinearLayoutManager()
        binding.list.adapter = concatAdapter
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

    /**
     * ???data adapter ??????
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                binding.refreshLayout.isEnabled = viewHolder == null
            }

        }).attachToRecyclerView(binding.list)
    }

    private fun setupSwipeSelectableSupport(selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>) {

        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            val paint = Paint()
            private var swipeEvent = false

            init {
                paint.textSize = 40f
                paint.isAntiAlias = true
            }


            override fun getSwipeEscapeVelocity(defaultValue: Float) = 1000000F

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 10F


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                binding.refreshLayout.isEnabled = viewHolder == null
            }

            private fun swipeViewHolder(viewHolder: RecyclerView.ViewHolder) {
                val adapterViewHolder = viewHolder as AbstractViewHolder<out DataItemHolder>
                val toggleSpecial =
                    selected.value.toggleSpecial(adapterViewHolder.itemHolder to viewHolder.absoluteAdapterPosition)
                selected.value =
                    toggleSpecial.let {
                        viewHolder.view.isSelected = it.second
                        it.first
                    }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val firstLine = 200
                    if (dX < firstLine) {
                        if (swipeEvent) {
                            swipeEvent = false
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX / 2, dY, actionState, isCurrentlyActive)
                    } else {
                        val secondLine = firstLine + 100
                        if (dX < secondLine) {
                            val firstMax = firstLine / 2
                            super.onChildDraw(c, recyclerView, viewHolder, firstMax + (dX - firstLine) / 4, dY, actionState, isCurrentlyActive)
                        } else if (dX >= secondLine) {
                            if (!swipeEvent) {
                                swipeViewHolder(viewHolder)
                                swipeEvent = true
                            }
                        }
                    }
                }
            }

        }).attachToRecyclerView(binding.list)
    }

    fun setupClickSelectableSupport(editing: MutableLiveData<Boolean>, lifecycleOwner: LifecycleOwner, selectableDrawer: SelectableDrawer) {
        editing.observe(lifecycleOwner, Observer {
            val adapter = binding.list.adapter
            binding.list.adapter = adapter
        })
        val value = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                if (editing.value == true) {
                    outRect.right = selectableDrawer.width(view, parent, state, parent.getChildAdapterPosition(view), (parent.getChildViewHolder(view) as AbstractViewHolder<*>).itemHolder)
                } else outRect.right = 0
            }

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                super.onDraw(c, parent, state)
                c.drawColor(Color.LTGRAY)
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
        vm.loadState.map { simple(it.loadState, it.itemCount) }.observe(lifecycleOwner, Observer {
            flash(it)
        })
        setupSwapSupport(adapter)
    }

    fun manualUp(adapter: ManualAdapter<*, *>, selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>? = null) {
        recyclerView.adapter = adapter
        setupLinearLayoutManager()
        if (selected != null) setupSwipeSelectableSupport(selected)
    }

    val recyclerView get() = binding.list

    private val binding = ListWithStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.list.setHasFixedSize(true)
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
         * ??????????????????????????????????????????
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
         * ??????????????????????????????????????????
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

    }

    interface SelectableDrawer {
        fun width(view: View, parent: RecyclerView, state: RecyclerView.State, childAdapterPosition: Int, absoluteAdapterPosition: DataItemHolder): Int
        fun draw(c: Canvas, top: Int, bottom: Int, childWidth: Int, childHeight: Int, parentWidth: Int, parentHeight: Int, child: View, parent: RecyclerView, state: RecyclerView.State)
    }
}

fun MutableList<Pair<DataItemHolder, Int>>?.toggleSpecial(pair: Pair<DataItemHolder, Int>): Pair<MutableList<Pair<DataItemHolder, Int>>, Boolean> {
    val mutableList = this ?: mutableListOf()
    val filter = mutableList.filter {
        !it.first.areItemsTheSame(pair.first)
    }.toMutableList()
    return filter to (filter.size == mutableList.size).apply {
        if (this) filter.add(pair)
    }
}

fun MutableList<Pair<DataItemHolder, Int>>.valueContains(pair: Pair<DataItemHolder, Int>): Boolean {
    val firstOrNull = firstOrNull {
        it.first.areItemsTheSame(pair.first)
    }
    return firstOrNull != null
}