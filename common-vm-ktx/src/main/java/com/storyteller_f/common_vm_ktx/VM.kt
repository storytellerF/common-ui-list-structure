package com.storyteller_f.common_vm_ktx

/**
 * @author storyteller_f
 */

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
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
                ViewModelProvider(store, factory).get(key, viewModelClass.java).also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized() = cached != null
}

@MainThread
fun <VM : ViewModel> Fragment.keyedViewModels(
    keyPrefixProvider: () -> String,
    viewModelClass: KClass<VM>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return KeyedViewModelLazy(keyPrefixProvider, viewModelClass, storeProducer, factoryPromise)
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModels(
    keyPrefix: String,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
) = keyedViewModels({ keyPrefix }, VM::class, { ownerProducer().viewModelStore }, factoryProducer)


@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.cVM(
    noinline factoryProducer: (() -> VM)
): Lazy<VM> {
    return ViewModelLazy(VM::class, { viewModelStore },
        {
            object : AbstractSavedStateViewModelFactory(this, null) {
                override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T = factoryProducer() as T

            }
        })
}
