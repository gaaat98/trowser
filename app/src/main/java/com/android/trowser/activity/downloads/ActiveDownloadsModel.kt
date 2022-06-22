package com.android.trowser.activity.downloads

import com.android.trowser.model.Download
import com.android.trowser.service.downloads.DownloadTask
import com.android.trowser.singleton.AppDatabase
import com.android.trowser.utils.observable.ObservableList
import com.android.trowser.utils.activemodel.ActiveModel
import java.io.File

class ActiveDownloadsModel: ActiveModel() {
    val activeDownloads = ObservableList<DownloadTask>()
    private val listeners = java.util.ArrayList<Listener>()

    interface Listener {
        fun onDownloadUpdated(downloadInfo: Download)
        fun onDownloadError(downloadInfo: Download, responseCode: Int, responseMessage: String)
        fun onAllDownloadsComplete()
    }

    suspend fun deleteItem(download: Download) {
        File(download.filepath).delete()
        AppDatabase.db.downloadDao().delete(download)
    }

    fun registerListener(listener: Listener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun cancelDownload(download: Download) {
        for (i in activeDownloads.indices) {
            val task = activeDownloads[i]
            if (task.downloadInfo.id == download.id) {
                task.downloadInfo.cancelled = true
                break
            }
        }
    }

    fun notifyListenersAboutError(task: DownloadTask, responseCode: Int, responseMessage: String) {
        for (i in listeners.indices) {
            listeners[i].onDownloadError(task.downloadInfo, responseCode, responseMessage)
        }
    }

    fun notifyListenersAboutDownloadProgress(task: DownloadTask) {
        for (i in listeners.indices) {
            listeners[i].onDownloadUpdated(task.downloadInfo)
        }
    }

    fun onDownloadEnded(task: DownloadTask) {
        activeDownloads.remove(task)
        if (activeDownloads.isEmpty()) {
            for (i in listeners.indices) {
                listeners[i].onAllDownloadsComplete()
            }
        }
    }
}