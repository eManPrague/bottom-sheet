package cz.eman.android.bottomsheet.core;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * Callback for monitoring events about bottom sheets.
 * Created by Michal [michal.mrocek@eman.cz] on 14.07.17.
 */

public interface BottomSheetCallback {

    /**
     * Called when the bottom sheet changes its state.
     *
     * @param bottomSheet The bottom sheet view.
     * @param newState    The new state. This will be one of {@link BottomSheetState#STATE_DRAGGING},
     *                    {@link BottomSheetState#STATE_SETTLING}, {@link BottomSheetState#STATE_EXPANDED},
     *                    {@link BottomSheetState#STATE_COLLAPSED}, or {@link BottomSheetState#STATE_HIDDEN}.
     */
    void onStateChanged(@NonNull View bottomSheet, BottomSheetState newState);

    /**
     * Called when the bottom sheet is being dragged.
     *
     * @param bottomSheet The bottom sheet view.
     * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
     *                    increases as this bottom sheet is moving upward. From 0 to 1 the sheet
     *                    is between collapsed and expanded states and from -1 to 0 it is
     *                    between hidden and collapsed states.
     */
    void onSlide(@NonNull View bottomSheet, float slideOffset);
}
