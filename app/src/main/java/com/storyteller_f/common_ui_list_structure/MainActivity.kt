package com.storyteller_f.common_ui_list_structure

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.ActivityMainBinding
import com.storyteller_f.common_ui_list_structure.databinding.RepoViewItemBinding
import com.storyteller_f.common_ui_list_structure.db.RepoComposite
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.view_holder_compose.ComposeSourceAdapter
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val viewModel by source({ 1 }, { 2 }, { one, two ->
        SourceProducer(
            { RepoComposite(requireRepoDatabase()) },
            { p, c ->
                requireReposService.searchRepos(p, c)
            },
            {
                Log.i(TAG, "view model: $one $two")
                requireRepoDatabase().reposDao().selectAll()
            },
            { it, _ -> RepoItemHolder(it) },
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
        binding.content.up(adapter, lifecycleScope)
        lifecycleScope.launchWhenResumed {
            viewModel.content?.collectLatest {
                adapter.submitData(it)
            }
        }
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            printInsets(insets)
            val insets1 =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> insets.getInsets(WindowInsets.Type.statusBars()).top
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        insets.systemWindowInsets.top
                    }
                    else -> {
                        0
                    }
                }
            Log.i(TAG, "onCreate: $insets1")
            insets
        }
        binding.buttonGroup.run {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                ButtonGroup()
            }
        }
        lifecycleScope.launch {
            delay(3000)
            startActivity(Intent(this@MainActivity, MainActivity2::class.java))
        }
    }

    @BindClickEvent(RepoItemHolder::class)
    fun clickRepo(view: View, itemHolder: RepoItemHolder) {
        println("click ${itemHolder.repo.fullName}")
    }

    private fun printInsets(insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.i(TAG, "printInsets: >R")
            val navigation = insets.getInsets(WindowInsets.Type.navigationBars())
            Log.i(TAG, "printInsets: navigator $navigation")
            val navigatorVisible = insets.isVisible(WindowInsets.Type.navigationBars())
            val statusVisible = insets.isVisible(WindowInsets.Type.statusBars())
            Log.i(TAG, "printInsets: n visible $navigatorVisible s visible $statusVisible")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.i(TAG, "printInsets: >Q")
            val systemWindowInsets = insets.systemWindowInsets
            Log.i(TAG, "printInsets: $systemWindowInsets")
        }
        val systemWindowInsetTop = insets.systemWindowInsetTop
        Log.i(TAG, "printInsets: $systemWindowInsetTop")
        Log.i(
            TAG,
            "printInsets: ${window.decorView.windowSystemUiVisibility} ${window.decorView.systemUiVisibility}"
        )
        val i = window.decorView.windowSystemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN
        Log.i(TAG, "printInsets: $i")
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    @Composable
    fun ButtonGroup() {
        Row(modifier = Modifier.background(Brush.linearGradient(listOf(Color.Black, Color.White)), RoundedCornerShape(3))) {
            Button(
                onClick = { hideSystemUI() },
            ) {
                Text(text = "full")
            }
            Button(
                onClick = { showSystemUI() },
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
class SeparatorViewHolder(composeView: ComposeView) :
    ComposeViewHolder<SeparatorItemHolder>(composeView) {
    override fun bindData(itemHolder: SeparatorItemHolder) {
        composeView.setContent {
            Card(backgroundColor = colorResource(id = R.color.separatorBackground)) {
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