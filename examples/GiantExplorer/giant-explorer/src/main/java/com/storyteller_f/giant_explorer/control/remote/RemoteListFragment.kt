package com.storyteller_f.giant_explorer.control.remote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.database.RemoteAccessSpec
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentRemoteListBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderRemoteAccessSpecBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class RemoteListFragment : Fragment() {
    private val adapter = ManualAdapter<RemoteAccessSpecHolder, RemoteAccessSpecViewHolder>()

    private var _binding: FragmentRemoteListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRemoteListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.manualUp(adapter)
        binding.content.flash(ListWithState.UIState.loading)
        scope.launch {
            requireDatabase.remoteAccessDao().list()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .shareIn(scope, SharingStarted.WhileSubscribed())
                .collectLatest {
                    binding.content.flash(ListWithState.UIState(false, it.isNotEmpty(), empty = false, progress = false, null, null))
                    adapter.submitList(it.map { spec ->
                        RemoteAccessSpecHolder(spec)
                    })
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@ItemHolder("remote access")
class RemoteAccessSpecHolder(val spec: RemoteAccessSpec) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        val remoteAccessSpec = other as RemoteAccessSpecHolder
        return remoteAccessSpec.spec == this.spec
    }
}

@BindItemHolder(RemoteAccessSpecHolder::class)
class RemoteAccessSpecViewHolder(private val binding: ViewHolderRemoteAccessSpecBinding) : BindingViewHolder<RemoteAccessSpecHolder>(binding) {
    override fun bindData(itemHolder: RemoteAccessSpecHolder) {
        binding.url.text = itemHolder.spec.toFtpSpec().toUri()

    }

}