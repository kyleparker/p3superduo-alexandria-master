package it.jaschke.alexandria.utils;

import android.app.Activity;
import android.content.DialogInterface;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by kyleparker on 9/14/2015.
 */
public class DialogUtils {

    public static final int DEFAULT_LAYOUT_ID = -1;
    public static final int DEFAULT_MESSAGE_ID = -1;
    public static final int DEFAULT_NEGATIVE_TEXT_ID = -1;
    public static final int DEFAULT_NEUTRAL_TEXT_ID = -1;
    public static final int DEFAULT_POSITIVE_TEXT_ID = -1;
    public static final int DEFAULT_TITLE_ID = -1;

    private static MaterialDialog createProgressDialog(final Activity activity, boolean spinner, int titleId, int messageId,
                                                       boolean cancelable, int max, DialogInterface.OnCancelListener onCancelListener,
                                                       Object... formatArgs) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                .cancelable(cancelable);

        if (titleId > DEFAULT_TITLE_ID) {
            builder.title(messageId > DEFAULT_MESSAGE_ID ? activity.getString(titleId) : activity.getString(titleId, formatArgs));
        }
        if (messageId > DEFAULT_MESSAGE_ID) {
            builder.content(activity.getString(messageId, formatArgs));
        }
        if (onCancelListener != null) {
            builder.cancelListener(onCancelListener);
        }
        if (spinner) {
            builder.progress(true, 0);
        } else {
            builder.progress(false, max, true);
        }

        return builder.build();
    }

    /**
     * Creates a spinner progress dialog.
     *
     * @param activity
     * @param titleId
     * @param messageId
     * @param cancelable
     * @return
     */
    public static MaterialDialog createSpinnerProgressDialog(Activity activity, int titleId, int messageId, boolean cancelable) {
        return createProgressDialog(activity, true, titleId, messageId, cancelable, 0, null);
    }
}
