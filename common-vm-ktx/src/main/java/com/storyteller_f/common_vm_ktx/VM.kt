@file:Suppress("unused")

package com.storyteller_f.common_vm_ktx

/**
 * @author storyteller_f
 */

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import kotlin.reflect.KClass

class ViewModelLazy<VM : ViewModel> @JvmOverloads constructor(
    val viewModelClass: KClass<VM>,
    val storeProducer: () -> ViewModelStore,
    val factoryProducer: () -> ViewModelProvider.Factory,
    val extrasProducer: () -> CreationExtras = { CreationExtras.Empty }
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer()
                val store = storeProducer()
                ViewModelProvider(
                    store,
                    factory,
                    extrasProducer()
                )[viewModelClass.java].also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}

/**
 * 带有关键字的vm
 */
class KeyedViewModelLazy<VM : ViewModel>(
    private val keyPrefixProvider: () -> String,
    private val viewModelClass: KClass<VM>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory,
    val extrasProducer: () -> CreationExtras = { CreationExtras.Empty }
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
                ViewModelProvider(store, factory, extrasProducer())[key, viewModelClass.java].also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized() = cached != null
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.keyPrefix(
    noinline keyPrefixProvider: () -> String,
    lazy: ViewModelLazy<VM>
) = KeyedViewModelLazy(keyPrefixProvider, lazy.viewModelClass, lazy.storeProducer, lazy.factoryProducer, lazy.extrasProducer)

@MainThread
inline fun <reified VM : ViewModel> Fragment.keyPrefix(
    keyPrefixProvider: String,
    lazy: ViewModelLazy<VM>
) = KeyedViewModelLazy({ keyPrefixProvider }, lazy.viewModelClass, lazy.storeProducer, lazy.factoryProducer, lazy.extrasProducer)

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

@ExtFuncFlat(type = ExtFuncFlatType.V5)
@MainThread
inline fun <reified VM : ViewModel, T, ARG> T.vm(
    crossinline arg: () -> ARG,
    noinline storeProducer: () -> ViewModelStore = { viewModelStore },
    noinline ownerProducer: () -> SavedStateRegistryOwner = { this },
    crossinline vmProducer: (ARG) -> VM,
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner =
    ViewModelLazy(VM::class, storeProducer, defaultFactory(arg, vmProducer, ownerProducer))

@ExtFuncFlat(type = ExtFuncFlatType.V5)
inline fun <reified VM : ViewModel, T, ARG> T.svm(
    crossinline arg: () -> ARG,
    noinline storeProducer: () -> ViewModelStore = { viewModelStore },
    noinline ownerProducer: () -> SavedStateRegistryOwner = { this },
    crossinline vmProducer: (SavedStateHandle, ARG) -> VM,
) where T : SavedStateRegistryOwner, T : ViewModelStoreOwner =
    ViewModelLazy(VM::class, storeProducer, stateDefaultFactory(arg, vmProducer, ownerProducer))

class GenericValueModel<T> : ViewModel() {
    val data = MutableLiveData<T>()
}

class GenericListValueModel<T> : ViewModel() {
    val data = MutableLiveData<List<T>>()
}

class HasStateValueModel<T>(stateHandle: SavedStateHandle, key: String, default: T) : ViewModel() {
    val data = stateHandle.getLiveData(key, default)
}
