package it.jaschke.alexandria;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.WindowManager;

import it.jaschke.alexandria.data.ProviderUtils;

// TODO: Consider implementing RTL support
/**
 * Base activity for Alexandria
 *
 * Created by kyleparker on 9/15/2015.
 */
public class BaseActivity extends AppCompatActivity {
    protected static AppCompatActivity mActivity;
    protected static ProviderUtils mProvider;
    private Toolbar mActionBarToolbar;

    protected boolean mShouldBeFloatingWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        mProvider = ProviderUtils.Factory.get(mActivity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // HACK: Resolve issue with MaterialDialog problem
        // https://github.com/afollestad/material-dialogs/issues/279
        mActivity = this;
    }

    /**
     * Retrieve the base toolbar for the activity.
     *
     * @return
     */
    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }

        return mActionBarToolbar;
    }

    /**
     * Floating window is enabled per the styles.xml in the sw600dp folder (floating window for tablets)
     */
    protected void setupFloatingWindow(int width, int height) {
        // configure this Activity as a floating window, dimming the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(width);
        params.height = getResources().getDimensionPixelSize(height);
        params.alpha = 1;
        params.dimAmount = 0.80f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    /**
     * Determine based on the style, whether the window should be floating or full screen
     * @return
     */
    protected boolean shouldBeFloatingWindow() {
        Resources.Theme theme = getTheme();
        TypedValue floatingWindowFlag = new TypedValue();
        return !((theme == null) || !theme.resolveAttribute(R.attr.isFloatingWindow, floatingWindowFlag, true)) && (floatingWindowFlag.data != 0);
    }
}
