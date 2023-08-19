package com.storyteller_f.yue_plugin

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private const val arg_uri = "param1"
private const val arg_position = "param2"

fun <T> Bundle.getParcelableCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}

class ImageViewFragment : Fragment() {
    private var uri: Uri? = null
    private var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getParcelableCompat(arg_uri, Uri::class.java)
            position = it.getInt(arg_position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val u = uri ?: return
        val path = u.path
        val findViewById = view.findViewById<ImageView>(R.id.image_view)

        try {
            if (u.scheme == ContentResolver.SCHEME_FILE && path != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    (requireParentFragment() as? YueFragment)?.plugin?.fileInputStream(path)?.use {
                        val decodeStream = BitmapFactory.decodeStream(it)
                        findViewById.setImageBitmap(decodeStream)
                    }
                }
            } else if (u.scheme == ContentResolver.SCHEME_CONTENT) {
                val parcelFileDescriptor =
                    requireContext().contentResolver.openFileDescriptor(u, "r")
                parcelFileDescriptor.use {
                    val fileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return
                    val decodeStream = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    findViewById.setImageBitmap(decodeStream)
                }
            }

        } catch (e: Exception) {
            val trimMargin = """$u
                |${e.localizedMessage}""".trimMargin()
            view.findViewById<TextView>(R.id.status).text = trimMargin
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(uri: Uri, position: Int) =
            ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(arg_uri, uri)
                    putInt(arg_position, position)
                }
            }
    }
}