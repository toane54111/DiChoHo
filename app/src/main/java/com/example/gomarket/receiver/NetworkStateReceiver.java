package com.example.gomarket.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class NetworkStateReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkStateReceiver";

    public static final String ACTION_NETWORK_CHANGED = "com.gomarket.NETWORK_CHANGED";
    public static final String EXTRA_IS_CONNECTED = "is_connected";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = isNetworkAvailable(context);
            Log.d(TAG, "Network state changed. Connected: " + isConnected);

            if (isConnected) {
                Toast.makeText(context, "Đã kết nối mạng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Mất kết nối mạng. Vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
            }

            // Gửi broadcast nội bộ để các Activity có thể lắng nghe
            Intent networkIntent = new Intent(ACTION_NETWORK_CHANGED);
            networkIntent.putExtra(EXTRA_IS_CONNECTED, isConnected);
            context.sendBroadcast(networkIntent);
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}
