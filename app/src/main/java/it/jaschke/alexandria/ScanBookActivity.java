package it.jaschke.alexandria;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import it.jaschke.alexandria.camera.BarcodeTrackerFactory;
import it.jaschke.alexandria.camera.CameraSourcePreview;
import it.jaschke.alexandria.camera.GraphicOverlay;
import it.jaschke.alexandria.objects.Book;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.utils.BezelImageView;
import it.jaschke.alexandria.utils.Constants;
import it.jaschke.alexandria.utils.DialogUtils;
import it.jaschke.alexandria.utils.UIUtils;

// DONE: Check whether the device is online and ready to search for a book
/**
 * Activity to scan a barcode to retrieve the ISBN for a book
 *
 * Created by kyleparker on 9/9/2015.
 */
public class ScanBookActivity extends BaseActivity {
    private static final String TAG = "ScanBookActivity";
    private static final int RC_HANDLE_GMS = 9001;

    private BroadcastReceiver mMessageReceiver;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private FloatingActionButton mFabAdd;
    private MaterialDialog mAddLoadingDialog;

    private Book mBook;

    private String mBookIsbn;
    private boolean mAddLoadingShown = false;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_book);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.graphic_overlay);

        if (savedInstanceState != null) {
            mBookIsbn = savedInstanceState.getString(Constants.EXTRA_EAN);
            mAddLoadingShown = savedInstanceState.getBoolean(Constants.KEY_ADD_LOADING_DIALOG);

            if (mAddLoadingShown) {
                mAddLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_searching, false);
                mAddLoadingDialog.show();
            }
        }

        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter(Constants.MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).registerReceiver(mMessageReceiver, filter);

        createCameraSource();
        setupToolbar();
        setupFAB();

        // HACK: cannot get FitsSystemWindow to work on LinearLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.theme_primary_dark));
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).unregisterReceiver(mMessageReceiver);

        if (mCameraSource != null) {
            mCameraSource.release();
        }
        if (mAddLoadingDialog != null && mAddLoadingDialog.isShowing()) {
            mAddLoadingDialog.dismiss();
        }

        super.onDestroy();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.EXTRA_EAN, mBookIsbn);
        outState.putBoolean(Constants.KEY_ADD_LOADING_DIALOG, mAddLoadingShown);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        final BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        final BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        barcodeFactory.setOnBarcodeCreatedListener(new BarcodeTrackerFactory.OnBarcodeCreatedListener() {
            @Override
            public void onBarcodeCreated(final Barcode barcode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (TextUtils.isEmpty(mBookIsbn)) {
                            mCameraSource.stop();

                            mBookIsbn = barcode.rawValue;
                            Log.e("***> book", mBookIsbn);

                            if (mAddLoadingDialog != null && mAddLoadingDialog.isShowing()) {
                                mAddLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID,
                                        R.string.dialog_searching, false);
                                mAddLoadingDialog.show();
                                mAddLoadingShown = true;
                            }

                            // Once we have an ISBN, start a book intent
                            Intent bookIntent = new Intent(mActivity, BookService.class);
                            bookIntent.putExtra(Constants.EXTRA_EAN, mBookIsbn);
                            bookIntent.putExtra(Constants.EXTRA_INSERT, false);
                            bookIntent.setAction(BookService.FETCH_BOOK);
                            mActivity.startService(bookIntent);
                        }
                    }
                });
            }
        });

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .build();
    }

    /**
     * Clear the book info on the display card
     */
    private void clearBookInfo() {
        TextView title = (TextView) findViewById(R.id.book_title);
        title.setText(mActivity.getString(R.string.content_book_not_found));

        findViewById(R.id.book_subtitle).setVisibility(View.GONE);
        findViewById(R.id.book_author).setVisibility(View.GONE);

        final BezelImageView bookCover = (BezelImageView) findViewById(R.id.book_cover);
        bookCover.setImageResource(R.drawable.ic_placeholder_book);

        displayFAB(false);
    }

    /**
     * Display the book info from the BookService
     */
    private void displayBookInfo() {
        if (mBook == null) {
            return;
        }

        TextView title = (TextView) findViewById(R.id.book_title);
        title.setText(mBook.getTitle());

        if (!TextUtils.isEmpty(mBook.getSubtitle())) {
            TextView subtitle = (TextView) findViewById(R.id.book_subtitle);
            subtitle.setText(mBook.getSubtitle());
            subtitle.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(mBook.author.getName())) {
            TextView authors = (TextView) findViewById(R.id.book_author);
            String[] authorsArr = mBook.author.getName().split(",");
            authors.setLines(authorsArr.length);
            authors.setText(mBook.author.getName().replace(",", "\n"));
            authors.setVisibility(View.VISIBLE);
        }

        if (Patterns.WEB_URL.matcher(mBook.getImageUrl()).matches()) {
            final ImageView bookCover = (ImageView) findViewById(R.id.book_cover);

            Picasso.with(mActivity)
                    .load(mBook.getImageUrl())
                    .resize(300, 300)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder_book)
                    .into(bookCover);

            bookCover.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Enable/disable the FAB to add the book to the library
     *
     * @param enabled
     */
    private void displayFAB(boolean enabled) {
        if (enabled) {
            mFabAdd.setEnabled(true);
            mFabAdd.setImageResource(R.drawable.ic_fab_check_enabled);
            mFabAdd.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.fab_normal)));
            mFabAdd.setRippleColor(mActivity.getResources().getColor(R.color.fab_pressed));

            if (UIUtils.isLollipop()) {
                mFabAdd.setOnTouchListener(UIUtils.getFABTouchListener(mActivity, mFabAdd));
            }
        } else {
            mFabAdd.setEnabled(false);
            mFabAdd.setImageResource(R.drawable.ic_fab_check_disabled);
            mFabAdd.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.fab_disabled)));
            mFabAdd.setRippleColor(mActivity.getResources().getColor(R.color.fab_disabled));
        }
    }

    /**
     * Setup the FAB
     */
    private void setupFAB() {
        mFabAdd = (FloatingActionButton) findViewById(R.id.fab_add_scan);
        mFabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // DONE: Insert book
                mProvider.insertBook(mBook);
                if (mBook.author != null) {
                    mProvider.insertAuthor(mBook.author);
                }
                if (mBook.category != null) {
                    mProvider.insertCategory(mBook.category);
                }

                Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_book_found), Toast.LENGTH_LONG).show();
                startActivity(new Intent(mActivity, MainActivity.class));
            }
        });

        displayFAB(!TextUtils.isEmpty(mBookIsbn));
    }

    /**
     * Setup the toolbar for the activity
     */
    private void setupToolbar() {
        final Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(R.drawable.ic_action_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.finish();
            }
        });
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle(mActivity.getString(R.string.title_scan_book));
            }
        });
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(Constants.MESSAGE_KEY, 0) > 0) {
                // 1 = book not found
                // 2 = book found
                // 3 = book already added
                int key = intent.getIntExtra(Constants.MESSAGE_KEY, 0);
                Log.e("***> key", key + "");

                switch (key) {
                    case Constants.Message.NOT_FOUND:
                        displayFAB(false);
                        clearBookInfo();
                        break;
                    case Constants.Message.BOOK_FOUND:
                        displayFAB(true);
                        mBook = intent.getParcelableExtra(Constants.KEY_BOOK);
                        displayBookInfo();
                        break;
                    case Constants.Message.ALREADY_ADDED:
                        displayFAB(false);
                        mBook = intent.getParcelableExtra(Constants.KEY_BOOK);
                        displayBookInfo();
                        Toast.makeText(mActivity, mActivity.getString(R.string.toast_error_book_already_added), Toast.LENGTH_LONG).show();
                        break;
                }
            }

            if (mAddLoadingDialog != null && mAddLoadingDialog.isShowing()) {
                mAddLoadingDialog.dismiss();
            }
            mAddLoadingShown = false;
        }
    }
}
