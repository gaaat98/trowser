package com.android.trowser.activity.main.dialogs.settings

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.fedir.segmentedbutton.SegmentedButton
import com.android.trowser.R
import com.android.trowser.activity.main.SettingsModel
import com.android.trowser.widgets.SegmentedButtonTabsAdapter

class SettingsDialog(context: Context, val model: SettingsModel) : Dialog(context), DialogInterface.OnDismissListener, VersionSettingsView.Callback {
    private var mainView: MainSettingsView? = null
    private var sbTabs: SegmentedButton

    init {
        setTitle(R.string.settings)
        setContentView(R.layout.dialog_settings)

        sbTabs = findViewById(R.id.sbTabs)

        val tabContentAdapter = object : SegmentedButtonTabsAdapter(sbTabs, findViewById(R.id.flTabsContent)) {
            override fun createContentViewForSegmentButtonId(id: Int): View {
                return when (id) {
                    R.id.btnMainTab -> {
                        mainView = MainSettingsView(context)
                        mainView!!
                    }
                    R.id.btnShortcutsTab -> ShortcutsSettingsView(context)
                    else -> {
                        val view = VersionSettingsView(context)
                        view.callback = this@SettingsDialog
                        view
                    }
                }
            }
        }

        setOnDismissListener(this)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        mainView?.save()
    }

    override fun onNeedToCloseSettings() {
        dismiss()
    }
}