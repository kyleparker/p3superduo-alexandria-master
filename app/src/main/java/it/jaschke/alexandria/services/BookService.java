package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.data.ProviderUtils;
import it.jaschke.alexandria.objects.Author;
import it.jaschke.alexandria.objects.Book;
import it.jaschke.alexandria.objects.Category;
import it.jaschke.alexandria.utils.Constants;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private Context mContext;
    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = this;

        if (intent != null) {
            final String action = intent.getAction();
            if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(Constants.EXTRA_EAN);
                final boolean insert = intent.getBooleanExtra(Constants.EXTRA_INSERT, false);
                fetchBook(ean, insert);
            }
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean, boolean insert) {
        Log.e("***> fetchBook", ean);
        if (ean.length() != 13) {
            return;
        }

        ProviderUtils provider = ProviderUtils.Factory.get(mContext);
        Book book = provider.getBook(ean);

        if (book != null) {
            Intent messageIntent = new Intent(Constants.MESSAGE_EVENT);
            messageIntent.putExtra(Constants.MESSAGE_KEY, Constants.Message.ALREADY_ADDED);
            messageIntent.putExtra(Constants.KEY_BOOK, book);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);

            return;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";
            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";
        final String URL = "previewLink";

        try {
            JSONObject bookJson = new JSONObject(bookJsonString);
            Log.e("***> bookJson", bookJson + "");
            JSONArray bookArray;
            if (bookJson.has(ITEMS)) {
                bookArray = bookJson.getJSONArray(ITEMS);
            } else {
                Intent messageIntent = new Intent(Constants.MESSAGE_EVENT);
                messageIntent.putExtra(Constants.MESSAGE_KEY, Constants.Message.NOT_FOUND);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if (bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc = "";
            if (bookInfo.has(DESC)) {
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

            String url = "";
            if (bookInfo.has(URL)) {
                url = bookInfo.getString(URL);
            }

            book = writeBackBook(insert, ean, title, subtitle, desc, imgUrl, url);

            Author author = new Author();
            if (bookInfo.has(AUTHORS)) {
                author = writeBackAuthors(insert, ean, bookInfo.getJSONArray(AUTHORS));
            }

            Category category = new Category();
            if (bookInfo.has(CATEGORIES)) {
                category = writeBackCategories(insert, ean, bookInfo.getJSONArray(CATEGORIES));
            }

            if (book != null) {
                book.author = author;
                book.category = category;
            }

            Intent messageIntent = new Intent(Constants.MESSAGE_EVENT);
            messageIntent.putExtra(Constants.MESSAGE_KEY, Constants.Message.BOOK_FOUND);
            messageIntent.putExtra(Constants.KEY_BOOK, book);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
        } catch (JSONException e) {
            Intent messageIntent = new Intent(Constants.MESSAGE_EVENT);
            messageIntent.putExtra(Constants.MESSAGE_KEY, Constants.Message.NOT_FOUND);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);

            e.printStackTrace();
            Log.e(LOG_TAG, "Error ", e);
        }
    }

    private Book writeBackBook(boolean insert, String ean, String title, String subtitle, String desc, String imgUrl, String url) {
        if (insert) {
            ContentValues values = new ContentValues();
            values.put(AlexandriaContract.BookEntry._ID, ean);
            values.put(AlexandriaContract.BookEntry.TITLE, title);
            values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
            values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
            values.put(AlexandriaContract.BookEntry.DESC, desc);
            values.put(AlexandriaContract.BookEntry.URL, url);
            values.put(AlexandriaContract.BookEntry.DELETED, false);
            getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI, values);
        }

        Book book = new Book();
        book.setDeleted(false);
        book.setDesc(desc);
        book.setId(ean);
        book.setImageUrl(imgUrl);
        book.setSubtitle(subtitle);
        book.setTitle(title);
        book.setUrl(url);

        return book;
    }

    private Author writeBackAuthors(boolean insert, String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values = new ContentValues();
        String authors = "";

        for (int i = 0; i < jsonArray.length(); i++) {
            authors += jsonArray.getString(i) + ",";

            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
            if (insert) {
                getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
            }
            values = new ContentValues();
        }

        Author author = new Author();
        author.setId(ean);
        author.setName(authors);

        return author;
    }

    private Category writeBackCategories(boolean insert, String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values = new ContentValues();
        String categories = "";

        for (int i = 0; i < jsonArray.length(); i++) {
            categories += jsonArray.getString(i);

            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            if (insert) {
                getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            }
            values = new ContentValues();
        }

        Category category = new Category();
        category.setId(ean);
        category.setName(categories);

        return category;
    }
}