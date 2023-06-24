package com.storyteller_f.common_ui_list_structure

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_pr.dipToInt
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.ActivityMainBinding
import com.storyteller_f.common_ui_list_structure.databinding.RepoViewItemBinding
import com.storyteller_f.common_ui_list_structure.db.composite.RepoComposite
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_ui_list_structure.test_model.TestViewModelActivity
import com.storyteller_f.common_ui_list_structure.test_navigation.TestNavigationResultActivity
import com.storyteller_f.common_vm_ktx.combine
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.source.SourceProducer
import com.storyteller_f.ui_list.source.source
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.view_holder_compose.ComposeSourceAdapter
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView
import com.storyteller_f.view_holder_compose.EdComposeViewEventEmitter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val editing = MutableLiveData(false)
    private val viewModel by source({ }, {
        SourceProducer(
            { RepoComposite(requireRepoDatabase) },
            { p, c ->
                requireReposService.searchRepos(p, c)
            },
            {
                requireRepoDatabase.reposDao().selectAll()
            },
            { repo, _ -> RepoItemHolder(repo) },
            { before, after ->
                when {
                    after == null -> null
                    before == null -> SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                    before.roundedStarCount <= after.roundedStarCount -> null
                    after.roundedStarCount >= 1 -> SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                    else -> SeparatorItemHolder("< 10.000+ stars")
                }
            }
        )
    })

    private val adapter =
        ComposeSourceAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.content.sourceUp(adapter, this)
        val dp24 = 24.dipToInt
        binding.content.setupClickSelectableSupport(editing, owner, object : ListWithState.SelectableDrawer {
            override fun width(view: View, parent: RecyclerView, state: RecyclerView.State, childAdapterPosition: Int, absoluteAdapterPosition: DataItemHolder): Int {
                return dp24 * 3
            }

            override fun draw(c: Canvas, top: Int, bottom: Int, childWidth: Int, childHeight: Int, parentWidth: Int, parentHeight: Int, child: View, parent: RecyclerView, state: RecyclerView.State) {
                val offset = (childHeight - dp24) / 2
                val t = top + offset
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_radio_button_unchecked_24)?.run {
                    setBounds(parentWidth - dp24 * 2, t, (parentWidth - dp24), t + dp24)
                    draw(c)
                }
            }

        })
        supportNavigatorBarImmersive(binding.root)
        scope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.content?.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            printInsets(insets)
            binding.buttonGroup.updateMargins {
                topMargin = insets.status.top
            }
            binding.content.recyclerView.updatePadding(bottom = insets.navigator.bottom)
            insets
        }
        binding.buttonGroup.run {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                ButtonGroup({
                    startActivity(Intent(this@MainActivity, TestViewModelActivity::class.java))
                }) {
                    startActivity(Intent(this@MainActivity, TestNavigationResultActivity::class.java))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val age = MutableLiveData(0)
        val gender = MutableLiveData(true)
        val r = MutableLiveData<Repo>(null)
        combine("age" to age, "gender" to gender, "repo" to r).observe(this) {
            val lAge = it["age"] as Int
            val lGender = it["gender"]
            val lRepo = it["repo"]
            Log.i(TAG, "onResume: $lAge $lGender $lRepo")
        }
        age.value = 2
        gender.value = false
        r.value = Repo(0, "", "", "", "", 12, 15, "")
    }

    @BindClickEvent(RepoItemHolder::class)
    fun clickRepo(itemHolder: RepoItemHolder) {
        Toast.makeText(this, itemHolder.repo.fullName, Toast.LENGTH_SHORT).show()
    }

    @BindClickEvent(SeparatorItemHolder::class, "card")
    fun clickLine(itemHolder: SeparatorItemHolder) {
        Toast.makeText(this, itemHolder.info, Toast.LENGTH_SHORT).show()
    }

    private fun printInsets(insets: WindowInsetsCompat) {
        val insets1 = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        Log.d(TAG, "printInsets: navigator ${insets1.bottom} status ${insets1.top}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    @Preview
    @Composable
    fun ButtonGroup(next: () -> Unit = {}, response: () -> Unit = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Button(
                onClick = {
                    editing.value = true
                },
            ) {
                Text(text = "edit")
            }
            Button(
                onClick = {
                    editing.value = false
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "redo")
            }
            Button(
                onClick = {
                    next()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "next")
            }
            Button(
                onClick = {
                    response()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "test")
            }
        }
    }

}

class RepoItemHolder(val repo: Repo) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return repo.id == (other as RepoItemHolder).repo.id
    }
}

@BindItemHolder(RepoItemHolder::class)
class RepoViewHolder(private val binding: RepoViewItemBinding) :
    BindingViewHolder<RepoItemHolder>(binding) {
    override fun bindData(itemHolder: RepoItemHolder) {
        binding.repoName.text = itemHolder.repo.name
        // if the description is missing, hide the TextView
        var descriptionVisibility = View.GONE
        if (itemHolder.repo.description != null) {
            binding.repoDescription.text = itemHolder.repo.description
            descriptionVisibility = View.VISIBLE
        }
        binding.repoDescription.visibility = descriptionVisibility

        binding.repoStars.text = itemHolder.repo.stars.toString()
        binding.repoForks.text = itemHolder.repo.forks.toString()

        // if the language is missing, hide the label and the value
        var languageVisibility = View.GONE
        if (!itemHolder.repo.language.isNullOrEmpty()) {
            val resources = this.itemView.context.resources
            binding.repoLanguage.text =
                resources.getString(
                    R.string.language,
                    itemHolder.repo.language
                )
            languageVisibility = View.VISIBLE
        }
        binding.repoLanguage.visibility = languageVisibility
    }

}


class SeparatorItemHolder(val info: String) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return (other as SeparatorItemHolder).info == info
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SeparatorItemHolder

        if (info != other.info) return false

        return true
    }

    override fun hashCode(): Int {
        return info.hashCode()
    }


}


@BindItemHolder(SeparatorItemHolder::class)
class SeparatorViewHolder(edComposeView: EDComposeView) :
    ComposeViewHolder<SeparatorItemHolder>(edComposeView) {
    override fun bindData(itemHolder: SeparatorItemHolder) {
        edComposeView.composeView.setContent {
            Separator(itemHolder, edComposeView)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun Separator(@PreviewParameter(RepoSeparatorProvider::class) itemHolder: SeparatorItemHolder, edComposeView: EdComposeViewEventEmitter = EdComposeViewEventEmitter.default) {
    Card(backgroundColor = colorResource(id = R.color.separatorBackground),
        modifier = Modifier
            .combinedClickable(
                onClick = { edComposeView.notifyClickEvent("card") },
                onLongClick = { edComposeView.notifyLongClickEvent("card") }
            )
            .fillMaxWidth()) {
        Text(
            text = itemHolder.info, modifier = Modifier.padding(12.dp),
            color = colorResource(
                id = R.color.separatorText
            ),
            fontSize = 25.sp,
        )
    }
}

class RepoSeparatorProvider : PreviewParameterProvider<SeparatorItemHolder> {
    override val values: Sequence<SeparatorItemHolder>
        get() = sequence {
            yield(SeparatorItemHolder("90.000+ starts"))
        }

}

private val RepoItemHolder.roundedStarCount: Int
    get() = this.repo.stars / 10_000