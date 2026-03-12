package com.example.gomarket.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.gomarket.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_LOCATION_UPDATED = "com.gomarket.LOCATION_UPDATED";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());

                    // Gửi broadcast với tọa độ mới
                    Intent broadcastIntent = new Intent(ACTION_LOCATION_UPDATED);
                    broadcastIntent.putExtra(EXTRA_LATITUDE, location.getLatitude());
                    broadcastIntent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
                    sendBroadcast(broadcastIntent);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");

        createNotificationChannel();
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 giây
                .setMinUpdateIntervalMillis(5000) // Tối thiểu 5 giây
                .build();

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dịch vụ vị trí",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Đang lấy vị trí để gợi ý món ăn phù hợp");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đi Chợ Hộ")
                .setContentText("Đang xác định vị trí của bạn...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationService destroyed");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
