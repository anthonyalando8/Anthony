package com.softtronic.socisnap;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;

public class NetworkChangeHandler extends Handler {
    private final Context context;
    private final NetworkChangeReceiver networkChangeReceiver;

    public NetworkChangeHandler(Context context, NetworkChangeReceiver networkChangeReceiver) {
        this.context = context;
        this.networkChangeReceiver = networkChangeReceiver;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case 0:
                context.registerReceiver(networkChangeReceiver,
                        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                break;
            case 1:
                try {
                    context.unregisterReceiver(networkChangeReceiver);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
                break;
            default:
                break;
        }
    }
}
