package com.storyteller_f.yue_plugin

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

private const val arg_uri = "param1"
private const val arg_position = "param2"

class ImageViewFragment : Fragment() {
    private var uri: Uri? = null
    private var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getParcelable(arg_uri)
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
        val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(u, "r")
        parcelFileDescriptor.use {
            val fileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return
            val decodeStream = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            view.findViewById<ImageView>(R.id.image_view).setImageBitmap(decodeStream)
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