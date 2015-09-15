package it.jaschke.alexandria;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.data.ProviderUtils;
import it.jaschke.alexandria.objects.Book;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.utils.Adapters;
import it.jaschke.alexandria.utils.Constants;
import it.jaschke.alexandria.utils.DialogUtils;
import it.jaschke.alexandria.utils.UIUtils;

// DONE: The status bar is not transparent when the navdrawer is open
// DONE: Implement a proper navdrawer, per the Android Design specs
// DONE: Fix navdrawer for tablet (portrait and landscape orientation)
// DONE: Add CoordindatorLayout to the activity/fragments
// DONE: Fix the back button behavior (blank activity for the book list fragment)
// DONE: Check the savedInstanceState to ensure the correct data is saved on rotation
// NOT IMPLEMENTED: Implement start screen functionality
// NOT IMPLEMENTED: Possible to add swipe-to-delete for the list? -- Cannot use the grid layout
public class MainActivity extends BaseActivity {
    private Toolbar mToolbar;
    protected Handler mHandler;

    private BroadcastReceiver mMessageReceiver;

    private List<Book> mBookList;
    private Adapters.BookListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private MaterialDialog mProgressLoadingDialog;
    private MaterialDialog mAddLoadingDialog;
    private MaterialDialog mAboutDialog;
    private MaterialDialog mAddBookDialog;
    private MaterialDialog mEnterIsbnDialog;
    private EditText mIsbn;

    // Navigation drawer
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    private static final TypeEvaluator<?> ARGB_EVALUATOR = new ArgbEvaluator();
    private ObjectAnimator mStatusBarColorAnimator;
    private boolean mProgressLoadingShown = false;
    private boolean mAddLoadingShown = false;
    private boolean mActionBarShown = true;
    private boolean mAboutDialogShown = false;
    private boolean mAddBookDialogShown = false;
    private boolean mEnterIsbnDialogShown = false;
    private int mNormalStatusBarColor;
    private String mSearchIsbn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // DONE: Remove tablet check and utilize the resource folders to retrieve the proper layout for the tablet
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mNormalStatusBarColor = getResources().getColor(R.color.theme_primary_dark);

        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter(Constants.MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).registerReceiver(mMessageReceiver, filter);

        setupToolbar();
        setupView();
        setupFAB();
        if (savedInstanceState != null) {
            mBookList = savedInstanceState.getParcelableArrayList(Constants.KEY_BOOK_LIST);
            mSearchIsbn = savedInstanceState.getString(Constants.KEY_SEARCH_ISBN);
            mAboutDialogShown = savedInstanceState.getBoolean(Constants.KEY_ABOUT_DIALOG);
            mAddBookDialogShown = savedInstanceState.getBoolean(Constants.KEY_ADD_BOOK_DIALOG);
            mEnterIsbnDialogShown = savedInstanceState.getBoolean(Constants.KEY_ENTER_ISBN_DIALOG);
            mAddLoadingShown = savedInstanceState.getBoolean(Constants.KEY_ADD_LOADING_DIALOG);
            mProgressLoadingShown = savedInstanceState.getBoolean(Constants.KEY_LOADING_DIALOG);

            if (mAboutDialogShown) {
                showAboutDialog();
            }
            if (mAddBookDialogShown) {
                showAddBookDialog();
            }
            if (mEnterIsbnDialogShown) {
                showEnterIsbnDialog();
            }
            if (mAddLoadingShown) {
                mAddLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_searching, false);
                mAddLoadingDialog.show();
            }
            if (mProgressLoadingShown) {
                mProgressLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_loading, false);
                mProgressLoadingDialog.show();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupNavDrawer();

        mDrawerToggle.syncState();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(NAVDRAWER_LAUNCH_DELAY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show items in the action bar relevant to this screen
        // if the drawer is not showing. Otherwise, let the drawer
        // decide what to show in the action bar.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_backup_db:
                handleBackup();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(Constants.KEY_BOOK_LIST, (ArrayList) mBookList);
        outState.putString(Constants.KEY_SEARCH_ISBN, mSearchIsbn);
        outState.putBoolean(Constants.KEY_ABOUT_DIALOG, mAboutDialogShown);
        outState.putBoolean(Constants.KEY_ADD_BOOK_DIALOG, mAddBookDialogShown);
        outState.putBoolean(Constants.KEY_ENTER_ISBN_DIALOG, mEnterIsbnDialogShown);
        outState.putBoolean(Constants.KEY_ADD_LOADING_DIALOG, mAddLoadingShown);
        outState.putBoolean(Constants.KEY_LOADING_DIALOG, mProgressLoadingShown);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookList();
        hideKeyboard();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).unregisterReceiver(mMessageReceiver);

        if (mAboutDialog != null && mAboutDialog.isShowing()) {
            mAboutDialog.dismiss();
        }
        if (mAddBookDialog != null && mAddBookDialog.isShowing()) {
            mAddBookDialog.dismiss();
        }
        if (mEnterIsbnDialog != null && mEnterIsbnDialog.isShowing()) {
            mEnterIsbnDialog.dismiss();
        }
        if (mAddLoadingDialog != null && mAddLoadingDialog.isShowing()) {
            mAddLoadingDialog.dismiss();
        }
        if (mProgressLoadingDialog != null && mProgressLoadingDialog.isShowing()) {
            mProgressLoadingDialog.dismiss();
        }
        super.onDestroy();
    }

    /**
     * ItemClickListener for the book list
     */
    private Adapters.BookListAdapter.OnItemClickListener mOnItemClickListener = new Adapters.BookListAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            Book book = mAdapter.getItem(position);
            if (book != null) {
                Intent intent = new Intent(mActivity, BookDetailActivity.class) ;
                intent.putExtra(Constants.EXTRA_EAN, book.getId());
                intent.putExtra(Constants.EXTRA_BOOK_TITLE, book.getTitle());
                startActivity(intent);
            }
        }
    };

    /**
     * Handle the show/hide option for the toolbar.
     *
     * @param shown
     */
    private void onActionBarAutoShowOrHide(boolean shown) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }

        mStatusBarColorAnimator = ObjectAnimator.ofInt(mDrawerLayout, "statusBarBackgroundColor",
                shown ? Color.BLACK : mNormalStatusBarColor,
                shown ? mNormalStatusBarColor : Color.BLACK).setDuration(NAVDRAWER_LAUNCH_DELAY);

        if (mDrawerLayout != null) {
            mStatusBarColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ViewCompat.postInvalidateOnAnimation(mDrawerLayout);
                }
            });
        }

        mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mStatusBarColorAnimator.start();
    }

    private void onNavDrawerStateChanged(boolean isOpen) {
        if (isOpen) {
            autoShowOrHideActionBar(true);
        }

        hideKeyboard();
    }

    private void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }

        mActionBarShown = show;
        onActionBarAutoShowOrHide(show);
    }

    private void handleAddBook() {
        // DONE: Error handling for digits only
        String search = mIsbn.getText().toString();
        long isbn;

        // Catch isbn10 numbers
        if (search.length() == 10 && !search.startsWith("978")) {
            search = "978" + search;
        }
        if (search.length() < 13) {
            Toast.makeText(mActivity, R.string.toast_error_invalid_isbn, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            isbn = Long.valueOf(search);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(mActivity, R.string.toast_error_invalid_isbn, Toast.LENGTH_LONG).show();
            return;
        }

        mAddLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_searching, false);
        mAddLoadingDialog.show();
        mAddLoadingShown = true;

        // Once we have an ISBN, start a book intent
        Intent bookIntent = new Intent(mActivity, BookService.class);
        bookIntent.putExtra(Constants.EXTRA_EAN, Long.toString(isbn));
        bookIntent.putExtra(Constants.EXTRA_INSERT, true);
        bookIntent.setAction(BookService.FETCH_BOOK);
        mActivity.startService(bookIntent);
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Return whether the navdrawer is open.
     *
     * @return
     */
    private boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    private void loadBookList() {
        mProgressLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_loading, false);
        mProgressLoadingDialog.show();
        mProgressLoadingShown = true;

        mAdapter = new Adapters.BookListAdapter(mActivity);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        Runnable load = new Runnable() {
            public void run() {
                try {
                    if (mBookList == null || mBookList.isEmpty()) {
                        mBookList = new ArrayList<>();
                        String searchString = mSearchView.getQuery().toString();

                        if (TextUtils.isEmpty(searchString)) {
                            mBookList = mProvider.getBookList();
                        } else {
                            mBookList = mProvider.getBookList("%" + searchString + "%");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    mActivity.runOnUiThread(loadBookListRunnable);
                }
            }
        };

        Thread thread = new Thread(null, load, "loadBookList");
        thread.start();
    }

    private final Runnable loadBookListRunnable = new Runnable() {
        public void run() {
            findViewById(R.id.search_container).setVisibility(View.VISIBLE);
            findViewById(R.id.book_list).setVisibility(View.VISIBLE);

            if (mBookList != null && mBookList.size() > 0) {
                mAdapter.addAll(mBookList);

                findViewById(R.id.empty_book_list_container).setVisibility(View.GONE);
            } else {
                findViewById(R.id.empty_book_list_container).setVisibility(View.VISIBLE);
                findViewById(R.id.search_container).setVisibility(View.GONE);
                findViewById(R.id.book_list).setVisibility(View.GONE);
            }

            mProgressLoadingDialog.dismiss();
            mProgressLoadingShown = false;
        }
    };

    private void setupFAB() {
        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add_book);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // DONE: Show dialog to select manual entry or scan
                if (UIUtils.isOnline(mActivity)) {
                    showAddBookDialog();
                } else {
                    Toast.makeText(mActivity, R.string.toast_error_offline, Toast.LENGTH_LONG).show();
                }
            }
        });

        if (UIUtils.isLollipop()) {
            fabAdd.setOnTouchListener(UIUtils.getFABTouchListener(mActivity, fabAdd));
        }
    }

    /**
     * Sets up the navigation drawer as appropriate.
     */
    private void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }

        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.theme_primary_dark));
        mDrawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout, R.string.navdrawer_open, R.string.navdrawer_close);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                onNavDrawerStateChanged(false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                onNavDrawerStateChanged(true);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                onNavDrawerStateChanged(isNavDrawerOpen());
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }
        });

        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_action_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        NavigationView navigation = (NavigationView) findViewById(R.id.navigation_view);
        navigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(final MenuItem menuItem) {
                mDrawerLayout.closeDrawer(GravityCompat.START);

                // launch the target Activity after a short delay, to allow the close animation to play
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_settings:
                                mBookList = new ArrayList<>();
                                startActivity(new Intent(mActivity, SettingsActivity.class));
                                break;
                            case R.id.menu_about:
                                hideKeyboard();
                                // DONE: About dialog
                                showAboutDialog();
                                break;
                        }
                    }
                }, NAVDRAWER_LAUNCH_DELAY);

                return false;
            }
        });
    }

    /**
     * Setup the toolbar for the activity
     */
    private void setupToolbar() {
        mToolbar = getActionBarToolbar();
    }

    /**
     * Setup the view and load the data for the book list
     */
    private void setupView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.book_list);
        // Define the gridlayout for the RecyclerView - column count will change based on rotation and device type
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mActivity, mActivity.getResources().getInteger(R.integer.books_per_row));
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mSearchView = (SearchView) findViewById(R.id.search_text);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnTouchListener(mOnTouchListener);
        mSearchView.setOnQueryTextListener(mOnQueryTextListener);
        mSearchView.clearFocus();
        mSearchView.setFocusable(false);
        mSearchView.setFocusableInTouchMode(false);
    }

    /**
     * Define an query text listener for the SearchView. This will hide the keyboard and call the
     * Spotify service using the query. The {@link it.jaschke.alexandria.restartLoader()} method will handle updating
     * the UI and display the results. If no results are found, a toast message will be displayed.
     */
    private SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            mBookList = new ArrayList<>();

            mSearchView.clearFocus();
            mSearchView.setFocusable(false);
            mSearchView.setFocusableInTouchMode(false);

            loadBookList();
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    /**
     * Handle the {@link View.OnTouchListener} event when a user touches the EditText search box. This will clear any
     * previously entered queries and set the box for a new search.
     */
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            // Clear the input when the user touches on the box - prepare for a new search
            mSearchView.setQuery("", false);
            mSearchView.clearFocus();
            return false;
        }
    };

    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(Constants.MESSAGE_KEY, 0) > 0) {
                // 1 = book not found getResources().
                // 2 = book found
                int key = intent.getIntExtra(Constants.MESSAGE_KEY, 0);

                switch (key) {
                    case Constants.Message.NOT_FOUND:
                        Toast.makeText(mActivity, mActivity.getString(R.string.toast_error_book_not_found), Toast.LENGTH_LONG).show();
                        break;
                    case Constants.Message.BOOK_FOUND:
                        loadBookList();
                        Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_book_found), Toast.LENGTH_LONG).show();
                        break;
                    case Constants.Message.ALREADY_ADDED:
                        Toast.makeText(mActivity, mActivity.getString(R.string.toast_error_book_already_added), Toast.LENGTH_LONG).show();
                        break;
                }
            }

            mAddLoadingDialog.dismiss();
            mAddLoadingShown = false;
        }
    }

    private void showAboutDialog() {
        mAboutDialogShown = true;

        mAboutDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.dialog_about)
                .content(R.string.dialog_about_text)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        mAboutDialogShown = false;
                    }
                })
                .positiveText(R.string.dialog_close)
                .build();

        mAboutDialog.show();
    }

    private void showAddBookDialog() {
        String[] items = mActivity.getResources().getStringArray(R.array.book_add_options);
        mAddBookDialogShown = true;

        mAddBookDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.dialog_add_book)
                .items(items)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int position, CharSequence charSequence) {
                        mAddBookDialogShown = false;

                        switch (position) {
                            case 0:
                                // Scan
                                LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).unregisterReceiver(mMessageReceiver);
                                startActivity(new Intent(mActivity, ScanBookActivity.class));
                                break;
                            case 1:
                                // Manual entry
                                showEnterIsbnDialog();
                                break;
                        }
                    }
                })
                .build();

        mAddBookDialog.show();
    }

    private void showEnterIsbnDialog() {
        // DONE: Fix rotation error when adding an book using the ISBN
        mEnterIsbnDialogShown = true;

        mEnterIsbnDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.dialog_enter_isbn)
                .customView(R.layout.dialog_add_book, true)
                .positiveText(R.string.dialog_add)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mEnterIsbnDialogShown = false;
                        try {
                            handleAddBook();
                        } catch (Exception ex) {
                            Toast.makeText(mActivity, R.string.toast_error_book_not_found, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        mEnterIsbnDialogShown = false;
                        dialog.dismiss();
                    }
                }).build();

        mEnterIsbnDialog.setCanceledOnTouchOutside(false);

        if (mEnterIsbnDialog.getCustomView() != null) {
            mIsbn = (EditText) mEnterIsbnDialog.getCustomView().findViewById(R.id.edit_isbn);
            if (!TextUtils.isEmpty(mSearchIsbn)) {
                mIsbn.setText(mSearchIsbn);
            }
            mIsbn.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                    mSearchIsbn = charSequence.toString();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        }

        mEnterIsbnDialog.show();
    }

    private void handleBackup() {
        Runnable load = new Runnable() {
            public void run() {
                try {
                    File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/alex");
                    if (!folder.exists()) {
                        if (!folder.mkdirs()) {
                            Log.e("***> error", "Default Save Path Creation Error");
                        }
                    }

                    File data = Environment.getDataDirectory();

                    if (folder.canWrite()) {
                        String backupName = Long.toString(System.currentTimeMillis());

                        String currentDBPath = "//data//it.jaschke.alexandria//databases//alexandria.db";
                        String backupDBPath = backupName + ".db";

                        File currentDB = new File(data, currentDBPath);
                        File backupDB = new File(folder, backupDBPath);

                        if (currentDB.exists()) {
                            FileInputStream is = new FileInputStream(currentDB);
                            FileOutputStream os = new FileOutputStream(backupDB);

                            FileChannel src = is.getChannel();
                            FileChannel dst = os.getChannel();
                            dst.transferFrom(src, 0, src.size());

                            is.close();
                            os.close();
                            src.close();
                            dst.close();
                        }

                        // Add media to the device gallery (MediaStore)
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(backupDB));
                        mActivity.sendBroadcast(intent);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    mActivity.runOnUiThread(backupRunnable);
                }
            }
        };

        Thread thread = new Thread(null, load, "handleBackup");
        thread.start();
    }

    private final Runnable backupRunnable = new Runnable() {
        public void run() {
            Toast.makeText(mActivity, "done", Toast.LENGTH_LONG).show();
        }
    };
}