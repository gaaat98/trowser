package com.android.trowser.utils.observable

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

/**
 * Just simple and more convenient replacement of java.util.Observable/Observer
 * to implement observer pattern
 */

interface Subscribable<O> {
    val observers: ArrayList<O>
    fun subscribe(observer: O, notifyOnSubscribe: Boolean = true) {
        if (!observers.contains(observer)) {
            observers.add(observer)
            if (notifyOnSubscribe) {
                notifyObserver(observer)
            }
        }
    }

    fun subscribe(lifecycleOwner: LifecycleOwner, observer: O) {
        subscribe(lifecycleOwner.lifecycle, true, observer)
    }

    fun subscribe(lifecycleOwner: LifecycleOwner, notifyOnSubscribe: Boolean = true, observer: O) {
        subscribe(lifecycleOwner.lifecycle, notifyOnSubscribe, observer)
    }

    fun subscribe(lifecycle: Lifecycle, observer: O) {
        subscribe(lifecycle, true, observer)
    }

    fun subscribe(lifecycle: Lifecycle, notifyOnSubscribe: Boolean = true, observer: O) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            subscribe(observer, notifyOnSubscribe)
        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (source.lifecycle.currentState) {
                    Lifecycle.State.INITIALIZED, Lifecycle.State.CREATED -> {
                        unsubscribe(observer)
                    }
                    Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> {
                        subscribe(observer, notifyOnSubscribe)
                    }
                    Lifecycle.State.DESTROYED -> {
                        lifecycle.removeObserver(this)
                        unsubscribe(observer)
                    }

                }
            }
        })
    }

    fun unsubscribe(observer: O) {
        observers.remove(observer)
    }

    fun notifyObservers() {
        for (observer in observers) {
            notifyObserver(observer)
        }
    }

    fun notifyObserver(observer: O)
}

typealias ValueObserver<T> = (value: T) -> Unit

open class ObservableValue<T>(default: T) : Subscribable<ValueObserver<T>> {
    open var value: T = default
        set(value) {
            field = value
            notifyObservers()
        }

    override val observers = ArrayList<ValueObserver<T>>()

    override fun notifyObservers() {
        for (observer in observers) {
            observer(value)
        }
    }

    override fun notifyObserver(observer: ValueObserver<T>) {
        observer(value)
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = this.value
    operator fun setValue(thisRef: Any, prop: KProperty<*>, value: T) {this.value = value}
}

fun <T>makeObservable(property: KMutableProperty<T>): ObservableValue<T> {
    return object : ObservableValue<T>(property.getter.call()) {
        override var value: T = property.getter.call()
            set(value) {
                property.setter.call(value)
                field = value
                notifyObservers()
            }
            get() = property.getter.call()
    }
}

typealias EventObserver = () -> Unit

class EventSource: Subscribable<EventObserver> {
    override val observers = ArrayList<EventObserver>()
    
    var wasEmitted: Boolean = false

    fun emit() {
        wasEmitted = true
        notifyObservers()
    }

    override fun notifyObservers() {
        if (!wasEmitted) return
        for (observer in observers) {
            observer()
        }
    }

    override fun notifyObserver(observer: EventObserver) {
        if (!wasEmitted) return
        observer()
    }
}

typealias ParameterizedEventObserver<T> = (T) -> Unit

class ParameterizedEventSource<T>: Subscribable<ParameterizedEventObserver<T>> {
    override val observers = ArrayList<ParameterizedEventObserver<T>>()
    var lastEvent: T? = null
    
    fun emit(event: T) {
        lastEvent = event
        notifyObservers()
    }

    override fun notifyObservers() {
        val event = lastEvent ?: return
        for (observer in observers) {
            observer(event)
        }
    }

    override fun notifyObserver(observer: ParameterizedEventObserver<T>) {
        val event = lastEvent ?: return
        observer(event)
    }
}