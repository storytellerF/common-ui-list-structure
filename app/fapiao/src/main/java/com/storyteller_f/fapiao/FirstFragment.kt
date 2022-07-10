package com.storyteller_f.fapiao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.flowWithLifecycle
import com.example.common_pr.withState
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.fapiao.database.FaPiaoEntity
import com.storyteller_f.fapiao.databinding.FragmentFirstBinding
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.ManualAdapter
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView
import com.storyteller_f.view_holder_compose.EdComposeViewEventEmitter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.*

class FirstFragment : SimpleFragment<FragmentFirstBinding>(FragmentFirstBinding::inflate) {

    private val adapter = ManualAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()
    private val selected = MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>()
    override fun onBindViewEvent(binding: FragmentFirstBinding) {
        binding.content.manualUp(adapter, selected)
        binding.content.flash(ListWithState.UIState.loading)
        scope.launch {
            requireDatabase.fapiaoDao().getAll()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .shareIn(scope, SharingStarted.WhileSubscribed())
                .collect {
                    binding.content.flash(ListWithState.UIState(false, it.isNotEmpty(), empty = it.isEmpty(), progress = false, null, null))
                    val map = it.map { faPiaoEntity ->
                        FaPiaoItemHolder(faPiaoEntity, selected)
                    }
                    adapter.submitList(map)
                }
        }
        selected.withState {
            println(it)
        }
    }
}

class FaPiaoItemHolder(val item: FaPiaoEntity, val selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>) : DataItemHolder() {
    override fun areItemsTheSame(other: DataItemHolder) = (other as FaPiaoItemHolder).item.code == item.code
}

@BindItemHolder(FaPiaoItemHolder::class)
class FaPiaoViewHolder(edComposeView: EDComposeView) : ComposeViewHolder<FaPiaoItemHolder>(edComposeView) {
    override fun bindData(itemHolder: FaPiaoItemHolder) {
        edComposeView.composeView.setContent {
            FaPiaoItemCompose(itemHolder, edComposeView)
        }
    }

}

class FaPiaoProvider : PreviewParameterProvider<FaPiaoItemHolder> {
    override val values: Sequence<FaPiaoItemHolder>
        get() = sequence {
            yield(FaPiaoItemHolder(FaPiaoEntity("test", "test", Date(), 12.5f), MutableLiveData()))
        }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun FaPiaoItemCompose(@PreviewParameter(FaPiaoProvider::class) itemHolder: FaPiaoItemHolder, edComposeView: EdComposeViewEventEmitter = EdComposeViewEventEmitter.default) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(10.dp),
        onClick = {
            edComposeView.notifyClickEvent("card")
        }
    ) {
        Column(modifier = Modifier
            .padding(5.dp)
            .background(Color.Transparent)) {
            Text(itemHolder.item.code)
            Text(itemHolder.item.number)
            Text(text = itemHolder.item.created.toInstant().toString())
            Text(text = itemHolder.item.total.toString())
        }
    }
}