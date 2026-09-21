package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;

public class CaptureService extends Service {

    private static final String TAG = "ArenaHelper";
    private static final String CHANNEL_ID = "arena_helper_channel";

    private static int projectionResultCode;
    private static Intent projectionData;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private TextRecognizer recognizer;

    private Handler handler;
    private boolean processing = false;

    private WindowManager windowManager;
    private TextView overlayText;

    private MediaProjection.Callback mediaProjectionCallback;

    private long lastOCRTime = 0;

    // OCR tehdään noin kerran sekunnissa.
    private static final long OCR_INTERVAL = 1000;

    public static void setProjectionData(
            int resultCode,
            Intent data
    ) {
        projectionResultCode = resultCode;
        projectionData = data;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler =
                new Handler(
                        Looper.getMainLooper()
                );

        recognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );

        createNotificationChannel();

        createOverlay();

        Log.d(
                TAG,
                "Arena Helper CaptureService started"
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        try {

            Notification notification =
                    createNotification();

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                startForeground(
                        1001,
                        notification,
                        ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                );

            } else {

                startForeground(
                        1001,
                        notification
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Foreground service failed",
                    e
            );

            updateOverlay(
                    "Arena Helper\n\n" +
                            "Foreground Service -virhe"
            );

            stopSelf();

            return START_NOT_STICKY;
        }

        if (projectionData != null) {

            startScreenCapture();

        } else {

            updateOverlay(
                    "Arena Helper\n\n" +
                            "Näytönjako puuttuu"
            );
        }

        return START_NOT_STICKY;
    }

    private void createOverlay() {

        try {

            windowManager =
                    (WindowManager)
                            getSystemService(
                                    WINDOW_SERVICE
                            );

            if (windowManager == null) {
                return;
            }

            overlayText =
                    new TextView(this);

            overlayText.setText(
                    "Arena Helper\n\n" +
                            "Näytönjako käynnissä"
            );

            overlayText.setTextColor(
                    Color.WHITE
            );

            overlayText.setTextSize(14);

            overlayText.setGravity(
                    Gravity.CENTER
            );

            overlayText.setPadding(
                    25,
                    15,
                    25,
                    15
            );

            overlayText.setBackgroundColor(
                    Color.argb(
                            220,
                            0,
                            0,
                            0
                    )
            );

            int overlayType;

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                overlayType =
                        WindowManager
                                .LayoutParams
                                .TYPE_APPLICATION_OVERLAY;

            } else {

                overlayType =
                        WindowManager
                                .LayoutParams
                                .TYPE_PHONE;
            }

            WindowManager.LayoutParams params =
                    new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            overlayType,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            PixelFormat.TRANSLUCENT
                    );

            params.gravity =
                    Gravity.TOP |
                            Gravity.CENTER_HORIZONTAL;

            params.y = 80;

            windowManager.addView(
                    overlayText,
                    params
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Overlay error",
                    e
            );
        }
    }

    private void updateOverlay(
            String text
    ) {

        if (overlayText == null ||
                handler == null) {
            return;
        }

        handler.post(() -> {

            if (overlayText != null) {

                overlayText.setText(text);
            }
        });
    }

    private void startScreenCapture() {

        try {

            updateOverlay(
                    "Arena Helper\n\n" +
                            "Käynnistetään OCR..."
            );

            MediaProjectionManager projectionManager =
                    (MediaProjectionManager)
                            getSystemService(
                                    Context.MEDIA_PROJECTION_SERVICE
                            );

            if (projectionManager == null) {

                throw new IllegalStateException(
                        "MediaProjectionManager puuttuu"
                );
            }

            mediaProjection =
                    projectionManager.getMediaProjection(
                            projectionResultCode,
                            projectionData
                    );

            if (mediaProjection == null) {

                throw new IllegalStateException(
                        "MediaProjection on null"
                );
            }

            mediaProjectionCallback =
                    new MediaProjection.Callback() {

                        @Override
                        public void onStop() {

                            Log.d(
                                    TAG,
                                    "MediaProjection stopped"
                            );

                            updateOverlay(
                                    "Arena Helper\n\n" +
                                            "Näytönjako pysäytettiin"
                            );
                        }
                    };

            mediaProjection.registerCallback(
                    mediaProjectionCallback,
                    handler
            );

            int width =
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels;

            int height =
                    getResources()
                            .getDisplayMetrics()
                            .heightPixels;

            int density =
                    getResources()
                            .getDisplayMetrics()
                            .densityDpi;

            Log.d(
                    TAG,
                    "Capture resolution: " +
                            width +
                            "x" +
                            height
            );

            imageReader =
                    ImageReader.newInstance(
                            width,
                            height,
                            PixelFormat.RGBA_8888,
                            2
                    );

            mediaProjection.createVirtualDisplay(
                    "ArenaHelperDisplay",
                    width,
                    height,
                    density,
                    0,
                    imageReader.getSurface(),
                    null,
                    handler
            );

            imageReader.setOnImageAvailableListener(
                    reader ->
                            processLatestImage(reader),
                    handler
            );

            updateOverlay(
                    "Arena Helper\n\n" +
                            "Näytönjako käynnissä\n" +
                            "Etsitään Arena-kortteja..."
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Screen capture error",
                    e
            );

            updateOverlay(
                    "Arena Helper\n\n" +
                            "Näytönjaon virhe\n\n" +
                            e.getClass()
                                    .getSimpleName()
            );
        }
    }

    private void processLatestImage(
            ImageReader reader
    ) {

        long now =
                System.currentTimeMillis();

        if (now - lastOCRTime <
                OCR_INTERVAL) {

            Image oldImage = null;

            try {

                oldImage =
                        reader.acquireLatestImage();

            } catch (Exception ignored) {
            }

            if (oldImage != null) {
                oldImage.close();
            }

            return;
        }

        if (processing) {

            Image oldImage = null;

            try {

                oldImage =
                        reader.acquireLatestImage();

            } catch (Exception ignored) {
            }

            if (oldImage != null) {
                oldImage.close();
            }

            return;
        }

        Image image = null;

        try {

            image =
                    reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            processing = true;
            lastOCRTime = now;

            Bitmap bitmap =
                    imageToBitmap(image);

            image.close();
            image = null;

            if (bitmap == null) {

                processing = false;
                return;
            }

            /*
             * Tässä vaiheessa emme syötä OCR:lle
             * koko Hearthstone-näyttöä.
             *
             * Ensin rajataan alue,
             * jossa Arena-kortit sijaitsevat.
             */
            Bitmap arenaBitmap =
                    cropArenaCards(bitmap);

            if (arenaBitmap != bitmap) {
                bitmap.recycle();
            }

            runOCR(arenaBitmap);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Screenshot processing error",
                    e
            );

            if (image != null) {
                image.close();
            }

            processing = false;
        }
    }

    /*
     * Näyttö on 2640 x 1200 vaakasuunnassa.
     *
     * Kolme Arena-korttia ovat suunnilleen
     * keskellä näyttöä vierekkäin.
     *
     * Rajaus jätetään tarkoituksella hieman
     * väljäksi ensimmäisessä versiossa.
     */
    private Bitmap cropArenaCards(
            Bitmap source
    ) {

        int width =
                source.getWidth();

        int height =
                source.getHeight();

        int left =
                (int) (width * 0.10f);

        int top =
                (int) (height * 0.12f);

        int right =
                (int) (width * 0.90f);

        int bottom =
                (int) (height * 0.92f);

        int cropWidth =
                right - left;

        int cropHeight =
                bottom - top;

        if (cropWidth <= 0 ||
                cropHeight <= 0) {

            return source;
        }

        try {

            return Bitmap.createBitmap(
                    source,
                    left,
                    top,
                    cropWidth,
                    cropHeight
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Arena crop failed",
                    e
            );

            return source;
        }
    }

    private Bitmap imageToBitmap(
            Image image
    ) {

        try {

            Image.Plane[] planes =
                    image.getPlanes();

            if (planes.length == 0) {
                return null;
            }

            ByteBuffer buffer =
                    planes[0].getBuffer();

            int pixelStride =
                    planes[0].getPixelStride();

            int rowStride =
                    planes[0].getRowStride();

            int rowPadding =
                    rowStride -
                            pixelStride *
                                    image.getWidth();

            int bitmapWidth =
                    image.getWidth() +
                            rowPadding /
                                    pixelStride;

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            bitmapWidth,
                            image.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );

            buffer.rewind();

            bitmap.copyPixelsFromBuffer(
                    buffer
            );

            return bitmap;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Bitmap conversion failed",
                    e
            );

            return null;
        }
    }

    private void runOCR(
            Bitmap bitmap
    ) {

        try {

            InputImage inputImage =
                    InputImage.fromBitmap(
                            bitmap,
                            0
                    );

            recognizer.process(
                    inputImage
            )

            .addOnSuccessListener(
                    text -> {

                        String result =
                                text.getText();

                        if (result == null) {
                            result = "";
                        }

                        result =
                                cleanOCRText(
                                        result
                                );

                        if (!result.isEmpty()) {

                            handleRecognizedText(
                                    result
                            );

                        } else {

                            updateOverlay(
                                    "Arena Helper\n\n" +
                                            "Arena-kortteja etsitään..."
                            );
                        }

                        bitmap.recycle();

                        processing = false;
                    }
            )

            .addOnFailureListener(
                    e -> {

                        Log.e(
                                TAG,
                                "OCR failed",
                                e
                        );

                        updateOverlay(
                                "Arena Helper\n\n" +
                                        "OCR-virhe"
                        );

                        bitmap.recycle();

                        processing = false;
                    }
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "OCR start failed",
                    e
            );

            bitmap.recycle();

            processing = false;
        }
    }

    private String cleanOCRText(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String[] lines =
                text.split("\\r?\\n");

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            line =
                    line.trim();

            if (line.length() < 3) {
                continue;
            }

            int letters = 0;

            for (int i = 0;
                    i < line.length();
                    i++) {

                char c =
                        line.charAt(i);

                if (Character.isLetter(c)) {
                    letters++;
                }
            }

            if (letters < 2) {
                continue;
            }

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append(line);
        }

        return result.toString().trim();
    }

    private void handleRecognizedText(
            String text
    ) {

        String displayText =
                text.trim();

        if (displayText.length() > 400) {

            displayText =
                    displayText.substring(
                            0,
                            400
                    ) + "...";
        }

        updateOverlay(
                "Arena Helper\n\n" +
                        "Arena-korteista tunnistettu:\n\n" +
                        displayText
        );

        Log.d(
                TAG,
                "Arena OCR:\n" +
                        text
        );
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Arena Helper",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Arena Helper screen capture"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    private Notification createNotification() {

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "Arena Helper"
                )
                .setContentText(
                        "Näytönjako käynnissä"
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_view
                )
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {

        if (mediaProjection != null &&
                mediaProjectionCallback != null) {

            try {

                mediaProjection.unregisterCallback(
                        mediaProjectionCallback
                );

            } catch (Exception ignored) {
            }

            mediaProjectionCallback = null;
        }

        if (overlayText != null &&
                windowManager != null) {

            try {

                windowManager.removeView(
                        overlayText
                );

            } catch (Exception ignored) {
            }

            overlayText = null;
        }

        if (imageReader != null) {

            try {
                imageReader.close();
            } catch (Exception ignored) {
            }

            imageReader = null;
        }

        if (mediaProjection != null) {

            try {
                mediaProjection.stop();
            } catch (Exception ignored) {
            }

            mediaProjection = null;
        }

        if (recognizer != null) {

            try {
                recognizer.close();
            } catch (Exception ignored) {
            }

            recognizer = null;
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {
        return null;
    }
}
