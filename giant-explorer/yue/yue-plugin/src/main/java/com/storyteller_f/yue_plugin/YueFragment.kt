package com.storyteller_f.yue_plugin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.storyteller_f.plugin_core.FileSystemProviderConstant
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import kotlinx.coroutines.launch
import java.io.File

private const val arg_uri = "uri"

class YueFragment : Fragment(), GiantExplorerPlugin {
    private var uri: Uri? = null
    var plugin: GiantExplorerPluginManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getParcelable(arg_uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_yue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val u = uri ?: return
        val list = mutableListOf<Uri>()

        val viewPager2 = view.findViewById<ViewPager2>(R.id.image_gallery)
        val adapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun getItemCount() = list.size

            override fun createFragment(position: Int) = ImageViewFragment.newInstance(list[position], position)
        }
        viewPager2.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            list.clear()
            if (listFiles(u, list)) {
                adapter.notifyItemRangeInserted(0, list.size)
            }
        }

    }

    private fun listFiles(u: Uri, list: MutableList<Uri>): Boolean {
        Log.i(TAG, "onViewCreated: ${u.authority}")

        if (u.authority?.contains("storyteller") == true) {
            val manager = plugin ?: return false
            val parentPath = manager.resolveParentPath(u.toString())
            val isolateRunning = requireContext().javaClass.canonicalName == "com.storyteller_f.yue.MainActivity"
            if (!isolateRunning && parentPath != null) {
                list.addAll(manager.listFiles(parentPath).map {
                    Uri.fromFile(File(it))
                })
            } else {
                val parentUri = manager.resolveParentUri(u.toString())?.toUri() ?: return false
                requireContext().contentResolver.query(parentUri, null, null, null, null)?.use {
                    while (it.moveToNext()) {
                        val path = it.getString(it.getColumnIndexOrThrow(FileSystemProviderConstant.filePath))
                        val mimeType = it.getString(it.getColumnIndexOrThrow(FileSystemProviderConstant.fileMimeType))
                        Log.i(TAG, "listFiles: $path $mimeType")
                        if (mimeType != null && mimeType.startsWith("image"))
                            list.add(Uri.Builder().scheme(u.scheme).authority(u.authority).path("/info$path").build())
                    }
                }
            }

        } else {
            list.add(u)
        }
        return true
    }

    companion object {
        private const val TAG = "YueFragment"
    }

    override fun accept(file: List<File>): Boolean {
        return file.all {
            it.extension == "jpg"
        }
    }

    override fun group(): List<String> {
        return listOf("view", "yue")
    }

    override fun plugPluginManager(pluginManager: GiantExplorerPluginManager) {
        plugin = pluginManager
    }
}