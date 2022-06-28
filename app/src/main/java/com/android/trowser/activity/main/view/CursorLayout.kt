package com.android.trowser.activity.main.view

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.android.trowser.utils.Utils
import com.android.trowser.activity.main.MainActivity


/**
 * Created by PDT on 25.08.2016.
 */
class CursorLayout : FrameLayout {
    companion object {
        private const val UNCHANGED = Integer.MIN_VALUE
        private const val CURSOR_DISAPPEAR_TIMEOUT = 5000
        private const val USE_SCROLL_HACK = true
        private const val SCROLL_HACK_PADDING = 300
    }

    private var cursorRadius: Int = 0
    private var cursorRadiusPressed: Int = 0
    private var maxCursorSpeed: Float = 0f
    private var scrollStartPadding = 100
    private var cursorStrokeWidth: Float = 0f
    private val cursorDirection = Point(0, 0)
    val cursorPosition = PointF(0f, 0f)
    private val cursorSpeed = PointF(0f, 0f)
    private val paint = Paint()
    private var lastCursorUpdate = System.currentTimeMillis() - CURSOR_DISAPPEAR_TIMEOUT
    private var dpadCenterPressed = false
    internal var tmpPointF = PointF()
    private var callback: Callback? = null
    private val cursorHideRunnable = Runnable { invalidate() }
    private var scrollHackStarted = false
    private val scrollHackCoords = PointF()
    private val scrollHackActiveRect = Rect()
    var fingerMode = false
    private lateinit var ma: MainActivity

    private val isCursorDissappear: Boolean
        get() {
            val newTime = System.currentTimeMillis()
            return newTime - lastCursorUpdate > CURSOR_DISAPPEAR_TIMEOUT
        }

    interface Callback {
        fun onUserInteraction()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }
        paint.isAntiAlias = true
        setWillNotDraw(false)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ma = context as MainActivity
        val display = wm.defaultDisplay
        val displaySize = Point()
        display.getSize(displaySize)
        cursorStrokeWidth = (displaySize.x / 400).toFloat()
        cursorRadius = displaySize.x / 110
        cursorRadiusPressed = cursorRadius + Utils.D2P(context, 5f).toInt()
        maxCursorSpeed = (displaySize.x / 25).toFloat()
        scrollStartPadding = displaySize.x / 15
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        callback?.onUserInteraction()
        return super.onInterceptTouchEvent(ev)
    }


    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (isInEditMode) {
            return
        }
        cursorPosition.set(w / 2.0f, h / 2.0f)
        scrollHackActiveRect.set(0, 0, width, height)
        scrollHackActiveRect.inset(SCROLL_HACK_PADDING, SCROLL_HACK_PADDING)
        postDelayed(cursorHideRunnable, CURSOR_DISAPPEAR_TIMEOUT.toLong())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        callback?.onUserInteraction()
        val keyCode = event.keyCode
        val action = event.action
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, UNCHANGED, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, UNCHANGED, false)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, UNCHANGED, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, UNCHANGED, false)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, UNCHANGED, -1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, UNCHANGED, 0, false)

                    // reaching the top toggles menu
                    val wv = getChildAt(0)
                    if (cursorPosition.y == 0f && wv != null && wv is WebViewEx && !wv.canScrollVertically(-1)) {
                        ma.toggleMenu()
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, UNCHANGED, 1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, UNCHANGED, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, -1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, -1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, 1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, 1, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                if (event.action == KeyEvent.ACTION_DOWN && !keyDispatcherState.isTracking(event)) {
                    if (fingerMode) {
                        exitFingerMode()
                        return false
                    } else {
                        keyDispatcherState.startTracking(event, this)
                        if (!isCursorDissappear) {
                            dpadCenterPressed = true
                            dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_DOWN)
                            postInvalidate()
                        }
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    keyDispatcherState.handleUpEvent(event)
                    //loadUrl("javascript:function simulateClick(x,y){var clickEvent=document.createEvent('MouseEvents');clickEvent.initMouseEvent('click',true,true,window,0,0,0,x,y,false,false,false,false,0,null);document.elementFromPoint(x,y).dispatchEvent(clickEvent)}simulateClick("+(int)cursorPosition.x+","+(int)cursorPosition.y+");");
                    // Obtain MotionEvent object
                    if (fingerMode) {
                        //nop
                    } else if (isCursorDissappear) {
                        lastCursorUpdate = System.currentTimeMillis()
                        postInvalidate()
                    } else {
                        dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_UP)
                        dpadCenterPressed = false
                        postInvalidate()
                    }
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun dispatchMotionEvent(x: Float, y: Float, action: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val properties = arrayOfNulls<MotionEvent.PointerProperties>(1)
        val pp1 = MotionEvent.PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = pp1
        val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(1)
        val pc1 = MotionEvent.PointerCoords()
        pc1.x = x
        pc1.y = y
        pc1.pressure = 1f
        pc1.size = 1f
        pointerCoords[0] = pc1
        val motionEvent = MotionEvent.obtain(downTime, eventTime,
                action, 1, properties,
                pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        dispatchTouchEvent(motionEvent)
    }

    private fun handleDirectionKeyEvent(event: KeyEvent, x: Int, y: Int, keyDown: Boolean) {
        lastCursorUpdate = System.currentTimeMillis()
        if (keyDown) {
            if (keyDispatcherState.isTracking(event)) {
                return
            }
            removeCallbacks(cursorUpdateRunnable)
            post(cursorUpdateRunnable)
            keyDispatcherState.startTracking(event, this)
        } else {
            keyDispatcherState.handleUpEvent(event)
            cursorSpeed.set(0f, 0f)
            if (scrollHackStarted) {
                dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_CANCEL)
                scrollHackStarted = false
            }
        }

        cursorDirection.set(if (x == UNCHANGED) cursorDirection.x else x, if (y == UNCHANGED) cursorDirection.y else y)
    }

    private fun scrollWebViewBy(wv: WebViewEx, scrollX: Int, scrollY: Int) {
        if (scrollX == 0 && scrollY == 0) {
            return
        }
        if ((scrollX != 0 && wv.canScrollHorizontally(scrollX)) || (scrollY != 0 && wv.canScrollVertically(scrollY))) {
            wv.scrollTo(wv.scrollX + scrollX, wv.scrollY + scrollY)
        } else if (USE_SCROLL_HACK && !dpadCenterPressed) {
            var justStarted = false
            if (!scrollHackStarted) {
                scrollHackCoords.set(
                        bound(cursorPosition.x, scrollHackActiveRect.left.toFloat(), scrollHackActiveRect.right.toFloat()),
                        bound(cursorPosition.y, scrollHackActiveRect.top.toFloat(), scrollHackActiveRect.bottom.toFloat()))
                dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_DOWN)
                scrollHackStarted = true
                justStarted = true
            }
            scrollHackCoords.x -= scrollX
            scrollHackCoords.y -= scrollY
            if (scrollHackCoords.x < scrollHackActiveRect.left || scrollHackCoords.x >= scrollHackActiveRect.right ||
                    scrollHackCoords.y < scrollHackActiveRect.top || scrollHackCoords.y >= scrollHackActiveRect.bottom) {
                scrollHackCoords.x += scrollX
                scrollHackCoords.y += scrollY
                dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_CANCEL)
                scrollHackStarted = false
                if (!justStarted) {
                    scrollWebViewBy(wv, scrollX, scrollY)
                }
                return
            }
            dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_MOVE)
        }
    }

    private fun bound(value: Float, max: Float): Float {
        return if (value > max) {
            max
        } else if (value < -max) {
            -max
        } else {
            value
        }
    }

    private fun bound(value: Float, min: Float, max: Float): Float {
        return if (value > max) {
            max
        } else if (value < min) {
            min
        } else {
            value
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isInEditMode) {
            return
        }

        if (fingerMode || !isCursorDissappear) {
            val cx = cursorPosition.x
            val cy = cursorPosition.y
            val radius = if (dpadCenterPressed) cursorRadiusPressed else cursorRadius

            paint.color = if (fingerMode)
                Color.argb(128, 200, 200, 255) else
                Color.argb(128, 255, 255, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, radius.toFloat(), paint)

            paint.color = Color.GRAY
            paint.strokeWidth = cursorStrokeWidth
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(cx, cy, radius.toFloat(), paint)

            if (fingerMode) {
                val halfRadius = radius.toFloat() / 2
                canvas.drawLine(cx - halfRadius, cy, cx + halfRadius, cy, paint)
                canvas.drawLine(cx, cy - halfRadius, cx, cy + halfRadius, paint)
            }
        }
    }

    fun goToFingerMode() {
        fingerMode = true
        postInvalidate()
    }

    fun exitFingerMode() {
        dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_UP)
        dpadCenterPressed = false
        fingerMode = false
        postInvalidate()
    }

    private val cursorUpdateRunnable = object : Runnable {
        override fun run() {
            removeCallbacks(cursorHideRunnable)

            val newTime = System.currentTimeMillis()
            val dTime = newTime - lastCursorUpdate
            lastCursorUpdate = newTime

            val accelerationFactor = 0.045f * dTime
            //float decelerationFactor = 1 - Math.min(0.5f, 0.005f * dTime);
            cursorSpeed.set(bound(cursorSpeed.x/* * decelerationFactor*/ + bound(cursorDirection.x.toFloat(), 1f) * accelerationFactor, maxCursorSpeed),
                    bound(cursorSpeed.y/* * decelerationFactor*/ + bound(cursorDirection.y.toFloat(), 1f) * accelerationFactor, maxCursorSpeed))
            if (Math.abs(cursorSpeed.x) < 0.1f) cursorSpeed.x = 0f
            if (Math.abs(cursorSpeed.y) < 0.1f) cursorSpeed.y = 0f
            if (cursorDirection.x == 0 && cursorDirection.y == 0 && cursorSpeed.x == 0f && cursorSpeed.y == 0f) {
                postDelayed(cursorHideRunnable, CURSOR_DISAPPEAR_TIMEOUT.toLong())
                return
            }
            tmpPointF.set(cursorPosition)
            cursorPosition.offset(cursorSpeed.x, cursorSpeed.y)
            if (cursorPosition.x < 0) {
                cursorPosition.x = 0f
            } else if (cursorPosition.x > width - 1) {
                cursorPosition.x = (width - 1).toFloat()
            }
            if (cursorPosition.y < 0) {
                cursorPosition.y = 0f
            } else if (cursorPosition.y > height - 1) {
                cursorPosition.y = (height - 1).toFloat()
            }
            if (tmpPointF != cursorPosition) {
                if (dpadCenterPressed) {
                    dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_MOVE)
                } else {
                    //dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_HOVER_MOVE);
                }
            }

            var dx = 0
            var dy = 0
            if (cursorPosition.y > height - scrollStartPadding) {
                if (cursorSpeed.y > 0) {
                    dy = cursorSpeed.y.toInt()
                }
            } else if (cursorPosition.y < scrollStartPadding) {
                if (cursorSpeed.y < 0) {
                    dy = cursorSpeed.y.toInt()
                }
            }
            if (cursorPosition.x > width - scrollStartPadding) {
                if (cursorSpeed.x > 0) {
                    dx = cursorSpeed.x.toInt()
                }
            } else if (cursorPosition.x < scrollStartPadding) {
                if (cursorSpeed.x < 0) {
                    dx = cursorSpeed.x.toInt()
                }
            }
            if (dx != 0 || dy != 0) {
                val child = getChildAt(0)
                if (child != null && child is WebViewEx) {
                        scrollWebViewBy(child, dx, dy)
                }
            }

            invalidate()
            post(this)
        }
    }
}
