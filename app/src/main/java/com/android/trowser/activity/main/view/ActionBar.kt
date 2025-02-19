package com.android.trowser.activity.main.view

import android.app.Activity
import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnKeyListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.android.trowser.R
import com.android.trowser.Trowser
import com.android.trowser.activity.downloads.ActiveDownloadsModel
import com.android.trowser.databinding.ViewActionbarBinding
import com.android.trowser.utils.Utils
import com.android.trowser.utils.activemodel.ActiveModelsRepository

class ActionBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val vb = ViewActionbarBinding.inflate( LayoutInflater.from(context),this)
    var callback: Callback? = null
    private var downloadAnimation: Animation? = null
    private var downloadsModel = ActiveModelsRepository.get(ActiveDownloadsModel::class, context as Activity)
    private var extendedAddressBarMode = false

    interface Callback {
        fun closeWindow()
        fun showDownloads()
        fun showFavorites()
        fun showHistory()
        fun showSettings()
        fun initiateVoiceSearch()
        fun search(text: String)
        fun onExtendedAddressBarMode()
        fun onUrlInputDone()
        fun toggleIncognitoMode()
    }

    private val etUrlFocusChangeListener = OnFocusChangeListener { _, focused ->
        if (focused) {
            enterExtendedAddressBarMode()

            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(vb.etUrl, InputMethodManager.SHOW_FORCED)
            handler.postDelayed(//workaround an android TV bug
                {
                    vb.etUrl.selectAll()
                }, 100)
        }
    }


    private val etUrlKeyListener = OnKeyListener { _, _, keyEvent ->
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(vb.etUrl.windowToken, 0)
                    callback?.search(vb.etUrl.text.toString())
                    dismissExtendedAddressBarMode()
                    callback?.onUrlInputDone()
                }
                return@OnKeyListener true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    callback?.onUrlInputDone()
                }
                return@OnKeyListener true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (keyEvent.action == KeyEvent.ACTION_UP && extendedAddressBarMode) {
                    dismissExtendedAddressBarMode()
                    vb.flUrl.requestFocus()
                }
                return@OnKeyListener true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // going back when cursor is at position zero, no text is selected and we press left again
                if (keyEvent.action == KeyEvent.ACTION_DOWN && extendedAddressBarMode && vb.etUrl.selectionEnd == 0 && vb.etUrl.selectionEnd == vb.etUrl.selectionStart) {
                    dismissExtendedAddressBarMode()
                    catchFocus()
                    return@OnKeyListener true
                }
            }

        }
        false
    }

    init {
        orientation = HORIZONTAL

        val incognitoMode = Trowser.config.incognitoMode

        vb.ibMenu.setOnClickListener { callback?.closeWindow() }
        vb.ibDownloads.setOnClickListener { callback?.showDownloads() }
        vb.ibFavorites.setOnClickListener { callback?.showFavorites() }
        vb.ibHistory.setOnClickListener { callback?.showHistory() }
        vb.ibIncognito.setOnClickListener { callback?.toggleIncognitoMode() }
        vb.ibSettings.setOnClickListener { callback?.showSettings() }

        vb.flUrl.setOnClickListener { vb.etUrl.requestFocus() }

        if (Utils.isFireTV(context)) {
            vb.ibMenu.nextFocusRightId = R.id.ibHistory
            removeView(vb.ibVoiceSearch)
        } else {
            vb.ibVoiceSearch.setOnClickListener { callback?.initiateVoiceSearch() }
        }

        vb.ibIncognito.isChecked = incognitoMode

        vb.etUrl.onFocusChangeListener = etUrlFocusChangeListener

        vb.etUrl.setOnKeyListener(etUrlKeyListener)


        downloadsModel.activeDownloads.subscribe(context as AppCompatActivity) {
            if (it.isNotEmpty()) {
                if (downloadAnimation == null) {
                    downloadAnimation = AnimationUtils.loadAnimation(context, R.anim.infinite_fadeinout_anim)
                    vb.ibDownloads.startAnimation(downloadAnimation)
                }
            } else {
                downloadAnimation?.apply {
                    this.reset()
                    vb.ibDownloads.clearAnimation()
                    downloadAnimation = null
                }
            }
        }
    }

    fun setAddressBoxText(text: String) {
        vb.etUrl.setText(text)
    }

    fun setAddressBoxTextColor(color: Int) {
        vb.etUrl.setTextColor(color)
    }

    private fun enterExtendedAddressBarMode() {
        if (extendedAddressBarMode) return
        extendedAddressBarMode = true

        for (c in children) {
            if (c is ImageButton)
                c.visibility = GONE
            else if(c is LinearLayout){
                c.children.forEach { if(it is ImageButton) it.visibility = GONE }
            }
        }

        TransitionManager.beginDelayedTransition(this)
        callback?.onExtendedAddressBarMode()
    }

    fun dismissExtendedAddressBarMode() {
        if (!extendedAddressBarMode) return
        extendedAddressBarMode = false

        for (c in children) {
            if (c is ImageButton)
                c.visibility = VISIBLE
            else if(c is LinearLayout){
                c.children.forEach { if(it is ImageButton) it.visibility = VISIBLE }
            }
        }

        TransitionManager.beginDelayedTransition(this)
    }

    fun isExtendedAddressBarMode(): Boolean {
        return extendedAddressBarMode
    }

    fun catchFocus() {
        vb.ibMenu.requestFocus()
    }
}