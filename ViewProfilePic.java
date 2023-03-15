package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class ViewProfilePic {
    public static AlertDialog createAlertDialog(Context context, String message, String profileLink) {
        View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.view_profile_pic, null);
        TextView textView = view.findViewById(R.id.alertName);
        ImageView imageView = view.findViewById(R.id.alertImage);
        textView.setText(message);
        try {
                Picasso.get().load(profileLink).into(imageView);
        }catch (Exception e){
            Picasso.get().load(R.drawable.no_user).into(imageView);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(true);
        return builder.create();
    }
}
