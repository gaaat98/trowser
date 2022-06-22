package com.android.trowser.activity.main

import android.view.ViewGroup
import com.android.trowser.Trowser
import com.android.trowser.activity.main.view.WebViewEx
import com.android.trowser.model.WebTabState
import com.android.trowser.singleton.AppDatabase
import com.android.trowser.utils.LogUtils
import com.android.trowser.utils.Utils
import com.android.trowser.utils.activemodel.ActiveModel
import com.android.trowser.utils.observable.ObservableList
import com.android.trowser.utils.observable.ObservableValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class TabsModel : ActiveModel() {
  companion object {
    var TAG: String = TabsModel::class.java.simpleName
  }

  var loaded = false
  val currentTab = ObservableValue<WebTabState?>(null)
  val tabsStates = ObservableList<WebTabState>()
  private val config = Trowser.config
  private var incognitoMode = config.incognitoMode

  init {
      tabsStates.subscribe({
        //auto-update positions on any list change
        var positionsChanged = false
        tabsStates.forEachIndexed { index, webTabState ->
          if (webTabState.position != index) {
            webTabState.position = index
            positionsChanged = true
          }
        }
        if (positionsChanged) {
          val tabsListClone = ArrayList(tabsStates)
          modelScope.launch {
            val tabsDao = AppDatabase.db.tabsDao()
            tabsDao.updatePositions(tabsListClone)
          }
        }
      }, false)
  }

  fun loadState() = modelScope.launch(Dispatchers.Main) {
    if (loaded) {
      //check is incognito mode changed
      if (incognitoMode != config.incognitoMode) {
        incognitoMode = config.incognitoMode
        loaded = false
      } else {
        return@launch
      }
    }
    val tabsDao = AppDatabase.db.tabsDao()
    tabsStates.replaceAll(tabsDao.getAll(config.incognitoMode))
    loaded = true
  }

  fun saveTab(tab: WebTabState) = modelScope.launch(Dispatchers.Main) {
    val tabsDB = AppDatabase.db.tabsDao()
    if (tab.selected) {
      tabsDB.unselectAll(config.incognitoMode)
    }
    tab.saveWebViewStateToFile()
    if (tab.id != 0L) {
      tabsDB.update(tab)
    } else {
      tab.id = tabsDB.insert(tab)
    }
  }

  fun onCloseTab(tab: WebTabState) {
    tabsStates.remove(tab)
    modelScope.launch(Dispatchers.Main) {
      val tabsDB = AppDatabase.db.tabsDao()
      tabsDB.delete(tab)
      launch { tab.removeFiles() }
    }
  }

  fun onCloseAllTabs() = modelScope.launch(Dispatchers.Main) {
    val tabsClone = ArrayList(tabsStates)
    tabsStates.clear()
    val tabsDB = AppDatabase.db.tabsDao()
    tabsDB.deleteAll(config.incognitoMode)
    withContext(Dispatchers.IO) {
      tabsClone.forEach { it.removeFiles() }
    }
  }

  fun onDetachActivity() {
    for (tab in tabsStates) {
      tab.recycleWebView()
    }
  }

  fun changeTab(newTab: WebTabState, webViewProvider: (tab: WebTabState) -> WebViewEx?, webViewParent: ViewGroup) {
    if (currentTab.value == newTab && newTab.webView != null) return
    tabsStates.forEach {
      it.selected = false
    }
    currentTab.value?.apply {
      webView?.apply {
        onPause()
        webViewParent.removeView(this)
      }
      onPause()
      saveTab(this)
    }

    newTab.selected = true
    currentTab.value = newTab
    var wv = newTab.webView
    if (wv == null) {
      wv = webViewProvider(newTab)
      if (wv == null) {
        return
      }
      newTab.restoreWebView()
      webViewParent.addView(newTab.webView)
    } else {
      (wv.parent as? ViewGroup)?.removeView(wv)
      webViewParent.addView(wv)
      wv.onResume()
    }
    wv.setNetworkAvailable(Utils.isNetworkConnected(Trowser.instance))
  }
}