package com.storyteller_f.giant_explorer.control.task

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.*
import com.storyteller_f.giant_explorer.database.BigTimeTask
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentTaskListBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView
import com.storyteller_f.view_holder_compose.EdComposeViewEventEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BackgroundTaskListFragment : SimpleFragment<FragmentTaskListBinding>(FragmentTaskListBinding::inflate) {
    private val adapter = ManualAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()

    override fun onBindViewEvent(binding: FragmentTaskListBinding) {
        binding.content.manualUp(adapter)
        binding.content.flash(ListWithState.UIState.loading)
        scope.launch {
            requireDatabase.bigTimeDao().fetch().map { list -> list.groupBy { it.category } }
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .shareIn(scope, SharingStarted.WhileSubscribed())
                .collectLatest {
                    binding.content.flash(ListWithState.UIState(false, it.isNotEmpty(), empty = false, progress = false, null, null))
                    val list = mutableListOf<DataItemHolder>()
                    it.forEach { (category, result) ->
                        list.add(TaskTypeHolder(category))
                        list.addAll(result.map { task ->
                            BigTimeTaskItemHolder(task)
                        })
                    }
                    adapter.submitList(list)
                }
        }
    }

    @BindClickEvent(BigTimeTaskItemHolder::class, "check")
    fun onCheck(itemHolder: BigTimeTaskItemHolder) {
        scope.launch {
            val waitingDialog = waitingDialog()
            try {
                withContext(Dispatchers.IO) {
                    requireDatabase.bigTimeDao().update(BigTimeTask(itemHolder.bigTimeWorker.uri, !itemHolder.bigTimeWorker.enable, itemHolder.bigTimeWorker.category))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.exceptionMessage, Toast.LENGTH_SHORT).show()
            } finally {
                waitingDialog.end()
            }
        }
    }

    companion object {
        private const val TAG = "FirstFragment"
    }
}

class TaskTypeHolder(val title: String) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder) = title == (other as TaskTypeHolder).title
}

@BindItemHolder(TaskTypeHolder::class)
class TaskTypeViewHolder(edComposeView: EDComposeView) : ComposeViewHolder<TaskTypeHolder>(edComposeView) {
    override fun bindData(itemHolder: TaskTypeHolder) {
        edComposeView.composeView.setContent {
            TaskType(itemHolder = itemHolder)
        }
    }

}

class TaskTypeProvider : PreviewParameterProvider<TaskTypeHolder> {
    override val values: Sequence<TaskTypeHolder>
        get() = sequence {
            yield(TaskTypeHolder("种子名称"))
        }
}

@Preview
@Composable
fun TaskType(@PreviewParameter(TaskTypeProvider::class) itemHolder: TaskTypeHolder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = itemHolder.title)
    }
}

class BigTimeTaskItemHolder(val bigTimeWorker: BigTimeTask) : DataItemHolder {
    val id = "${bigTimeWorker.uri}:${bigTimeWorker.category}"
    override fun areItemsTheSame(other: DataItemHolder) = id == (other as BigTimeTaskItemHolder).id
}

@BindItemHolder(BigTimeTaskItemHolder::class)
class BigTimeTaskViewHolder(edComposeView: EDComposeView) : ComposeViewHolder<BigTimeTaskItemHolder>(edComposeView) {
    override fun bindData(itemHolder: BigTimeTaskItemHolder) {
        edComposeView.composeView.setContent {
            BigTimeTaskView(itemHolder = itemHolder, edComposeView)
        }
    }

}

class BigTimeTaskProvider : PreviewParameterProvider<BigTimeTaskItemHolder> {
    override val values: Sequence<BigTimeTaskItemHolder>
        get() = sequence {
            yield(BigTimeTaskItemHolder(BigTimeTask(File("/storage/emulated/0/Download").toUri(), true, "md")))
        }

}

@Preview
@Composable
fun BigTimeTaskView(@PreviewParameter(BigTimeTaskProvider::class) itemHolder: BigTimeTaskItemHolder, edComposeView: EdComposeViewEventEmitter = EdComposeViewEventEmitter.default) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = itemHolder.bigTimeWorker.uri.toString(), modifier = Modifier.weight(1f))
        Checkbox(checked = itemHolder.bigTimeWorker.enable, onCheckedChange = { edComposeView.notifyClickEvent("check") })
    }
}