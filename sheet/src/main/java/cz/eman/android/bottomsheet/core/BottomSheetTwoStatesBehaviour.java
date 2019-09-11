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

package cz.eman.android.bottomsheet.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;


/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as
 * a bottom sheet.
 */
public class BottomSheetTwoStatesBehaviour<V extends View> extends CoordinatorLayout.Behavior<V> implements BottomSheet {

    private int initialHeight;

    /**
     * Peek at the 16:9 ratio keyline of its parent.
     *
     * <p>This can be used as a parameter for {@link #setPeekHeight(int)}.
     * {@link #getPeekHeight()} will return this when the value is set.</p>
     */
    public static final int PEEK_HEIGHT_AUTO = -1;

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;

    private float maximumVelocity;

    private int peekHeight;

    private int peekHeightSmall;
    private int peekHeightBig;

    private boolean peekHeightAuto;

    private int peekHeightMin;

    int mMinOffset;
    int mMaxOffset;

    boolean hideable;

    private boolean mSkipCollapsed;

    private BottomSheetState mState = BottomSheetState.STATE_COLLAPSED;

    ViewDragHelper mViewDragHelper;

    private boolean mIgnoreEvents;

    private int mLastNestedScrollDy;

    private boolean mNestedScrolled;

    int mParentHeight;

    WeakReference<V> mViewRef;

    WeakReference<View> mNestedScrollingChildRef;

    private BottomSheetCallback mCallback;

    private VelocityTracker mVelocityTracker;

    int mActivePointerId;

    private int mInitialY;

    boolean mTouchingScrollingChild;
    boolean mDragEnabled;

    /**
     * Default constructor for instantiating BottomSheetBehaviors.
     */
    public BottomSheetTwoStatesBehaviour() {
    }

    /**
     * Default constructor for inflating BottomSheetBehaviors from layout.
     *
     * @param context The {@link Context}.
     * @param attrs   The {@link AttributeSet}.
     */
    public BottomSheetTwoStatesBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                android.support.design.R.styleable.BottomSheetBehavior_Layout);
        TypedValue value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(a.getDimensionPixelSize(
                    android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO));
        }
        setHideable(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setSkipCollapsed(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed,
                false));
        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mDragEnabled = true;
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), mState, peekHeightBig, peekHeightSmall, initialHeight);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        if (ss.state == BottomSheetState.STATE_DRAGGING || ss.state == BottomSheetState.STATE_SETTLING) {
            mState = BottomSheetState.STATE_COLLAPSED;
        } else {
            mState = ss.state;
        }

        peekHeightBig = ss.peekHeightBig;
        peekHeightSmall = ss.peekHeightSmall;
        peekHeight = ss.peekHeightSmall;
        initialHeight = ss.peekHeight;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }
        int savedTop = child.getTop();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);
        // Offset the bottom sheet
        mParentHeight = parent.getHeight();
        int peekHeight;
        if (peekHeightAuto) {
            if (peekHeightMin == 0) {
                peekHeightMin = parent.getResources().getDimensionPixelSize(
                        android.support.design.R.dimen.design_bottom_sheet_peek_height_min);
            }
            peekHeight = Math.max(peekHeightMin, mParentHeight - parent.getWidth() * 9 / 16);
        } else {
            peekHeight = this.peekHeight;
        }
        mMinOffset = Math.max(0, mParentHeight - child.getHeight());
        mMaxOffset = Math.max(mParentHeight - peekHeight, mMinOffset);
        if (mState == BottomSheetState.STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMinOffset);
        } else if (hideable && mState == BottomSheetState.STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, mParentHeight);
        } else if (mState == BottomSheetState.STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, initialHeight == 0 ? mMaxOffset : mParentHeight - initialHeight);
        } else if (mState == BottomSheetState.STATE_DRAGGING || mState == BottomSheetState.STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }
        mViewRef = new WeakReference<>(child);
        mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown() || !mDragEnabled) {
            mIgnoreEvents = true;
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchingScrollingChild = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                int initialX = (int) event.getX();
                mInitialY = (int) event.getY();
                View scroll = mNestedScrollingChildRef.get();
                if (scroll != null && parent.isPointInChildBounds(scroll, initialX, mInitialY)) {
                    mActivePointerId = event.getPointerId(event.getActionIndex());
                    mTouchingScrollingChild = true;
                }
                mIgnoreEvents = mActivePointerId == MotionEvent.INVALID_POINTER_ID &&
                        !parent.isPointInChildBounds(child, initialX, mInitialY);
                break;
        }
        if (!mIgnoreEvents && mViewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = mNestedScrollingChildRef.get();
        return action == MotionEvent.ACTION_MOVE && scroll != null &&
                !mIgnoreEvents && mState != BottomSheetState.STATE_DRAGGING &&
                !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) &&
                Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown() || !mDragEnabled) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        if (mState == BottomSheetState.STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
            // Record the velocity
            if (action == MotionEvent.ACTION_DOWN) {
                reset();
            }
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);
            // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
            // to capture the bottom sheet in case it is not captured and the touch slop is passed.
            if (action == MotionEvent.ACTION_MOVE && !mIgnoreEvents) {
                if (Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop()) {
                    mViewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
                }
            }
        }
        return !mIgnoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        mLastNestedScrollDy = 0;
        mNestedScrolled = false;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && mDragEnabled;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx,
                                  int dy, int[] consumed) {
        View scrollingChild = mNestedScrollingChildRef.get();
        if (target != scrollingChild) {
            return;
        }
        int currentTop = child.getTop();
        int newTop = currentTop - dy;
        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {
                consumed[1] = currentTop - mMinOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(BottomSheetState.STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(BottomSheetState.STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTop <= mMaxOffset || hideable) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(BottomSheetState.STATE_DRAGGING);
                } else {
                    consumed[1] = currentTop - mMaxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(BottomSheetState.STATE_COLLAPSED);
                }
            }
        }
        dispatchOnSlide(child.getTop());
        mLastNestedScrollDy = dy;
        mNestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (child.getTop() == mMinOffset) {
            setStateInternal(BottomSheetState.STATE_EXPANDED);
            return;
        }
        if (target != mNestedScrollingChildRef.get() || !mNestedScrolled) {
            return;
        }
        int top;

        int currentTop = child.getTop();
        int pxFromBottom = child.getHeight() - currentTop;

        BottomSheetState targetState;
        if (mLastNestedScrollDy > 0) { // Moving up

            if (pxFromBottom > peekHeightBig) {
                top = mMinOffset;
                targetState = BottomSheetState.STATE_EXPANDED;
            } else {
                top = child.getHeight() - peekHeightBig;
                targetState = BottomSheetState.STATE_COLLAPSED;
            }
        } else if (hideable && shouldHide(child, getYVelocity())) {
            top = mParentHeight;
            targetState = BottomSheetState.STATE_HIDDEN;
        } else if (mLastNestedScrollDy == 0.f) {

            if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                top = child.getHeight() - peekHeightSmall;
                targetState = BottomSheetState.STATE_COLLAPSED;
            } else if (pxFromBottom < peekHeightBig) {
                top = child.getHeight() - peekHeightBig;
                targetState = BottomSheetState.STATE_COLLAPSED;
            } else {
                top = mMinOffset;
                targetState = BottomSheetState.STATE_EXPANDED;
            }
        } else {
            if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                top = mMaxOffset;
                targetState = BottomSheetState.STATE_COLLAPSED;
            } else {
                top = child.getHeight() - peekHeightBig;
                targetState = BottomSheetState.STATE_COLLAPSED;
            }
        }

        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            setStateInternal(BottomSheetState.STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }

        mNestedScrolled = false;
    }


    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target,
                                    float velocityX, float velocityY) {
        return target == mNestedScrollingChildRef.get() &&
                (mState != BottomSheetState.STATE_EXPANDED ||
                        super.onNestedPreFling(coordinatorLayout, child, target,
                                velocityX, velocityY));
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or
     *                   {@link #PEEK_HEIGHT_AUTO} to configure the sheet to peek automatically
     *                   at 16:9 ratio keyline.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    public final void setPeekHeight(int peekHeight) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true;
                layout = true;
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false;
            this.peekHeight = Math.max(0, peekHeight);
//            mMaxOffset = mParentHeight - peekHeight;
            layout = true;
        }
        if (layout && mState == BottomSheetState.STATE_COLLAPSED && mViewRef != null) {
            V view = mViewRef.get();
            if (view != null) {
                view.requestLayout();
            }
        }
    }

    public void setPeekHeights(int smaller, int bigger) {
        setPeekHeights(smaller, bigger, true);
    }

    public void setPeekHeights(int smaller, int bigger, boolean affectInitHeight) {
        setPeekHeight(smaller);

        peekHeightSmall = smaller;
        peekHeightBig = bigger;

        // Initial height cannot be smaller than collapsed state
        if (affectInitHeight) {
            if (initialHeight < smaller) {
                initialHeight = smaller;
                if (mViewRef != null && mViewRef.get() != null) {
                    mMaxOffset = mViewRef.get().getHeight() - smaller;
                }
            } else {
                initialHeight = 0;
            }
        }
    }

    public void setPeekHeightSemiCollapsed(int bigger) {
        peekHeightBig = bigger;
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet in pixels, or {@link #PEEK_HEIGHT_AUTO}
     *         if the sheet is configured to peek automatically at 16:9 ratio keyline
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    public final int getPeekHeight() {
        return peekHeightAuto ? PEEK_HEIGHT_AUTO : peekHeight;
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public void setHideable(boolean hideable) {
        this.hideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public boolean isHideable() {
        return hideable;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        mSkipCollapsed = skipCollapsed;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public boolean getSkipCollapsed() {
        return mSkipCollapsed;
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    public void setBottomSheetCallback(BottomSheetCallback callback) {
        mCallback = callback;
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link BottomSheetState#STATE_COLLAPSED}, {@link BottomSheetState#STATE_EXPANDED}, or
     *              {@link BottomSheetState#STATE_HIDDEN}.
     */
    public final void setState(final BottomSheetState state) {
        if (state == mState && state != BottomSheetState.STATE_COLLAPSED) {
            return;
        }
        if (mViewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == BottomSheetState.STATE_COLLAPSED || state == BottomSheetState.STATE_EXPANDED ||
                    (hideable && state == BottomSheetState.STATE_HIDDEN)) {
                mState = state;
            }
            return;
        }
        final V child = mViewRef.get();
        if (child == null) {
            return;
        }
        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(() -> startSettlingAnimation(child, state));
        } else {
            startSettlingAnimation(child, state);
        }
    }

    private final void animateToPixels(final int pixels, final int collapsedSmall, final int collapsedBig, final boolean setHeights) {
        if (mViewRef != null && mViewRef.get() != null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            final V child = mViewRef.get();

            ViewParent parent = child.getParent();
            if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
                child.post(new Runnable() {
                    @Override
                    public void run() {
                        startAnimation(child, pixels, collapsedSmall, collapsedBig, setHeights);
                    }
                });
            } else {
                startAnimation(child, pixels, collapsedSmall, collapsedBig, setHeights);
            }
        }
    }

    public final void animateAndSetHeights(final int pixels) {
        animateToPixels(pixels, pixels, pixels, true);
    }

    public final void animateAndSetHeights(final int pixels, final int collapsedSmall, final int collapsedBig) {
        animateToPixels(pixels, collapsedSmall, collapsedBig, true);
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of {@link BottomSheetState#STATE_EXPANDED}, {@link BottomSheetState#STATE_COLLAPSED}, {@link BottomSheetState#STATE_DRAGGING},
     * and {@link BottomSheetState#STATE_SETTLING}.
     */
    public final BottomSheetState getState() {
        return mState;
    }

    void setStateInternal(BottomSheetState state) {
        if (mState == state) {
            return;
        }
        mState = state;
        View bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            mCallback.onStateChanged(bottomSheet, state);
        }
    }

    private void reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    boolean shouldHide(View child, float yvel) {
        if (mSkipCollapsed) {
            return true;
        }
        if (child.getTop() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) peekHeight > HIDE_THRESHOLD;
    }

    private View findScrollingChild(View view) {
        if (view instanceof NestedScrollingChild) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private float getYVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        return VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId);
    }

    void startSettlingAnimation(View child, BottomSheetState state) {
        int top;
        if (state == BottomSheetState.STATE_COLLAPSED) {
            top = child.getHeight() - peekHeightBig;

            // Collapse to smaller peek height if possible
            if (mViewRef.get() != null && mViewRef.get().getTop() == top) {
                top = child.getHeight() - peekHeightSmall;
            }
        } else if (state == BottomSheetState.STATE_EXPANDED) {
            top = mMinOffset;
        } else if (hideable && state == BottomSheetState.STATE_HIDDEN) {
            top = mParentHeight;
        } else {
            return;
            // throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(BottomSheetState.STATE_SETTLING);
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top, 300)) {
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        }
    }

    void startAnimation(final View child, final int pixels, final int collapsedSmall, final int collapsedBig,  final boolean setHeights) {
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), mParentHeight - pixels, 300)) {
            setStateInternal(BottomSheetState.STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new Runnable() {
                @Override
                public void run() {
                    new SettleRunnable(child, BottomSheetState.STATE_COLLAPSED).run();
                    if (setHeights) {
                        setPeekHeights(collapsedSmall, collapsedBig);
                    }
                }
            });
        } else if (setHeights) {
            setStateInternal(BottomSheetState.STATE_COLLAPSED);
            setPeekHeights(collapsedSmall, collapsedBig, false);
            setInitialHeight(pixels);
        }
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mState == BottomSheetState.STATE_DRAGGING) {
                return false;
            }
            if (mTouchingScrollingChild) {
                return false;
            }
            if (mState == BottomSheetState.STATE_EXPANDED && mActivePointerId == pointerId) {
                View scroll = mNestedScrollingChildRef.get();
                if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                    // Let the content scroll up
                    return false;
                }
            }
            return mViewRef != null && mViewRef.get() == child;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(top);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(BottomSheetState.STATE_DRAGGING);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top;
            int currentTop = releasedChild.getTop();
            int pxFromBottom = releasedChild.getHeight() - currentTop;

            BottomSheetState targetState;
            if (yvel < 0) { // Moving up

                if (pxFromBottom > peekHeightBig) {
                    top = mMinOffset;
                    targetState = BottomSheetState.STATE_EXPANDED;
                } else {
                    top = releasedChild.getHeight() - peekHeightBig;
                    targetState = BottomSheetState.STATE_COLLAPSED;
                }
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                top = mParentHeight;
                targetState = BottomSheetState.STATE_HIDDEN;
            } else if (yvel == 0.f) {

                if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                    top = releasedChild.getHeight() - peekHeightSmall;
                    targetState = BottomSheetState.STATE_COLLAPSED;
                } else if (pxFromBottom < peekHeightBig) {
                    top = releasedChild.getHeight() - peekHeightBig;
                    targetState = BottomSheetState.STATE_COLLAPSED;
                } else {
                    top = mMinOffset;
                    targetState = BottomSheetState.STATE_EXPANDED;
                }
            } else {
                if (0 < pxFromBottom && pxFromBottom < peekHeightBig) {
                    top = mMaxOffset;
                    targetState = BottomSheetState.STATE_COLLAPSED;
                } else {
                    top = releasedChild.getHeight() - peekHeightBig;
                    targetState = BottomSheetState.STATE_COLLAPSED;
                }
            }

            if (mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                setStateInternal(BottomSheetState.STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild,
                        new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return constrain(top, mMinOffset, hideable ? mParentHeight : mMaxOffset);
        }

        int constrain(int amount, int low, int high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            if (hideable) {
                return mParentHeight - mMinOffset;
            } else {
                return mMaxOffset - mMinOffset;
            }
        }
    };

    void dispatchOnSlide(int top) {
        View bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            int maxOffset = Math.max(mParentHeight - peekHeightBig, mParentHeight - peekHeightMin);
            if (top > maxOffset) {
                mCallback.onSlide(bottomSheet, (float) (maxOffset - top) /
                        (mParentHeight - maxOffset));
            } else {
                mCallback.onSlide(bottomSheet,
                        (float) (mMaxOffset - top) / ((mMaxOffset - mMinOffset)));
            }
        }
    }

    @VisibleForTesting
    int getPeekHeightMin() {
        return peekHeightMin;
    }

    private class SettleRunnable implements Runnable {

        private final View mView;

        private final BottomSheetState mTargetState;

        SettleRunnable(View view, BottomSheetState targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends AbsSavedState {
        final BottomSheetState state;

        final int peekHeightBig;
        final int peekHeightSmall;
        final int peekHeight;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = BottomSheetState.valueOf(source.readString());
            peekHeightBig = source.readInt();
            peekHeightSmall = source.readInt();
            peekHeight = source.readInt();
        }

        public SavedState(Parcelable superState, BottomSheetState state, int peekBig, int peekSmall, int peek) {
            super(superState);
            this.state = state;
            this.peekHeightBig = peekBig;
            this.peekHeightSmall = peekSmall;
            this.peekHeight = peek;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(state.name());
            out.writeInt(peekHeightBig);
            out.writeInt(peekHeightSmall);
            out.writeInt(peekHeight);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }

    public int getPeekHeightCollapsed() {
        return peekHeightSmall;
    }

    public int getPeekHeightSemiCollapsed() {
        return peekHeightBig;
    }

    /**
     * A utility function to get the {@link BottomSheetTwoStatesBehaviour} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link BottomSheetTwoStatesBehaviour}.
     * @return The {@link BottomSheetTwoStatesBehaviour} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> BottomSheetTwoStatesBehaviour<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof BottomSheetTwoStatesBehaviour)) {
            throw new IllegalArgumentException(
                    "The view is not associated with BottomSheetBehavior");
        }
        return (BottomSheetTwoStatesBehaviour<V>) behavior;
    }

    public void setInitialHeight(int initialHeight) {
        this.initialHeight = initialHeight;
    }

    public void setAutoInitHeight() {
        if (mViewRef != null) {
            View child = mViewRef.get();
            if (child != null && mState == BottomSheetState.STATE_COLLAPSED) {
                int bigTop = child.getHeight() - peekHeightBig;

                if (child.getTop() == bigTop) {
                    setInitialHeight(peekHeightBig);
                } else {
                    setInitialHeight(peekHeightSmall);
                }
            }
        }
    }

    public void setDragEnabled(boolean dragEnabled) {
        mDragEnabled = dragEnabled;
    }
}
