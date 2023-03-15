package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Context;

public class AlertMsg {
    public static AlertDialog createAlertDialog(Context context, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text);
        builder.setCancelable(true);
        builder.setNegativeButton("Ok", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
