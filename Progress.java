package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class Progress {
   public static AlertDialog createAlertDialog(Context context, String message) {
       View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.pb_loading, null);
       TextView textView = view.findViewById(R.id.txtLoading);
       textView.setText(message);
       AlertDialog.Builder builder = new AlertDialog.Builder(context);
       builder.setView(view);
       builder.setCancelable(false);
       return builder.create();
   }
}
