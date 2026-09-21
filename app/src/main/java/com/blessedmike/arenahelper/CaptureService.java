package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognition;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CaptureService extends Service {

    private static final String CHANNEL_ID =
            "ArenaHelperCapture";

    private static final int NOTIFICATION_ID =
            1001;

    private static final long OCR_INTERVAL =
            1500;

    private static int projectionResultCode;

    private static Intent projectionData;

    private MediaProjection mediaProjection;

    private VirtualDisplay virtualDisplay;

    private ImageReader imageReader;

    private TextRecognizer recognizer;

    private Handler handler;

    private WindowManager windowManager;

    private TextView overlayText;

    private boolean processing = false;

    private int screenWidth;

    private int screenHeight;

    private int screenDensity;


    // =========================================================
    // PROJECTION DATA
    // =========================================================

    public static void setProjectionData(
            int resultCode,
            Intent data
    ) {

        projectionResultCode =
                resultCode;

        projectionData =
                data;
    }


    // =========================================================
    // CARD 1 STABILIZATION
    // =========================================================

    private String stableCard1 = "";

    private String candidateCard1 = "";

    private int candidateCard1Count = 0;

    private static final int CARD1_CONFIRMATIONS = 2;


    // =========================================================
    // ON CREATE
    // =========================================================

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


    // =========================================================
    // START COMMAND
    // =========================================================

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

        try {

            /*
             * Käytetään ensin MainActivityn
             * tallentamia projection-tietoja.
             */

            if (projectionData != null) {

                startCapture(
                        projectionResultCode,
                        projectionData
                );

                return START_STICKY;
            }


            /*
             * Jos tiedot tulevat suoraan
             * Service-intentistä.
             */

            int resultCode =
                    intent != null
                            ? intent.getIntExtra(
                                    "resultCode",
                                    -1
                            )
                            : -1;

            Intent data =
                    intent != null
                            ? intent.getParcelableExtra(
                                    "data"
                            )
                            : null;


            if (data != null &&
                    resultCode != -1) {

                startCapture(
                        resultCode,
                        data
                );

            } else {

                showError(
                        "MediaProjection-data puuttuu"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Capture-virhe: " +
                            e.getMessage()
            );
        }

        return START_STICKY;
    }


    // =========================================================
    // START CAPTURE
    // =========================================================

    private void startCapture(
            int resultCode,
            Intent data
    ) {

        if (mediaProjection != null) {
            return;
        }

        WindowManager wm =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        wm.getDefaultDisplay()
                .getRealMetrics(metrics);

        screenWidth =
                metrics.widthPixels;

        screenHeight =
                metrics.heightPixels;

        screenDensity =
                metrics.densityDpi;


        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );


        mediaProjection =
                manager.getMediaProjection(
                        resultCode,
                        data
                );


        if (mediaProjection == null) {

            showError(
                    "MediaProjection ei käynnistynyt"
            );

            return;
        }


        imageReader =
                ImageReader.newInstance(
                        screenWidth,
                        screenHeight,
                        PixelFormat.RGBA_8888,
                        2
                );


        virtualDisplay =
                mediaProjection.createVirtualDisplay(
                        "ArenaHelper",
                        screenWidth,
                        screenHeight,
                        screenDensity,
                        DisplayManager
                                .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(),
                        null,
                        handler
                );


        imageReader.setOnImageAvailableListener(
                reader ->
                        processLatestImage(
                                reader
                        ),
                handler
        );


        showStatus(
                "Arena Helper aktiivinen"
        );
    }


    // =========================================================
    // IMAGE PROCESSING
    // =========================================================

    private void processLatestImage(
            ImageReader reader
    ) {

        if (processing) {
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

            Bitmap bitmap =
                    imageToBitmap(image);

            image.close();

            image = null;


            if (bitmap == null) {

                processing = false;

                return;
            }


            processThreeCards(
                    bitmap
            );

        } catch (Exception e) {

            e.printStackTrace();

            if (image != null) {

                try {
                    image.close();
                } catch (Exception ignored) {
                }
            }

            processing = false;
        }
    }


    // =========================================================
    // THREE CARDS
    // =========================================================

    private void processThreeCards(
            Bitmap screen
    ) {

        int width =
                screen.getWidth();

        int height =
                screen.getHeight();


        /*
         * ÄLÄ MUUTA NÄITÄ RAJAUKSIA.
         * Nykyinen OCR toimii näillä.
         */

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


        Bitmap card1Bitmap =
                safeCrop(
                        screen,
                        card1Left,
                        nameTop,
                        card1Right,
                        nameBottom
                );


        Bitmap card2Bitmap =
                safeCrop(
                        screen,
                        card2Left,
                        nameTop,
                        card2Right,
                        nameBottom
                );


        Bitmap card3Bitmap =
                safeCrop(
                        screen,
                        card3Left,
                        nameTop,
                        card3Right,
                        nameBottom
                );


        List<Bitmap> bitmaps =
                new ArrayList<>();

        bitmaps.add(
                enlargeForOCR(
                        card1Bitmap
                )
        );

        bitmaps.add(
                enlargeForOCR(
                        card2Bitmap
                )
        );

        bitmaps.add(
                enlargeForOCR(
                        card3Bitmap
                )
        );


        String[] results =
                new String[3];


        runCardOCR(
                bitmaps,
                results,
                0
        );
    }


    // =========================================================
    // SAFE CROP
    // =========================================================

    private Bitmap safeCrop(
            Bitmap source,
            int left,
            int top,
            int right,
            int bottom
    ) {

        left =
                Math.max(
                        0,
                        Math.min(
                                left,
                                source.getWidth() - 1
                        )
                );


        top =
                Math.max(
                        0,
                        Math.min(
                                top,
                                source.getHeight() - 1
                        )
                );


        right =
                Math.max(
                        left + 1,
                        Math.min(
                                right,
                                source.getWidth()
                        )
                );


        bottom =
                Math.max(
                        top + 1,
                        Math.min(
                                bottom,
                                source.getHeight()
                        )
                );


        return Bitmap.createBitmap(
                source,
                left,
                top,
                right - left,
                bottom - top
        );
    }


    // =========================================================
    // ENLARGE
    // =========================================================

    private Bitmap enlargeForOCR(
            Bitmap source
    ) {

        if (source == null) {
            return null;
        }

        int newWidth =
                source.getWidth() * 2;

        int newHeight =
                source.getHeight() * 2;


        return Bitmap.createScaledBitmap(
                source,
                newWidth,
                newHeight,
                true
        );
    }


    // =========================================================
    // OCR
    // =========================================================

    private void runCardOCR(
            List<Bitmap> bitmaps,
            String[] results,
            int index
    ) {

        if (index >= bitmaps.size()) {

            checkOCRFinished(
                    results
            );

            return;
        }


        Bitmap bitmap =
                bitmaps.get(index);


        if (bitmap == null) {

            results[index] =
                    "Ei tunnistettu";


            runCardOCR(
                    bitmaps,
                    results,
                    index + 1
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

                            results[index] =
                                    cleanCardName(
                                            text
                                                    .getText()
                                    );


                            runCardOCR(
                                    bitmaps,
                                    results,
                                    index + 1
                            );
                        }
                )

                .addOnFailureListener(
                        e -> {

                            results[index] =
                                    "Ei tunnistettu";


                            runCardOCR(
                                    bitmaps,
                                    results,
                                    index + 1
                            );
                        }
                );
    }


    // =========================================================
    // CLEAN OCR
    // =========================================================

    private String cleanCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }


        String cleaned =
                text.trim();


        cleaned =
                cleaned.replace(
                        "\n",
                        " "
                );


        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                );


        cleaned =
                cleaned.replace(
                        "Soldierof",
                        "Soldier of"
                );


        cleaned =
                cleaned.replace(
                        "Soldier 2of",
                        "Soldier of"
                );


        cleaned =
                cleaned.replace(
                        "Soldier 20f",
                        "Soldier of"
                );


        cleaned =
                fixSoldierOfInfinite(
                        cleaned
                );


        if (cleaned.isEmpty()) {
            return "";
        }


        return capitalizeFirstLetter(
                cleaned
        );
    }


    // =========================================================
    // SOLDIER OF THE INFINITE FIX
    // =========================================================

    private String fixSoldierOfInfinite(
            String text
    ) {

        if (text == null) {
            return "";
        }


        String normalized =
                text
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );


        if (normalized.contains(
                "soldierofinfinite"
        )) {

            return "Soldier of the Infinite";
        }


        if (normalized.contains(
                "soldieroftheinfinite"
        )) {

            return "Soldier of the Infinite";
        }


        if (normalized.contains(
                "soldier2oftheinfinite"
        )) {

            return "Soldier of the Infinite";
        }


        if (normalized.contains(
                "soldier20ftheinfinite"
        )) {

            return "Soldier of the Infinite";
        }


        if (normalized.equals(
                "soldier"
        )) {

            return "Soldier of the Infinite";
        }


        return text;
    }


    // =========================================================
    // CARD 1 STABILIZATION
    // =========================================================

    private String stabilizeCard1(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            if (stableCard1.isEmpty()) {
                return "Ei tunnistettu";
            }

            return stableCard1;
        }


        value =
                fixSoldierOfInfinite(
                        value
                );


        if (stableCard1.isEmpty()) {

            stableCard1 =
                    value;

            candidateCard1 =
                    value;

            candidateCard1Count =
                    0;

            return stableCard1;
        }


        if (similarCard1(
                value,
                stableCard1
        )) {

            candidateCard1 =
                    stableCard1;

            candidateCard1Count =
                    0;

            return stableCard1;
        }


        if (candidateCard1.equals(
                value
        )) {

            candidateCard1Count++;

        } else {

            candidateCard1 =
                    value;

            candidateCard1Count =
                    1;
        }


        if (candidateCard1Count >=
                CARD1_CONFIRMATIONS) {

            stableCard1 =
                    candidateCard1;

            candidateCard1Count =
                    0;
        }


        return stableCard1;
    }


    // =========================================================
    // CARD 1 SIMILARITY
    // =========================================================

    private boolean similarCard1(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }


        String x =
                normalizeCard1ForComparison(
                        a
                );


        String y =
                normalizeCard1ForComparison(
                        b
                );


        if (x.equals(y)) {
            return true;
        }


        int distance =
                levenshteinDistance(
                        x,
                        y
                );


        int max =
                Math.max(
                        x.length(),
                        y.length()
                );


        if (max <= 5) {

            return distance <= 1;
        }


        return distance <= 3;
    }


    // =========================================================
    // NORMALIZE CARD 1
    // =========================================================

    private String normalizeCard1ForComparison(
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


    // =========================================================
    // LEVENSHTEIN
    // =========================================================

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
                        a.charAt(i - 1) ==
                                b.charAt(j - 1)
                                ? 0
                                : 1;


                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
                                ),
                                dp[i - 1][j - 1] +
                                        cost
                        );
            }
        }


        return dp[
                a.length()
        ][
                b.length()
        ];
    }


    // =========================================================
    // CAPITALIZE
    // =========================================================

    private String capitalizeFirstLetter(
            String text
    ) {

        if (text == null ||
                text.isEmpty()) {

            return text;
        }


        return Character.toUpperCase(
                text.charAt(0)
        ) +
                text.substring(1);
    }


    // =========================================================
    // OCR FINISHED
    // =========================================================

    private void checkOCRFinished(
            String[] results
    ) {

        String card1 =
                stabilizeCard1(
                        results[0]
                );


        String card2 =
                results[1];


        String card3 =
                results[2];


        showThreeCards(
                card1,
                card2,
                card3
        );


        processing =
                false;
    }


    // =========================================================
    // SHOW THREE CARDS + ADVISOR
    // =========================================================

    private void showThreeCards(
            String card1,
            String card2,
            String card3
    ) {

        /*
         * TÄRKEÄ:
         *
         * Käytetään nykyisen ArenaAdvisor.java:n
         * olemassa olevaa metodia:
         *
         * recommend(String, String, String)
         */

        String recommendation =
                ArenaAdvisor.recommend(
                        card1,
                        card2,
                        card3
                );


        String display =
                "KORTTI 1: " +
                        card1 +
                        "\n\n" +

                        "KORTTI 2: " +
                        card2 +
                        "\n\n" +

                        "KORTTI 3: " +
                        card3 +
                        "\n\n" +

                        "--------------------\n\n" +

                        recommendation;


        showStatus(
                display
        );
    }


    // =========================================================
    // IMAGE -> BITMAP
    // =========================================================

    private Bitmap imageToBitmap(
            Image image
    ) {

        Image.Plane[] planes =
                image.getPlanes();


        if (planes.length == 0) {
            return null;
        }


        Image.Plane plane =
                planes[0];


        ByteBuffer buffer =
                plane.getBuffer();


        int pixelStride =
                plane.getPixelStride();


        int rowStride =
                plane.getRowStride();


        int rowPadding =
                rowStride -
                        pixelStride *
                                screenWidth;


        Bitmap bitmap =
                Bitmap.createBitmap(
                        screenWidth +
                                rowPadding /
                                        pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                );


        bitmap.copyPixelsFromBuffer(
                buffer
        );


        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                screenWidth,
                screenHeight
        );
    }


    // =========================================================
    // OVERLAY
    // =========================================================

    private void createOverlay() {

        overlayText =
                new TextView(this);


        overlayText.setText(
                "Arena Helper käynnistyy..."
        );


        overlayText.setTextSize(
                16
        );


        overlayText.setTextColor(
                android.graphics.Color.WHITE
        );


        overlayText.setBackgroundColor(
                0xCC000000
        );


        overlayText.setPadding(
                20,
                20,
                20,
                20
        );


        int overlayType;


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            overlayType =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            overlayType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }


        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );


        params.gravity =
                Gravity.TOP |
                        Gravity.CENTER_HORIZONTAL;


        params.y =
                100;


        try {

            windowManager.addView(
                    overlayText,
                    params
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // UPDATE OVERLAY
    // =========================================================

    private void showStatus(
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


    private void showError(
            String text
    ) {

        showStatus(
                "Arena Helper\n\n" +
                        text
        );
    }


    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

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


    // =========================================================
    // NOTIFICATION
    // =========================================================

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
                .setOngoing(
                        true
                )
                .build();
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        super.onDestroy();


        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
        }


        if (imageReader != null) {

            try {

                imageReader.close();

            } catch (Exception ignored) {
            }
        }


        if (virtualDisplay != null) {

            try {

                virtualDisplay.release();

            } catch (Exception ignored) {
            }
        }


        if (mediaProjection != null) {

            try {

                mediaProjection.stop();

            } catch (Exception ignored) {
            }
        }


        if (recognizer != null) {

            try {

                recognizer.close();

            } catch (Exception ignored) {
            }
        }


        if (overlayText != null) {

            try {

                windowManager.removeView(
                        overlayText
                );

            } catch (Exception ignored) {
            }

            overlayText = null;
        }
    }


    // =========================================================
    // BIND
    // =========================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
