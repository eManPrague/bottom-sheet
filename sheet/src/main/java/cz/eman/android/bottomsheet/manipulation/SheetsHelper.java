package cz.eman.android.bottomsheet.manipulation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;

import cz.eman.android.bottomsheet.R;
import cz.eman.android.bottomsheet.core.BottomSheet;
import cz.eman.android.bottomsheet.core.BottomSheetCallback;
import cz.eman.android.bottomsheet.core.BottomSheetState;
import cz.eman.android.bottomsheet.core.BottomSheetTwoStatesBehaviour;
import cz.eman.android.bottomsheet.utils.DesignUtils;

/**
 * Class that we all love. It helps us manage bottom sheet <3
 * Created by Michal [michal.mrocek@eman.cz] on 13.07.17.
 */

public class SheetsHelper {

    private static final float SLIDE_OFFSET_SHEET_EXPANDED = 1F;

    @Nullable
    private SheetsHelperView sheetsHelperView;

    private int mapLogoPaddingLeft;
    private int mapLogoPaddingBottom;

    private int collapsedHeight;
    private int semiCollapsedHeight;

    private int mapTopPadding;

    @Nullable
    private BottomSheet currentSheet;

    @Nullable
    private View bottomSheetView;

    /**
     * Call {@link SheetsHelper.Builder} to construct brand new helper
     *
     * @param view
     */
    private SheetsHelper(@Nullable SheetsHelperView view) {
        sheetsHelperView = view;
    }

    /**
     * Attaches bottom sheet that can have two collapsed states. Sheet will automatically collapse
     * to nearest collapsed state when user swipes it.
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    public void init(@NonNull View view, @NonNull BottomSheet behaviour) {
        initManually(view, behaviour, collapsedHeight, semiCollapsedHeight, semiCollapsedHeight);
    }

    /**
     * Attaches any bottom sheet that can have only have one collapsed state.
     * Height is equal to {@link #semiCollapsedHeight}
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    public void initSemiCollapsed(@NonNull View view, @NonNull BottomSheet behaviour) {
        initManually(view, behaviour, semiCollapsedHeight, semiCollapsedHeight, semiCollapsedHeight);
    }

    /**
     * Attaches any bottom sheet that can have only have one collapsed state.
     * Height is equal to {@link #collapsedHeight}
     *
     * @param view      view in which is sheet
     * @param behaviour bottom sheet behaviour
     */
    public void initCollapsed(@NonNull View view, @NonNull BottomSheet behaviour) {
        initManually(view, behaviour, collapsedHeight, collapsedHeight, collapsedHeight);
    }

    /**
     * Attaches any bottom sheet. This method is general one and could be used with any view.
     * However it's recommended to use standard approach:
     * <ul>
     * <li>{@link #init(View, BottomSheet)}</li>
     * <li>{@link #initSemiCollapsed(View, BottomSheet)}</li>
     * <li>{@link #initCollapsed(View, BottomSheet)}</li>
     * </ul>
     *
     * @param view                view in which is sheet
     * @param behaviour           bottom sheet behaviour
     * @param collapsedHeight     height of card when is collapsed
     * @param semiCollapsedHeight height of card when is half collapsed, may be equal to previous parameter
     * @param initialHeight       initial height of card
     */
    public void initManually(@NonNull View view, @NonNull BottomSheet behaviour, int collapsedHeight, int semiCollapsedHeight, int initialHeight) {
        currentSheet = behaviour;
        bottomSheetView = view;

        this.collapsedHeight = collapsedHeight;
        this.semiCollapsedHeight = semiCollapsedHeight;

        behaviour.setPeekHeights(collapsedHeight, semiCollapsedHeight);
        behaviour.setInitialHeight(initialHeight);
        behaviour.setBottomSheetCallback(getDefaultCallback());

        setMapPaddingAndTranslation();
    }

    /**
     * Animates to automatic peek height
     * Automatic peek height includes big one {@link #semiCollapsedHeight} and small one {@link #collapsedHeight}.
     * Closer one to current peek height is chosen.
     * <p>
     * This method is handy when you want to switch from one collapsed state back to two.
     * For example detail of item supports just one state and on back press your view must support again
     * two states. Call this method when you want to restore the behaviour and animate to closest of peek heights.
     */
    public void animateToTwoStates() {
        if (currentSheet != null) {
            // do not change anything when it is already set up - it could possibly break some animations
            if (currentSheet.getPeekHeightCollapsed() != collapsedHeight || currentSheet.getPeekHeightSemiCollapsed() != semiCollapsedHeight) {
                currentSheet.animateAndSetHeights(semiCollapsedHeight, collapsedHeight, semiCollapsedHeight);
            }

            setMapPaddingAndTranslation();
        }
    }

    /**
     * Sets up to automatic peek height
     * Automatic peek height includes big one {@link #semiCollapsedHeight} and small one {@link #collapsedHeight}.
     * <p>
     * This method is handy when you want to switch from one collapsed state back to two.
     * For example detail of item supports just one state and on back press your view must support again
     * two states. Call this method when you want to restore the behaviour.
     * <p>
     * This might cause card immediate bump up when {@link #collapsedHeight} is bigger than previous one!
     */
    public void restoreTwoStates() {
        if (currentSheet != null) {
            // do not change anything when it is already set up - it could possibly break some animations
            if (currentSheet.getPeekHeightCollapsed() != collapsedHeight || currentSheet.getPeekHeightSemiCollapsed() != semiCollapsedHeight) {
                currentSheet.setPeekHeights(collapsedHeight, semiCollapsedHeight, false);
            }

            setMapPaddingAndTranslation();
        }
    }

    /**
     * Animates to {@link #semiCollapsedHeight} - in this state card be only collapsed to "big" state
     */
    public void animateToSemiCollapsed() {
        if (currentSheet != null) {
            currentSheet.animateAndSetHeights(semiCollapsedHeight);
            setMapPaddingAndTranslation();

        }
    }

    /**
     * Sets up big peek heights - in this state card be only collapsed to "big" {@link #semiCollapsedHeight} state
     */
    public void restoreSemiCollapsedState() {
        if (currentSheet != null) {
            currentSheet.setPeekHeights(semiCollapsedHeight, semiCollapsedHeight);
            setMapPaddingAndTranslation();
        }
    }

    /**
     * Default callback for bottom sheet that will influence status bar background and items colours
     *
     * @return callback
     */
    @NonNull
    private BottomSheetCallback getDefaultCallback() {
        return new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, BottomSheetState newState) {
                // Disabling map moving when sheet is expanded
                SheetsHelper.this.onStateChanged(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                onSheetSlide(bottomSheet, slideOffset);
            }
        };
    }

    /**
     * Callback from sheet when its state changed
     * here we will change rendering of underlying layer to achieve smoother animations
     *
     * @param newState current state of bottom sheet
     */
    private void onStateChanged(BottomSheetState newState) {
        if (bottomSheetView != null && sheetsHelperView != null) {
            sheetsHelperView.setMapGesturesEnabled(newState != BottomSheetState.STATE_EXPANDED);
            sheetsHelperView.setMapVisible(newState != BottomSheetState.STATE_EXPANDED);

            // Acceleration helps us get smoother animations
            if (newState == BottomSheetState.STATE_DRAGGING || newState == BottomSheetState.STATE_SETTLING) {
                sheetsHelperView.getMapContainer().setLayerType(View.LAYER_TYPE_HARDWARE, null);
                bottomSheetView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                sheetsHelperView.getMapContainer().setLayerType(View.LAYER_TYPE_NONE, null);
                bottomSheetView.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            if (currentSheet != null) {
                currentSheet.setAutoInitHeight();
            }
        }
    }

    /**
     * Callback triggered when sheet slides, we react to it and change status bar properties
     *
     * @param bottomSheet bottom sheet
     * @param slideOffset offset in range from 0 to 1
     */
    private void onSheetSlide(@NonNull View bottomSheet, float slideOffset) {
        if (sheetsHelperView != null) {
            setMapPaddingAndTranslation(bottomSheet, collapsedHeight, semiCollapsedHeight);

            int nonExpanded = sheetsHelperView.getStatusBarColorDefault();
            int expanded = sheetsHelperView.getStatsBarColorExpanded();
            slideOffset = Math.max(0, slideOffset);
            if (slideOffset >= 0.9) {
                sheetsHelperView.setStatusBarColor(DesignUtils.evaluateColor((float) (slideOffset - 0.9) * 10, nonExpanded, expanded));
            } else if (slideOffset < 0.9) {
                sheetsHelperView.setStatusBarColor(nonExpanded);
            }

            sheetsHelperView.showDarkStatusBarIcons(slideOffset >= 0.975);
        }
    }

    /**
     * Sets map padding which affects zooming and also moves items above sheet
     */
    public void setMapPaddingAndTranslation() {
        if (currentSheet != null && bottomSheetView != null) {
            // View has not gone through full layout cycle, wait till this one is done
            if (!ViewCompat.isLaidOut(bottomSheetView)) {
                // Adding onLayoutChangedListener does not work properly cause Sheet reacts after view is laid out
                bottomSheetView.postDelayed(this::setMapPaddingAndTranslation, 75);
            } else {
                setMapPaddingAndTranslation(bottomSheetView, collapsedHeight, semiCollapsedHeight);
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
    private void setMapPaddingAndTranslation(@NonNull View bottomSheet, int collapsedHeight, int maxMapPadding) {
        if (sheetsHelperView != null) {
            int calculatedPadding = bottomSheet.getHeight() - bottomSheet.getTop();
            int realPadding = Math.min(calculatedPadding, maxMapPadding);

            int translationY = Math.min(0, -(realPadding - collapsedHeight) / 2);
            int paddingTop = Math.max(0, mapTopPadding - translationY);
            int paddingBot = Math.max(0, realPadding + translationY);

            sheetsHelperView.setMapPadding(mapLogoPaddingLeft, paddingTop, 0, paddingBot + mapLogoPaddingBottom);
            sheetsHelperView.getMapContainer().setTranslationY(translationY);
            for (View view : sheetsHelperView.getBottomItems()) {
                view.setTranslationY(-realPadding);
            }
        }
    }

    public BottomSheetState getState() {
        if (currentSheet != null) {
            return currentSheet.getState();
        }

        return BottomSheetState.STATE_HIDDEN;
    }

    public void setState(BottomSheetState newState) {
        if (currentSheet != null) {
            currentSheet.setState(newState);
        }
    }

    /**
     * Call after instance stare is restored to restore status bar state
     *.
     * This only does something when sheet was previously expanded
     */
    public void onInstanceStateRestored() {
        if (currentSheet != null && bottomSheetView != null && currentSheet.getState() == BottomSheetState.STATE_EXPANDED) {
            onSheetSlide(bottomSheetView, SLIDE_OFFSET_SHEET_EXPANDED);
        }
    }

    public static class Builder {
        private SheetsHelper mHelper;

        public Builder(Context context) {
            this(context, null);
        }

        /**
         * Creates new instance of Builder and sets defaults.
         * <ul>
         * <li>map padding is by default equal to sum of status bar and toolbar height</li>
         * <li>padding of Google logo to 16dp for both sides</li>
         * </ul>
         *
         * @param context   current context
         * @param sheetView callback view
         */
        public Builder(@NonNull Context context, @Nullable SheetsHelperView sheetView) {
            mHelper = new SheetsHelper(sheetView);
            int logoPaddingLeft = context.getResources().getDimensionPixelSize(R.dimen.sheet_logo_additional_padding_left);
            int logoPaddingBottom = context.getResources().getDimensionPixelSize(R.dimen.sheet_logo_additional_padding_bottom);

            setGoogleLogoPadding(logoPaddingLeft, logoPaddingBottom);
            setMapTopPadding(DesignUtils.getStatusBarHeight(context) + DesignUtils.getToolbarHeight(context));
        }

        /**
         * Call to set custom Google map padding. By default we will setup constants so logo will have
         * 16dp from left and bottom.
         *
         * @param leftPx   padding from left side
         * @param bottomPx padding from bottom side
         * @return buolder
         */
        public Builder setGoogleLogoPadding(int leftPx, int bottomPx) {
            mHelper.mapLogoPaddingBottom = bottomPx;
            mHelper.mapLogoPaddingLeft = leftPx;
            return this;
        }

        /**
         * Sets height of sheet when it is fully collapsed
         *
         * @param heightPx height in px
         * @return builder
         */
        public Builder setCollapsedHeight(int heightPx) {
            mHelper.collapsedHeight = heightPx;
            return this;
        }

        /**
         * Sets height of sheet when it is partially collapsed
         *
         * @param heightPx height in px
         * @return builder
         */
        public Builder setSemiCollapsedHeight(int heightPx) {
            mHelper.semiCollapsedHeight = heightPx;
            return this;
        }

        /**
         * Sets top padding of a map
         *
         * @param paddingPx padding
         * @return builder
         */
        public Builder setMapTopPadding(int paddingPx) {
            mHelper.mapTopPadding = paddingPx;
            return this;
        }

        /**
         * @return CONSTRUCT ME !!!
         */
        public SheetsHelper build() {
            return mHelper;
        }
    }
}