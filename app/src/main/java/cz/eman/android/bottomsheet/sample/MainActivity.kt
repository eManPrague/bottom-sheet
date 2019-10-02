package cz.eman.android.bottomsheet.sample

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

import cz.eman.android.bottomsheet.core.BottomSheetTwoStatesBehaviour
import cz.eman.android.bottomsheet.manipulation.SheetsHelper
import cz.eman.android.bottomsheet.manipulation.SheetsHelperView
import cz.eman.android.bottomsheet.utils.setDarkStatusBarIcons
import cz.eman.android.bottomsheet.utils.updateStatusBarColor
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SheetsHelperView {

    private lateinit var behaviour: BottomSheetTwoStatesBehaviour<*>
    private lateinit var helper: SheetsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemUiVisibility()
        attachSheet()
    }

    /**
     * Call to initialize sheet
     */
    private fun attachSheet() {
        behaviour = BottomSheetTwoStatesBehaviour.from(bottomSheet)
        helper = SheetsHelper.Builder(this, this)
                .setCollapsedHeight(resources.getDimensionPixelSize(R.dimen.sheet_collapsed_height))
                .setSemiCollapsedHeight(resources.getDimensionPixelSize(R.dimen.sheet_semicollapsed_height))
                .build()


        helper.init(bottomSheet, behaviour) // allows swipe between two collapsed states
//         helper.initSemiCollapsed(bottomSheet, behaviour); // just one collapsed state - semi collapsed
//         helper.initCollapsed(bottomSheet, behaviour); // just one collapsed state - collapsed
    }

    /**
     * Mandatory to call to setup UI, above Android O we also make status bar light
     */
    private fun setSystemUiVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        helper.onInstanceStateRestored()
    }

    override fun getMapContainer(): ViewGroup {
        return mapContainer
    }

    override fun setMapVisible(visible: Boolean) {
        mapContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun getBottomItems(): Array<View> {
        // these items will be always above sheet until 'semi-collapsed' constant is reached. Sheet will overlap it afterwards.
        return arrayOf(buttonAboveMap)
    }

    override fun showDarkStatusBarIcons(show: Boolean) {
        this.setDarkStatusBarIcons(show)
    }

    override fun setStatusBarColor(color: Int) {
        this.updateStatusBarColor(color)
    }

    override fun setMapPadding(left: Int, top: Int, right: Int, bottom: Int) {
        // TODO GoogleMap impl
        // mGoogleMap.setPadding(left, top, right, bottom);
    }

    override fun setMapGesturesEnabled(enabled: Boolean) {
        // TODO GoogleMap impl
        // mGoogleMap.getUiSettings().setAllGesturesEnabled(enabled);
    }

    override fun getStatusBarColorDefault(): Int {
        return ContextCompat.getColor(this, R.color.color_status_bar_transparent)
    }

    override fun getStatsBarColorExpanded(): Int {
        return ContextCompat.getColor(this, R.color.color_status_bar_full)
    }
}
