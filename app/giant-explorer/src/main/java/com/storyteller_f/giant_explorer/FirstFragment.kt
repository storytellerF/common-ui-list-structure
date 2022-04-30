package com.storyteller_f.giant_explorer

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Arrangement
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
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.database.BigTimeTask
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFirstBinding
import com.storyteller_f.ui_list.core.AbstractAdapterViewHolder
import com.storyteller_f.ui_list.core.AdapterViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.ManualAdapter
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView
import com.storyteller_f.view_holder_compose.EdComposeViewEventEmitter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FirstFragment : CommonFragment<FragmentFirstBinding>(FragmentFirstBinding::inflate) {
    private val adapter = ManualAdapter<DataItemHolder, AbstractAdapterViewHolder<DataItemHolder>>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onBindViewEvent(binding: FragmentFirstBinding) {
        binding.content.manualUp(adapter)
        scope.launch {
            requireDatabase().bigTimeDao().fetch().map { list -> list.groupBy { it.workerName } }.collect {
                binding.content.flash(ListWithState.UIState(false, it.isNotEmpty(), empty = false, progress = false, null, null))
                val list = mutableListOf<DataItemHolder>()
                it.forEach { (workName, result) ->
                    list.add(TaskTypeHolder(workName))
                    list.addAll(result.map {
                        BigTimeTaskItemHolder(it)
                    })
                }
                adapter.submitList(list)
            }
        }
    }

    companion object {
        private const val TAG = "FirstFragment"
    }
}

class TaskTypeHolder(val title: String) : DataItemHolder() {
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

class BigTimeTaskItemHolder(val bigTimeWorker: BigTimeTask) : DataItemHolder() {
    val id = "${bigTimeWorker.absolutePath}:${bigTimeWorker.workerName}"
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
            yield(BigTimeTaskItemHolder(BigTimeTask("/storage/emulated/0/Download", true, "md")))
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
        Text(text = itemHolder.bigTimeWorker.absolutePath, modifier = Modifier.weight(1f))
        Checkbox(checked = itemHolder.bigTimeWorker.enable, onCheckedChange = { edComposeView.notifyClickEvent("check") })
    }
}