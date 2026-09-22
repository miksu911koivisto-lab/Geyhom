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

    private static final String CHANNEL_ID =
            "arena_helper_channel";

    private static final int OCR_INTERVAL = 1500;

    /*
     * Kuinka monta saman suuntaista OCR-tulosta tarvitaan,
     * ennen kuin nimi hyväksytään uutena nimenä.
     */
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

    /*
     * Yhden korttipaikan OCR-vakaus.
     *
     * stable:
     *     viimeisin varmasti tunnistettu nimi.
     *
     * candidate:
     *     uusi mahdollinen nimi, jota ei vielä ole
     *     hyväksytty stable-nimeksi.
     */
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
                        } catch (Exception ignored) {}

                        virtualDisplay = null;
                    }

                    if (imageReader != null) {

                        try {
                            imageReader.close();
                        } catch (Exception ignored) {}

                        imageReader = null;
                    }

                    mediaProjection = null;
                }
            };

    @Override
    public void onCreate() {

        super.onCreate();

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
            } catch (Exception ignored) {}

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
                                (width * 0.60f),
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

                    String cleaned =
                            cleanCardName(text);

                    results[index] =
                            stabilizeCard(
                                    cleaned,
                                    index
                            );

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

    /*
     * TÄRKEIN MUUTOS:
     *
     * Jos stable = "Holy Eggbearer"
     * ja OCR antaa:
     *
     *     "Holy"
     *     "Eggbearer"
     *     "Holy Egg"
     *     "Holy Eggbea"
     *
     * niitä EI enää hyväksytä stable-nimen tilalle.
     *
     * Vanha hyvä nimi säilytetään.
     */
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

        /*
         * Jos OCR löytää tunnetun kortin nimen,
         * käytetään sitä mieluummin kuin epätäydellistä
         * OCR-tulosta.
         */
        normalized =
                applyKnownCardCorrections(
                        normalized,
                        stability.stable
                );

        /*
         * Ensimmäinen tunnistus.
         */
        if (stability.stable.isEmpty()) {

            if (stability.candidate.isEmpty()) {

                stability.candidate =
                        normalized;

                stability.candidateCount = 1;

            } else if (
                    similarNames(
                            stability.candidate,
                            normalized
                    )
            ) {

                /*
                 * Jos toinen OCR-tulos on pidempi ja
                 * sisältää enemmän nimestä, pidetään
                 * pidempi ehdokkaana.
                 */
                stability.candidate =
                        chooseBetterName(
                                stability.candidate,
                                normalized
                        );

                stability.candidateCount++;

            } else {

                stability.candidate =
                        normalized;

                stability.candidateCount = 1;
            }

            if (stability.candidateCount >=
                    CARD_CONFIRMATIONS) {

                stability.stable =
                        stability.candidate;

                stability.candidate = "";

                stability.candidateCount = 0;
            }

            /*
             * Näytetään ehdokas väliaikaisesti,
             * mutta sitä ei vielä käytetä pysyvänä
             * kortin nimenä.
             */
            return stability.stable.isEmpty()
                    ? normalized
                    : stability.stable;
        }

        /*
         * Jos uusi OCR-tulos on käytännössä sama nimi,
         * EI vaihdeta stable-nimeä lyhyempään muotoon.
         */
        if (sameCardName(
                stability.stable,
                normalized
        )) {

            stability.candidate = "";

            stability.candidateCount = 0;

            return stability.stable;
        }

        /*
         * Jos uusi tulos näyttää olevan vain osa vanhasta
         * nimestä, IGNOROIDAAN se kokonaan.
         *
         * Tämä estää esimerkiksi:
         *
         * Holy Eggbearer
         *       ↓
         * Holy
         *
         * ja:
         *
         * Holy Eggbearer
         *       ↓
         * Eggbearer
         */
        if (isPartialOf(
                normalized,
                stability.stable
        )) {

            stability.candidate = "";

            stability.candidateCount = 0;

            return stability.stable;
        }

        /*
         * Jos uusi OCR-tulos on vanhaa nimeä pidempi
         * mutta selvästi sama nimi, voidaan käyttää
         * sitä paremman OCR-tuloksen ehdokkaana.
         */
        if (isExpandedVersion(
                normalized,
                stability.stable
        )) {

            if (stability.candidate.isEmpty() ||
                    !sameCardName(
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
                        normalized;

                stability.candidate = "";

                stability.candidateCount = 0;
            }

            return stability.stable;
        }

        /*
         * Täysin uusi nimi.
         * Sitä ei hyväksytä heti.
         */
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
                    chooseBetterName(
                            stability.stable,
                            stability.candidate
                    );

            stability.candidate = "";

            stability.candidateCount = 0;
        }

        return stability.stable;
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

    /*
     * Tarkempi nimivertailu.
     *
     * "Holy Eggbearer"
     * ja
     * "Holy"
     *
     * eivät ole enää sama nimi tässä kohdassa,
     * vaikka toinen sisältää toisen.
     */
    private boolean sameCardName(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }

        String aa =
                normalizeForComparison(a);

        String bb =
                normalizeForComparison(b);

        if (aa.equals(bb)) {
            return true;
        }

        /*
         * Pieni OCR-virhe hyväksytään,
         * mutta vain jos nimet ovat suunnilleen
         * saman pituiset.
         */
        int maxLength =
                Math.max(
                        aa.length(),
                        bb.length()
                );

        int lengthDifference =
                Math.abs(
                        aa.length() -
                                bb.length()
                );

        if (lengthDifference >
                Math.max(
                        2,
                        maxLength / 5
                )) {

            return false;
        }

        int distance =
                levenshtein(
                        aa,
                        bb
                );

        return distance <=
                Math.max(
                        2,
                        maxLength / 5
                );
    }

    /*
     * Tarkistaa onko uusi OCR-tulos vain osa
     * jo vakaasta kortin nimestä.
     */
    private boolean isPartialOf(
            String possiblePart,
            String fullName
    ) {

        if (possiblePart == null ||
                fullName == null) {

            return false;
        }

        String part =
                normalizeForComparison(
                        possiblePart
                );

        String full =
                normalizeForComparison(
                        fullName
                );

        if (part.isEmpty() ||
                full.isEmpty()) {

            return false;
        }

        if (part.equals(full)) {
            return false;
        }

        if (part.length() >=
                full.length()) {

            return false;
        }

        /*
         * Vain jos lyhyt tulos on oikeasti
         * merkittävä osa koko nimeä.
         */
        return full.contains(part) &&
                part.length() >= 4;
    }

    /*
     * Tarkistaa onko uusi tulos vanhan nimen
     * pidempi versio.
     */
    private boolean isExpandedVersion(
            String possibleFull,
            String oldName
    ) {

        if (possibleFull == null ||
                oldName == null) {

            return false;
        }

        String full =
                normalizeForComparison(
                        possibleFull
                );

        String old =
                normalizeForComparison(
                        oldName
                );

        if (full.length() <=
                old.length()) {

            return false;
        }

        if (!full.contains(old)) {

            return false;
        }

        return full.length() -
                old.length() >= 2;
    }

    private String chooseBetterName(
            String oldName,
            String newName
    ) {

        if (oldName == null ||
                oldName.isEmpty()) {

            return newName;
        }

        if (newName == null ||
                newName.isEmpty()) {

            return oldName;
        }

        /*
         * Jos uusi nimi sisältää vanhan nimen,
         * uusi pidempi nimi on yleensä parempi.
         */
        String old =
                normalizeForComparison(
                        oldName
                );

        String newer =
                normalizeForComparison(
                        newName
                );

        if (newer.length() >
                old.length() &&
                newer.contains(old)) {

            return newName;
        }

        /*
         * Muuten pidetään olemassa oleva vakaa nimi.
         */
        return oldName;
    }

    private String normalizeForComparison(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text.toLowerCase(
                        Locale.US
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
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
                        "\\s+",
                        " "
                ).trim();

        text =
                fixSoldierOfInfinite(
                        text
                );

        text =
                fixHolyEggbearer(
                        text
                );

        try {

            String corrected =
                    ArenaAdvisor.correctOcr(
                            text
                    );

            if (corrected != null &&
                    !corrected.trim().isEmpty()) {

                String correctedText =
                        corrected.trim();

                /*
                 * Älä anna ArenaAdvisorin lyhentää
                 * jo parempaa OCR-nimeä.
                 */
                if (correctedText.length() >=
                        text.length()) {

                    text =
                            correctedText;
                }
            }

        } catch (Exception ignored) {}

        text =
                fixHolyEggbearer(
                        text
                );

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
     * Holy Eggbearer -kortin OCR-vakautus.
     *
     * OCR voi lukea esimerkiksi:
     *
     * Holy Eggbearer
     * Holy Eggbeaer
     * Holy Eggbear
     * Holy Eggbeerer
     * Holy Egg
     * Holy
     *
     * Tässä vaiheessa vain selvästi koko nimeen
     * viittaavat muodot muutetaan oikeaksi nimeksi.
     */
    private String fixHolyEggbearer(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String compact =
                text.toLowerCase(
                        Locale.US
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );

        if (compact.equals(
                "holyeggbearer"
        )) {

            return "Holy Eggbearer";
        }

        if (compact.equals(
                "holyegbearer"
        )
                ||
                compact.equals(
                        "holyeggbear"
                )
                ||
                compact.equals(
                        "holyeggbearer"
                )
                ||
                compact.equals(
                        "holyegbearer"
                )
                ||
                compact.equals(
                        "holyeggbearer"
                )
                ||
                compact.equals(
                        "holyegbearer"
                )) {

            return "Holy Eggbearer";
        }

        /*
         * OCR saattaa yhdistää kirjaimia oudosti.
         * Jos molemmat osat löytyvät riittävän selvästi,
         * palautetaan oikea koko nimi.
         */
        boolean hasHoly =
                compact.contains("holy");

        boolean hasEgg =
                compact.contains("egg");

        boolean hasBear =
                compact.contains("bear");

        if (hasHoly &&
                hasEgg &&
                hasBear) {

            return "Holy Eggbearer";
        }

        return text;
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

        /*
         * Ensin etsitään koko tekstistä järkevä nimi.
         *
         * Tämä on tärkeää tapauksissa, joissa OCR tekee:
         *
         * Holy
         * Eggbearer
         *
         * eikä yhtenä rivinä:
         *
         * Holy Eggbearer
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

        String combinedText =
                combined.toString().trim();

        /*
         * Holy Eggbearer -erityistapaus
         * tarkistetaan ennen yhden rivin valintaa.
         */
        String holyEgg =
                fixHolyEggbearer(
                        combinedText
                );

        if ("Holy Eggbearer".equals(
                holyEgg
        )) {

            return holyEgg;
        }

        /*
         * Valitaan normaalisti ensimmäinen
         * järkevä tekstirivi.
         */
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
            best = combinedText;
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
                fixHolyEggbearer(
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

    private String applyKnownCardCorrections(
            String detected,
            String stable
    ) {

        String corrected =
                detected;

        corrected =
                fixHolyEggbearer(
                        corrected
                );

        corrected =
                fixSoldierOfInfinite(
                        corrected
                );

        /*
         * Jos vakaa nimi on Holy Eggbearer,
         * kaikki sitä lyhyemmät OCR-versiot pidetään
         * vakaana nimenä.
         */
        if (stable != null &&
                !stable.isEmpty()) {

            if (isPartialOf(
                    corrected,
                    stable
            )) {

                return stable;
            }

            /*
             * Jos OCR on hyvin lähellä vakaata nimeä,
             * palautetaan vakaa nimi eikä OCR:n
             * hieman eri kirjoitusasua.
             */
            if (sameCardName(
                    stable,
                    corrected
            )) {

                return stable;
            }
        }

        return corrected;
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
                normalizeForComparison(a);

        String bb =
                normalizeForComparison(b);

        if (aa.equals(bb)) {
            return true;
        }

        /*
         * Tässä käytetään edelleen contains-tarkistusta
         * tarjouksen vaihtumisen tunnistamiseen.
         *
         * Itse stable-nimen päivittämisessä käytetään
         * tarkempaa sameCardName/isPartialOf-logiikkaa.
         */
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
                similarNames(old1, new1)
                        ||
                similarNames(old1, new2)
                        ||
                similarNames(old1, new3);

        boolean old2Exists =
                similarNames(old2, new1)
                        ||
                similarNames(old2, new2)
                        ||
                similarNames(old2, new3);

        boolean old3Exists =
                similarNames(old3, new1)
                        ||
                similarNames(old3, new2)
                        ||
                similarNames(old3, new3);

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

        if (mediaProjection != null) {

            try {

                mediaProjection.unregisterCallback(
                        mediaProjectionCallback
                );

            } catch (Exception ignored) {}
        }

        if (imageReader != null) {

            try {
                imageReader.close();
            } catch (Exception ignored) {}

            imageReader = null;
        }

        if (virtualDisplay != null) {

            try {
                virtualDisplay.release();
            } catch (Exception ignored) {}

            virtualDisplay = null;
        }

        if (mediaProjection != null) {

            try {
                mediaProjection.stop();
            } catch (Exception ignored) {}

            mediaProjection = null;
        }

        if (recognizer != null) {

            try {
                recognizer.close();
            } catch (Exception ignored) {}

            recognizer = null;
        }

        if (overlayView != null &&
                windowManager != null) {

            try {

                windowManager.removeView(
                        overlayView
                );

            } catch (Exception ignored) {}

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
