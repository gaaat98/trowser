package com.android.trowser.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton

class CheckableImageButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs), Checkable {
    private val checkedStateSet = intArrayOf(attr.state_checked)
    private var mChecked = false

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
        refreshDrawableState()
    }

    override fun toggle() {
        mChecked = !mChecked
        refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, checkedStateSet)
        }
        return drawableState
    }
}