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

    /*
     * Yksittäinen kortti pitää tunnistaa
     * vähintään kaksi kertaa ennen kuin se
     * hyväksytään vakaaksi.
     */
    private static final int CARD_CONFIRMATIONS = 2;

    /*
     * Koko kolmen kortin tarjous pitää nähdä
     * kaksi kertaa samana ennen hyväksymistä.
     */
    private static final int OFFER_CONFIRMATIONS = 2;

    private static final long PICK_COOLDOWN_MS = 1200L;

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

    /*
     * Viimeisin OCR:n ehdottama tarjous.
     */
    private String pendingOffer1 = "";
    private String pendingOffer2 = "";
    private String pendingOffer3 = "";
    private int pendingOfferCount = 0;

    /*
     * Varmistettu tämänhetkinen Arena-tarjous.
     */
    private String activeOffer1 = "";
    private String activeOffer2 = "";
    private String activeOffer3 = "";

    /*
     * Estää saman tarjouksen aikana saman klikkauksen
     * tallentamisen useita kertoja.
     */
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

    /*
     * AccessibilityService kutsuu tätä, kun se saa
     * Hearthstonesta klikkaustapahtuman.
     */
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

        /*
         * Klikkausta ei hyväksytä ennen kuin
         * kaikki kolme korttia on varmasti
         * tunnistettu ja tarjous vahvistettu.
         */
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

        /*
         * Jos accessibility-node antaa tekstin,
         * käytetään sitä ensisijaisesti.
         */
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

        /*
         * Jos tekstiä ei saada, käytetään klikkauksen
         * vaakasuuntaista sijaintia.
         */
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

        /*
         * Samat korttialueet kuin OCR:ssa.
         */
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

    private final CardSt
