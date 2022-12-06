package com.storyteller_f.yue_plugin

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import java.io.File

private const val arg_path = "path"
private const val arg_binder = "binder"
private const val arg_uri = "uri"

class YueFragment : Fragment(), GiantExplorerPlugin {
    private var path: String? = null
    private var uri: Uri? = null
    private var binder: IBinder? = null
    lateinit var plugin: GiantExplorerPluginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            path = it.getString(arg_path)
            binder = it.getBinder(arg_binder)
            uri = it.getParcelable(arg_uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_yue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("p $path u $uri")
        val textView = view.findViewById<TextView>(R.id.name)
        textView.text = path

        val u = uri ?: return
        val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(u, "r")
        parcelFileDescriptor.use {
            val fileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return
            val decodeStream = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            view.findViewById<ImageView>(R.id.image).setImageBitmap(decodeStream)
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(path: String, binder: IBinder, uri: Uri) =
            YueFragment().apply {
                arguments = Bundle().apply {
                    putString(arg_path, path)
                    putBinder(arg_binder, binder)
                    putParcelable(arg_uri, uri)
                }
            }
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