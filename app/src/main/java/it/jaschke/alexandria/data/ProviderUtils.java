package it.jaschke.alexandria.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.objects.Author;
import it.jaschke.alexandria.objects.Book;
import it.jaschke.alexandria.objects.Category;

/**
 * Data access for the content provider
 *
 * Created by kyleparker on 9/14/2015.
 */
public class ProviderUtils {
    private final ContentResolver contentResolver;

    public ProviderUtils(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void deleteBooks() {
        contentResolver.delete(AlexandriaContract.BookEntry.CONTENT_URI, null, null);
    }

    public void deleteBook(String id) {
        contentResolver.delete(AlexandriaContract.BookEntry.CONTENT_URI, AlexandriaContract.BookEntry._ID + "='" + id + "'", null);
    }

    public Uri insertAuthor(Author obj) {
        return contentResolver.insert(AlexandriaContract.AuthorEntry.CONTENT_URI, createContentValues(obj));
    }

    public Uri insertBook(Book obj) {
        return contentResolver.insert(AlexandriaContract.BookEntry.CONTENT_URI, createContentValues(obj));
    }

    public Uri insertCategory(Category obj) {
        return contentResolver.insert(AlexandriaContract.CategoryEntry.CONTENT_URI, createContentValues(obj));
    }

    public Book getBook(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }

        Cursor cursor = contentResolver.query(AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(id)),
                null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return createBook(cursor);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public List<Book> getBookList() {
        ArrayList<Book> list = new ArrayList<>();

        String selection = AlexandriaContract.BookEntry.DELETED + " = 0 ";
        Cursor cursor = contentResolver.query(AlexandriaContract.BookEntry.CONTENT_URI, null, selection, null, null);

        if (cursor != null) {
            list.ensureCapacity(cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    list.add(createBook(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return list;
    }

    public List<Book> getBookList(String query) {
        ArrayList<Book> list = new ArrayList<>();

        String selection = "(" + AlexandriaContract.BookEntry.TITLE + " LIKE ? " +
                " OR " + AlexandriaContract.BookEntry.SUBTITLE + " LIKE ? )" +
                " AND " + AlexandriaContract.BookEntry.DELETED + " = 0 ";
        String[] selectionArgs = new String[] { query, query };

        Cursor cursor = contentResolver.query(AlexandriaContract.BookEntry.CONTENT_URI, null, selection, selectionArgs, null);

        if (cursor != null) {
            list.ensureCapacity(cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    list.add(createBook(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return list;
    }

    /**
     *
     * @param obj
     * @return
     */
    private ContentValues createContentValues(Author obj) {
        ContentValues values = new ContentValues();

        values.put(AlexandriaContract.AuthorEntry._ID, obj.getId());
        values.put(AlexandriaContract.AuthorEntry.AUTHOR, obj.getName());

        return values;
    }

    /**
     *
     * @param obj
     * @return
     */
    private ContentValues createContentValues(Book obj) {
        ContentValues values = new ContentValues();

        values.put(AlexandriaContract.BookEntry.DELETED, obj.getDeleted());
        values.put(AlexandriaContract.BookEntry.DESC, obj.getDesc());
        values.put(AlexandriaContract.BookEntry._ID, obj.getId());
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, obj.getImageUrl());
        values.put(AlexandriaContract.BookEntry.SUBTITLE, obj.getSubtitle());
        values.put(AlexandriaContract.BookEntry.TITLE, obj.getTitle());
        values.put(AlexandriaContract.BookEntry.URL, obj.getUrl());

        return values;
    }

    /**
     *
     * @param obj
     * @return
     */
    private ContentValues createContentValues(Category obj) {
        ContentValues values = new ContentValues();

        values.put(AlexandriaContract.CategoryEntry._ID, obj.getId());
        values.put(AlexandriaContract.CategoryEntry.CATEGORY, obj.getName());

        return values;
    }

    /**
     *
     * @param cursor
     * @return
     */
    private Author createAuthor(Cursor cursor) {
        int idxId = cursor.getColumnIndex(AlexandriaContract.AuthorEntry._ID);
        int idxName = cursor.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR);

        Author item = new Author();

        if (idxId > -1) {
            item.setId(cursor.getString(idxId));
        }
        if (idxName > -1) {
            item.setName(cursor.getString(idxName));
        }

        return item;
    }

    /**
     *
     * @param cursor
     * @return
     */
    private Book createBook(Cursor cursor) {
        int idxDesc = cursor.getColumnIndex(AlexandriaContract.BookEntry.DESC);
        int idxId = cursor.getColumnIndex(AlexandriaContract.BookEntry._ID);
        int idxImageUrl = cursor.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL);
        int idxSubtitle = cursor.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE);
        int idxTitle = cursor.getColumnIndex(AlexandriaContract.BookEntry.TITLE);
        int idxUrl = cursor.getColumnIndex(AlexandriaContract.BookEntry.URL);

        Book item = new Book();

        if (idxDesc > -1) {
            item.setDesc(cursor.getString(idxDesc));
        }
        if (idxId > -1) {
            item.setId(cursor.getString(idxId));
        }
        if (idxImageUrl > -1) {
            item.setImageUrl(cursor.getString(idxImageUrl));
        }
        if (idxSubtitle > -1) {
            item.setSubtitle(cursor.getString(idxSubtitle));
        }
        if (idxTitle > -1) {
            item.setTitle(cursor.getString(idxTitle));
        }
        if (idxUrl > -1) {
            item.setUrl(cursor.getString(idxUrl));
        }

        item.author = createAuthor(cursor);
        item.category = createCategory(cursor);

        return item;
    }

    /**
     *
     * @param cursor
     * @return
     */
    private Category createCategory(Cursor cursor) {
        int idxId = cursor.getColumnIndex(AlexandriaContract.CategoryEntry._ID);
        int idxName = cursor.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY);

        Category item = new Category();

        if (idxId > -1) {
            item.setId(cursor.getString(idxId));
        }
        if (idxName > -1) {
            item.setName(cursor.getString(idxName));
        }

        return item;
    }

    /**
     * A factory which can produce instances of {@link ProviderUtils}
     */
    public static class Factory {
        private static Factory instance = new Factory();

        /**
         * Creates and returns an instance of {@link ProviderUtils} which uses the given context to access its data.
         */
        public static ProviderUtils get(Context context) {
            return instance.newForContext(context);
        }

        /**
         * Creates an instance of {@link ProviderUtils}.
         */
        protected ProviderUtils newForContext(Context context) {
            return new ProviderUtils(context.getContentResolver());
        }
    }
}
