package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class CaptureService extends Service {

    private static final String CHANNEL = "arena_helper";

    private static int projectionResultCode;
    private static Intent projectionData;

    private WindowManager wm;
    private TextView overlay;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    public static void setProjectionData(int resultCode, Intent data) {
        projectionResultCode = resultCode;
        projectionData = data;
    }

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
            NotificationChannel channel = new NotificationChannel(
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
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlay = new TextView(this);
        overlay.setText("Arena Helper\nKäynnistetään...");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(15);
        overlay.setPadding(24, 16, 24, 16);
        overlay.setBackgroundColor(0xDD222222);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 120;

        try {
            wm.addView(overlay, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOverlay(String text) {
        if (overlay != null) {
            overlay.post(() -> overlay.setText(text));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (projectionData == null || projectionResultCode == 0) {
            updateOverlay(
                    "Arena Helper\nNäytön kaappaus ei onnistunut"
            );
            return START_NOT_STICKY;
        }

        startScreenCapture();

        return START_NOT_STICKY;
    }

    private void startScreenCapture() {

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        if (manager == null) {
            updateOverlay("Arena Helper\nMediaProjection ei saatavilla");
            return;
        }

        mediaProjection = manager.getMediaProjection(
                projectionResultCode,
                projectionData
        );

        if (mediaProjection == null) {
            updateOverlay(
                    "Arena Helper\nMediaProjection epäonnistui"
            );
            return;
        }

        android.util.DisplayMetrics metrics =
                getResources().getDisplayMetrics();

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(
                reader -> {

                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();

                        if (image != null) {
                            updateOverlay(
                                    "Arena Helper\n" +
                                    "Kuva saatu ✓\n" +
                                    width + " × " + height
                            );
                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }

                },
                null
        );

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ArenaHelperCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
        );

        updateOverlay(
                "Arena Helper\nKuvakaappaus aktiivinen ✓"
        );
    }

    @Override
    public void onDestroy() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (wm != null && overlay != null) {
            try {
                wm.removeView(overlay);
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
