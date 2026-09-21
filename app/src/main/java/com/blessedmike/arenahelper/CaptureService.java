package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class CaptureService extends Service {

    private static final String CHANNEL_ID = "ArenaHelperChannel";
    private static final int NOTIFICATION_ID = 1001;

    private static int projectionResultCode;
    private static Intent projectionData;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private Handler handler;
    private TextRecognizer recognizer;

    private WindowManager windowManager;
    private TextView overlayText;

    private boolean processing = false;

    // ---------------------------------------------------------
    // CARD 1 STABILIZATION
    // ---------------------------------------------------------

    private String stableCard1 = "";
    private String candidateCard1 = "";
    private int candidateCard1Count = 0;

    private static final int CARD1_CONFIRMATIONS = 2;

    // ---------------------------------------------------------
    // MEDIA PROJECTION CALLBACK
    // ---------------------------------------------------------

    private final MediaProjection.Callback mediaProjectionCallback =
            new MediaProjection.Callback() {

                @Override
                public void onStop() {

                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }

                    if (imageReader != null) {
                        imageReader.close();
                        imageReader = null;
                    }

                    mediaProjection = null;
                }
            };

    // ---------------------------------------------------------
    // PROJECTION DATA
    // ---------------------------------------------------------

    public static void setProjectionData(
            int resultCode,
            Intent data
    ) {
        projectionResultCode = resultCode;
        projectionData = data;
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

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

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        createNotificationChannel();
        createOverlay();
    }

    // ---------------------------------------------------------
    // START
    // ---------------------------------------------------------

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        if (intent != null) {

            int resultCode =
                    intent.getIntExtra(
                            "resultCode",
                            projectionResultCode
                    );

            Intent data =
                    intent.getParcelableExtra(
                            "data"
                    );

            if (data != null) {
                projectionData = data;
            }

            projectionResultCode = resultCode;
        }

        startCapture();

        return START_STICKY;
    }

    // ---------------------------------------------------------
    // START CAPTURE
    // ---------------------------------------------------------

    private void startCapture() {

        if (projectionData == null) {
            showOverlay(
                    "Capture error: permission data puuttuu"
            );
            return;
        }

        try {

            MediaProjectionManager projectionManager =
                    (MediaProjectionManager)
                            getSystemService(
                                    MEDIA_PROJECTION_SERVICE
                            );

            mediaProjection =
                    projectionManager.getMediaProjection(
                            projectionResultCode,
                            projectionData
                    );

            if (mediaProjection == null) {

                showOverlay(
                        "Capture error: MediaProjection null"
                );

                return;
            }

            DisplayMetrics metrics =
                    new DisplayMetrics();

            WindowManager wm =
                    (WindowManager)
                            getSystemService(
                                    WINDOW_SERVICE
                            );

            wm.getDefaultDisplay()
                    .getRealMetrics(metrics);

            int width =
                    metrics.widthPixels;

            int height =
                    metrics.heightPixels;

            int density =
                    metrics.densityDpi;

            // -------------------------------------------------
            // IMAGE READER
            // -------------------------------------------------

            imageReader =
                    ImageReader.newInstance(
                            width,
                            height,
                            PixelFormat.RGBA_8888,
                            2
                    );

            imageReader.setOnImageAvailableListener(
                    reader -> processLatestImage(),
                    handler
            );

            // -------------------------------------------------
            // IMPORTANT ANDROID REQUIREMENT
            //
            // CALLBACK MUST BE REGISTERED BEFORE
            // createVirtualDisplay()
            // -------------------------------------------------

            mediaProjection.registerCallback(
                    mediaProjectionCallback,
                    handler
            );

            // -------------------------------------------------
            // VIRTUAL DISPLAY
            // -------------------------------------------------

            virtualDisplay =
                    mediaProjection.createVirtualDisplay(
                            "ArenaHelperCapture",
                            width,
                            height,
                            density,
                            DisplayManager
                                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            imageReader.getSurface(),
                            null,
                            handler
                    );

            showOverlay(
                    "Arena Helper aktiivinen"
            );

        } catch (Exception e) {

            showOverlay(
                    "Capture error: " +
                            e.getMessage()
            );
        }
    }

    // ---------------------------------------------------------
    // IMAGE
    // ---------------------------------------------------------

    private void processLatestImage() {

        if (processing) {
            return;
        }

        if (imageReader == null) {
            return;
        }

        processing = true;

        Image image = null;

        try {

            image =
                    imageReader.acquireLatestImage();

            if (image == null) {

                processing = false;
                return;
            }

            Bitmap bitmap =
                    imageToBitmap(image);

            image.close();
            image = null;

            if (bitmap == null) {

                processing = false;
                return;
            }

            processThreeCards(bitmap);

        } catch (Exception e) {

            if (image != null) {

                try {
                    image.close();
                } catch (Exception ignored) {
                }
            }

            processing = false;
        }
    }

    // ---------------------------------------------------------
    // THREE CARDS
    // ---------------------------------------------------------

    private void processThreeCards(
            Bitmap screen
    ) {

        int width =
                screen.getWidth();

        int height =
                screen.getHeight();

        // -------------------------------------------------
        // THESE ARE THE ORIGINAL WORKING OCR AREAS
        // -------------------------------------------------

        int nameTop =
                (int)
                        (height * 0.45f);

        int nameBottom =
                (int)
                        (height * 0.55f);

        // CARD 1
        int card1Left =
                (int)
                        (width * 0.065f);

        int card1Right =
                (int)
                        (width * 0.38f);

        // CARD 2
        int card2Left =
                (int)
                        (width * 0.355f);

        int card2Right =
                (int)
                        (width * 0.645f);

        // CARD 3
        int card3Left =
                (int)
                        (width * 0.60f);

        int card3Right =
                (int)
                        (width * 0.935f);

        Bitmap card1 =
                cropCard(
                        screen,
                        card1Left,
                        nameTop,
                        card1Right,
                        nameBottom
                );

        Bitmap card2 =
                cropCard(
                        screen,
                        card2Left,
                        nameTop,
                        card2Right,
                        nameBottom
                );

        Bitmap card3 =
                cropCard(
                        screen,
                        card3Left,
                        nameTop,
                        card3Right,
                        nameBottom
                );

        runCardOCR(
                card1,
                card2,
                card3
        );
    }

    // ---------------------------------------------------------
    // CROP + 2X ENLARGE
    // ---------------------------------------------------------

    private Bitmap cropCard(
            Bitmap source,
            int left,
            int top,
            int right,
            int bottom
    ) {

        left =
                Math.max(
                        0,
                        left
                );

        top =
                Math.max(
                        0,
                        top
                );

        right =
                Math.min(
                        source.getWidth(),
                        right
                );

        bottom =
                Math.min(
                        source.getHeight(),
                        bottom
                );

        int width =
                right - left;

        int height =
                bottom - top;

        if (width <= 0 ||
                height <= 0) {

            return null;
        }

        Bitmap cropped =
                Bitmap.createBitmap(
                        source,
                        left,
                        top,
                        width,
                        height
                );

        // ORIGINAL OCR USED 2X ENLARGEMENT
        return Bitmap.createScaledBitmap(
                cropped,
                width * 2,
                height * 2,
                true
        );
    }

    // ---------------------------------------------------------
    // RUN OCR
    // ---------------------------------------------------------

    private void runCardOCR(
            Bitmap card1,
            Bitmap card2,
            Bitmap card3
    ) {

        final String[] results =
                new String[3];

        results[0] = "";
        results[1] = "";
        results[2] = "";

        final int[] finished =
                new int[]{0};

        // -------------------------------------------------
        // CARD 1
        // -------------------------------------------------

        recognizeNormalCard(
                card1,
                text -> {

                    String cleaned =
                            cleanCardName(text);

                    cleaned =
                            stabilizeCard1(
                                    cleaned
                            );

                    results[0] =
                            cleaned;

                    finished[0]++;

                    checkOCRFinished(
                            results,
                            finished
                    );
                }
        );

        // -------------------------------------------------
        // CARD 2
        // -------------------------------------------------

        recognizeNormalCard(
                card2,
                text -> {

                    results[1] =
                            cleanCardName(text);

                    finished[0]++;

                    checkOCRFinished(
                            results,
                            finished
                    );
                }
        );

        // -------------------------------------------------
        // CARD 3
        // -------------------------------------------------

        recognizeNormalCard(
                card3,
                text -> {

                    results[2] =
                            cleanCardName(text);

                    finished[0]++;

                    checkOCRFinished(
                            results,
                            finished
                    );
                }
        );
    }

    // ---------------------------------------------------------
    // NORMAL OCR
    // ---------------------------------------------------------

    private void recognizeNormalCard(
            Bitmap bitmap,
            OCRCallback callback
    ) {

        if (bitmap == null) {

            callback.onResult(
                    "Ei tunnistettu"
            );

            return;
        }

        InputImage inputImage =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );

        recognizer
                .process(inputImage)
                .addOnSuccessListener(
                        text -> {

                            String result =
                                    text.getText();

                            if (result == null ||
                                    result.trim().isEmpty()) {

                                callback.onResult(
                                        "Ei tunnistettu"
                                );

                            } else {

                                callback.onResult(
                                        result
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                callback.onResult(
                                        "Ei tunnistettu"
                                )
                );
    }

    // ---------------------------------------------------------
    // OCR FINISHED
    // ---------------------------------------------------------

    private void checkOCRFinished(
            String[] results,
            int[] finished
    ) {

        if (finished[0] < 3) {
            return;
        }

        showThreeCards(
                results[0],
                results[1],
                results[2]
        );
    }

    // ---------------------------------------------------------
    // CLEAN OCR
    // ---------------------------------------------------------

    private String cleanCardName(
            String text
    ) {

        if (text == null) {
            return "Ei tunnistettu";
        }

        text =
                text.replace(
                        "\n",
                        " "
                );

        text =
                text.replace(
                        "\r",
                        " "
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        text =
                text.trim();

        text =
                fixSoldierOfInfinite(
                        text
                );

        if (text.isEmpty()) {
            return "Ei tunnistettu";
        }

        return text;
    }

    // ---------------------------------------------------------
    // SOLDIER OF THE INFINITE
    // ---------------------------------------------------------

    private String fixSoldierOfInfinite(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                normalize(text);

        if (
                normalized.contains(
                        "soldier"
                )
                &&
                normalized.contains(
                        "infinite"
                )
        ) {

            return "Soldier of the Infinite";
        }

        if (
                normalized.contains(
                        "sotdier"
                )
                ||
                normalized.contains(
                        "so1dier"
                )
                ||
                normalized.contains(
                        "soldiero"
                )
                ||
                normalized.contains(
                        "soldieroftheinfinite"
                )
                ||
                normalized.contains(
                        "soldier2oftheinfinite"
                )
        ) {

            return "Soldier of the Infinite";
        }

        if (
                normalized.startsWith(
                        "soldier"
                )
                &&
                normalized.length() >= 8
        ) {

            return "Soldier of the Infinite";
        }

        return text;
    }

    // ---------------------------------------------------------
    // CARD 1 STABILIZATION
    // ---------------------------------------------------------

    private String stabilizeCard1(
            String detected
    ) {

        detected =
                fixSoldierOfInfinite(
                        detected
                );

        if (!isValidCard(detected)) {

            if (stableCard1.isEmpty()) {
                return "Ei tunnistettu";
            }

            return stableCard1;
        }

        if (stableCard1.isEmpty()) {

            candidateCard1 =
                    detected;

            candidateCard1Count = 1;

            return detected;
        }

        if (
                similarCard1(
                        stableCard1,
                        detected
                )
        ) {

            stableCard1 =
                    detected;

            candidateCard1 =
                    stableCard1;

            candidateCard1Count = 0;

            return stableCard1;
        }

        if (
                similarCard1(
                        candidateCard1,
                        detected
                )
        ) {

            candidateCard1Count++;

        } else {

            candidateCard1 =
                    detected;

            candidateCard1Count = 1;
        }

        if (
                candidateCard1Count >=
                        CARD1_CONFIRMATIONS
        ) {

            stableCard1 =
                    fixSoldierOfInfinite(
                            candidateCard1
                    );

            candidateCard1Count = 0;
        }

        if (stableCard1.isEmpty()) {
            return detected;
        }

        return stableCard1;
    }

    // ---------------------------------------------------------
    // SIMILARITY
    // ---------------------------------------------------------

    private boolean similarCard1(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }

        String aa =
                normalize(a);

        String bb =
                normalize(b);

        if (aa.isEmpty() ||
                bb.isEmpty()) {

            return false;
        }

        if (aa.equals(bb)) {
            return true;
        }

        int distance =
                levenshteinDistance(
                        aa,
                        bb
                );

        int max =
                Math.max(
                        aa.length(),
                        bb.length()
                );

        if (max <= 6) {
            return distance <= 1;
        }

        if (max <= 12) {
            return distance <= 2;
        }

        return distance <= 3;
    }

    // ---------------------------------------------------------
    // LEVENSHTEIN
    // ---------------------------------------------------------

    private int levenshteinDistance(
            String a,
            String b
    ) {

        int[][] dp =
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (int i = 0;
                i <= a.length();
                i++) {

            dp[i][0] = i;
        }

        for (int j = 0;
                j <= b.length();
                j++) {

            dp[0][j] = j;
        }

        for (int i = 1;
                i <= a.length();
                i++) {

            for (int j = 1;
                    j <= b.length();
                    j++) {

                int cost =
                        a.charAt(i - 1)
                                ==
                        b.charAt(j - 1)
                                ? 0
                                : 1;

                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
                                ),
                                dp[i - 1][j - 1]
                                        + cost
                        );
            }
        }

        return dp[a.length()][b.length()];
    }

    // ---------------------------------------------------------
    // VALID CARD
    // ---------------------------------------------------------

    private boolean isValidCard(
            String text
    ) {

        if (text == null) {
            return false;
        }

        text =
                text.trim();

        return !text.isEmpty()
                &&
                !text.equalsIgnoreCase(
                        "Ei tunnistettu"
                );
    }

    // ---------------------------------------------------------
    // NORMALIZE
    // ---------------------------------------------------------

    private String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    // ---------------------------------------------------------
    // SHOW CARDS + ADVISOR
    // ---------------------------------------------------------

    private void showThreeCards(
            String card1,
            String card2,
            String card3
    ) {

        if (!isValidCard(card1)) {
            card1 = "Ei tunnistettu";
        }

        if (!isValidCard(card2)) {
            card2 = "Ei tunnistettu";
        }

        if (!isValidCard(card3)) {
            card3 = "Ei tunnistettu";
        }

        String recommendation =
                ArenaAdvisor.recommend(
                        card1,
                        card2,
                        card3
                );

        String result =
                "Kortti 1: " +
                        card1 +
                        "\n\n" +

                        "Kortti 2: " +
                        card2 +
                        "\n\n" +

                        "Kortti 3: " +
                        card3 +
                        "\n\n" +

                        "SUOSITUS:\n" +
                        recommendation;

        showOverlay(result);

        processing = false;
    }

    // ---------------------------------------------------------
    // IMAGE TO BITMAP
    // ---------------------------------------------------------

    private Bitmap imageToBitmap(
            Image image
    ) {

        Image.Plane[] planes =
                image.getPlanes();

        if (planes == null ||
                planes.length == 0) {

            return null;
        }

        Image.Plane plane =
                planes[0];

        java.nio.ByteBuffer buffer =
                plane.getBuffer();

        int pixelStride =
                plane.getPixelStride();

        int rowStride =
                plane.getRowStride();

        int rowPadding =
                rowStride -
                        pixelStride *
                                image.getWidth();

        int bitmapWidth =
                image.getWidth()
                        +
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

        if (bitmapWidth !=
                image.getWidth()) {

            Bitmap cropped =
                    Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            image.getWidth(),
                            image.getHeight()
                    );

            bitmap.recycle();

            bitmap = cropped;
        }

        return bitmap;
    }

    // ---------------------------------------------------------
    // OVERLAY
    // ---------------------------------------------------------

    private void createOverlay() {

        if (windowManager == null) {
            return;
        }

        overlayText =
                new TextView(this);

        overlayText.setText(
                "Arena Helper aktiivinen"
        );

        overlayText.setTextSize(16);

        overlayText.setPadding(
                20,
                20,
                20,
                20
        );

        overlayText.setGravity(
                Gravity.CENTER
        );

        int type;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            type =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            type =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE
                                |
                                WindowManager.LayoutParams
                                .FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP |
                        Gravity.CENTER_HORIZONTAL;

        params.y = 80;

        try {

            windowManager.addView(
                    overlayText,
                    params
            );

        } catch (Exception ignored) {
        }
    }

    // ---------------------------------------------------------
    // SHOW OVERLAY
    // ---------------------------------------------------------

    private void showOverlay(
            String text
    ) {

        if (overlayText == null) {
            return;
        }

        handler.post(
                () ->
                        overlayText.setText(
                                text
                        )
        );
    }

    // ---------------------------------------------------------
    // NOTIFICATION CHANNEL
    // ---------------------------------------------------------

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

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    // ---------------------------------------------------------
    // NOTIFICATION
    // ---------------------------------------------------------

    private Notification createNotification() {

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "Arena Helper"
                )
                .setContentText(
                        "Korttien tunnistus aktiivinen"
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_view
                )
                .setOngoing(true)
                .build();
    }

    // ---------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------

    @Override
    public void onDestroy() {

        if (mediaProjection != null) {

            try {
                mediaProjection.unregisterCallback(
                        mediaProjectionCallback
                );
            } catch (Exception ignored) {
            }
        }

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

        if (recognizer != null) {

            recognizer.close();
            recognizer = null;
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

        super.onDestroy();
    }

    // ---------------------------------------------------------
    // BIND
    // ---------------------------------------------------------

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {
        return null;
    }

    // ---------------------------------------------------------
    // OCR CALLBACK
    // ---------------------------------------------------------

    private interface OCRCallback {

        void onResult(String text);
    }
}
