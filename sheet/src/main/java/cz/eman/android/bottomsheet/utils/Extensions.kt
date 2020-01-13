package cz.eman.android.bottomsheet.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import android.view.View
import android.view.ViewGroup
import androidx.core.view.NestedScrollingChild

/**
 * @author Andrej Martinák <andrej.martinak@eman.cz>
 */

fun Context.getStatusBarHeight(): Int {
    val resourceId = this.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else 0
}

fun Context.getToolbarHeight(): Int {
    return this.resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_bar_default_height_material)
}

/**
 * Will tint icons of action bar to dark or light color
 *
 * @param show true when dark icons should be shown
 */
fun Activity.setDarkStatusBarIcons(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val visibility = this.window.decorView.systemUiVisibility
        if (show) {
            this.window.decorView.systemUiVisibility = visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            this.window.decorView.systemUiVisibility = visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}

/**
 * Sets color to status bar
 */
fun Activity.updateStatusBarColor(@ColorInt color: Int) {
    this.window.statusBarColor = color
}

/**
 * Sets color to navigation bar
 *
 * @param color color of bar
 */
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    this.window.navigationBarColor = color
}

/**
 * Returns first [NestedScrollingChild] of a ViewGroup, or null
 */
fun View.findFirstScrollingChild(): View? {
    if (this is NestedScrollingChild) {
        return this
    }
    if (this is ViewGroup) {
        var i = 0
        val count = this.childCount
        while (i < count) {
            val scrollingChild = this.getChildAt(i).findFirstScrollingChild()
            if (scrollingChild != null) {
                return scrollingChild
            }
            i++
        }
    }
    return null
}