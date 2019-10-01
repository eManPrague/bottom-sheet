package cz.eman.android.bottomsheet.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import android.view.View

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
fun Activity.setStatusBarColor(@ColorInt color: Int) {
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