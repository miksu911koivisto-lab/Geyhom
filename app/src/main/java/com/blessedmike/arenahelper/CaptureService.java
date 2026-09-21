package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
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

    public static void setProjectionData(int resultCode, Intent data) {
        projectionResultCode = resultCode;
        projectionData = data;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );

        createNotificationChannel();

        Log.d(TAG, "Arena Helper CaptureService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(
                1001,
                createNotification()
        );

        if (projectionData != null) {
            startScreenCapture();
        } else {
            Log.e(TAG, "Projection data is missing");
        }

        return START_STICKY;
    }

    private void startScreenCapture() {

        try {

            MediaProjectionManager projectionManager =
                    (MediaProjectionManager)
                            getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            if (projectionManager == null) {
                Log.e(TAG, "MediaProjectionManager is null");
                return;
            }

            mediaProjection =
                    projectionManager.getMediaProjection(
                            projectionResultCode,
                            projectionData
                    );

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null");
                return;
            }

            int width = getResources()
                    .getDisplayMetrics()
                    .widthPixels;

            int height = getResources()
                    .getDisplayMetrics()
                    .heightPixels;

            int density = getResources()
                    .getDisplayMetrics()
                    .densityDpi;

            imageReader = ImageReader.newInstance(
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
                    reader -> processLatestImage(reader),
                    handler
            );

            Log.d(TAG, "Screen capture started");

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not start screen capture",
                    e
            );
        }
    }

    private void processLatestImage(ImageReader reader) {

        if (processing) {
            Image oldImage = null;

            try {
                oldImage = reader.acquireLatestImage();
            } catch (Exception ignored) {
            }

            if (oldImage != null) {
                oldImage.close();
            }

            return;
        }

        Image image = null;

        try {

            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            processing = true;

            Bitmap bitmap = imageToBitmap(image);

            image.close();
            image = null;

            if (bitmap == null) {
                processing = false;
                return;
            }

            runOCR(bitmap);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Error processing screenshot",
                    e
            );

            if (image != null) {
                image.close();
            }

            processing = false;
        }
    }

    private Bitmap imageToBitmap(Image image) {

        try {

            Image.Plane[] planes = image.getPlanes();

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

            Bitmap bitmap = Bitmap.createBitmap(
                    image.getWidth() +
                            rowPadding /
                                    pixelStride,
                    image.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            bitmap.copyPixelsFromBuffer(buffer);

            return bitmap;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not convert image to bitmap",
                    e
            );

            return null;
        }
    }

    private void runOCR(Bitmap bitmap) {

        try {

            InputImage inputImage =
                    InputImage.fromBitmap(
                            bitmap,
                            0
                    );

            recognizer.process(inputImage)
                    .addOnSuccessListener(
                            text -> {

                                String result =
                                        text.getText();

                                if (result != null &&
                                        !result.trim().isEmpty()) {

                                    Log.d(
                                            TAG,
                                            "OCR RESULT:\n" +
                                                    result
                                    );

                                    handleRecognizedText(
                                            result
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

                                bitmap.recycle();

                                processing = false;
                            }
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not start OCR",
                    e
            );

            bitmap.recycle();

            processing = false;
        }
    }

    private void handleRecognizedText(String text) {

        /*
         * Tässä kohdassa käsitellään Hearthstonesta
         * tunnistettua tekstiä.
         *
         * Seuraavassa vaiheessa tähän voidaan lisätä
         * korttien nimien tunnistus ja Arena-valinnan
         * automaattinen arviointi.
         */

        String cleanedText =
                text.trim();

        if (!cleanedText.isEmpty()) {

            Log.d(
                    TAG,
                    "Arena Helper recognized:\n" +
                            cleanedText
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Arena Helper",
                            NotificationManager.IMPORTANCE_LOW
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
                        "Arena Helper on aktiivinen"
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_view
                )
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {

        Log.d(
                TAG,
                "Arena Helper CaptureService stopped"
        );

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
    public IBinder onBind(Intent intent) {
        return null;
    }
}
