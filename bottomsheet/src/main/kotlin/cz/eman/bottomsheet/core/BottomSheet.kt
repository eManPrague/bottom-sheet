package cz.eman.bottomsheet.core

/**
 * Interface for communication with [BottomSheetTwoStatesBehavior]
 *
 * @author Andrej Martinák <andrej.martinak@eman.cz>
 * @since 1.0.0
 */
interface BottomSheet {

    fun getPeekHeightSemiCollapsed(): Int

    fun getPeekHeightCollapsed(): Int

    fun setState(state: BottomSheetState)

    fun getState(): BottomSheetState

    /**
     * Sets up two peek heights, first one must be smaller or equal to second
     *
     * @param smaller height in px
     * @param bigger  height in px
     * @since 1.0.0
     */
    fun setPeekHeights(smaller: Int, bigger: Int)

    /**
     * Sets up two peek heights, first one must be smaller or equal to second
     *
     * @param smaller          height in px
     * @param bigger           height in px
     * @param affectInitHeight true when initial height of bottom sheet should be affected
     * @since 1.0.0
     */
    fun setPeekHeights(smaller: Int, bigger: Int, affectInitHeight: Boolean)

    /**
     * Sets up initial height of bottom sheet, pass just values that are equal to one of peek heights
     * of UI might get laggy
     *
     * @param height height in px
     * @since 1.0.0
     */
    fun setInitialHeight(height: Int)

    /**
     * Sets callback for bottom sheet which reports sheet sliding and state changes
     * @param callback callback
     * @since 1.0.0
     */
    fun setBottomSheetCallback(callback: BottomSheetCallback?)

    /**
     * Sets if sheet can be hidden = it can go lower than smallest of peek heights
     * @param hideable true if it should be hideable
     * @since 1.0.0
     */
    fun setHideable(hideable: Boolean)

    /**
     * Enabled or disables dragging
     * @param enabled true if enabled
     * @since 1.0.0
     */
    fun setDragEnabled(enabled: Boolean)

    /**
     * Animates sheet to given height. After animation is done, visible part of the sheet
     * will be equal to height passed as parameter.
     *
     * Animate always to one of current peek heights or after user touches screen sheet will jump onto
     * nearest peek height
     *
     * @param height target height
     * @since 1.0.0
     */
    fun animateAndSetHeights(height: Int)

    /**
     * Animates sheet to given height. After animation is done, visible part of the sheet
     * will be equal to height passed as parameter.
     *
     * @param pixels target height, one of collapsedHeight and semiCollapsedHeight
     * @param collapsedHeight new collapsed height for sheet
     * @param semiCollapsedHeight new semi collapsed height for sheet
     * @since 1.0.0
     */
    fun animateAndSetHeights(pixels: Int, collapsedHeight: Int, semiCollapsedHeight: Int)

    /**
     * Automatically selects initial height for sheet. This method takes to account just currently
     * set peek heights and selects best for current sheet state.
     * @since 1.0.0
     */
    fun setAutoInitHeight()
}
