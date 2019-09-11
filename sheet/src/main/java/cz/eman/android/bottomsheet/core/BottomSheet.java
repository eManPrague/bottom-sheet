package cz.eman.android.bottomsheet.core;

import android.support.annotation.Nullable;

/**
 * Interface for communication with {@link BottomSheetTwoStatesBehaviour}
 * Created by Michal [michal.mrocek@eman.cz] on 14.07.17.
 */
public interface BottomSheet {

    /**
     * Sets up two peek heights, first one must be smaller or equal to second
     *
     * @param smaller height in px
     * @param bigger  height in px
     */
    void setPeekHeights(int smaller, int bigger);

    /**
     * Sets up two peek heights, first one must be smaller or equal to second
     *
     * @param smaller          height in px
     * @param bigger           height in px
     * @param affectInitHeight true when initial height of bottom sheet should be affected
     */
    void setPeekHeights(int smaller, int bigger, boolean affectInitHeight);

    /**
     * Sets up initial height of bottom sheet, pass just values that are equal to one of peek heights
     * of UI might get laggy
     *
     * @param height height in px
     */
    void setInitialHeight(int height);

    /**
     * Sets callback for bottom sheet which reports sheet sliding and state changes
     * @param callback callback
     */
    void setBottomSheetCallback(@Nullable BottomSheetCallback callback);

    int getPeekHeightSemiCollapsed();

    int getPeekHeightCollapsed();

    void setPeekHeightSemiCollapsed(int heightSemiCollapsed);

    /**
     * Sets if sheet can be hidden = it can go lower than smallest of peek heights
     * @param hideable true if it should be hideable
     */
    void setHideable(boolean hideable);

    /**
     * Enabled or disables dragging
     * @param enabled true if enabled
     */
    void setDragEnabled(boolean enabled);

    /**
     * @return current state of the bottom sheet
     */
    BottomSheetState getState();

    /**
     * Sets state to bottom sheet
     * @param state state
     */
    void setState(final BottomSheetState state);

    /**
     * Animates sheet to given height. After animation is done, visible part of the sheet
     * will be equal to height passed as parameter.
     *
     * Animate always to one of current peek heights or after user touches screen sheet will jump onto
     * nearest peek height
     *
     * @param height target height
     */
    void animateAndSetHeights(int height);

    /**
     * Animates sheet to given height. After animation is done, visible part of the sheet
     * will be equal to height passed as parameter.
     *
     * @param pixels target height, one of collapsedHeight and semiCollapsedHeight
     * @param collapsedHeight new collapsed height for sheet
     * @param semiCollapsedHeight new semi collapsed height for sheet
     */
    void animateAndSetHeights(int pixels, int collapsedHeight, int semiCollapsedHeight);

    /**
     * Automatically selects initial height for sheet. This method takes to account just currently
     * set peek heights and selects best for current sheet state.
     */
    void setAutoInitHeight();
}
