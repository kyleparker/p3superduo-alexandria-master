package it.jaschke.alexandria;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import it.jaschke.alexandria.objects.Book;
import it.jaschke.alexandria.utils.Constants;
import it.jaschke.alexandria.utils.DialogUtils;
import it.jaschke.alexandria.utils.UIUtils;

// DONE: Ability to delete a book
/**
 *
 * Created by kyleparker on 9/11/2015.
 */
public class BookDetailActivity extends BaseActivity {
    private MaterialDialog mProgressLoadingDialog;

    private Book mBook = null;

    private String mEAN;
    private String mBookTitle;
    private boolean mProgressLoadingShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mShouldBeFloatingWindow = shouldBeFloatingWindow();
        if (mShouldBeFloatingWindow) {
            setupFloatingWindow(R.dimen.floating_window_width, R.dimen.floating_window_height);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        getExtras();
        setupToolbar();
        if (savedInstanceState != null) {
            mBook = savedInstanceState.getParcelable(Constants.KEY_BOOK);
            mProgressLoadingShown = savedInstanceState.getBoolean(Constants.KEY_LOADING_DIALOG);

            if (mProgressLoadingShown) {
                mProgressLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_loading, false);
                mProgressLoadingDialog.show();
            }
        }
        loadBook();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                handleShareAction();
                break;
            case R.id.menu_remove:
                // DONE: handle remove book
                showDeleteBookDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(Constants.KEY_BOOK, mBook);
        outState.putBoolean(Constants.KEY_LOADING_DIALOG, mProgressLoadingShown);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressLoadingDialog != null && mProgressLoadingDialog.isShowing()) {
            mProgressLoadingDialog.dismiss();
        }
    }

    private void deleteBook() {
        mProgressLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_loading, false);
        mProgressLoadingDialog.show();
        mProgressLoadingShown = true;

        Runnable load = new Runnable() {
            public void run() {
                try {
                    mProvider.deleteBook(mEAN);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    mActivity.runOnUiThread(deleteBookRunnable);
                }
            }
        };

        Thread thread = new Thread(null, load, "deleteBook");
        thread.start();
    }

    private final Runnable deleteBookRunnable = new Runnable() {
        public void run() {
            mActivity.startActivity(new Intent(mActivity, MainActivity.class));
        }
    };

    /**
     * Get extras from the intent bundle
     */
    private void getExtras() {
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            mEAN = bundle.getString(Constants.EXTRA_EAN);
            mBookTitle = bundle.getString(Constants.EXTRA_BOOK_TITLE);
        }
    }

    private void handleShareAction() {
        String subject = mActivity.getString(R.string.content_share_subject, mBook.getTitle());
        String message = mActivity.getString(R.string.content_share_message, mBook.getUrl(), mBook.getId());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        mActivity.startActivity(Intent.createChooser(intent, mActivity.getString(R.string.content_share_book)));
    }

    private void loadBook() {
        mProgressLoadingDialog = DialogUtils.createSpinnerProgressDialog(mActivity, DialogUtils.DEFAULT_TITLE_ID, R.string.dialog_loading, false);
        mProgressLoadingDialog.show();

        Runnable load = new Runnable() {
            public void run() {
                try {
                    if (mBook == null) {
                        mBook = mProvider.getBook(mEAN);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    mActivity.runOnUiThread(loadBookRunnable);
                }
            }
        };

        Thread thread = new Thread(null, load, "loadBook");
        thread.start();
    }

    private final Runnable loadBookRunnable = new Runnable() {
        public void run() {
            if (mBook == null) {
                return;
            }

            final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
            collapsingToolbarLayout.setTitle(mBook.getTitle());

            if (!TextUtils.isEmpty(mBook.getSubtitle())) {
                TextView subtitle = (TextView) findViewById(R.id.book_subtitle);
                subtitle.setText(mBook.getSubtitle());
                subtitle.setVisibility(View.VISIBLE);
            }

            ((TextView) findViewById(R.id.book_desc)).setText(mBook.getDesc());

            if (!TextUtils.isEmpty(mBook.author.getName())) {
                TextView authors = (TextView) findViewById(R.id.authors);
                findViewById(R.id.authors_heading).setVisibility(View.VISIBLE);

                String[] authorsArr = mBook.author.getName().split(",");
                authors.setLines(authorsArr.length);
                authors.setText(mBook.author.getName().replace(",", "\n"));
                authors.setVisibility(View.VISIBLE);
            }

            final ImageView bookCover = (ImageView) findViewById(R.id.book_cover);

            // DONE: This needs to be swapped out for Picasso
            if (Patterns.WEB_URL.matcher(mBook.getImageUrl()).matches()) {
                // Else if the user is on a phone, style the status bar, toolbar and background parallax image
                Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                            public void onGenerated(Palette palette) {
                                bookCover.setImageBitmap(bitmap);
                                Palette.Swatch darkSwatch = palette.getDarkVibrantSwatch() == null ?
                                        palette.getDarkMutedSwatch() : palette.getDarkVibrantSwatch();

                                if (darkSwatch != null) {
                                    View headerBackground = findViewById(R.id.header_background);
                                    headerBackground.setBackgroundColor(darkSwatch.getRgb());
                                    collapsingToolbarLayout.setContentScrimColor(darkSwatch.getRgb());

                                    if (UIUtils.isLollipop()) {
                                        Window window = mActivity.getWindow();
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                        window.setStatusBarColor(darkSwatch.getRgb());
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                };

                Picasso.with(mActivity)
                        .load(mBook.getImageUrl())
                        .resize(600, 600)
                        .centerCrop()
                        .placeholder(R.drawable.ic_placeholder_book)
                        .into(target);

                bookCover.setVisibility(View.VISIBLE);
            } else {
                bookCover.setImageResource(R.drawable.ic_placeholder_book);
            }

            if (!TextUtils.isEmpty(mBook.category.getName())) {
                TextView categories = (TextView) findViewById(R.id.categories);
                findViewById(R.id.categories_heading).setVisibility(View.VISIBLE);
                categories.setText(mBook.category.getName());
                categories.setVisibility(View.VISIBLE);
            }

            mProgressLoadingDialog.dismiss();
            mProgressLoadingShown = false;
        }
    };

    private void showDeleteBookDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .content(mActivity.getString(R.string.dialog_remove_book_message, mBookTitle))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        deleteBook();
                    }
                })
                .positiveText(R.string.dialog_remove)
                .negativeText(R.string.dialog_cancel)
                .build();

        dialog.show();
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
            }
        });
    }
}
