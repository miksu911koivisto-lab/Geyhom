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

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        builder.setContentTitle("Arena Helper")
                .setContentText("Arena Helper on päällä")
                .setSmallIcon(android.R.drawable.ic_menu_info_details);

        startForeground(10, builder.build());
        showOverlay();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Arena Helper", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void showOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new TextView(this);
        overlay.setText("Arena Helper\nKäynnissä");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(15);
        overlay.setPadding(24, 16, 24, 16);
        overlay.setBackgroundColor(0xDD222222);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.y = 120;

        try { wm.addView(overlay, p); } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        if (wm != null && overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
