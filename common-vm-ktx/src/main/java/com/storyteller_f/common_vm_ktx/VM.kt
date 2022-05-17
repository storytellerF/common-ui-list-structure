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
inline fun <reified VM : ViewModel, T, ARG> T.kvm(
    keyPrefix: String,
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner, T : HasDefaultViewModelProviderFactory =
    keyedViewModels({ keyPrefix }, VM::class, { viewModelStore }, defaultFactory(arg, vmProducer))

@MainThread
inline fun <reified VM : ViewModel, T, ARG> T.kvm(
    noinline keyPrefixProvider: () -> String,
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner =
    KeyedViewModelLazy(keyPrefixProvider, VM::class, { viewModelStore }, defaultFactory(arg, vmProducer))


inline fun <reified VM : ViewModel, T, ARG> T.defaultFactory(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM,
    noinline ownerProducer: () -> SavedStateRegistryOwner = { this },
): () -> ViewModelProvider.Factory where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = {
    object : AbstractSavedStateViewModelFactory(ownerProducer(), null) {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T = modelClass.cast(vmProducer(arg()))!!
    }
}

inline fun <reified VM : ViewModel, T, ARG> T.stateDefaultFactory(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (SavedStateHandle, ARG) -> VM,
    noinline ownerProducer: () -> SavedStateRegistryOwner = { this },
): () -> ViewModelProvider.Factory where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = {
    object : AbstractSavedStateViewModelFactory(ownerProducer(), null) {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T = modelClass.cast(vmProducer(handle, arg()))!!
    }
}

@MainThread
inline fun <reified VM : ViewModel, T, ARG> T.vm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner =
    ViewModelLazy(VM::class, { viewModelStore }, defaultFactory(arg, vmProducer))

inline fun <reified VM : ViewModel, T, ARG> T.svm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (SavedStateHandle, ARG) -> VM
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner =
    ViewModelLazy(VM::class, { viewModelStore }, stateDefaultFactory(arg, vmProducer))

/**
 * 虽然是Fragment 的扩展函数，但是调用的activity的
 */
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.avm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) = ViewModelLazy(VM::class, { requireActivity().viewModelStore }, defaultFactory(arg, vmProducer) { requireActivity() })

@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.kavm(
    noinline keyPrefixProvider: () -> String,
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) = KeyedViewModelLazy(keyPrefixProvider, VM::class, { requireActivity().viewModelStore }, defaultFactory(arg, vmProducer) { requireActivity() })


@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.asvm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (SavedStateHandle, ARG) -> VM
) = ViewModelLazy(VM::class, { requireActivity().viewModelStore }, stateDefaultFactory(arg, vmProducer) { requireActivity() })

/**
 * 虽然是Fragment 的扩展函数，但是调用的parent fragment的
 */
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.pvm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) = ViewModelLazy(VM::class, { requireParentFragment().viewModelStore }, defaultFactory(arg, vmProducer) { requireParentFragment() })

@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.kpvm(
    noinline keyPrefixProvider: () -> String,
    crossinline arg: () -> ARG,
    crossinline vmProducer: (ARG) -> VM
) = KeyedViewModelLazy(keyPrefixProvider, VM::class, { requireParentFragment().viewModelStore }, defaultFactory(arg, vmProducer) { requireParentFragment() })


@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.psvm(
    crossinline arg: () -> ARG,
    crossinline vmProducer: (SavedStateHandle, ARG) -> VM
) = ViewModelLazy(VM::class, { requireParentFragment().viewModelStore }, stateDefaultFactory(arg, vmProducer) { requireParentFragment() })

class GenericValueModel<T> : ViewModel() {
    val data = MutableLiveData<T>()
}

class GenericListValueModel<T> : ViewModel() {
    val data = MutableLiveData<List<T>>()
}

class HasStateValueModel<T>(stateHandle: SavedStateHandle, key: String, default: T) : ViewModel() {
    val data = stateHandle.getLiveData(key, default)
}
