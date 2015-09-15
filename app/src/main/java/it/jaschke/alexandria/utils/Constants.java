package it.jaschke.alexandria.utils;

/**
 * Constants for the app
 *
 * Created by kyleparker on 9/11/2015.
 */
public class Constants {
    private static final String EXTRA = "alexandria.extra.";

    public static final String EXTRA_BOOK_TITLE = EXTRA + "BOOK_TITLE";
    public static final String EXTRA_EAN = EXTRA + "EAN";
    public static final String EXTRA_INSERT = EXTRA + "INSERT";

    public static final String KEY_ABOUT_DIALOG = "KEY_ABOUT_DIALOG";
    public static final String KEY_ADD_BOOK_DIALOG = "KEY_ADD_BOOK_DIALOG";
    public static final String KEY_ADD_LOADING_DIALOG = "KEY_ADD_LOADING_DIALOG";
    public static final String KEY_BOOK = "KEY_BOOK";
    public static final String KEY_BOOK_LIST = "KEY_BOOK_LIST";
    public static final String KEY_ENTER_ISBN_DIALOG = "KEY_ENTER_ISBN_DIALOG";
    public static final String KEY_LOADING_DIALOG = "KEY_LOADING_DIALOG";
    public static final String KEY_SEARCH_ISBN = "KEY_SEARCH_ISBN";

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";


    public class Message {
        public static final int NOT_FOUND = 1;
        public static final int BOOK_FOUND = 2;
        public static final int ALREADY_ADDED = 3;
    }

}
