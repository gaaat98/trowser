package com.android.trowser.activity.downloads

import com.android.trowser.model.Download
import com.android.trowser.singleton.AppDatabase
import com.android.trowser.utils.observable.ObservableValue
import com.android.trowser.utils.activemodel.ActiveModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadsHistoryModel: ActiveModel() {
  val allItems = ArrayList<Download>()
  val lastLoadedItems = ObservableValue<List<Download>>(ArrayList())
  private var loading = false

  fun loadNextItems() = modelScope.launch(Dispatchers.Main) {
    if (loading) {
      return@launch
    }
    loading = true

    val newItems = AppDatabase.db.downloadDao().allByLimitOffset(allItems.size.toLong())
    lastLoadedItems.value = newItems
    allItems.addAll(newItems)

    loading = false
  }
}