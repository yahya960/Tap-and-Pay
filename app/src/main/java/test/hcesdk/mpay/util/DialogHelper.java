package test.hcesdk.mpay.util;


import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethod;

import test.hcesdk.mpay.R;

/**
 * Utility class to create different dialog alert
 */
public class DialogHelper {

    /**
     * Get an instance of an AlertDialog that displays a message and a OK button only.
     * @param c the Application Context
     * @param msg the dialog message
     * @return an instance of AlertDialog that displays a message.
     */
    public static AlertDialog createAlertDialog(Context c, String msg) {
        return createAlertDialog(c, c.getString(R.string.app_name), msg);
    }

    /**
     * Get an instance of an AlertDialog that displays a message with a title and a OK button
     * only.
     * @param c the Application Context
     * @param t the dialog title
     * @param msg the dialog message
     * @return an instance of AlertDialog that displays a message.
     */
    public static AlertDialog createAlertDialog(Context c, String t, String msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setMessage(msg);
        b.setTitle(t);
        b.setPositiveButton(android.R.string.ok, null);
        return b.create();
    }

    /**
     * Get an instance of an AlertDialog that creates a list of IDVMethod and let the user choose
     * one method in the list.
     * @param c the Application Context
     * @param t the dialog title
     * @param meth the list of available IDVMethod
     * @param ok the Ok button listener
     * @param cancel the Cancel button listener
     * @return an instance of an AlertDialog
     */
    public static AlertDialog createInputMethodDialog(Context c, String t, IDVMethod[] meth,
                                                      DialogInterface.OnClickListener ok,
                                                      DialogInterface.OnClickListener cancel) {
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(t);
        String[] items = new String[meth.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = meth[i].getType();
        }
        b.setItems(items, ok);
        b.setNegativeButton(android.R.string.cancel, cancel);
        return b.create();
    }

    /**
     * Get an AlertDialog that display a title, message and a custom listener when the user
     * clicks OK. This function will display a Cancel button that will dismiss the dialog only.
     * @param c the application context
     * @param t the title
     * @param m the message
     * @param ok the listener to be executed when the clicks OK
     * @return the AlertDialog instance ready to be used.
     */
    public static AlertDialog createAlertDialog(Context c, String t, String m,
                                                DialogInterface.OnClickListener ok) {
        return createAlertDialog(c, t, m, ok, null);
    }
    /**
     * Get an AlertDialog that display a title, message and a custom listener when the user
     * clicks OK and Cancel.
     * @param c the application context
     * @param t the title
     * @param m the message
     * @param ok the listener to be executed when the user clicks OK
     * @param cancel the listener to be executed when the user clicks Cancel
     * @return the AlertDialog ready to be used
     */
    public static AlertDialog createAlertDialog(Context c, String t, String m,
                                                DialogInterface.OnClickListener ok,
                                                DialogInterface.OnClickListener cancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(t);
        builder.setMessage(m);
        if(cancel != null) {
            builder.setNegativeButton(android.R.string.cancel, cancel);
        }
        builder.setPositiveButton(android.R.string.ok, ok);
        return builder.create();
    }

    /**
     * Get an instance of an AlertDialog that contains an EditText as the input. The EditText
     * reference must be kept by the calling client in order to retreive the content when the user
     * press the ok button from the AlertDialog.
     * @param c The application context
     * @param t the title
     * @param et the EditText
     * @param ok The Ok button listener
     * @param cancel The Cancel button listener
     * @return an instance of AlertDialog to retreive the OTP from the user
     */
    public static AlertDialog createOTPInputDialog(Context c, String t, EditText et,
                                                   DialogInterface.OnClickListener ok,
                                                   DialogInterface.OnClickListener cancel) {
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(t);
        b.setPositiveButton(android.R.string.ok, ok);
        b.setNegativeButton(android.R.string.cancel, cancel);
        b.setView(et);
        return b.create();
    }

    public static AlertDialog createLifecycleOptionsDialog(Context c, String t, String[] list,
                                                                                  DialogInterface.OnClickListener ok) {
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(t);
        b.setItems(list, ok);
        b.setNegativeButton(android.R.string.cancel, null);
        return b.create();
    }

    public static AlertDialog createBenchmarkingDialog(Context context, String title, String[] option,
                                                       int selected, DialogInterface.OnClickListener ok) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(title);
        b.setSingleChoiceItems(option, selected, ok);
        return b.create();
    }

    public static AlertDialog createUpdateApplicationDialog(Context c) {
        return createAlertDialog(c, "Application Update Required",
                "You must update your application to the latest version");
    }
}

