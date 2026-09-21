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

import java.nio.ByteBuffer;
import java.util.Locale;

public class CaptureService extends Service {

    private static final String CHANNEL_ID = "arena_helper_channel";
    private static final int OCR_INTERVAL = 1500;
    private static final int CARD_CONFIRMATIONS = 2;
    private static final int OFFER_CONFIRMATIONS = 2;

    private WindowManager windowManager;
    private TextView overlayView;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private TextRecognizer recognizer;

    private boolean processing = false;

    private String confirmedCard1 = "";
    private String confirmedCard2 = "";
    private String confirmedCard3 = "";

    private String pendingOffer1 = "";
    private String pendingOffer2 = "";
    private String pendingOffer3 = "";
    private int pendingOfferCount = 0;

    private String lastRecordedPick = "";

    private static int projectionResultCode;
    private static Intent projectionData;

    public static void setProjectionData(
            int resultCode,
            Intent data
    ) {
        projectionResultCode = resultCode;
        projectionData = data;
    }

    private static class CardStability {
        String stable = "";
        String candidate = "";
        int candidateCount = 0;
    }

    private final CardStability card1Stability =
            new CardStability();

    private final CardStability card2Stability =
            new CardStability();

    private final CardStability card3Stability =
            new CardStability();

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle("Arena Helper")
                        .setContentText("Avustaja aktiivinen")
                        .setSmallIcon(
                                android.R.drawable
                                        .ic_menu_info_details
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                1,
                notification
        );

        recognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );

        createOverlay();

        startCapture();
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

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            if (manager != null) {
                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    private void createOverlay() {

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        overlayView =
                new TextView(this);

        overlayView.setText(
                "ARENA HELPER\n\n" +
                "Kortteja luetaan..."
        );

        overlayView.setTextSize(13);
        overlayView.setTextColor(0xFFFFFFFF);
        overlayView.setBackgroundColor(0xCC000000);
        overlayView.setPadding(
                18,
                18,
                18,
                18
        );

        WindowManager.LayoutParams params;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            params =
                    new WindowManager.LayoutParams(
                            WindowManager.LayoutParams
                                    .WRAP_CONTENT,
                            WindowManager.LayoutParams
                                    .WRAP_CONTENT,
                            WindowManager.LayoutParams
                                    .TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams
                                    .FLAG_NOT_FOCUSABLE
                                    |
                                    WindowManager.LayoutParams
                                            .FLAG_NOT_TOUCHABLE,
                            PixelFormat.TRANSLUCENT
                    );

        } else {

            params =
                    new WindowManager.LayoutParams(
                            WindowManager.LayoutParams
                                    .WRAP_CONTENT,
                            WindowManager.LayoutParams
                                    .WRAP_CONTENT,
                            WindowManager.LayoutParams
                                    .TYPE_PHONE,
                            WindowManager.LayoutParams
                                    .FLAG_NOT_FOCUSABLE
                                    |
                                    WindowManager.LayoutParams
                                            .FLAG_NOT_TOUCHABLE,
                            PixelFormat.TRANSLUCENT
                    );
        }

        params.gravity =
                Gravity.TOP | Gravity.LEFT;

        params.x = 20;
        params.y = 100;

        if (windowManager != null) {

            windowManager.addView(
                    overlayView,
                    params
            );
        }
    }

    private void startCapture() {

        if (projectionData == null) {
            return;
        }

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        mediaProjection =
                manager.getMediaProjection(
                        projectionResultCode,
                        projectionData
                );

        if (mediaProjection == null) {
            return;
        }

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

        imageReader.setOnImageAvailableListener(
                reader ->
                        processLatestImage(reader),
                handler
        );
    }

    private void processLatestImage(
            ImageReader reader
    ) {

        if (processing) {

            Image image =
                    reader.acquireLatestImage();

            if (image != null) {
                image.close();
            }

            return;
        }

        Image image =
                reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        processing = true;

        try {

            Bitmap bitmap =
                    imageToBitmap(image);

            image.close();

            if (bitmap == null) {

                processing = false;
                return;
            }

            runOCR(bitmap);

        } catch (Exception e) {

            try {
                image.close();
            } catch (Exception ignored) {
            }

            processing = false;
        }
    }

    private Bitmap imageToBitmap(
            Image image
    ) {

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

        int width =
                image.getWidth();

        int height =
                image.getHeight();

        int rowPadding =
                rowStride -
                        pixelStride * width;

        Bitmap bitmap =
                Bitmap.createBitmap(
                        width +
                                rowPadding /
                                        pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                );

        buffer.rewind();

        bitmap.copyPixelsFromBuffer(
                buffer
        );

        if (bitmap.getWidth() != width) {

            Bitmap cropped =
                    Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            width,
                            height
                    );

            bitmap.recycle();

            return cropped;
        }

        return bitmap;
    }

    private void runOCR(
            Bitmap source
    ) {

        int width =
                source.getWidth();

        int height =
                source.getHeight();

        int nameTop =
                (int)
                        (height * 0.45f);

        int nameBottom =
                (int)
                        (height * 0.55f);

        Bitmap card1 =
                cropCard(
                        source,
                        (int)
                                (width * 0.065f),
                        (int)
                                (width * 0.38f),
                        nameTop,
                        nameBottom
                );

        Bitmap card2 =
                cropCard(
                        source,
                        (int)
                                (width * 0.355f),
                        (int)
                                (width * 0.645f),
                        nameTop,
                        nameBottom
                );

        Bitmap card3 =
                cropCard(
                        source,
                        (int)
                                (width * 0.60f),
                        (int)
                                (width * 0.935f),
                        nameTop,
                        nameBottom
                );

        source.recycle();

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

    private Bitmap cropCard(
            Bitmap source,
            int left,
            int right,
            int top,
            int bottom
    ) {

        left = Math.max(0, left);
        top = Math.max(0, top);

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

        if (right <= left ||
                bottom <= top) {
            return null;
        }

        Bitmap cropped =
                Bitmap.createBitmap(
                        source,
                        left,
                        top,
                        right - left,
                        bottom - top
                );

        return enlargeForOCR(cropped);
    }

    private Bitmap enlargeForOCR(
            Bitmap bitmap
    ) {

        if (bitmap == null) {
            return null;
        }

        Bitmap enlarged =
                Bitmap.createScaledBitmap(
                        bitmap,
                        bitmap.getWidth() * 2,
                        bitmap.getHeight() * 2,
                        true
                );

        bitmap.recycle();

        return enlarged;
    }

    private void recognizeNormalCard(
            Bitmap bitmap,
            int index,
            String[] results
    ) {

        if (bitmap == null) {

            results[index] =
                    getStableCard(index);

            checkOCRFinished(results);

            return;
        }

        InputImage image =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );

        recognizer.process(image)
                .addOnSuccessListener(text -> {

                    String cleaned =
                            cleanCardName(text);

                    results[index] =
                            stabilizeCard(
                                    cleaned,
                                    index
                            );

                    bitmap.recycle();

                    checkOCRFinished(results);

                })
                .addOnFailureListener(e -> {

                    results[index] =
                            getStableCard(index);

                    bitmap.recycle();

                    checkOCRFinished(results);
                });
    }

    private void checkOCRFinished(
            String[] results
    ) {

        if (results[0] == null ||
                results[1] == null ||
                results[2] == null) {
            return;
        }

        updateCards(
                results[0],
                results[1],
                results[2]
        );

        processing = false;
    }

    private String getStableCard(
            int index
    ) {

        if (index == 0) {
            return card1Stability.stable;
        }

        if (index == 1) {
            return card2Stability.stable;
        }

        return card3Stability.stable;
    }

    private String stabilizeCard(
            String detected,
            int index
    ) {

        if (detected == null ||
                detected.trim().isEmpty()) {

            return getStableCard(index);
        }

        String normalized =
                normalizeDetectedCardName(
                        detected
                );

        if (normalized.isEmpty()) {
            return getStableCard(index);
        }

        CardStability stability;

        if (index == 0) {

            stability =
                    card1Stability;

        } else if (index == 1) {

            stability =
                    card2Stability;

        } else {

            stability =
                    card3Stability;
        }

        if (stability.stable.isEmpty()) {

            if (stability.candidate.isEmpty() ||
                    !similarNames(
                            stability.candidate,
                            normalized
                    )) {

                stability.candidate =
                        normalized;

                stability.candidateCount = 1;

            } else {

                stability.candidateCount++;
            }

            if (stability.candidateCount >=
                    CARD_CONFIRMATIONS) {

                stability.stable =
                        stability.candidate;

                stability.candidate = "";
                stability.candidateCount = 0;
            }

            return stability.stable.isEmpty()
                    ? normalized
                    : stability.stable;
        }

        if (similarNames(
                stability.stable,
                normalized
        )) {

            stability.stable =
                    normalized;

            stability.candidate = "";
            stability.candidateCount = 0;

            return stability.stable;
        }

        if (stability.candidate.isEmpty() ||
                !similarNames(
                        stability.candidate,
                        normalized
                )) {

            stability.candidate =
                    normalized;

            stability.candidateCount = 1;

        } else {

            stability.candidateCount++;
        }

        if (stability.candidateCount >=
                CARD_CONFIRMATIONS) {

            stability.stable =
                    stability.candidate;

            stability.candidate = "";
            stability.candidateCount = 0;
        }

        return stability.stable;
    }

    private String normalizeDetectedCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                text.trim();

        text =
                text.replace(
                        "&#039;",
                        "'"
                );

        text =
                text.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        text =
                fixSoldierOfInfinite(
                        text
                );

        try {

            String corrected =
                    ArenaAdvisor.correctOcr(
                            text
                    );

            if (corrected != null &&
                    !corrected.trim().isEmpty()) {

                text =
                        corrected.trim();
            }

        } catch (Exception ignored) {
        }

        text =
                text.replace(
                        "&#039;",
                        "'"
                );

        text =
                text.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        return text.trim();
    }

    private String cleanCardName(
            Text text
    ) {

        if (text == null) {
            return "";
        }

        String raw =
                text.getText();

        if (raw == null) {
            return "";
        }

        String[] lines =
                raw.split("\\r?\\n");

        String best = "";

        for (String line : lines) {

            if (line == null) {
                continue;
            }

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

                best =
                        line;

                break;
            }
        }

        if (best.isEmpty()) {
            best = raw.trim();
        }

        best =
                best.replaceAll(
                        "^[^A-Za-zÀ-ÿ0-9]+",
                        ""
                )
                .replaceAll(
                        "[^A-Za-zÀ-ÿ0-9'&\\-\\.\\s]+$",
                        ""
                )
                .trim();

        best =
                fixSoldierOfInfinite(
                        best
                );

        best =
                best.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        best =
                best.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        best =
                best.replace(
                        "&#039;",
                        "'"
                );

        if (!best.isEmpty()) {

            best =
                    Character.toUpperCase(
                            best.charAt(0)
                    )
                    + best.substring(1);
        }

        return best;
    }

    private String fixSoldierOfInfinite(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                text.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (normalized.contains(
                "soldierofinfinite"
        )
                || normalized.contains(
                "so1dierofinfinite"
        )
                || normalized.contains(
                "sotdierofinfinite"
        )
                || normalized.contains(
                "soldierofihfinite"
        )
                || normalized.contains(
                "soldierofihfini"
        )) {

            return "Soldier of the Infinite";
        }

        String stable =
                card1Stability.stable;

        if (stable != null &&
                !stable.isEmpty()) {

            String stableNormalized =
                    stable.toLowerCase(
                                    Locale.US
                            )
                            .replaceAll(
                                    "[^a-z0-9]",
                                    ""
                            );

            if (stableNormalized.contains(
                    "soldierofinfinite"
            )) {

                return "Soldier of the Infinite";
            }
        }

        return text;
    }

    private boolean similarNames(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {
            return false;
        }

        String aa =
                a.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        String bb =
                b.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (aa.equals(bb)) {
            return true;
        }

        if (aa.contains(bb) ||
                bb.contains(aa)) {
            return true;
        }

        int distance =
                levenshtein(
                        aa,
                        bb
                );

        int maxLength =
                Math.max(
                        aa.length(),
                        bb.length()
                );

        return distance <=
                Math.max(
                        2,
                        maxLength / 5
                );
    }

    private int levenshtein(
            String a,
            String b
    ) {

        int[][] dp =
                new int[a.length() + 1]
                        [b.length() + 1];

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

    private void updateCards(
            String card1,
            String card2,
            String card3
    ) {

        if (card1 == null) {
            card1 = "";
        }

        if (card2 == null) {
            card2 = "";
        }

        if (card3 == null) {
            card3 = "";
        }

        if (!card1.isEmpty() &&
                !card2.isEmpty() &&
                !card3.isEmpty()) {

            handleOffer(
                    card1,
                    card2,
                    card3
            );
        }

        updateOverlay(
                card1,
                card2,
                card3
        );
    }

    private void handleOffer(
            String card1,
            String card2,
            String card3
    ) {

        if (pendingOffer1.isEmpty()) {

            pendingOffer1 = card1;
            pendingOffer2 = card2;
            pendingOffer3 = card3;
            pendingOfferCount = 1;

            return;
        }

        boolean sameOffer =
                similarNames(
                        pendingOffer1,
                        card1
                )
                        &&
                similarNames(
                        pendingOffer2,
                        card2
                )
                        &&
                similarNames(
                        pendingOffer3,
                        card3
                );

        if (sameOffer) {

            pendingOfferCount++;

        } else {

            pendingOffer1 = card1;
            pendingOffer2 = card2;
            pendingOffer3 = card3;
            pendingOfferCount = 1;
        }

        if (pendingOfferCount <
                OFFER_CONFIRMATIONS) {
            return;
        }

        String new1 = card1;
        String new2 = card2;
        String new3 = card3;

        if (!confirmedCard1.isEmpty() &&
                !confirmedCard2.isEmpty() &&
                !confirmedCard3.isEmpty()) {

            String missing =
                    findMissingPickedCard(
                            confirmedCard1,
                            confirmedCard2,
                            confirmedCard3,
                            new1,
                            new2,
                            new3
                    );

            if (missing != null &&
                    !missing.isEmpty() &&
                    !missing.equals(
                            lastRecordedPick
                    )) {

                ArenaAdvisor.recordPickedCard(
                        missing
                );

                lastRecordedPick =
                        missing;
            }
        }

        confirmedCard1 = new1;
        confirmedCard2 = new2;
        confirmedCard3 = new3;
    }

    private String findMissingPickedCard(
            String old1,
            String old2,
            String old3,
            String new1,
            String new2,
            String new3
    ) {

        boolean old1Exists =
                similarNames(
                        old1,
                        new1
                )
                        ||
                similarNames(
                        old1,
                        new2
                )
                        ||
                similarNames(
                        old1,
                        new3
                );

        boolean old2Exists =
                similarNames(
                        old2,
                        new1
                )
                        ||
                similarNames(
                        old2,
                        new2
                )
                        ||
                similarNames(
                        old2,
                        new3
                );

        boolean old3Exists =
                similarNames(
                        old3,
                        new1
                )
                        ||
                similarNames(
                        old3,
                        new2
                )
                        ||
                similarNames(
                        old3,
                        new3
                );

        int missingCount = 0;
        String missing = "";

        if (!old1Exists) {
            missingCount++;
            missing = old1;
        }

        if (!old2Exists) {
            missingCount++;
            missing = old2;
        }

        if (!old3Exists) {
            missingCount++;
            missing = old3;
        }

        if (missingCount == 1) {
            return missing;
        }

        return "";
    }

    private void updateOverlay(
            String card1,
            String card2,
            String card3
    ) {

        if (overlayView == null) {
            return;
        }

        final String score1 =
                ArenaAdvisor.getCardScore(
                        card1
                );

        final String score2 =
                ArenaAdvisor.getCardScore(
                        card2
                );

        final String score3 =
                ArenaAdvisor.getCardScore(
                        card3
                );

        final String recommendation =
                ArenaAdvisor.recommend(
                        card1,
                        card2,
                        card3
                );

        String display =
                "KORTTI 1\n" +
                card1 +
                "\nARVO: " +
                score1 +
                "\n\n" +

                "KORTTI 2\n" +
                card2 +
                "\nARVO: " +
                score2 +
                "\n\n" +

                "KORTTI 3\n" +
                card3 +
                "\nARVO: " +
                score3 +
                "\n\n" +

                "SUOSITUS\n" +
                recommendation;

        overlayView.setText(
                display
        );
    }

    @Override
    public void onDestroy() {

        processing = false;

        if (imageReader != null) {

            try {
                imageReader.close();
            } catch (Exception ignored) {
            }

            imageReader = null;
        }

        if (virtualDisplay != null) {

            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }

            virtualDisplay = null;
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

        if (overlayView != null &&
                windowManager != null) {

            try {
                windowManager.removeView(
                        overlayView
                );
            } catch (Exception ignored) {
            }

            overlayView = null;
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
