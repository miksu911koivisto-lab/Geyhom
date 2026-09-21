package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class CaptureService extends Service {

    private static final String CHANNEL_ID = "arena_helper";

    private WindowManager windowManager;
    private TextView overlay;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Arena Helper")
                    .setContentText("Arena Helper on aktiivinen")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Arena Helper")
                    .setContentText("Arena Helper on aktiivinen")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    1,
                    notification,
                    android.content.pm.ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(1, notification);
        }

        showOverlay();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Arena Helper",
                            NotificationManager.IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showOverlay() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {

            return;
        }

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        if (windowManager == null) {
            return;
        }

        overlay = new TextView(this);

        overlay.setText("ARENA HELPER\n\nAVUSTAJA AKTIIVINEN");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(18);
        overlay.setGravity(Gravity.CENTER);

        overlay.setPadding(
                40,
                30,
                40,
                30
        );

        overlay.setBackgroundColor(0xEE222222);

        int windowType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        params.y = 150;

        try {

            windowManager.addView(
                    overlay,
                    params
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (windowManager != null
                && overlay != null) {

            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {
            }

            overlay = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
