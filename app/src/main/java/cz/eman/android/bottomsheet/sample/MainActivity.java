package cz.eman.android.bottomsheet.sample;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import cz.eman.android.bottomsheet.core.BottomSheetTwoStatesBehaviour;
import cz.eman.android.bottomsheet.manipulation.SheetsHelper;
import cz.eman.android.bottomsheet.manipulation.SheetsHelperView;
import cz.eman.android.bottomsheet.utils.WindowCompat;

public class MainActivity extends AppCompatActivity implements SheetsHelperView {

    private ViewGroup mapContainer;
    private ViewGroup bottomSheetView;

    private Button buttonAboveMap;

    private BottomSheetTwoStatesBehaviour behaviour;
    private SheetsHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSystemUiVisibility();

        initViews();
        attachSheet();
    }

    private void initViews() {
        mapContainer = findViewById(R.id.map_container);
        buttonAboveMap = findViewById(R.id.button_above_map);
        bottomSheetView = findViewById(R.id.bottom_sheet);
    }

    /**
     * Call to initialize sheet
     */
    private void attachSheet() {
        behaviour = BottomSheetTwoStatesBehaviour.from(bottomSheetView);
        helper = new SheetsHelper.Builder(this, this)
                .setCollapsedHeight(getResources().getDimensionPixelSize(R.dimen.sheet_collapsed_height))
                .setSemiCollapsedHeight(getResources().getDimensionPixelSize(R.dimen.sheet_semicollapsed_height))
                .build();


        helper.init(bottomSheetView, behaviour); // allows swipe between two collapsed states
        // helper.initSemiCollapsed(bottomSheetView, behaviour); // just one collapsed state - semi collapsed
        // helper.initCollapsed(bottomSheetView, behaviour); // just one collapsed state - collapsed
    }

    /**
     * Mandatory to call to setup UI, above Android O we also make status bar light
     */
    private void setSystemUiVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        helper.onInstanceStateRestored();
    }

    @NonNull
    @Override
    public ViewGroup getMapContainer() {
        return mapContainer;
    }

    @Override
    public void setMapVisible(boolean visible) {
        mapContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @NonNull
    @Override
    public View[] getBottomItems() {
        // these items will be always above sheet until 'semi-collapsed' constant is reached. Sheet will overlap it afterwards.
        return new View[]{buttonAboveMap};
    }

    @Override
    public void showDarkStatusBarIcons(boolean show) {
        WindowCompat.setDarkStatusBarIcons(this, show);
    }

    @Override
    public void setStatusBarColor(int color) {
        WindowCompat.setStatusBarColor(this, color);
    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {
        // TODO GoogleMap impl
        // mGoogleMap.setPadding(left, top, right, bottom);
    }

    @Override
    public void setMapGesturesEnabled(boolean enabled) {
        // TODO GoogleMap impl
        // mGoogleMap.getUiSettings().setAllGesturesEnabled(enabled);
    }

    @Override
    public int getStatusBarColorDefault() {
        return ContextCompat.getColor(this, R.color.color_status_bar_transparent);
    }

    @Override
    public int getStatsBarColorExpanded() {
        return ContextCompat.getColor(this, R.color.color_status_bar_full);
    }
}
