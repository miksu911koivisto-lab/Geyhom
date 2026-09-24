package com.blessedmike.arenahelper;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ArenaAccessibilityService
        extends AccessibilityService {

    private static final String TAG =
            "ArenaAccessibility";

    private static final String HEARTHSTONE_PACKAGE =
            "com.blizzard.wtcg.hearthstone";

    /*
     * Androidin järjestelmäkäyttöliittymä.
     *
     * Erityisen tärkeä:
     * ilmoitusverho / statuspalkki käyttää yleensä
     * com.android.systemui-pakettia.
     *
     * SystemUI:n tapahtumat EIVÄT saa muuttaa
     * Hearthstonen aktiivisuustilaa.
     */
    private static final String SYSTEM_UI_PACKAGE =
            "com.android.systemui";

    /*
     * Joissakin Android-versioissa järjestelmä voi
     * lähettää tapahtumia myös android-paketista.
     */
    private static final String ANDROID_PACKAGE =
            "android";

    /*
     * Pieni viive ennen Hearthstone-tilan sammuttamista.
     *
     * Tämä estää lyhyet Androidin ikkuna-/pakettivaihdokset
     * aiheuttamasta overlayn vilkkumista.
     */
    private static final long DEACTIVATE_DELAY_MS =
            700L;

    private boolean hearthstoneActive = false;

    private final Handler handler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final Runnable deactivateRunnable =
            new Runnable() {

                @Override
                public void run() {

                    /*
                     * Jos Hearthstone on ehtinyt tulla takaisin
                     * etualalle viiveen aikana, mitään ei tehdä.
                     */
                    if (hearthstoneActive) {
                        return;
                    }

                    CaptureService.setHearthstoneActive(
                            false
                    );

                    Log.d(
                            TAG,
                            "Hearthstone overlay piilotettu"
                    );
                }
            };

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        CharSequence packageName =
                event.getPackageName();

        String packageNameString =
                packageName == null
                        ? ""
                        : packageName.toString();

        int type =
                event.getEventType();

        /*
         * ============================================================
         * 1. HEARTHSTONE
         * ============================================================
         *
         * Kun Hearthstone lähettää ikkunatapahtuman,
         * varmistetaan että overlay on näkyvissä.
         */
        if (HEARTHSTONE_PACKAGE.equals(
                packageNameString
        )) {

            /*
             * Perutaan mahdollinen aikaisemmin ajastettu
             * piilotus.
             */
            handler.removeCallbacks(
                    deactivateRunnable
            );

            if (!hearthstoneActive) {

                hearthstoneActive = true;

                CaptureService.setHearthstoneActive(
                        true
                );

                Log.d(
                        TAG,
                        "Hearthstone aktiivinen"
                );
            }

            /*
             * Vain klikkaustapahtumat käsitellään
             * korttivalintoina.
             */
            if (type !=
                    AccessibilityEvent.TYPE_VIEW_CLICKED) {

                return;
            }

            handleHearthstoneClick(event);

            return;
        }

        /*
         * ============================================================
         * 2. SYSTEM UI
         * ============================================================
         *
         * TÄMÄ ON TÄRKEIN KORJAUS.
         *
         * Kun käyttäjä:
         *
         *   Hearthstone
         *        ↓
         *   ilmoitusverho alas
         *        ↓
         *   ilmoitusverho ylös
         *
         * Android lähettää tapahtumia yleensä
         * com.android.systemui-paketista.
         *
         * Niihin EI reagoida millään tavalla.
         *
         * Näin overlayn tila ei vaihdu:
         *
         *   true → false → true
         *
         * vaan pysyy:
         *
         *   true
         */
        if (SYSTEM_UI_PACKAGE.equals(packageNameString)
                || ANDROID_PACKAGE.equals(packageNameString)
                || "com.android.keyguard".equals(packageNameString)) {

            Log.d(TAG, "System / Notification shade event ignored: " + packageNameString);
            return;
        }

        /*
         * ============================================================
         * 4. TYHJÄ PACKAGE
         * ============================================================
         *
         * Android voi lähettää hetkellisesti tapahtuman,
         * jossa packageName on tyhjä.
         *
         * Sitä ei tulkita Hearthstonesta poistumiseksi.
         */
        if (packageNameString.isEmpty()) {

            return;
        }

        /*
         * ============================================================
         * 5. MUU SOVELLUS
         * ============================================================
         *
         * Jos käyttäjä oikeasti siirtyy toiseen sovellukseen,
         * Hearthstone voidaan piilottaa.
         *
         * Käytetään kuitenkin pientä viivettä, jotta Androidin
         * lyhyet ikkuna-vaihdokset eivät aiheuta välähdystä.
         */
        if (!HEARTHSTONE_PACKAGE.equals(
                packageNameString
        )) {

            if (hearthstoneActive
                    &&
                    (
                            type ==
                                    AccessibilityEvent
                                            .TYPE_WINDOW_STATE_CHANGED
                            ||
                            type ==
                                    AccessibilityEvent
                                            .TYPE_WINDOWS_CHANGED
                    )) {

                /*
                 * Älä sammuta heti.
                 */
                hearthstoneActive = false;

                handler.removeCallbacks(
                        deactivateRunnable
                );

                handler.postDelayed(
                        deactivateRunnable,
                        DEACTIVATE_DELAY_MS
                );

                Log.d(
                        TAG,
                        "Muu sovellus havaittu: "
                                + packageNameString
                                + " - piilotus ajastettu"
                );
            }

            return;
        }
    }

    /*
     * ================================================================
     * HEARTHSTONE-KLIKKAUS
     * ================================================================
     */
    private void handleHearthstoneClick(
            AccessibilityEvent event
    ) {

        AccessibilityNodeInfo source =
                event.getSource();

        if (source == null) {
            return;
        }

        try {

            Rect bounds =
                    new Rect();

            source.getBoundsInScreen(
                    bounds
            );

            String text =
                    getNodeText(source);

            Log.d(
                    TAG,
                    "Hearthstone click: "
                            + bounds
                            + " text="
                            + text
            );

            CaptureService.onAccessibilityClick(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    text
            );

        } finally {

            source.recycle();
        }
    }

    /*
     * ================================================================
     * NODE TEXT
     * ================================================================
     */
    private String getNodeText(
            AccessibilityNodeInfo node
    ) {

        if (node == null) {
            return "";
        }

        CharSequence text =
                node.getText();

        if (text != null &&
                !text.toString().trim().isEmpty()) {

            return text.toString();
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null &&
                !description.toString()
                        .trim()
                        .isEmpty()) {

            return description.toString();
        }

        return "";
    }

    /*
     * ================================================================
     * INTERRUPT
     * ================================================================
     */
    @Override
    public void onInterrupt() {

        /*
         * AccessibilityService todella keskeytettiin.
         * Tässä tapauksessa overlay voidaan piilottaa.
         */
        handler.removeCallbacks(
                deactivateRunnable
        );

        hearthstoneActive = false;

        CaptureService.setHearthstoneActive(
                false
        );

        Log.d(
                TAG,
                "AccessibilityService interrupted"
        );
    }

    /*
     * ================================================================
     * SERVICE DESTROY
     * ================================================================
     */
    @Override
    public void onDestroy() {

        handler.removeCallbacks(
                deactivateRunnable
        );

        hearthstoneActive = false;

        super.onDestroy();

        Log.d(
                TAG,
                "AccessibilityService destroyed"
        );
    }
}
