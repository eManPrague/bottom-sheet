package cz.eman.bottomsheet.manipulation

import android.content.Context
import androidx.core.view.ViewCompat
import android.view.View

import cz.eman.bottomsheet.R
import cz.eman.bottomsheet.core.BottomSheet
import cz.eman.bottomsheet.core.BottomSheetCallback
import cz.eman.bottomsheet.core.BottomSheetState
import cz.eman.bottomsheet.utils.getStatusBarHeight
import cz.eman.bottomsheet.utils.getToolbarHeight
import kotlin.math.max
import kotlin.math.min

/**
 * @author eMan s.r.o.
 * @since 1.0.0
 */
class SheetsHelper private constructor(private val sheetsHelperView: SheetsHelperView) {

    private var mapLogoPaddingLeft: Int = 0
    private var mapLogoPaddingBottom: Int = 0

    private var collapsedHeight: Int = 0
    private var semiCollapsedHeight: Int = 0

    private var mapTopPadding: Int = 0

    private var currentSheet: BottomSheet? = null

    private var bottomSheetView: View? = null

    var state: BottomSheetState
        get() = currentSheet?.getState() ?: BottomSheetState.STATE_HIDDEN
        set(newState) {
            currentSheet?.run { setState(newState) }
        }

    /**
     * Attaches bottom sheet that can have two collapsed states. Sheet will automatically collapse
     * to nearest collapsed state when user swipes it.
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    fun init(view: View, behaviour: BottomSheet) {
        initManually(view, behaviour, collapsedHeight, semiCollapsedHeight, semiCollapsedHeight)
    }

    /**
     * Attaches any bottom sheet that can have only have one collapsed state.
     * Height is equal to [.semiCollapsedHeight]
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    fun initSemiCollapsed(view: View, behaviour: BottomSheet) {
        initManually(view, behaviour, semiCollapsedHeight, semiCollapsedHeight, semiCollapsedHeight)
    }

    /**
     * Attaches any bottom sheet that can have only have one collapsed state.
     * Height is equal to [.collapsedHeight]
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    fun initCollapsed(view: View, behaviour: BottomSheet) {
        initManually(view, behaviour, collapsedHeight, collapsedHeight, collapsedHeight)
    }

    /**
     * Attaches any bottom sheet. This method is general one and could be used with any view.
     * However it's recommended to use standard approach:
     *
     *  * [.init]
     *  * [.initSemiCollapsed]
     *  * [.initCollapsed]
     *
     *
     * @param view                view in which is sheet
     * @param behaviour           bottom sheet behaviour
     * @param collapsedHeight     height of card when is collapsed
     * @param semiCollapsedHeight height of card when is half collapsed, may be equal to previous parameter
     * @param initialHeight       initial height of card
     */
    fun initManually(
        view: View,
        behaviour: BottomSheet,
        collapsedHeight: Int,
        semiCollapsedHeight: Int,
        initialHeight: Int
    ) {
        currentSheet = behaviour
        bottomSheetView = view

        this.collapsedHeight = collapsedHeight
        this.semiCollapsedHeight = semiCollapsedHeight

        behaviour.setPeekHeights(collapsedHeight, semiCollapsedHeight)
        behaviour.setInitialHeight(initialHeight)
        behaviour.setBottomSheetCallback(getDefaultCallback())

        setMapPaddingAndTranslation()
    }

    /**
     * Default callback for bottom sheet that will influence status bar background and items colours
     *
     * @return callback
     */
    private fun getDefaultCallback(): BottomSheetCallback {
        return object : BottomSheetCallback {

            override fun onStateChanged(bottomSheet: View, newState: BottomSheetState) {
                // Disabling map moving when sheet is expanded
                this@SheetsHelper.onStateChanged(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                onSheetSlide(bottomSheet, slideOffset)
            }
        }
    }

    /**
     * Animates to automatic peek height
     * Automatic peek height includes big one [.semiCollapsedHeight] and small one [.collapsedHeight].
     * Closer one to current peek height is chosen.
     *
     *
     * This method is handy when you want to switch from one collapsed state back to two.
     * For example detail of item supports just one state and on back press your view must support again
     * two states. Call this method when you want to restore the behaviour and animate to closest of peek heights.
     */
    fun animateToTwoStates() {
        if (currentSheet != null) {
            // do not change anything when it is already set up - it could possibly break some animations
            if (currentSheet!!.getPeekHeightCollapsed() != collapsedHeight || currentSheet!!.getPeekHeightSemiCollapsed() != semiCollapsedHeight) {
                currentSheet!!.animateAndSetHeights(
                    semiCollapsedHeight,
                    collapsedHeight,
                    semiCollapsedHeight
                )
            }

            setMapPaddingAndTranslation()
        }
    }

    /**
     * Sets up to automatic peek height
     * Automatic peek height includes big one [.semiCollapsedHeight] and small one [.collapsedHeight].
     *
     *
     * This method is handy when you want to switch from one collapsed state back to two.
     * For example detail of item supports just one state and on back press your view must support again
     * two states. Call this method when you want to restore the behaviour.
     *
     *
     * This might cause card immediate bump up when [.collapsedHeight] is bigger than previous one!
     */
    fun restoreTwoStates() {
        if (currentSheet != null) {
            // do not change anything when it is already set up - it could possibly break some animations
            if (currentSheet!!.getPeekHeightCollapsed() != collapsedHeight || currentSheet!!.getPeekHeightSemiCollapsed() != semiCollapsedHeight) {
                currentSheet!!.setPeekHeights(collapsedHeight, semiCollapsedHeight, false)
            }

            setMapPaddingAndTranslation()
        }
    }

    /**
     * Animates to [.semiCollapsedHeight] - in this state card be only collapsed to "big" state
     */
    fun animateToSemiCollapsed() {
        if (currentSheet != null) {
            currentSheet!!.animateAndSetHeights(semiCollapsedHeight)
            setMapPaddingAndTranslation()

        }
    }

    /**
     * Sets up big peek heights - in this state card be only collapsed to "big" [.semiCollapsedHeight] state
     */
    fun restoreSemiCollapsedState() {
        if (currentSheet != null) {
            currentSheet!!.setPeekHeights(semiCollapsedHeight, semiCollapsedHeight)
            setMapPaddingAndTranslation()
        }
    }

    /**
     * Callback from sheet when its state changed
     * here we will change rendering of underlying layer to achieve smoother animations
     *
     * @param newState current state of bottom sheet
     */
    private fun onStateChanged(newState: BottomSheetState) {
        if (bottomSheetView != null) {
            sheetsHelperView.setMapGesturesEnabled(newState !== BottomSheetState.STATE_EXPANDED)
            sheetsHelperView.setMapVisible(newState !== BottomSheetState.STATE_EXPANDED)

            // Acceleration helps us get smoother animations
            if (newState === BottomSheetState.STATE_DRAGGING || newState === BottomSheetState.STATE_SETTLING) {
                sheetsHelperView.getMapContainer().setLayerType(View.LAYER_TYPE_HARDWARE, null)
                bottomSheetView!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                sheetsHelperView.getMapContainer().setLayerType(View.LAYER_TYPE_NONE, null)
                bottomSheetView!!.setLayerType(View.LAYER_TYPE_NONE, null)
            }

            if (currentSheet != null) {
                currentSheet!!.setAutoInitHeight()
            }
        }
    }

    /**
     * Callback triggered when sheet slides, we react to it and change status bar properties
     *
     * @param bottomSheet bottom sheet
     * @param slideOffset offset in range from 0 to 1
     */
    private fun onSheetSlide(bottomSheet: View, slideOffset: Float) {
        var slideOffset = slideOffset
        setMapPaddingAndTranslation(bottomSheet, collapsedHeight, semiCollapsedHeight)

        val nonExpanded = sheetsHelperView.getStatusBarColorDefault()
        val expanded = sheetsHelperView.getStatsBarColorExpanded()
        slideOffset = max(0f, slideOffset)
        if (slideOffset >= 0.9) {
            sheetsHelperView.setStatusBarColor(
                evaluateStatusBarColor(
                    (slideOffset - 0.9).toFloat() * 10,
                    nonExpanded,
                    expanded
                )
            )
        } else if (slideOffset < 0.9) {
            sheetsHelperView.setStatusBarColor(nonExpanded)
        }

        sheetsHelperView.showDarkStatusBarIcons(slideOffset >= 0.975)
    }

    private fun evaluateStatusBarColor(fraction: Float, startInt: Int, endInt: Int): Int {
        val startA = startInt shr 24 and 0xff
        val startR = startInt shr 16 and 0xff
        val startG = startInt shr 8 and 0xff
        val startB = startInt and 0xff

        val endA = endInt shr 24 and 0xff
        val endR = endInt shr 16 and 0xff
        val endG = endInt shr 8 and 0xff
        val endB = endInt and 0xff

        return startA + (fraction * (endA - startA)).toInt() shl 24 or (
                startR + (fraction * (endR - startR)).toInt() shl 16) or (
                startG + (fraction * (endG - startG)).toInt() shl 8) or
                startB + (fraction * (endB - startB)).toInt()
    }

    /**
     * Sets map padding which affects zooming and also moves items above sheet
     */
    fun setMapPaddingAndTranslation() {
        if (currentSheet != null && bottomSheetView != null) {
            // View has not gone through full layout cycle, wait till this one is done
            if (!ViewCompat.isLaidOut(bottomSheetView!!)) {
                // Adding onLayoutChangedListener does not work properly cause Sheet reacts after view is laid out
                bottomSheetView!!.postDelayed({ this.setMapPaddingAndTranslation() }, 75)
            } else {
                setMapPaddingAndTranslation(bottomSheetView!!, collapsedHeight, semiCollapsedHeight)
            }
        }
    }

    /**
     * Updates padding and translation to map so every marker in map will be visible
     * This also adds translation to view --> parallax effect
     *
     * @param bottomSheet     current bottom sheet
     * @param collapsedHeight height of bottom sheet when it is collapsed
     * @param maxMapPadding   max padding + translation, when this value is reached then sheet will just continue overlapping the map
     */
    private fun setMapPaddingAndTranslation(
        bottomSheet: View,
        collapsedHeight: Int,
        maxMapPadding: Int
    ) {
        val calculatedPadding = bottomSheet.height - bottomSheet.top
        val realPadding = min(calculatedPadding, maxMapPadding)

        val translationY = min(0, -(realPadding - collapsedHeight) / 2)
        val paddingTop = max(0, mapTopPadding - translationY)
        val paddingBot = max(0, realPadding + translationY)

        sheetsHelperView.setMapPadding(
            mapLogoPaddingLeft,
            paddingTop,
            0,
            paddingBot + mapLogoPaddingBottom
        )
        sheetsHelperView.getMapContainer().translationY = translationY.toFloat()

        for (view in sheetsHelperView.getBottomItems()) {
            view.translationY = (-realPadding).toFloat()
        }
    }

    /**
     * Call after instance stare is restored to restore status bar state
     * .
     * This only does something when sheet was previously expanded
     */
    fun onInstanceStateRestored() {
        if (currentSheet != null && bottomSheetView != null && currentSheet!!.getState() === BottomSheetState.STATE_EXPANDED) {
            onSheetSlide(bottomSheetView!!, SLIDE_OFFSET_SHEET_EXPANDED)
        }
    }

    /**
     * Creates new instance of Builder and sets defaults.
     *
     *  - map padding is by default equal to sum of status bar and toolbar height
     *  - padding of Google logo to 16dp for both sides
     *
     * @param context   current context
     * @param sheetView callback view
     */
    class Builder(private val context: Context, private val sheetView: SheetsHelperView) {

        private val sheetsHelper: SheetsHelper = SheetsHelper(sheetView)

        init {
            val logoPaddingLeft =
                context.resources.getDimensionPixelSize(R.dimen.sheet_logo_additional_padding_left)
            val logoPaddingBottom =
                context.resources.getDimensionPixelSize(R.dimen.sheet_logo_additional_padding_bottom)

            setGoogleLogoPadding(logoPaddingLeft, logoPaddingBottom)
            setMapTopPadding(context.getStatusBarHeight() + context.getToolbarHeight())
        }

        /**
         * Call to set custom Google map padding. By default we will setup constants so logo will have
         * 16dp from left and bottom.
         *
         * @param leftPx   padding from left side
         * @param bottomPx padding from bottom side
         * @return buolder
         */
        fun setGoogleLogoPadding(leftPx: Int, bottomPx: Int): Builder {
            sheetsHelper.mapLogoPaddingBottom = bottomPx
            sheetsHelper.mapLogoPaddingLeft = leftPx
            return this
        }

        /**
         * Sets height of sheet when it is fully collapsed
         *
         * @param heightPx height in px
         * @return builder
         */
        fun setCollapsedHeight(heightPx: Int): Builder {
            sheetsHelper.collapsedHeight = heightPx
            return this
        }

        /**
         * Sets height of sheet when it is partially collapsed
         *
         * @param heightPx height in px
         * @return builder
         */
        fun setSemiCollapsedHeight(heightPx: Int): Builder {
            sheetsHelper.semiCollapsedHeight = heightPx
            return this
        }

        /**
         * Sets top padding of a map
         *
         * @param paddingPx padding
         * @return builder
         */
        fun setMapTopPadding(paddingPx: Int): Builder {
            sheetsHelper.mapTopPadding = paddingPx
            return this
        }

        fun build(): SheetsHelper {
            return sheetsHelper
        }
    }

    companion object {
        private const val SLIDE_OFFSET_SHEET_EXPANDED = 1f
    }
}