package cz.eman.bottomsheet.manipulation

import androidx.annotation.ColorInt
import android.view.View
import android.view.ViewGroup

/**
 * Interface which out view must implement on order to work properly with [BottomSheetTwoStatesBehavior]
 * @author eMan s.r.o.
 * @since 1.0.0
 */
interface SheetsHelperView {

    /**
     * @return container which has google map inflated in itself
     */
    fun getMapContainer(): ViewGroup

    /**
     * @return instances of views that should move with card (zoom button, refresh button, ...) and are placed above it
     */
    fun getBottomItems(): Array<View>

    /**
     * @return color of status bar when sheet is collapsed, dragging or hidden
     */
    fun getStatusBarColorDefault(): Int

    /**
     * @return color of status bar when sheet is expanded
     */
    fun getStatsBarColorExpanded(): Int

    /**
     * @param visible true to show map or false to hide it
     */
    fun setMapVisible(visible: Boolean)

    /**
     * @param show if true dark status bar icons will be shown above Marshmallow
     */
    fun showDarkStatusBarIcons(show: Boolean)

    /**
     * Sets color of status bar, works above Lollipop
     *
     * @param color color
     */
    fun setStatusBarColor(@ColorInt color: Int)

    /**
     * Called to setup map padding depending on sheet position
     *
     * @param left   left padding of map
     * @param top    top padding of map
     * @param right  right padding of map
     * @param bottom bottom padding of map
     */
    fun setMapPadding(left: Int, top: Int, right: Int, bottom: Int)

    /**
     * Controls if gestures on the map are enabled, this method is here just to secure that complex
     * taps on sheet will not propagate to map
     *
     * @param enabled true when it should be enabled
     */
    fun setMapGesturesEnabled(enabled: Boolean)

}