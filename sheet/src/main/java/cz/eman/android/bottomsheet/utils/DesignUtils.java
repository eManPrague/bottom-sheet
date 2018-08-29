package cz.eman.android.bottomsheet.utils;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Michal [michal.mrocek@eman.cz] on 14.02.18.
 */

public class DesignUtils {

    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return  context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getToolbarHeight(@NonNull Context context) {
        return context.getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_action_bar_default_height_material);
    }

    public static int evaluateColor(float fraction, int startInt, int endInt) {
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24 |
                (startR + (int) (fraction * (endR - startR))) << 16 |
                (startG + (int) (fraction * (endG - startG))) << 8 |
                (startB + (int) (fraction * (endB - startB)));
    }
}
