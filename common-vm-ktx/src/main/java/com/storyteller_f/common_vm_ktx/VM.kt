@file:Suppress("unused")

package com.storyteller_f.common_vm_ktx

/**
 * @author storyteller_f
 */

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

/**
 * 带有关键字的vm
 */
class KeyedViewModelLazy<VM : ViewModel>(
    private val keyPrefixProvider: () -> String,
    private val viewModelClass: KClass<VM>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer()
                val store = storeProducer()
                val canonicalName: String = viewModelClass.java.canonicalName
                    ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")

                val key = "${keyPrefixProvider()} : $canonicalName"
                ViewModelProvider(store, factory)[key, viewModelClass.java].also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized() = cached != null
}

@MainThread
fun <VM : ViewModel, T> T.keyedViewModels(
    keyPrefixProvider: () -> String,
    viewModelClass: KClass<VM>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner, T : HasDefaultViewModelProviderFactory {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return KeyedViewModelLazy(keyPrefixProvider, viewModelClass, storeProducer, factoryPromise)
}

/**
 * 实际使用 Fragment.keyedViewModels 构建，仅方便使用
 */
@MainThread
inline fun <reified VM : ViewModel, T> T.kvm(
    keyPrefix: String,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner, T : HasDefaultViewModelProviderFactory =
    keyedViewModels({ keyPrefix }, VM::class, { ownerProducer().viewModelStore }, factoryProducer)

@MainThread
inline fun <reified VM : ViewModel, T> T.kvm(
    noinline keyPrefixProvider: () -> String,
    crossinline factoryProducer: () -> VM
): Lazy<VM> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return KeyedViewModelLazy(keyPrefixProvider, VM::class, { viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(this, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = modelClass.cast(factoryProducer())!!
            }
        })
}

@MainThread
inline fun <reified VM : ViewModel, T> T.vm(
    crossinline factoryProducer: () -> VM
): Lazy<VM> where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    return ViewModelLazy(VM::class, { viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(this, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = modelClass.cast(factoryProducer())!!
            }
        })
}

/**
 * 虽然是Fragment 的扩展函数，但是调用的activity的
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.avm(
    crossinline factoryProducer: () -> VM
): Lazy<VM> {
    return ViewModelLazy(VM::class, { requireActivity().viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = modelClass.cast(factoryProducer())!!
            }
        })
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.asvm(
    crossinline factoryProducer: (SavedStateHandle) -> VM
): Lazy<VM> {
    return ViewModelLazy(VM::class, { requireActivity().viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = modelClass.cast(factoryProducer(handle))!!
            }
        })
}

inline fun <reified VM : ViewModel, T> T.svm(
    crossinline factoryProducer: (SavedStateHandle) -> VM
): Lazy<VM> where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    return ViewModelLazy(VM::class, { viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(this, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = modelClass.cast(factoryProducer(handle))!!
            }
        })
}

class GenericValueModel<T> : ViewModel() {
    val data = MutableLiveData<T>()
}

class GenericListValueModel<T> : ViewModel() {
    val data = MutableLiveData<List<T>>()
}

class HasStateValueModel<T>(stateHandle: SavedStateHandle, key: String, default: T) : ViewModel() {
    val data = stateHandle.getLiveData<T>(key, default)
}
