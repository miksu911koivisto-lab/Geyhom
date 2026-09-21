package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class CaptureService extends Service {

    private static final String CHANNEL_ID = "arena_helper";

    private static int projectionResultCode;
    private static Intent projectionData;

    private WindowManager windowManager;
    private TextView overlay;

    private MediaProjection mediaProjection;
    private android.hardware.display.VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private boolean captureStarted = false;

    public static void setProjectionData(
            int resultCode,
            Intent data) {

        projectionResultCode = resultCode;
        projectionData = data;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(
                    this,
                    CHANNEL_ID
            )
                    .setContentTitle("Arena Helper")
                    .setContentText("Näytön kaappaus aktiivinen")
                    .setSmallIcon(
                            android.R.drawable.ic_menu_info_details
                    )
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Arena Helper")
                    .setContentText("Näytön kaappaus aktiivinen")
                    .setSmallIcon(
                            android.R.drawable.ic_menu_info_details
                    )
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
            startForeground(
                    1,
                    notification
            );
        }

        showOverlay(
                "ARENA HELPER\n\nAVUSTAJA AKTIIVINEN"
        );
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
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showOverlay(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            return;
        }

        windowManager =
                (WindowManager)
                        getSystemService(WINDOW_SERVICE);

        if (windowManager == null) {
            return;
        }

        overlay = new TextView(this);

        overlay.setText(text);
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
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;
        } else {
            windowType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
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
                Gravity.TOP
                        | Gravity.CENTER_HORIZONTAL;

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

    private void updateOverlay(String text) {

        if (overlay == null) {
            return;
        }

        overlay.post(() ->
                overlay.setText(text)
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (!captureStarted) {

            if (projectionData == null) {

                updateOverlay(
                        "ARENA HELPER\n\n"
                                + "NÄYTÖN LUPA PUUTTUU"
                );

            } else {

                startScreenCapture();
            }
        }

        return START_NOT_STICKY;
    }

    private void startScreenCapture() {

        try {

            MediaProjectionManager manager =
                    (MediaProjectionManager)
                            getSystemService(
                                    MEDIA_PROJECTION_SERVICE
                            );

            if (manager == null) {
                updateOverlay(
                        "ARENA HELPER\n\n"
                                + "MEDIA PROJECTION VIRHE"
                );
                return;
            }

            mediaProjection =
                    manager.getMediaProjection(
                            projectionResultCode,
                            projectionData
                    );

            if (mediaProjection == null) {
                updateOverlay(
                        "ARENA HELPER\n\n"
                                + "KAAPPAUS EI KÄYNNISTYNYT"
                );
                return;
            }

            mediaProjection.registerCallback(
                    new MediaProjection.Callback() {

                        @Override
                        public void onStop() {

                            captureStarted = false;

                            updateOverlay(
                                    "ARENA HELPER\n\n"
                                            + "KUVAUS PYSÄYTETTY"
                            );

                            stopCaptureResources();
                        }
                    },
                    null
            );

            DisplayMetrics metrics =
                    getResources()
                            .getDisplayMetrics();

            int width =
                    metrics.widthPixels;

            int height =
                    metrics.heightPixels;

            int density =
                    metrics.densityDpi;

            imageReader =
                    ImageReader.newInstance(
                            width,
                            height,
                            PixelFormat.RGBA_8888,
                            2
                    );

            imageReader.setOnImageAvailableListener(
                    reader -> {

                        Image image = null;

                        try {

                            image =
                                    reader.acquireLatestImage();

                            if (image != null) {

                                /*
                                 * TÄSSÄ KOHDASSA SAAMME
                                 * HEARTHSTONEN RUUTUKUVAN.
                                 *
                                 * Seuraavassa vaiheessa
                                 * käsittelemme tämän kuvan
                                 * ja tunnistamme kortit.
                                 */

                                updateOverlay(
                                        "ARENA HELPER\n\n"
                                                + "KUVA SAATU ✓"
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

            virtualDisplay =
                    mediaProjection.createVirtualDisplay(
                            "ArenaHelperCapture",
                            width,
                            height,
                            density,
                            android.hardware.display.DisplayManager
                                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            imageReader.getSurface(),
                            null,
                            null
                    );

            captureStarted = true;

            updateOverlay(
                    "ARENA HELPER\n\n"
                            + "KUVAUS KÄYNNISTYY..."
            );

        } catch (Exception e) {

            e.printStackTrace();

            updateOverlay(
                    "ARENA HELPER\n\n"
                            + "KUVAUKSEN VIRHE"
            );
        }
    }

    private void stopCaptureResources() {

        if (virtualDisplay != null) {

            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }

            virtualDisplay = null;
        }

        if (imageReader != null) {

            try {
                imageReader.close();
            } catch (Exception ignored) {
            }

            imageReader = null;
        }

        mediaProjection = null;
    }

    @Override
    public void onDestroy() {

        stopCaptureResources();

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
