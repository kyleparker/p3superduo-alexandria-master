package it.jaschke.alexandria;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

// DONE: Update to current standards
// NOT IMPLEMENTED: Preset current selection of start screen
// DONE: Add a "clear list" option
/**
 * Standard settings activity with a preference fragment
 *
 * Created by saj on 27/01/15.
 */
public class SettingsActivity extends BaseActivity {
    private final static String SETTINGS_NAME = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mShouldBeFloatingWindow = shouldBeFloatingWindow();
        if (mShouldBeFloatingWindow) {
            setupFloatingWindow(R.dimen.floating_window_width, R.dimen.floating_window_height);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();

        // HACK: cannot get FitsSystemWindow to work on LinearLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.theme_primary_dark));
        }

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsPreferenceFragment()).commit();
    }

    /**
     * Setup the toolbar for the activity
     */
    private void setupToolbar() {
        final Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(mShouldBeFloatingWindow ? R.drawable.ic_action_close : R.drawable.ic_action_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.finish();
//                mActivity.startActivity(new Intent(mActivity, MainActivity.class));
            }
        });
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle(mActivity.getString(R.string.title_settings));
            }
        });
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(SETTINGS_NAME);
            preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);

            addPreferencesFromResource(R.xml.preferences);

            Preference clearLibrary = findPreference(getString(R.string.settings_clear_library_key));
            clearLibrary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDeleteBooksDialog();
                    return true;
                }
            });
        }

        private void showDeleteBooksDialog() {
            MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                    .content(mActivity.getString(R.string.dialog_remove_books_message))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            mProvider.deleteBooks();
                            Toast.makeText(mActivity, R.string.toast_success_books_removed, Toast.LENGTH_LONG).show();
                        }
                    })
                    .positiveText(R.string.dialog_remove)
                    .negativeText(R.string.dialog_cancel)
                    .build();

            dialog.show();
        }
    }
}
