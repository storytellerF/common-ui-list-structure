package com.storyteller_f.common_ui_list_structure

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.navigator
import com.storyteller_f.common_ui.status
import com.storyteller_f.common_ui.updateMargins
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.ActivityMainBinding
import com.storyteller_f.common_ui_list_structure.databinding.RepoViewItemBinding
import com.storyteller_f.common_ui_list_structure.db.composite.RepoComposite
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_vm_ktx.combine
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.view_holder_compose.ComposeSourceAdapter
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val toast by lazy {
        Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }
    private val viewModel by source({ }, {
        SourceProducer(
            { RepoComposite(requireRepoDatabase()) },
            { p, c ->
                requireReposService.searchRepos(p, c)
            },
            {
                requireRepoDatabase().reposDao().selectAll()
            },
            { repo, _ -> RepoItemHolder(repo) },
            { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    null
                } else if (before == null) {
                    // we're at the beginning of the list
                    SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                } else if (before.roundedStarCount > after.roundedStarCount) {
                    if (after.roundedStarCount >= 1) {
                        SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                    } else {
                        SeparatorItemHolder("< 10.000+ stars")
                    }
                } else {
                    // no separator
                    null
                }
            }
        )
    })

    private val adapter =
        ComposeSourceAdapter<DataItemHolder, AbstractAdapterViewHolder<DataItemHolder>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.content.sourceUp(adapter, lifecycleScope)
        lifecycleScope.launchWhenResumed {
            viewModel.content?.collectLatest {
                adapter.submitData(it)
            }
        }
        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            printInsets(insets)
            binding.buttonGroup.updateMargins {
                topMargin = insets.status().top
            }
            binding.content.recyclerView.updatePadding(bottom = insets.navigator().bottom)
            insets
        }
        binding.buttonGroup.run {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                ButtonGroup()
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
            println("hello $lAge $lGender $lRepo")
        }
        age.value = 2
        gender.value = false
        r.value = Repo(0, "", "", "", "", 12, 15, "")
    }

    @BindClickEvent(RepoItemHolder::class)
    fun clickRepo(itemHolder: RepoItemHolder) {
        println("click ${itemHolder.repo.fullName}")
    }

    @BindClickEvent(SeparatorItemHolder::class, "card")
    fun clickLine() {
        startActivity(Intent(this, MainActivity2::class.java))
    }

    private fun printInsets(insets: WindowInsets) {
        val insets1 = WindowInsetsCompat.toWindowInsetsCompat(insets)
            .getInsets(WindowInsetsCompat.Type.navigationBars())
        println("navigator ${insets1.bottom} status ${insets1.top}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
    @Preview
    @Composable
    fun ButtonGroup() {
        Row(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color.Black, Color.White)),
                RoundedCornerShape(3)
            )
        ) {
            Button(
                onClick = {
                    toast.apply {
                        setText("full")
                        show()
                    }
                },
            ) {
                Text(text = "full")
            }
            Button(
                onClick = {
                    toast.apply {
                        setText("recovery")
                        show()
                    }
                },
            ) {
                Text(text = "recovery")
            }
        }
    }

}

class RepoItemHolder(val repo: Repo) : DataItemHolder() {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return repo.id == (other as RepoItemHolder).repo.id
    }
}

@BindItemHolder(RepoItemHolder::class)
class Repo2ViewHolder(private val binding: RepoViewItemBinding) :
    AdapterViewHolder<RepoItemHolder>(binding) {
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


class SeparatorItemHolder(val info: String) : DataItemHolder() {
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
    @OptIn(ExperimentalFoundationApi::class)
    override fun bindData(itemHolder: SeparatorItemHolder) {
        edComposeView.composeView.setContent {
            Card(backgroundColor = colorResource(id = R.color.separatorBackground),
                modifier = Modifier.combinedClickable(
                    onClick = { edComposeView.notifyClickEvent("card") },
                    onLongClick = { edComposeView.notifyLongClickEvent("card") }
                )) {
                Text(
                    text = itemHolder.info, modifier = Modifier.padding(12.dp),
                    color = colorResource(
                        id = R.color.separatorText
                    ),
                    fontSize = 25.sp,
                )
            }

        }
    }
}

private val RepoItemHolder.roundedStarCount: Int
    get() = this.repo.stars / 10_000