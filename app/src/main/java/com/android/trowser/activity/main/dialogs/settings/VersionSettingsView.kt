package com.android.trowser.activity.main.dialogs.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.webkit.WebViewCompat
import com.android.trowser.BuildConfig
import com.android.trowser.R
import com.android.trowser.activity.IncognitoModeMainActivity
import com.android.trowser.activity.main.MainActivity
import com.android.trowser.activity.main.SettingsModel
import com.android.trowser.databinding.ViewSettingsVersionBinding
import com.android.trowser.utils.activemodel.ActiveModelsRepository
import com.android.trowser.utils.activity

@SuppressLint("SetTextI18n")
class VersionSettingsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {
    private var vb = ViewSettingsVersionBinding.inflate(LayoutInflater.from(getContext()), this, true)
    var settingsModel = ActiveModelsRepository.get(SettingsModel::class, activity!!)
    var callback: Callback? = null

    private val updateChannelSelectedListener: AdapterView.OnItemSelectedListener

    interface Callback {
        fun onNeedToCloseSettings()
    }

    init {
        vb.tvVersion.text = context.getString(R.string.version_s, BuildConfig.VERSION_NAME)

        val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
        val webViewVersion = (webViewPackage?.packageName ?: "unknown") + ":" + (webViewPackage?.versionName ?: "unknown")
        vb.tvWebViewVersion.text = webViewVersion

        vb.tvLink.text = Html.fromHtml("<p><u>https://github.com/truefedex/tv-bro</u></p>")
        vb.tvLink.setOnClickListener {
            loadUrl(vb.tvLink.text.toString())
        }

        vb.tvUkraine.setOnClickListener {
            loadUrl("https://tv-bro-3546c.web.app/msg001.html")
        }

        vb.chkAutoCheckUpdates.isChecked = settingsModel.needAutockeckUpdates

        vb.chkAutoCheckUpdates.setOnCheckedChangeListener { buttonView, isChecked ->
            settingsModel.saveAutoCheckUpdates(isChecked)

            updateUIVisibility()
        }

        updateChannelSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedChannel = settingsModel.updateChecker.versionCheckResult!!.availableChannels[position]
                if (selectedChannel == settingsModel.updateChannel) return
                settingsModel.saveUpdateChannel(selectedChannel)
                settingsModel.checkUpdate(true) {
                    updateUIVisibility()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        vb.spUpdateChannel.onItemSelectedListener = updateChannelSelectedListener

        vb.btnUpdate.setOnClickListener {
            callback?.onNeedToCloseSettings()
            settingsModel.showUpdateDialogIfNeeded(activity as MainActivity, true)
        }

        updateUIVisibility()
    }

    private fun loadUrl(url: String) {
        callback?.onNeedToCloseSettings()
        val activityClass = if (settingsModel.config.incognitoMode)
            IncognitoModeMainActivity::class.java else MainActivity::class.java
        val intent = Intent(activity, activityClass)
        intent.data = Uri.parse(url)
        activity?.startActivity(intent)
    }

    private fun updateUIVisibility() {
        if (settingsModel.updateChecker.versionCheckResult == null) {
            settingsModel.checkUpdate(false) {
                if (settingsModel.updateChecker.versionCheckResult != null) {
                    updateUIVisibility()
                }
            }
            return
        }

        vb.tvUpdateChannel.visibility = if (settingsModel.needAutockeckUpdates) View.VISIBLE else View.INVISIBLE
        vb.spUpdateChannel.visibility = if (settingsModel.needAutockeckUpdates) View.VISIBLE else View.INVISIBLE

        if (settingsModel.needAutockeckUpdates) {
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item,
                    settingsModel.updateChecker.versionCheckResult!!.availableChannels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            vb.spUpdateChannel.adapter = adapter
            val selected = settingsModel.updateChecker.versionCheckResult!!.availableChannels.indexOf(settingsModel.updateChannel)
            if (selected != -1) {
                vb.spUpdateChannel.onItemSelectedListener = null
                vb.spUpdateChannel.setSelection(selected)
                vb.spUpdateChannel.onItemSelectedListener = updateChannelSelectedListener
            }
        }

        val hasUpdate = settingsModel.updateChecker.hasUpdate()
        vb.tvNewVersion.visibility = if (settingsModel.needAutockeckUpdates && hasUpdate) View.VISIBLE else View.INVISIBLE
        vb.btnUpdate.visibility = if (settingsModel.needAutockeckUpdates && hasUpdate) View.VISIBLE else View.INVISIBLE
        if (hasUpdate) {
            vb.tvNewVersion.text = context.getString(R.string.new_version_available_s, settingsModel.updateChecker.versionCheckResult!!.latestVersionName)
        }
    }
}