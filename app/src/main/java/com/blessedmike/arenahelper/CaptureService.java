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

    private static final String CHANNEL = "arena_helper";

    private WindowManager wm;
    private TextView overlay;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Arena Helper")
                .setContentText("Arena Helper on päällä")
                .setSmallIcon(android.R.drawable.ic_menu_info_details);

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    10,
                    builder.build(),
                    android.content.pm.ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(10, builder.build());
        }

        showOverlay();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
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

        if (Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)) {

            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (wm == null) {
            return;
        }

        overlay = new TextView(this);

        overlay.setText("Arena Helper\n✓ AVUSTAJA AKTIIVINEN");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(16);
        overlay.setPadding(30, 20, 30, 20);
        overlay.setBackgroundColor(0xDD222222);

        int type;

        if (Build.VERSION.SDK_INT >= 26) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        params.y = 150;

        try {

            wm.addView(overlay, params);

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

        if (wm != null && overlay != null) {

            try {
                wm.removeView(overlay);
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
