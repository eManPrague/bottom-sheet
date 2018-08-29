package cz.eman.android.bottomsheet.utils;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Sort of compat class for operations with {@link android.view.Window}
 * Created by Michal [michal.mrocek@eman.cz] on 05.09.17.
 */

public class WindowCompat {

    /**
     * Will tint icons of action bar to dark or light color
     *
     * @param activity activity
     * @param show     true when dark icons should be shown
     */
    public static void setDarkStatusBarIcons(@NonNull Activity activity, boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int visibility = activity.getWindow().getDecorView().getSystemUiVisibility();
            if (show) {
                activity.getWindow().getDecorView().setSystemUiVisibility(visibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                activity.getWindow().getDecorView().setSystemUiVisibility(visibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    /**
     * Sets color to status bar
     *
     * @param activity activity
     * @param color    color of bar
     */
    public static void setStatusBarColor(@NonNull Activity activity, @ColorInt int color) {
        activity.getWindow().setStatusBarColor(color);
    }

    /**
     * Sets color to navigation bar
     *
     * @param activity activity
     * @param color    color of bar
     */
    public static void setNavigationBarColor(@NonNull Activity activity, @ColorInt int color) {
        activity.getWindow().setNavigationBarColor(color);
    }
}
