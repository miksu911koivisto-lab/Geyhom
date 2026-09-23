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
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    private static final String TAG =
            "ArenaHelperCapture";

    private static final String CHANNEL_ID =
            "arena_helper_channel";

    private static final int CARD_CONFIRMATIONS = 2;

    private static final int OFFER_CONFIRMATIONS = 2;

    private static final long PICK_COOLDOWN_MS =
            1200L;

    private static CaptureService activeInstance;

    private WindowManager windowManager;
    private TextView overlayView;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private TextRecognizer recognizer;

    private boolean processing = false;

    private boolean hearthstoneActive = false;

    private String pendingOffer1 = "";
    private String pendingOffer2 = "";
    private String pendingOffer3 = "";
    private int pendingOfferCount = 0;

    private String activeOffer1 = "";
    private String activeOffer2 = "";
    private String activeOffer3 = "";

    private boolean pickAlreadyRecordedForOffer =
            false;

    private long lastPickTime = 0L;

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

    public static void setHearthstoneActive(
            boolean active
    ) {

        CaptureService service =
                activeInstance;

        if (service == null) {
            return;
        }

        service.updateHearthstoneActive(
                active
        );
    }

    private void updateHearthstoneActive(
            boolean active
    ) {

        handler.post(() -> {

            hearthstoneActive = active;

            if (overlayView == null) {
                return;
            }

            if (hearthstoneActive) {

                overlayView.setVisibility(
                        View.VISIBLE
                );

                Log.d(
                        TAG,
                        "Hearthstone aktiivinen - overlay näkyviin"
                );

            } else {

                overlayView.setVisibility(
                        View.GONE
                );

                Log.d(
                        TAG,
                        "Hearthstone ei aktiivinen - overlay piiloon"
                );
            }
        });
    }

    public static void onAccessibilityClick(
            int left,
            int top,
            int right,
            int bottom,
            String text
    ) {

        CaptureService service =
                activeInstance;

        if (service == null) {
            return;
        }

        service.handleAccessibilityClick(
                left,
                top,
                right,
                bottom,
                text
        );
    }

    private void handleAccessibilityClick(
            int left,
            int top,
            int right,
            int bottom,
            String accessibilityText
    ) {

        if (pickAlreadyRecordedForOffer) {
            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastPickTime <
                PICK_COOLDOWN_MS) {

            return;
        }

        if (activeOffer1.isEmpty() ||
                activeOffer2.isEmpty() ||
                activeOffer3.isEmpty()) {

            return;
        }

        int centerX =
                left +
                        ((right - left) / 2);

        int centerY =
                top +
                        ((bottom - top) / 2);

        String clickedCard =
                findCardFromClick(
                        centerX,
                        centerY,
                        accessibilityText
                );

        if (clickedCard == null ||
                clickedCard.isEmpty()) {

            return;
        }

        recordConfirmedPick(
                clickedCard
        );
    }

    private String findCardFromClick(
            int centerX,
            int centerY,
            String accessibilityText
    ) {

        if (accessibilityText != null &&
                !accessibilityText.trim().isEmpty()) {

            String text =
                    normalizeDetectedCardName(
                            accessibilityText
                    );

            if (!text.isEmpty()) {

                if (similarNames(
                        text,
                        activeOffer1
                )) {

                    return activeOffer1;
                }

                if (similarNames(
                        text,
                        activeOffer2
                )) {

                    return activeOffer2;
                }

                if (similarNames(
                        text,
                        activeOffer3
                )) {

                    return activeOffer3;
                }
            }
        }

        DisplayMetrics metrics =
                getResources()
                        .getDisplayMetrics();

        int width =
                metrics.widthPixels;

        if (width <= 0) {
            return "";
        }

        float x =
                centerX /
                        (float) width;

        if (x >= 0.065f &&
                x < 0.355f) {

            return activeOffer1;
        }

        if (x >= 0.355f &&
                x < 0.60f) {

            return activeOffer2;
        }

        if (x >= 0.60f &&
                x <= 0.935f) {

            return activeOffer3;
        }

        return "";
    }

    private void recordConfirmedPick(
            String pickedCard
    ) {

        if (pickedCard == null ||
                pickedCard.trim().isEmpty()) {

            return;
        }

        pickedCard =
                normalizeDetectedCardName(
                        pickedCard
                );

        if (pickedCard.isEmpty()) {
            return;
        }

        if (pickAlreadyRecordedForOffer) {
            return;
        }

        try {

            ArenaAdvisor.recordPickedCard(
                    pickedCard
            );

            lastRecordedPick =
                    pickedCard;

            lastPickTime =
                    System.currentTimeMillis();

            pickAlreadyRecordedForOffer =
                    true;

            Log.d(
                    TAG,
                    "Arena-valinta tallennettu: "
                            + pickedCard
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Valitun kortin tallennus epäonnistui",
                    e
            );
        }
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

    private final MediaProjection.Callback
            mediaProjectionCallback =
            new MediaProjection.Callback() {

                @Override
                public void onStop() {

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
            };

    @Override
    public void onCreate() {

        super.onCreate();

        activeInstance = this;

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "Arena Helper"
                        )
                        .setContentText(
                                "Avustaja aktiivinen"
                        )
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
                        TextRecognizerOptions
                                .DEFAULT_OPTIONS
                );

        createOverlay();

        hearthstoneActive = false;

        if (overlayView != null) {

            overlayView.setVisibility(
                    View.GONE
            );
        }

        startCapture();
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

        overlayView.setTypeface(
                Typeface.create(
                        "sans-serif-condensed",
                        Typeface.NORMAL
                )
        );

        overlayView.setTextSize(13);

        overlayView.setTextColor(
                0xFFFFFFFF
        );

        overlayView.setBackgroundColor(
                0xCC000000
        );

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

        mediaProjection.registerCallback(
                mediaProjectionCallback,
                handler
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
                reader ->
                        processLatestImage(reader),
                handler
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

            Log.e(
                    TAG,
                    "Kuvan käsittely epäonnistui",
                    e
            );
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

        /*
         * ============================================================
         * KORTTIEN 1 JA 2 OCR-ALUE
         * ============================================================
         *
         * Aiempi alue:
         *
         * 42.5 % - 50.5 %
         *
         * Se oli vain 8 % näytön korkeudesta.
         *
         * Nyt korttien 1 ja 2 alue on hieman korkeampi:
         *
         * 39 % - 53 %
         *
         * Tämä antaa ML Kitille enemmän mahdollisuuksia nähdä
         * koko kortin nimi.
         *
         * Kortti 3 pidetään alkuperäisellä alueella, koska sen
         * tunnistus toimii jo paremmin.
         */
        int normalNameTop =
                (int)
                        (height * 0.390f);

        int normalNameBottom =
                (int)
                        (height * 0.530f);

        int card3NameTop =
                (int)
                        (height * 0.425f);

        int card3NameBottom =
                (int)
                        (height * 0.505f);

        Bitmap card1 =
                cropCard(
                        source,
                        (int)
                                (width * 0.055f),
                        (int)
                                (width * 0.39f),
                        normalNameTop,
                        normalNameBottom
                );

        Bitmap card2 =
                cropCard(
                        source,
                        (int)
                                (width * 0.345f),
                        (int)
                                (width * 0.61f),
                        normalNameTop,
                        normalNameBottom
                );

        Bitmap card3 =
                cropCard(
                        source,
                        (int)
                                (width * 0.60f),
                        (int)
                                (width * 0.935f),
                        card3NameTop,
                        card3NameBottom
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

        return enlargeForOCR(
                cropped
        );
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

            checkOCRFinished(
                    results
            );

            return;
        }

        InputImage image =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );

        recognizer.process(image)
                .addOnSuccessListener(text -> {

                    String cleaned;

                    if (index == 2) {

                        cleaned =
                                cleanCard3Name(text);

                    } else {

                        cleaned =
                                cleanCardName(text);
                    }

                    if (index == 2) {

                        results[index] =
                                stabilizeCard3(
                                        cleaned
                                );

                    } else {

                        results[index] =
                                stabilizeCard(
                                        cleaned,
                                        index
                                );
                    }

                    bitmap.recycle();

                    checkOCRFinished(
                            results
                    );
                })
                .addOnFailureListener(e -> {

                    results[index] =
                            getStableCard(index);

                    bitmap.recycle();

                    checkOCRFinished(
                            results
                    );
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

    private CardStability getCardStability(
            int index
    ) {

        if (index == 0) {
            return card1Stability;
        }

        if (index == 1) {
            return card2Stability;
        }

        return card3Stability;
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

        CardStability stability =
                getCardStability(index);

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

                if (isLongerVersion(
                        normalized,
                        stability.candidate
                )) {

                    stability.candidate =
                            normalized;
                }

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

            if (isLongerVersion(
                    normalized,
                    stability.stable
            )) {

                stability.stable =
                        normalized;
            }

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

            if (isLongerVersion(
                    normalized,
                    stability.candidate
            )) {

                stability.candidate =
                        normalized;
            }

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

    private String stabilizeCard3(
            String detected
    ) {

        if (detected == null ||
                detected.trim().isEmpty()) {

            return card3Stability.stable;
        }

        String normalized =
                normalizeDetectedCardName(
                        detected
                );

        if (normalized.isEmpty()) {
            return card3Stability.stable;
        }

        if (!card3Stability.stable.isEmpty() &&
                isPartialOfStableCard3(
                        normalized,
                        card3Stability.stable
                )) {

            card3Stability.candidate = "";
            card3Stability.candidateCount = 0;

            return card3Stability.stable;
        }

        if (!card3Stability.stable.isEmpty() &&
                similarNames(
                        card3Stability.stable,
                        normalized
                )) {

            if (isLongerVersion(
                    normalized,
                    card3Stability.stable
            )) {

                card3Stability.stable =
                        normalized;
            }

            card3Stability.candidate = "";
            card3Stability.candidateCount = 0;

            return card3Stability.stable;
        }

        if (card3Stability.stable.isEmpty()) {

            if (card3Stability.candidate.isEmpty() ||
                    !similarNames(
                            card3Stability.candidate,
                            normalized
                    )) {

                card3Stability.candidate =
                        normalized;

                card3Stability.candidateCount = 1;

            } else {

                if (isLongerVersion(
                        normalized,
                        card3Stability.candidate
                )) {

                    card3Stability.candidate =
                            normalized;
                }

                card3Stability.candidateCount++;
            }

            if (card3Stability.candidateCount >=
                    CARD_CONFIRMATIONS) {

                card3Stability.stable =
                        card3Stability.candidate;

                card3Stability.candidate = "";
                card3Stability.candidateCount = 0;
            }

            return card3Stability.stable.isEmpty()
                    ? normalized
                    : card3Stability.stable;
        }

        if (card3Stability.candidate.isEmpty() ||
                !similarNames(
                        card3Stability.candidate,
                        normalized
                )) {

            card3Stability.candidate =
                    normalized;

            card3Stability.candidateCount = 1;

        } else {

            if (isLongerVersion(
                    normalized,
                    card3Stability.candidate
            )) {

                card3Stability.candidate =
                        normalized;
            }

            card3Stability.candidateCount++;
        }

        if (card3Stability.candidateCount >=
                CARD_CONFIRMATIONS) {

            card3Stability.stable =
                    card3Stability.candidate;

            card3Stability.candidate = "";
            card3Stability.candidateCount = 0;
        }

        return card3Stability.stable;
    }

    private boolean isPartialOfStableCard3(
            String detected,
            String stable
    ) {

        if (detected == null ||
                stable == null) {

            return false;
        }

        String detectedNormalized =
                detected.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        String stableNormalized =
                stable.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (detectedNormalized.isEmpty() ||
                stableNormalized.isEmpty()) {

            return false;
        }

        if (detectedNormalized.equals(
                stableNormalized
        )) {

            return false;
        }

        if (detectedNormalized.length() >=
                stableNormalized.length()) {

            return false;
        }

        if (stableNormalized.contains(
                detectedNormalized
        )) {

            return true;
        }

        String[] detectedWords =
                detected.toLowerCase(Locale.US)
                        .replaceAll(
                                "[^a-z0-9' ]",
                                " "
                        )
                        .trim()
                        .split("\\s+");

        String[] stableWords =
                stable.toLowerCase(Locale.US)
                        .replaceAll(
                                "[^a-z0-9' ]",
                                " "
                        )
                        .trim()
                        .split("\\s+");

        if (detectedWords.length < 2 ||
                stableWords.length < 2) {

            return false;
        }

        int longestSequence = 0;

        for (int i = 0;
             i < detectedWords.length;
             i++) {

            for (int j = 0;
                 j < stableWords.length;
                 j++) {

                int sequence = 0;

                while (
                        i + sequence <
                                detectedWords.length
                                &&
                        j + sequence <
                                stableWords.length
                        &&
                        sameCard3Word(
                                detectedWords[
                                        i + sequence
                                ],
                                stableWords[
                                        j + sequence
                                ]
                        )
                ) {

                    sequence++;
                }

                if (sequence >
                        longestSequence) {

                    longestSequence =
                            sequence;
                }
            }
        }

        if (longestSequence >= 3) {
            return true;
        }

        if (longestSequence >= 2 &&
                detectedWords.length <= 3) {

            return true;
        }

        return false;
    }

    private boolean sameCard3Word(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }

        a =
                a.toLowerCase(
                        Locale.US
                );

        b =
                b.toLowerCase(
                        Locale.US
                );

        if (a.equals(b)) {
            return true;
        }

        if (a.length() > 3 &&
                b.length() > 3) {

            if (a.endsWith("s") &&
                    a.substring(
                            0,
                            a.length() - 1
                    ).equals(b)) {

                return true;
            }

            if (b.endsWith("s") &&
                    b.substring(
                            0,
                            b.length() - 1
                    ).equals(a)) {

                return true;
            }
        }

        return false;
    }

    private boolean isLongerVersion(
            String longer,
            String shorter
    ) {

        if (longer == null ||
                shorter == null) {

            return false;
        }

        String longNormalized =
                longer.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        String shortNormalized =
                shorter.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (longNormalized.length() <=
                shortNormalized.length()) {

            return false;
        }

        return longNormalized.contains(
                shortNormalized
        );
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

    /*
     * ============================================================
     * KORTTI 1 JA 2
     * ============================================================
     */
    private String cleanCardName(
            Text text
    ) {

        if (text == null) {
            return "";
        }

        String raw =
                text.getText();

        if (raw == null ||
                raw.trim().isEmpty()) {

            return "";
        }

        String[] lines =
                raw.split("\\r?\\n");

        String bestCandidate = "";

        /*
         * Ensimmäinen vaihe:
         * käydään jokainen OCR-rivi läpi.
         */
        for (String line : lines) {

            if (line == null) {
                continue;
            }

            line =
                    line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String candidate =
                    cleanSingleCardNameLine(
                            line
                    );

            if (candidate.isEmpty()) {
                continue;
            }

            /*
             * Yritetään korjata OCR-rivi ArenaAdvisorilla.
             */
            String corrected =
                    candidate;

            try {

                String advisorCorrected =
                        ArenaAdvisor.correctOcr(
                                candidate
                        );

                if (advisorCorrected != null &&
                        !advisorCorrected.trim().isEmpty()) {

                    corrected =
                            advisorCorrected.trim();
                }

            } catch (Exception ignored) {
            }

            /*
             * Jos korjaus muutti nimeä, se on yleensä
             * vahva merkki siitä, että kyseessä on kortti.
             */
            if (!corrected.equalsIgnoreCase(
                    candidate
            )) {

                return corrected;
            }

            /*
             * Pidempi OCR-tulos säilytetään ehdokkaana.
             */
            if (corrected.length() >
                    bestCandidate.length()) {

                bestCandidate =
                        corrected;
            }
        }

        /*
         * Toinen vaihe:
         *
         * ML Kit saattaa jakaa kortin nimen useaksi riviksi.
         * Yhdistetään kaikki rivit ja annetaan koko teksti
         * ArenaAdvisorille.
         */
        StringBuilder combined =
                new StringBuilder();

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            line =
                    line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (combined.length() > 0) {
                combined.append(" ");
            }

            combined.append(line);
        }

        String combinedCandidate =
                cleanSingleCardNameLine(
                        combined.toString()
                );

        if (!combinedCandidate.isEmpty()) {

            String corrected =
                    combinedCandidate;

            try {

                String advisorCorrected =
                        ArenaAdvisor.correctOcr(
                                combinedCandidate
                        );

                if (advisorCorrected != null &&
                        !advisorCorrected.trim().isEmpty()) {

                    corrected =
                            advisorCorrected.trim();
                }

            } catch (Exception ignored) {
            }

            if (!corrected.equalsIgnoreCase(
                    combinedCandidate
            )) {

                return corrected;
            }

            if (corrected.length() >
                    bestCandidate.length()) {

                bestCandidate =
                        corrected;
            }
        }

        /*
         * Viimeinen fallback.
         */
        return bestCandidate;
    }

    private String cleanSingleCardNameLine(
            String line
    ) {

        if (line == null) {
            return "";
        }

        String best =
                line.trim();

        best =
                best.replace(
                        "&#039;",
                        "'"
                );

        best =
                best.replaceAll(
                        "^[^A-Za-zÀ-ÿ0-9]+",
                        ""
                );

        best =
                best.replaceAll(
                        "[^A-Za-zÀ-ÿ0-9'&\\-\\.\\s]+$",
                        ""
                );

        best =
                best.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        best =
                fixSoldierOfInfinite(
                        best
                );

        best =
                best.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        if (!best.isEmpty()) {

            best =
                    Character.toUpperCase(
                            best.charAt(0)
                    )
                    +
                    best.substring(1);
        }

        return best.trim();
    }

    private String cleanCard3Name(
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

        StringBuilder combined =
                new StringBuilder();

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            line =
                    line.trim();

            if (line.isEmpty()) {
                continue;
            }

            int letters = 0;

            for (
                    int i = 0;
                    i < line.length();
                    i++
            ) {

                if (Character.isLetter(
                        line.charAt(i)
                )) {

                    letters++;
                }
            }

            if (letters < 2) {
                continue;
            }

            if (combined.length() > 0) {
                combined.append(" ");
            }

            combined.append(line);
        }

        String best =
                combined.toString().trim();

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
                    +
                    best.substring(1);
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
                ||
                normalized.contains(
                        "so1dierofinfinite"
                )
                ||
                normalized.contains(
                        "sotdierofinfinite"
                )
                ||
                normalized.contains(
                        "soldierofihfinite"
                )
                ||
                normalized.contains(
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

        if (aa.isEmpty() ||
                bb.isEmpty()) {

            return false;
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
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (
                int i = 0;
                i <= a.length();
                i++
        ) {

            dp[i][0] = i;
        }

        for (
                int j = 0;
                j <= b.length();
                j++
        ) {

            dp[0][j] = j;
        }

        for (
                int i = 1;
                i <= a.length();
                i++
        ) {

            for (
                    int j = 1;
                    j <= b.length();
                    j++
            ) {

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

        return dp[
                a.length()
        ][
                b.length()
        ];
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

        ArenaAdvisor.detectClassFromCards(
                card1,
                card2,
                card3
        );

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

            pickAlreadyRecordedForOffer =
                    false;
        }

        if (pendingOfferCount <
                OFFER_CONFIRMATIONS) {

            return;
        }

        activeOffer1 =
                pendingOffer1;

        activeOffer2 =
                pendingOffer2;

        activeOffer3 =
                pendingOffer3;
    }

    private String formatCardBlock(
            String title,
            String cardName,
            String score
    ) {

        StringBuilder block =
                new StringBuilder();

        block.append(title)
                .append("\n");

        block.append(
                cardName == null ||
                        cardName.isEmpty()
                        ? "—"
                        : cardName
        );

        block.append("\nARVO: ")
                .append(
                        score == null ||
                                score.isEmpty()
                                ? "—"
                                : score
                );

        return block.toString();
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

        final double recommendationScore =
                ArenaAdvisor.getRecommendationScore();

        final String currentClass =
                ArenaAdvisor.getCurrentClass();

        StringBuilder display =
                new StringBuilder();

        display.append(
                "ARENA HELPER\n"
        );

        display.append(
                "CLASS: "
        )
                .append(
                        currentClass
                )
                .append(
                        "\n\n"
                );

        display.append(
                formatCardBlock(
                        "KORTTI 1",
                        card1,
                        score1
                )
        );

        display.append("\n\n");

        display.append(
                formatCardBlock(
                        "KORTTI 2",
                        card2,
                        score2
                )
        );

        display.append("\n\n");

        display.append(
                formatCardBlock(
                        "KORTTI 3",
                        card3,
                        score3
                )
        );

        display.append("\n\n");

        display.append(
                "━━━━━━━━━━━━━━━━\n"
        );

        display.append(
                "★ SUOSITUS ★\n"
        );

        display.append(
                recommendation == null ||
                        recommendation.isEmpty()
                        ? "—"
                        : recommendation
        );

        display.append("\nARVO ")
                .append(
                        String.format(
                                Locale.US,
                                "%.2f",
                                recommendationScore
                        )
                );

        display.append(
                "\nPARAS NÄISTÄ"
        );

        overlayView.setText(
                display.toString()
        );
    }

    @Override
    public void onDestroy() {

        if (activeInstance == this) {
            activeInstance = null;
        }

        processing = false;
        hearthstoneActive = false;

        if (mediaProjection != null) {

            try {
                mediaProjection.unregisterCallback(
                        mediaProjectionCallback
                );
            } catch (Exception ignored) {
            }
        }

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
