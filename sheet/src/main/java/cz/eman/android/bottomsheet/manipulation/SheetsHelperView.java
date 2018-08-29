package cz.eman.android.bottomsheet.manipulation;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * Interface which out view must implement on order to work properly with {@link cz.eman.android.bottomsheet.core.BottomSheetTwoStatesBehaviour}
 * Created by Michal [michal.mrocek@eman.cz] on 14.02.18.
 */

public interface SheetsHelperView {

    /**
     * @return container which has google map inflated in itself
     */
    @NonNull
    ViewGroup getMapContainer();

    /**
     * @param visible true to show map or false to hide it
     */
    void setMapVisible(boolean visible);

    /**
     * @return instances of views that should move with card (zoom button, refresh button, ...) and are placed above it
     */
    @NonNull
    View[] getBottomItems();

    /**
     * @param show if true dark status bar icons will be shown above Marshmallow
     */
    void showDarkStatusBarIcons(boolean show);

    /**
     * Sets color of status bar, works above Lollipop
     *
     * @param color color
     */
    void setStatusBarColor(@ColorInt int color);

    /**
     * Called to setup map padding depending on sheet position
     *
     * @param left   left padding of map
     * @param top    top padding of map
     * @param right  right padding of map
     * @param bottom bottom padding of map
     */
    void setMapPadding(int left, int top, int right, int bottom);

    /**
     * Controls if gestures on the map are enabled, this method is here just to secure that complex
     * taps on sheet will not propagate to map
     *
     * @param enabled true when it should be enabled
     */
    void setMapGesturesEnabled(boolean enabled);

    /**
     * @return color of status bar when sheet is collapsed, dragging or hidden
     */
    @ColorInt
    int getStatusBarColorDefault();

    /**
     * @return color of status bar when sheet is expanded
     */
    @ColorInt
    int getStatsBarColorExpanded();

}