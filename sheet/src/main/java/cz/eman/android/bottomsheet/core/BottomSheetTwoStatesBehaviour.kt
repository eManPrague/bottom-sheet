/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.eman.android.bottomsheet.core

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.ParcelableCompat
import androidx.core.os.ParcelableCompatCreatorCallbacks
import androidx.customview.view.AbsSavedState
import androidx.core.view.NestedScrollingChild
import androidx.core.view.VelocityTrackerCompat
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.*
import cz.eman.android.bottomsheet.R
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max


/**
 * An interaction behavior plugin for a child view of [CoordinatorLayout] to make it work as
 * a bottom sheet.
 */
class BottomSheetTwoStatesBehaviour<V : View>(context: Context, attrs: AttributeSet)
    : CoordinatorLayout.Behavior<V>(context, attrs), BottomSheet {

    private var initialHeight: Int = 0

    private val maximumVelocity: Float

    @get:VisibleForTesting
    internal var peekHeightMin: Int = 0
        private set

    private var peekHeight: Int = 0
    private var peekHeightSmall: Int = 0
    private var peekHeightBig: Int = 0
    private var peekHeightAuto: Boolean = false

    var mMinOffset: Int = 0
    var maxOffset: Int = 0

    private var hideable: Boolean = false

    /**
     * Describes whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    var skipCollapsed: Boolean = false

    private var state = BottomSheetState.STATE_COLLAPSED

    internal var viewDragHelper: ViewDragHelper? = null

    private var ignoreEvents: Boolean = false

    private var lastNestedScrollDy: Int = 0

    private var nestedScrolled: Boolean = false

    internal var mParentHeight: Int = 0

    internal var viewRef: WeakReference<V>? = null

    internal var nestedScrollingChildRef: WeakReference<View>? = null

    private var callback: BottomSheetCallback? = null

    private var velocityTracker: VelocityTracker? = null

    internal var activePointerId: Int = 0

    private var initialY: Int = 0

    internal var touchingScrollingChild: Boolean = false
    internal var dragEnabled: Boolean = false

    private val yVelocity: Float
        get() {
            velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity)
            return VelocityTrackerCompat.getYVelocity(velocityTracker!!, activePointerId)
        }

    private val mDragCallback = object : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (state === BottomSheetState.STATE_DRAGGING) {
                return false
            }
            if (touchingScrollingChild) {
                return false
            }
            if (state === BottomSheetState.STATE_EXPANDED && activePointerId == pointerId) {
                val scroll = nestedScrollingChildRef?.get()
                if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                    // Let the content scroll up
                    return false
                }
            }
            return viewRef != null && viewRef!!.get() === child
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            dispatchOnSlide(top)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top: Int
            val currentTop = releasedChild.top
            val pxFromBottom = releasedChild.height - currentTop

            val targetState: BottomSheetState
            if (yvel < 0) { // Moving up

                if (pxFromBottom > peekHeightBig) {
                    top = mMinOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                } else {
                    top = releasedChild.height - peekHeightBig
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                top = mParentHeight
                targetState = BottomSheetState.STATE_HIDDEN
            } else if (yvel == 0f) {

                if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                    top = releasedChild.height - peekHeightSmall
                    targetState = BottomSheetState.STATE_COLLAPSED
                } else if (pxFromBottom < peekHeightBig) {
                    top = releasedChild.height - peekHeightBig
                    targetState = BottomSheetState.STATE_COLLAPSED
                } else {
                    top = mMinOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                }
            } else {
                if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                    top = maxOffset
                    targetState = BottomSheetState.STATE_COLLAPSED
                } else {
                    top = releasedChild.height - peekHeightBig
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
            }

            if (viewDragHelper!!.settleCapturedViewAt(releasedChild.left, top)) {
                setStateInternal(BottomSheetState.STATE_SETTLING)
                ViewCompat.postOnAnimation(releasedChild,
                        SettleRunnable(releasedChild, targetState))
            } else {
                setStateInternal(targetState)
            }
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return constrain(top, mMinOffset, if (hideable) mParentHeight else maxOffset)
        }

        internal fun constrain(amount: Int, low: Int, high: Int): Int {
            return if (amount < low) low else if (amount > high) high else amount
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return if (hideable) {
                mParentHeight - mMinOffset
            } else {
                maxOffset - mMinOffset
            }
        }
    }

    init {
        val attr = context.obtainStyledAttributes(attrs,
                R.styleable.BottomSheetBehavior_Layout)
        val value = attr.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight)
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data)
        } else {
            setPeekHeight(attr.getDimensionPixelSize(
                    R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO))
        }
        setHideable(attr.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false))
        skipCollapsed = attr.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed,
                false)
        attr.recycle()
        val configuration = ViewConfiguration.get(context)
        maximumVelocity = configuration.scaledMaximumFlingVelocity.toFloat()
        dragEnabled = true
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable? {
        super.onSaveInstanceState(parent, child)?.run {
            return SavedState(this, state, peekHeightBig, peekHeightSmall, initialHeight)
        }
        return null
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(parent, child, savedState.superState!!)

        // Intermediate states are restored as collapsed state
        this.state = if (savedState.state === BottomSheetState.STATE_DRAGGING
                || savedState.state === BottomSheetState.STATE_SETTLING) {
            BottomSheetState.STATE_COLLAPSED
        } else {
            savedState.state
        }

        peekHeightBig = savedState.peekHeightBig
        peekHeightSmall = savedState.peekHeightSmall
        peekHeight = savedState.peekHeightSmall
        initialHeight = savedState.peekHeight
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.fitsSystemWindows = true
        }

        val savedTop = child.top
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection)
        // Offset the bottom sheet
        mParentHeight = parent.height

        val peekHeight: Int
        if (peekHeightAuto) {
            if (peekHeightMin == 0) {
                peekHeightMin = parent.resources.getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min)
            }
            peekHeight = max(peekHeightMin, mParentHeight - parent.width * 9 / 16)
        } else {
            peekHeight = this.peekHeight
        }
        mMinOffset = max(0, mParentHeight - child.height)
        maxOffset = max(mParentHeight - peekHeight, mMinOffset)

        if (state === BottomSheetState.STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMinOffset)
        } else if (hideable && state === BottomSheetState.STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, mParentHeight)
        } else if (state === BottomSheetState.STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, if (initialHeight == 0) maxOffset else mParentHeight - initialHeight)
        } else if (state === BottomSheetState.STATE_DRAGGING || state === BottomSheetState.STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.top)
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, mDragCallback)
        }
        viewRef = WeakReference(child)
        nestedScrollingChildRef = WeakReference<View>(findScrollingChild(child))
        return true
    }

    private fun findScrollingChild(view: View): View? {
        if (view is NestedScrollingChild) {
            return view
        }
        if (view is ViewGroup) {
            var i = 0
            val count = view.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(view.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }
        return null
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (!child.isShown || !dragEnabled) {
            ignoreEvents = true
            return false
        }
        val action = event.action
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val initialX = event.x.toInt()
                initialY = event.y.toInt()
                val scroll = nestedScrollingChildRef?.get()
                if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                    activePointerId = event.getPointerId(event.actionIndex)
                    touchingScrollingChild = true
                }
                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID && !parent.isPointInChildBounds(child, initialX, initialY)
            }
        }
        if (!ignoreEvents && viewDragHelper!!.shouldInterceptTouchEvent(event)) {
            return true
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        val scroll = nestedScrollingChildRef?.get()
        return action == MotionEvent.ACTION_MOVE && scroll != null &&
                !ignoreEvents && state !== BottomSheetState.STATE_DRAGGING &&
                !parent.isPointInChildBounds(scroll, event.x.toInt(), event.y.toInt()) &&
                abs(initialY - event.y) > viewDragHelper!!.touchSlop
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (!child.isShown || !dragEnabled) {
            return false
        }
        val action = event.action
        if (state === BottomSheetState.STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (viewDragHelper != null) {
            viewDragHelper!!.processTouchEvent(event)
            // Record the velocity
            if (action == MotionEvent.ACTION_DOWN) {
                reset()
            }
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker!!.addMovement(event)
            // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
            // to capture the bottom sheet in case it is not captured and the touch slop is passed.
            if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
                if (abs(initialY - event.y) > viewDragHelper!!.touchSlop) {
                    viewDragHelper!!.captureChildView(child, event.getPointerId(event.actionIndex))
                }
            }
        }
        return !ignoreEvents
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V,
                                     directTargetChild: View, target: View, nestedScrollAxes: Int): Boolean {
        lastNestedScrollDy = 0
        nestedScrolled = false
        return nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0 && dragEnabled
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int,
                                   dy: Int, consumed: IntArray) {
        val scrollingChild = nestedScrollingChildRef?.get()
        if (target !== scrollingChild) {
            return
        }
        val currentTop = child.top
        val newTop = currentTop - dy
        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {
                consumed[1] = currentTop - mMinOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(BottomSheetState.STATE_EXPANDED)
            } else {
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(BottomSheetState.STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            if (target.canScrollVertically(-1)) {
                if (newTop <= maxOffset || hideable) {
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setStateInternal(BottomSheetState.STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - maxOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setStateInternal(BottomSheetState.STATE_COLLAPSED)
                }
            }
        }
        dispatchOnSlide(child.top)
        lastNestedScrollDy = dy
        nestedScrolled = true
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View) {
        if (child.top == mMinOffset) {
            setStateInternal(BottomSheetState.STATE_EXPANDED)
            return
        }
        if (target !== nestedScrollingChildRef?.get() || !nestedScrolled) {
            return
        }
        val top: Int

        val currentTop = child.top
        val pxFromBottom = child.height - currentTop

        val targetState: BottomSheetState
        if (lastNestedScrollDy > 0) { // Moving up

            if (pxFromBottom > peekHeightBig) {
                top = mMinOffset
                targetState = BottomSheetState.STATE_EXPANDED
            } else {
                top = child.height - peekHeightBig
                targetState = BottomSheetState.STATE_COLLAPSED
            }
        } else if (hideable && shouldHide(child, yVelocity)) {
            top = mParentHeight
            targetState = BottomSheetState.STATE_HIDDEN
        } else if (lastNestedScrollDy.toFloat() == 0f) {
            when {
                pxFromBottom in 1 until peekHeightBig -> {
                    top = child.height - peekHeightSmall
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
                pxFromBottom < peekHeightBig -> {
                    top = child.height - peekHeightBig
                    targetState = BottomSheetState.STATE_COLLAPSED
                }
                else -> {
                    top = mMinOffset
                    targetState = BottomSheetState.STATE_EXPANDED
                }
            }
        } else {
            if (pxFromBottom in 1 until peekHeightBig) {
                top = maxOffset
                targetState = BottomSheetState.STATE_COLLAPSED
            } else {
                top = child.height - peekHeightBig
                targetState = BottomSheetState.STATE_COLLAPSED
            }
        }

        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, top)) {
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, targetState))
        } else {
            setStateInternal(targetState)
        }

        nestedScrolled = false
    }


    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View,
                                  velocityX: Float, velocityY: Float): Boolean {
        return target === nestedScrollingChildRef?.get() && (
                state !== BottomSheetState.STATE_EXPANDED
                        || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
                )
    }

    override fun setPeekHeights(smaller: Int, bigger: Int) {
        setPeekHeights(smaller, bigger, true)
    }

    override fun setPeekHeights(smaller: Int, bigger: Int, affectInitHeight: Boolean) {
        setPeekHeight(smaller)

        peekHeightSmall = smaller
        peekHeightBig = bigger

        // Initial height cannot be smaller than collapsed state
        if (affectInitHeight) {
            if (initialHeight < smaller) {
                initialHeight = smaller
                viewRef?.let {
                    it.get()?.run { maxOffset = this.height - smaller }
                }
            } else {
                initialHeight = 0
            }
        }
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or
     * [PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically at 16:9 ratio keyline.
     *
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun setPeekHeight(peekHeight: Int) {
        var layout = false
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true
                layout = true
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false
            this.peekHeight = max(0, peekHeight)
            //            maxOffset = mParentHeight - peekHeight;
            layout = true
        }
        if (layout && state === BottomSheetState.STATE_COLLAPSED && viewRef != null) {
            val view = viewRef!!.get()
            view?.requestLayout()
        }
    }

    fun setPeekHeightSemiCollapsed(bigger: Int) {
        peekHeightBig = bigger
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet in pixels, or [PEEK_HEIGHT_AUTO]
     * if the sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun getPeekHeight(): Int {
        return if (peekHeightAuto) PEEK_HEIGHT_AUTO else peekHeight
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable true to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    override fun setHideable(hideable: Boolean) {
        this.hideable = hideable
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return `true` if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    fun isHideable(): Boolean {
        return hideable
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    override fun setBottomSheetCallback(callback: BottomSheetCallback?) {
        this.callback = callback
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of [BottomSheetState.STATE_COLLAPSED], [BottomSheetState.STATE_EXPANDED], or
     * [BottomSheetState.STATE_HIDDEN].
     */
    override fun setState(state: BottomSheetState) {
        if (state === this.state && state !== BottomSheetState.STATE_COLLAPSED) {
            return
        }
        if (viewRef == null) {
            // The view is not laid out yet; modify state and let onLayoutChild handle it later
            if (state === BottomSheetState.STATE_COLLAPSED
                    || state === BottomSheetState.STATE_EXPANDED
                    || hideable && state === BottomSheetState.STATE_HIDDEN) {
                this.state = state
            }
            return
        }
        val child = viewRef!!.get() ?: return

        // Start the animation; wait until a pending layout if there is one.
        val parent = child.parent
        if (parent != null && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)) {
            child.post { startSettlingAnimation(child, state) }
        } else {
            startSettlingAnimation(child, state)
        }
    }

    override fun animateAndSetHeights(pixels: Int) {
        animateToPixels(pixels, pixels, pixels, true)
    }

    override fun animateAndSetHeights(pixels: Int, collapsedSmall: Int, collapsedBig: Int) {
        animateToPixels(pixels, collapsedSmall, collapsedBig, true)
    }

    private fun animateToPixels(pixels: Int, collapsedSmall: Int, collapsedBig: Int, setHeights: Boolean) {
        viewRef?.get()?.let { child ->
            // The view is not laid out yet; modify state and let onLayoutChild handle it later

            val parent = child.parent
            if (parent != null && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)) {
                child.post { startAnimation(child, pixels, collapsedSmall, collapsedBig, setHeights) }
            } else {
                startAnimation(child, pixels, collapsedSmall, collapsedBig, setHeights)
            }
        }
    }

    internal fun startAnimation(child: View, pixels: Int, collapsedSmall: Int, collapsedBig: Int, setHeights: Boolean) {
        if (viewDragHelper!!.smoothSlideViewTo(child, child.left, mParentHeight - pixels, 300)) {
            setStateInternal(BottomSheetState.STATE_SETTLING)
            ViewCompat.postOnAnimation(child) {
                SettleRunnable(child, BottomSheetState.STATE_COLLAPSED).run()
                if (setHeights) {
                    setPeekHeights(collapsedSmall, collapsedBig)
                }
            }
        } else if (setHeights) {
            setStateInternal(BottomSheetState.STATE_COLLAPSED)
            setPeekHeights(collapsedSmall, collapsedBig, false)
            setInitialHeight(pixels)
        }
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of [BottomSheetState.STATE_EXPANDED], [BottomSheetState.STATE_COLLAPSED],
     * [BottomSheetState.STATE_DRAGGING], and [BottomSheetState.STATE_SETTLING].
     */
    override fun getState(): BottomSheetState {
        return state
    }

    internal fun setStateInternal(state: BottomSheetState) {
        if (this.state === state) {
            return
        }
        this.state = state
        val bottomSheet = viewRef?.get()
        if (bottomSheet != null && callback != null) {
            callback!!.onStateChanged(bottomSheet, state)
        }
    }

    private fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        if (velocityTracker != null) {
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    internal fun shouldHide(child: View, yVel: Float): Boolean {
        if (skipCollapsed) {
            return true
        }
        if (child.top < maxOffset) {
            // It should not hide, but collapse.
            return false
        }
        val newTop = child.top + yVel * HIDE_FRICTION
        return abs(newTop - maxOffset) / peekHeight.toFloat() > HIDE_THRESHOLD
    }

    internal fun startSettlingAnimation(child: View?, state: BottomSheetState) {
        var top: Int
        if (state === BottomSheetState.STATE_COLLAPSED) {
            top = child!!.height - peekHeightBig

            // Collapse to smaller peek height if possible
            if (viewRef!!.get() != null && viewRef!!.get()?.top == top) {
                top = child.height - peekHeightSmall
            }
        } else if (state === BottomSheetState.STATE_EXPANDED) {
            top = mMinOffset
        } else if (hideable && state === BottomSheetState.STATE_HIDDEN) {
            top = mParentHeight
        } else {
            return
            // throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(BottomSheetState.STATE_SETTLING)
        if (viewDragHelper!!.smoothSlideViewTo(child, child!!.left, top, 300)) {
            ViewCompat.postOnAnimation(child, SettleRunnable(child, state))
        }
    }

    internal fun dispatchOnSlide(top: Int) {
        val bottomSheet = viewRef?.get()
        if (bottomSheet != null && callback != null) {
            val maxOffset = max(mParentHeight - peekHeightBig, mParentHeight - peekHeightMin)
            if (top > maxOffset) {
                callback!!.onSlide(bottomSheet, (maxOffset - top).toFloat() / (mParentHeight - maxOffset))
            } else {
                callback!!.onSlide(bottomSheet,
                        (this.maxOffset - top).toFloat() / (this.maxOffset - mMinOffset))
            }
        }
    }

    override fun getPeekHeightCollapsed(): Int {
        return peekHeightSmall
    }

    override fun getPeekHeightSemiCollapsed(): Int {
        return peekHeightBig
    }

    override fun setInitialHeight(initialHeight: Int) {
        this.initialHeight = initialHeight
    }

    override fun setAutoInitHeight() {
        if (viewRef != null) {
            val child = viewRef?.get()
            if (child != null && state === BottomSheetState.STATE_COLLAPSED) {
                val bigTop = child.height - peekHeightBig

                if (child.top == bigTop) {
                    setInitialHeight(peekHeightBig)
                } else {
                    setInitialHeight(peekHeightSmall)
                }
            }
        }
    }

    override fun setDragEnabled(dragEnabled: Boolean) {
        this.dragEnabled = dragEnabled
    }

    private inner class SettleRunnable internal constructor(private val mView: View, private val mTargetState: BottomSheetState) : Runnable {

        override fun run() {
            if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this)
            } else {
                setStateInternal(mTargetState)
            }
        }
    }

    protected class SavedState : AbsSavedState {
        internal val state: BottomSheetState

        internal val peekHeightBig: Int
        internal val peekHeightSmall: Int
        internal val peekHeight: Int

        @JvmOverloads
        constructor(source: Parcel, loader: ClassLoader? = null) : super(source, loader) {

            state = BottomSheetState.valueOf(source.readString())
            peekHeightBig = source.readInt()
            peekHeightSmall = source.readInt()
            peekHeight = source.readInt()
        }

        constructor(superState: Parcelable, state: BottomSheetState, peekBig: Int, peekSmall: Int, peek: Int) : super(superState) {
            this.state = state
            this.peekHeightBig = peekBig
            this.peekHeightSmall = peekSmall
            this.peekHeight = peek
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(state.name)
            out.writeInt(peekHeightBig)
            out.writeInt(peekHeightSmall)
            out.writeInt(peekHeight)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = ParcelableCompat.newCreator(
                    object : ParcelableCompatCreatorCallbacks<SavedState> {
                        override fun createFromParcel(`in`: Parcel, loader: ClassLoader): SavedState {
                            return SavedState(`in`, loader)
                        }

                        override fun newArray(size: Int): Array<SavedState?> {
                            return arrayOfNulls(size)
                        }
                    })
        }
    }

    companion object {

        /**
         * Peek at the 16:9 ratio keyline of its parent.
         *
         *
         * This can be used for setting [peekHeight].
         * Accessing [peekHeight] will return this when the value is set.
         */
        const val PEEK_HEIGHT_AUTO = -1

        private const val HIDE_THRESHOLD = 0.5f
        private const val HIDE_FRICTION = 0.1f

        /**
         * A utility function to get the [BottomSheetTwoStatesBehaviour] associated with the `view`.
         *
         * @param view The [View] with [BottomSheetTwoStatesBehaviour].
         * @return The [BottomSheetTwoStatesBehaviour] associated with the `view`.
         */
        fun <V : View> from(view: V): BottomSheetTwoStatesBehaviour<V> {
            val params = view.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "The view is not a child of CoordinatorLayout" }

            val behavior = params.behavior
            require(behavior is BottomSheetTwoStatesBehaviour<*>) { "The view is not associated with BottomSheetBehavior" }
            return behavior as BottomSheetTwoStatesBehaviour<V>
        }
    }
}
