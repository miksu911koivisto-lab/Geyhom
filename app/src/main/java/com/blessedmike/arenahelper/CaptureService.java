package com.blessedmike.arenahelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

private static final String CHANNEL_ID =
        "arena_helper_channel";

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

private static final long OCR_INTERVAL = 1500;

/*
 * ============================================================
 * KORTTI 1:N OCR-VAKAUTUS
 * ============================================================
 */

private String stableCard1 = "";

private String candidateCard1 = "";

private int candidateCard1Count = 0;

private static final int CARD1_CONFIRMATIONS = 2;

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
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            overlayType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        overlayType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE
                                |
                                WindowManager.LayoutParams
                                .FLAG_NOT_TOUCHABLE
                                |
                                WindowManager.LayoutParams
                                .FLAG_LAYOUT_NO_LIMITS,
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
                projectionManager
                        .getMediaProjection(
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

        /*
         * TÄRKEÄ:
         * callback rekisteröidään ennen
         * VirtualDisplayn luomista.
         */
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
                "SCREEN: " +
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
                        "Korttien haku käynnissä..."
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

        closeLatestImage(reader);
        return;
    }

    if (processing) {

        closeLatestImage(reader);
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

        processThreeCards(bitmap);

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

private void closeLatestImage(
        ImageReader reader
) {

    Image image = null;

    try {

        image =
                reader.acquireLatestImage();

    } catch (Exception ignored) {
    }

    if (image != null) {
        image.close();
    }
}

private void processThreeCards(
        Bitmap source
) {

    int width =
            source.getWidth();

    int height =
            source.getHeight();

    Bitmap card1 = null;
    Bitmap card2 = null;
    Bitmap card3 = null;

    try {

        int nameTop =
                (int) (
                        height * 0.45f
                );

        int nameBottom =
                (int) (
                        height * 0.55f
                );

        int nameHeight =
                nameBottom -
                        nameTop;

        /*
         * KORTTI 1
         */
        int card1Left =
                (int) (
                        width * 0.065f
                );

        int card1Right =
                (int) (
                        width * 0.38f
                );

        /*
         * KORTTI 2
         */
        int card2Left =
                (int) (
                        width * 0.355f
                );

        int card2Right =
                (int) (
                        width * 0.645f
                );

        /*
         * KORTTI 3
         */
        int card3Left =
                (int) (
                        width * 0.60f
                );

        int card3Right =
                (int) (
                        width * 0.935f
                );

        card1Left =
                Math.max(
                        0,
                        card1Left
                );

        card2Left =
                Math.max(
                        0,
                        card2Left
                );

        card3Left =
                Math.max(
                        0,
                        card3Left
                );

        card1Right =
                Math.min(
                        width,
                        card1Right
                );

        card2Right =
                Math.min(
                        width,
                        card2Right
                );

        card3Right =
                Math.min(
                        width,
                        card3Right
                );

        Log.d(
                TAG,
                "CARD 1 CROP: " +
                        card1Left +
                        "-" +
                        card1Right
        );

        Log.d(
                TAG,
                "CARD 2 CROP: " +
                        card2Left +
                        "-" +
                        card2Right
        );

        Log.d(
                TAG,
                "CARD 3 CROP: " +
                        card3Left +
                        "-" +
                        card3Right
        );

        card1 =
                Bitmap.createBitmap(
                        source,
                        card1Left,
                        nameTop,
                        card1Right -
                                card1Left,
                        nameHeight
                );

        card2 =
                Bitmap.createBitmap(
                        source,
                        card2Left,
                        nameTop,
                        card2Right -
                                card2Left,
                        nameHeight
                );

        card3 =
                Bitmap.createBitmap(
                        source,
                        card3Left,
                        nameTop,
                        card3Right -
                                card3Left,
                        nameHeight
                );

        Bitmap prepared1 =
                enlargeForOCR(card1);

        Bitmap prepared2 =
                enlargeForOCR(card2);

        Bitmap prepared3 =
                enlargeForOCR(card3);

        card1.recycle();
        card2.recycle();
        card3.recycle();

        card1 = null;
        card2 = null;
        card3 = null;

        if (!source.isRecycled()) {
            source.recycle();
        }

        runCardOCR(
                prepared1,
                prepared2,
                prepared3
        );

    } catch (Exception e) {

        Log.e(
                TAG,
                "Card splitting failed",
                e
        );

        if (card1 != null &&
                !card1.isRecycled()) {

            card1.recycle();
        }

        if (card2 != null &&
                !card2.isRecycled()) {

            card2.recycle();
        }

        if (card3 != null &&
                !card3.isRecycled()) {

            card3.recycle();
        }

        if (!source.isRecycled()) {
            source.recycle();
        }

        processing = false;

        updateOverlay(
                "Arena Helper\n\n" +
                        "Korttikuvan käsittelyvirhe"
        );
    }
}

private Bitmap enlargeForOCR(
        Bitmap source
) {

    int newWidth =
            source.getWidth() * 2;

    int newHeight =
            source.getHeight() * 2;

    Bitmap enlarged =
            Bitmap.createBitmap(
                    newWidth,
                    newHeight,
                    Bitmap.Config.ARGB_8888
            );

    Canvas canvas =
            new Canvas(enlarged);

    Paint paint =
            new Paint(
                    Paint.ANTI_ALIAS_FLAG
            );

    paint.setFilterBitmap(true);

    canvas.drawBitmap(
            source,
            null,
            new android.graphics.Rect(
                    0,
                    0,
                    newWidth,
                    newHeight
            ),
            paint
    );

    return enlarged;
}

private void runCardOCR(
        Bitmap card1,
        Bitmap card2,
        Bitmap card3
) {

    final String[] results =
            new String[3];

    recognizeNormalCard(
            card1,
            0,
            results
    );

    recognizeNormalCard(
            card2,
            1,
            results
    );

    recognizeNormalCard(
            card3,
            2,
            results
    );
}

private void recognizeNormalCard(
        Bitmap bitmap,
        int index,
        String[] results
) {

    try {

        InputImage inputImage =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );

        recognizer
                .process(inputImage)
                .addOnSuccessListener(
                        text -> {

                            String raw =
                                    text.getText();

                            if (raw == null) {
                                raw = "";
                            }

                            Log.d(
                                    TAG,
                                    "RAW OCR CARD " +
                                            (index + 1) +
                                            ": " +
                                            raw
                            );

                            String cleaned =
                                    cleanCardName(
                                            raw
                                    );

                            if (index == 0) {

                                results[0] =
                                        stabilizeCard1(
                                                cleaned
                                        );

                            } else {

                                results[index] =
                                        cleaned;
                            }

                            Log.d(
                                    TAG,
                                    "CLEAN OCR CARD " +
                                            (index + 1) +
                                            ": " +
                                            results[index]
                            );

                            if (!bitmap.isRecycled()) {
                                bitmap.recycle();
                            }

                            checkOCRFinished(
                                    results
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Log.e(
                                    TAG,
                                    "OCR failed CARD " +
                                            (index + 1),
                                    e
                            );

                            if (index == 0 &&
                                    !stableCard1.isEmpty()) {

                                results[0] =
                                        stableCard1;

                            } else {

                                results[index] =
                                        "";
                            }

                            if (!bitmap.isRecycled()) {
                                bitmap.recycle();
                            }

                            checkOCRFinished(
                                    results
                            );
                        }
                );

    } catch (Exception e) {

        Log.e(
                TAG,
                "OCR start failed CARD " +
                        (index + 1),
                e
        );

        if (index == 0 &&
                !stableCard1.isEmpty()) {

            results[0] =
                    stableCard1;

        } else {

            results[index] = "";
        }

        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }

        checkOCRFinished(
                results
        );
    }
}

/*
 * ============================================================
 * KORTTI 1:N VAKAUTUS
 * ============================================================
 */

private String stabilizeCard1(
        String detected
) {

    if (detected == null ||
            detected.isEmpty()) {

        return stableCard1;
    }

    String normalized =
            normalizeCard1ForComparison(
                    detected
            );

    Log.d(
            TAG,
            "CARD 1 STABILIZER INPUT: " +
                    detected
    );

    Log.d(
            TAG,
            "CARD 1 STABILIZER NORMALIZED: " +
                    normalized
    );

    if (!stableCard1.isEmpty() &&
            similarCard1(
                    stableCard1,
                    detected
            )) {

        candidateCard1 =
                detected;

        candidateCard1Count++;

        return stableCard1;
    }

    String soldierFix =
            fixSoldierOfInfinite(
                    detected
            );

    if (!soldierFix.isEmpty()) {

        Log.d(
                TAG,
                "CARD 1 SOLDIER FIX: " +
                        soldierFix
        );

        stableCard1 =
                soldierFix;

        candidateCard1 =
                soldierFix;

        candidateCard1Count =
                CARD1_CONFIRMATIONS;

        return stableCard1;
    }

    if (candidateCard1.isEmpty() ||
            !similarCard1(
                    candidateCard1,
                    detected
            )) {

        candidateCard1 =
                detected;

        candidateCard1Count = 1;

    } else {

        candidateCard1Count++;
    }

    Log.d(
            TAG,
            "CARD 1 CANDIDATE: " +
                    candidateCard1 +
                    " COUNT=" +
                    candidateCard1Count
    );

    if (candidateCard1Count >=
            CARD1_CONFIRMATIONS) {

        stableCard1 =
                candidateCard1;

        Log.d(
                TAG,
                "CARD 1 NEW STABLE: " +
                        stableCard1
        );
    }

    if (!stableCard1.isEmpty()) {

        return stableCard1;
    }

    return detected;
}

private String normalizeCard1ForComparison(
        String text
) {

    if (text == null) {
        return "";
    }

    text =
            text.toLowerCase();

    text =
            text.replaceAll(
                    "[^a-z0-9]",
                    ""
            );

    return text;
}

private boolean similarCard1(
        String a,
        String b
) {

    if (a == null ||
            b == null) {

        return false;
    }

    String aa =
            normalizeCard1ForComparison(
                    a
            );

    String bb =
            normalizeCard1ForComparison(
                    b
            );

    if (aa.isEmpty() ||
            bb.isEmpty()) {

        return false;
    }

    if (aa.equals(bb)) {
        return true;
    }

    if (aa.contains(bb) ||
            bb.contains(aa)) {

        return true;
    }

    int distance =
            levenshteinDistance(
                    aa,
                    bb
            );

    int maxLength =
            Math.max(
                    aa.length(),
                    bb.length()
            );

    return maxLength > 0 &&
            distance <=
                    Math.max(
                            2,
                            maxLength / 5
                    );
}

private int levenshteinDistance(
        String a,
        String b
) {

    int[] previous =
            new int[b.length() + 1];

    int[] current =
            new int[b.length() + 1];

    for (int j = 0;
         j <= b.length();
         j++) {

        previous[j] = j;
    }

    for (int i = 1;
         i <= a.length();
         i++) {

        current[0] = i;

        for (int j = 1;
             j <= b.length();
             j++) {

            int cost =
                    a.charAt(i - 1) ==
                            b.charAt(j - 1)
                            ? 0
                            : 1;

            current[j] =
                    Math.min(
                            Math.min(
                                    current[j - 1] + 1,
                                    previous[j] + 1
                            ),
                            previous[j - 1] + cost
                    );
        }

        int[] temp =
                previous;

        previous =
                current;

        current =
                temp;
    }

    return previous[b.length()];
}

/*
 * ============================================================
 * SOLDIER OF THE INFINITE -KORJAUS
 * ============================================================
 */

private String fixSoldierOfInfinite(
        String text
) {

    if (text == null) {
        return "";
    }

    String normalized =
            text.toLowerCase();

    normalized =
            normalized.replaceAll(
                    "[^a-z0-9]",
                    ""
            );

    if (normalized.startsWith("soldier")) {

        if (normalized.contains("infinite")) {

            return "Soldier of the Infinite";
        }

        if (normalized.equals("soldiero") ||
                normalized.equals("soldier0") ||
                normalized.equals("soldier")) {

            if (!stableCard1.isEmpty()) {
                return stableCard1;
            }

            return "";
        }
    }

    return "";
}

/*
 * ============================================================
 * OCR-TULOKSEN PUHDISTUS
 * ============================================================
 */

private String cleanCardName(
        String text
) {

    if (text == null) {
        return "";
    }

    text =
            text.trim();

    if (text.isEmpty()) {
        return "";
    }

    String[] lines =
            text.split("\\r?\\n");

    String bestLine = "";

    for (String line : lines) {

        line =
                line.trim();

        if (line.length() < 2) {
            continue;
        }

        line =
                line.replaceAll(
                        "^[^A-Za-zÅÄÖåäö0-9]+",
                        ""
                );

        line =
                line.replaceAll(
                        "[^A-Za-zÅÄÖåäö0-9'\\- ]+$",
                        ""
                );

        line =
                line.trim();

        int letters = 0;

        for (int i = 0;
             i < line.length();
             i++) {

            if (Character.isLetter(
                    line.charAt(i)
            )) {

                letters++;
            }
        }

        if (letters >= 2) {

            bestLine =
                    line;

            break;
        }
    }

    if (bestLine.isEmpty()) {
        return "";
    }

    bestLine =
            bestLine.replaceAll(
                    "(?i)\\bsoldierof\\b",
                    "Soldier of"
            );

    bestLine =
            bestLine.replaceAll(
                    "(?i)\\bsoldier\\s*2\\s*of\\b",
                    "Soldier of"
            );

    bestLine =
            bestLine.replaceAll(
                    "(?i)\\bsoldier\\s*2\\s*0f\\b",
                    "Soldier of"
            );

    bestLine =
            bestLine.replaceAll(
                    "\\s+",
                    " "
            ).trim();

    bestLine =
            capitalizeFirstLetter(
                    bestLine
            );

    return bestLine;
}

private String capitalizeFirstLetter(
        String text
) {

    if (text == null ||
            text.isEmpty()) {

        return text;
    }

    char[] chars =
            text.toCharArray();

    for (int i = 0;
         i < chars.length;
         i++) {

        if (Character.isLetter(
                chars[i]
        )) {

            chars[i] =
                    Character.toUpperCase(
                            chars[i]
                    );

            break;
        }
    }

    return new String(chars);
}

private void checkOCRFinished(
        String[] results
) {

    if (results[0] == null ||
            results[1] == null ||
            results[2] == null) {

        return;
    }

    showThreeCards(results);

    processing = false;
}

/*
 * ============================================================
 * KORTIT + ARENAADVISOR
 * ============================================================
 */

private void showThreeCards(
        String[] results
) {

    String card1 =
            results[0] == null ||
                    results[0].isEmpty()
                    ? "Ei tunnistettu"
                    : results[0];

    String card2 =
            results[1] == null ||
                    results[1].isEmpty()
                    ? "Ei tunnistettu"
                    : results[1];

    String card3 =
            results[2] == null ||
                    results[2].isEmpty()
                    ? "Ei tunnistettu"
                    : results[2];

    /*
     * Lähetetään OCR:n tunnistamat nimet
     * ArenaAdvisorille.
     */
    String recommendation;

    try {

        recommendation =
                ArenaAdvisor.recommend(
                        card1,
                        card2,
                        card3
                );

    } catch (Exception e) {

        Log.e(
                TAG,
                "ArenaAdvisor error",
                e
        );

        recommendation =
                "Suositusta ei voitu laskea";
    }

    if (recommendation == null ||
            recommendation.trim().isEmpty()) {

        recommendation =
                "Ei suositusta";
    }

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
                    "--------------------" +
                    "\n\n" +
                    "SUOSITUS:\n" +
                    recommendation;

    updateOverlay(display);

    Log.d(
            TAG,
            "CARD 1: " + card1
    );

    Log.d(
            TAG,
            "CARD 2: " + card2
    );

    Log.d(
            TAG,
            "CARD 3: " + card3
    );

    Log.d(
            TAG,
            "ARENA ADVISOR: " +
                    recommendation
    );
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
