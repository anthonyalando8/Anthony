package com.softtronic.socisnap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private boolean isConnected = false;
    private Handler handler;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected != this.isConnected) {
            this.isConnected = isConnected;
            showNetworkStateToast(isConnected);
        }
    }

    private void showNetworkStateToast(boolean isConnected) {
        String text = isConnected ? "Network connected" : "Network disconnected";
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        if (!isConnected) {
            // Schedule a check after 5 seconds
            if (handler == null) {
                handler = new Handler();
            }
            handler.postDelayed(this::checkNetworkState, 5000);
        }
    }

    private void checkNetworkState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected != this.isConnected) {
            this.isConnected = isConnected;
            showNetworkStateToast(isConnected);
        }
    }
}
