package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreenActivity extends AppCompatActivity {
    FirebaseUser currentUser;
    MaterialButton signIn, signUp;
    LinearLayout btnLayout;
    private FirebaseAuth mAuth;
    private NetworkChangeHandler networkChangeHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_splash_screen);
        signIn = findViewById(R.id.btnLoginSplash);
        signUp = findViewById(R.id.btnSignUpSplash);
        btnLayout = findViewById(R.id.btnLayout);

        // create the networkChangeReceiver instance variable
        NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();


        NotificationHelper notificationHelper = new NotificationHelper();
        notificationHelper.showNotification(this, "Alert", "Hello User, Welcome");

        // create the networkChangeHandler instance variable, passing in this activity and the networkChangeReceiver
        networkChangeHandler = new NetworkChangeHandler(this, networkChangeReceiver);

        // send a message to the networkChangeHandler to register the receiver
        networkChangeHandler.sendEmptyMessage(0);



        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        new Handler().postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                btnLayout.setVisibility(View.VISIBLE);
                signIn.setOnClickListener(v->{
                    AlertDialog alertDialog = Progress.createAlertDialog(this, "Please Wait...");
                    alertDialog.show();
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        // Device has an active internet connection
                        alertDialog.dismiss();
                        Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Device does not have an active internet connection
                        alertDialog.dismiss();
                        AlertDialog customAlert = AlertMsg.createAlertDialog(this, "No internet connection");
                        customAlert.show();
                    }

                });
                signUp.setOnClickListener(v->{
                    AlertDialog alertDialog = Progress.createAlertDialog(this, "Please Wait...");
                    alertDialog.show();
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        // Device has an active internet connection
                        alertDialog.dismiss();
                        Intent intent = new Intent(SplashScreenActivity.this, SignUpActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Device does not have an active internet connection
                        alertDialog.dismiss();
                        AlertDialog customAlert = AlertMsg.createAlertDialog(this, "No internet connection");
                        customAlert.show();
                    }
                });
            } else {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    // Device has an active internet connection
                    Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                } else {
                    // Device does not have an active internet connection
                    AlertDialog customAlert = AlertMsg.createAlertDialog(this, "No internet connection!");
                    customAlert.show();

                }

            }
        }, 1500);
    }
    @Override
    protected void onPause() {
        super.onPause();

        // send a message to the networkChangeHandler to unregister the receiver
        networkChangeHandler.sendEmptyMessage(1);
    }
}