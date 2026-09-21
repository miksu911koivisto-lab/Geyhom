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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;

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
    private boolean processingImage = false;

    private TextRecognizer recognizer;

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
                    .setContentText("Korttien tunnistus aktiivinen")
                    .setSmallIcon(
                            android.R.drawable.ic_menu_info_details
                    )
                    .build();

        } else {

            notification = new Notification.Builder(this)
                    .setContentTitle("Arena Helper")
                    .setContentText("Korttien tunnistus aktiivinen")
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

        recognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );

        showOverlay(
                "ARENA HELPER\n\n"
                        + "KORTTIEN TUNNISTUS KÄYNNISTYY..."
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

        overlay.setBackgroundColor(
                0xEE222222
        );

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

                        if (processingImage) {
                            return;
                        }

                        Image image = null;

                        try {

                            image =
                                    reader.acquireLatestImage();

                            if (image == null) {
                                return;
                            }

                            processingImage = true;

                            recognizeText(image);

                        } catch (Exception e) {

                            e.printStackTrace();

                            processingImage = false;

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
                            + "KORTTEJA ETSITÄÄN..."
            );

        } catch (Exception e) {

            e.printStackTrace();

            updateOverlay(
                    "ARENA HELPER\n\n"
                            + "KUVAUKSEN VIRHE"
            );
        }
    }

    private void recognizeText(Image image) {

        try {

            Image.Plane[] planes =
                    image.getPlanes();

            if (planes.length == 0) {

                image.close();
                processingImage = false;
                return;
            }

            ByteBuffer buffer =
                    planes[0].getBuffer();

            int pixelStride =
                    planes[0].getPixelStride();

            int rowStride =
                    planes[0].getRowStride();

            int rowPadding =
                    rowStride
                            - pixelStride
                            * image.getWidth();

            int bitmapWidth =
                    image.getWidth()
                            + rowPadding
                            / pixelStride;

            android.graphics.Bitmap bitmap =
                    android.graphics.Bitmap.createBitmap(
                            bitmapWidth,
                            image.getHeight(),
                            android.graphics.Bitmap.Config.ARGB_8888
                    );

            buffer.rewind();

            bitmap.copyPixelsFromBuffer(buffer);

            InputImage inputImage =
                    InputImage.fromBitmap(
                            bitmap,
                            0
                    );

            recognizer.process(inputImage)
                    .addOnSuccessListener(
                            result -> {

                                String text =
                                        result.getText();

                                if (text == null
                                        || text.trim().isEmpty()) {

                                    updateOverlay(
                                            "ARENA HELPER\n\n"
                                                    + "TEKSTIÄ EI LÖYTYNYT"
                                    );

                                } else {

                                    String cleanText =
                                            text.trim();

                                    if (cleanText.length() > 250) {

                                        cleanText =
                                                cleanText.substring(
                                                        0,
                                                        250
                                                );
                                    }

                                    updateOverlay(
                                            "ARENA HELPER\n\n"
                                                    + cleanText
                                    );
                                }
                            }
                    )
                    .addOnFailureListener(
                            error -> {

                                updateOverlay(
                                        "ARENA HELPER\n\n"
                                                + "OCR-VIRHE"
                                );
                            }
                    )
                    .addOnCompleteListener(
                            task -> {

                                bitmap.recycle();

                                image.close();

                                processingImage = false;
                            }
                    );

        } catch (Exception e) {

            e.printStackTrace();

            try {
                image.close();
            } catch (Exception ignored) {
            }

            processingImage = false;
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

        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }

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
