package it.jaschke.alexandria.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.view.MotionEvent;
import android.view.View;

import it.jaschke.alexandria.R;

/**
 * Created by kyleparker on 9/1/2015.
 */
public class UIUtils {

    /**
     * API Level >= 21
     * Lollipop. A flat one with beautiful shadows. But still tasty.
     *
     * @return
     */
    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Check the wifi state of the device - if not on, send the user to the settings to enable
     *
     * @param context
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected() && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    /**
     * Define the touch listener for the FAB - this will handle the elevation during the press event
     * @param context
     * @param fab
     * @return
     */
    public static View.OnTouchListener getFABTouchListener(final Context context, final FloatingActionButton fab) {
        return new View.OnTouchListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        fab.setElevation(context.getResources().getDimension(R.dimen.elevation_fab_pressed));
                        break;
                    case MotionEvent.ACTION_UP:
                        fab.setElevation(context.getResources().getDimension(R.dimen.elevation_fab_default));
                        break;
                }

                return false;
            }
        };
    }
}
