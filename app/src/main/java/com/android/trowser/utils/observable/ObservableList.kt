package com.android.trowser.utils.observable

import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*
import java.util.function.Predicate
import java.util.function.UnaryOperator
import kotlin.collections.ArrayList

typealias ListChangeObserver<T> = (ObservableList<T>) -> Unit

class ObservableList<T>(): ArrayList<T>(), Subscribable<ListChangeObserver<T>> {
  override val observers = ArrayList<ListChangeObserver<T>>()

  private fun notifyChanged() {
    notifyObservers()
  }

  override fun notifyObservers() {
    val tmp = observers[observers.size-1]
    try{
      for (observer in observers) {
        observer(this)
      }
    }catch(ex: ConcurrentModificationException){
      Log.d("ObservableList", "Mysterious observers resize", ex)
      tmp(this)
    }
  }

  override fun notifyObserver(observer: ListChangeObserver<T>) {
    observer(this)
  }

  override fun add(element: T): Boolean {
    val result = super.add(element)
    notifyChanged()
    return result
  }

  override fun add(index: Int, element: T) {
    super.add(index, element)
    notifyChanged()
  }

  override fun addAll(elements: Collection<T>): Boolean {
    val result = super.addAll(elements)
    notifyChanged()
    return result
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    val result = super.addAll(index, elements)
    notifyChanged()
    return result
  }

  override fun clear() {
    super.clear()
    notifyChanged()
  }

  override fun remove(element: T): Boolean {
    val result = super.remove(element)
    notifyChanged()
    return result
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    val result = super.removeAll(elements)
    notifyChanged()
    return result
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    val result = super.retainAll(elements)
    notifyChanged()
    return result
  }

  @RequiresApi(VERSION_CODES.N)
  override fun removeIf(filter: Predicate<in T>): Boolean {
    val result = super.removeIf(filter)
    notifyChanged()
    return result
  }

  override fun removeAt(index: Int): T {
    val result = super.removeAt(index)
    notifyChanged()
    return result
  }

  override fun set(index: Int, element: T): T {
    val result = super.set(index, element)
    notifyChanged()
    return result
  }

  @RequiresApi(VERSION_CODES.N)
  override fun replaceAll(operator: UnaryOperator<T>) {
    super.replaceAll(operator)
    notifyChanged()
  }

  override fun removeRange(fromIndex: Int, toIndex: Int) {
    super.removeRange(fromIndex, toIndex)
    notifyChanged()
  }

  fun swap(i: Int, j: Int) {
    super.set(i, super.set(j, super.get(i)))
    notifyChanged()
  }

  fun replaceAll(elements: Collection<T>): Boolean {
    super.clear()
    val result = super.addAll(elements)
    notifyChanged()
    return result
  }
}