package com.storyteller_f.common_ui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.storyteller_f.common_vm_ktx.StateValueModel
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.stateValueModel
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.compat_ktx.getParcelableCompat
import com.storyteller_f.compat_ktx.getSerializableCompat
import java.util.UUID

class ActivityAction(val action: (CommonActivity, Parcelable) -> Unit, val registerKey: String)
class FragmentAction(val action: (Registry, Parcelable) -> Unit, val registerKey: String)

val waitingInActivity = mutableMapOf<String, ActivityAction>()
val waitingInFragment = mutableMapOf<String, FragmentAction>()

/**
 * 可以被请求。返回的结构通过requestKey 辨别
 */
interface ResponseFragment {
    val vm: StateValueModel<UUID?>
}

data class RequestKey(private val uuid: UUID?)

internal val ResponseFragment.requestKey: RequestKey
    get() = RequestKey(vm.data.value)

/**
 * 可以发起请求，发起的请求通过registryKey 辨别
 */
interface Registry {
    /**
     * 代表当前关心的事件
     */
    fun registryKey(): String = "${this.javaClass.simpleName}-registry"
}

fun <T : Parcelable> Fragment.setFragmentResult(requestKey: RequestKey, result: T) =
    fm.setFragmentResult(requestKey.toString(), Bundle().apply {
        putParcelable(fragmentResultKey, result)
    })

fun <T, F> F.setFragmentResult(result: T) where T : Parcelable, F : Fragment, F : ResponseFragment =
    setFragmentResult(requestKey, result)

val fragmentResultKey
    get() = "result"

val <T : Fragment> T.responseModel
    get() = keyPrefix("response", svm({ arguments }) { handle, arg ->
        stateValueModel(arg?.getSerializableCompat("uuid", UUID::class.java), handle)
    })

inline fun <T : Parcelable, F> F.buildCallback(result: Class<T>, crossinline action: (F, T) -> Unit): (String, Bundle) -> Unit where F : Fragment, F : Registry {
    return { responseKey: String, r: Bundle ->
        if (waitingInFragment.containsKey(responseKey)) {
            val parcelable = r.getParcelableCompat(fragmentResultKey, result)
            parcelable?.let { action(this, it) }
            waitingInFragment.remove(responseKey)
        }
    }
}

fun <T : Parcelable, A> A.buildCallback(
    result: Class<T>,
    action: A.(T) -> Unit
): (String, Bundle) -> Unit where A : CommonActivity {
    return { s: String, r: Bundle ->
        if (waitingInActivity.containsKey(s)) {
            r.getParcelableCompat(fragmentResultKey, result)?.let {
                action(it)
            }
            waitingInActivity.remove(s)
        }
    }
}

/**
 * 如果启动是通过navigation 启动dialog，需要使用parentFragmentManager 接受结果
 */
fun <T : Parcelable, F> F.waitingResponseInFragment(requestKey: RequestKey, action: F.(T) -> Unit, callback: (String, Bundle) -> Unit) where F : Registry, F : Fragment {
    val key = requestKey.toString()
    @Suppress("UNCHECKED_CAST")
    waitingInFragment[key] = FragmentAction(action as (Registry, Parcelable) -> Unit, registryKey())
    fm.setFragmentResultListener(key, owner, callback)
}

private fun <A, T : Parcelable> A.waitingResponseInActivity(requestKey: RequestKey, action: A.(T) -> Unit, callback: (String, Bundle) -> Unit) where A : CommonActivity {
    val key = requestKey.toString()
    @Suppress("UNCHECKED_CAST")
    waitingInActivity[key] = ActivityAction(action as (CommonActivity, Parcelable) -> Unit, registryKey())
    fm.setFragmentResultListener(key, this, callback)
}

private fun <A> A.show(dialog: Class<out CommonDialogFragment>, parameters: Bundle?): UUID where A : LifecycleOwner {
    val randomUUID = UUID.randomUUID()
    parameters?.putSerializable("uuid", randomUUID)
    val dialogFragment = dialog.newInstance().apply {
        arguments = parameters
    }
    dialogFragment.show(fm, randomUUID.toString())
    return randomUUID
}

//todo 自定义NavController
fun NavController.request(
    @IdRes resId: Int,
    args: Bundle = Bundle(),
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
): RequestKey {
    val randomUUID = UUID.randomUUID()
    args.putSerializable("uuid", randomUUID)
    navigate(resId, args, navOptions, navigatorExtras)
    return randomUUID.requestKey()
}

fun <F> F.request(
    dialog: Class<out CommonDialogFragment>,
    parameters: Bundle = Bundle()
): RequestKey where F : LifecycleOwner = show(dialog, parameters).requestKey()

private fun UUID.requestKey(): RequestKey = RequestKey(this)

fun <T : Parcelable, F> F.observe(
    requestKey: RequestKey,
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    val callback = buildCallback(result, action)
    waitingResponseInFragment(requestKey, action, callback)
}

fun <T : Parcelable, A> A.observe(
    requestKey: RequestKey,
    result: Class<T>,
    action: A.(T) -> Unit
) where A : CommonActivity {
    val callback = buildCallback(result, action)
    waitingResponseInActivity(requestKey, action, callback)
}

internal fun CommonActivity.observeResponse() {
    waitingInActivity.forEach { (requestKey, value) ->
        val action = value.action
        if (value.registerKey == registryKey()) {
            val callback = buildCallback(Parcelable::class.java, action)
            val supportFragmentManager = fm
            supportFragmentManager.clearFragmentResultListener(requestKey)
            supportFragmentManager.setFragmentResultListener(requestKey, this, callback)
        }
    }
}

internal fun CommonFragment.observeResponse() {
    waitingInFragment.forEach { (requestKey, value) ->
        if (value.registerKey == registryKey()) {
            val callback = buildCallback(Parcelable::class.java, value.action)
            val fragmentManager = fm
            fragmentManager.clearFragmentResultListener(requestKey)
            fragmentManager.setFragmentResultListener(requestKey, owner, callback)
        }
    }
}