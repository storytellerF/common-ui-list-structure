package com.storyteller_f.common_pr

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.storyteller_f.common_ui.Registry
import com.storyteller_f.common_ui.RequestKey
import com.storyteller_f.common_ui.observeResponse

context (RequestKey)
fun <T : Parcelable, F> F.observe(
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    val requestKey = this@RequestKey
    observeResponse(requestKey, result, action)
}

context (RequestKey)
fun <T : Parcelable, A> A.observe(
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    val requestKey = this@RequestKey
    observeResponse(requestKey, result, action)
}

context (F)
fun <T : Parcelable, F> RequestKey.observe(
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    val requestKey = this@RequestKey
    observeResponse(requestKey, result, action)
}

context (A)
fun <T : Parcelable, A> RequestKey.observe(
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    val requestKey = this@RequestKey
    observeResponse(requestKey, result, action)
}