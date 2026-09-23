package com.blessedmike.arenahelper;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
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
     * Androidin järjestelmäkäyttöliittymä ei saa
     * sammuttaa Hearthstone-overlayta.
     *
     * Näitä tapahtumia voi tulla esimerkiksi kun:
     * - ilmoitusverho vedetään alas
     * - ilmoitusverho nostetaan ylös
     * - järjestelmä näyttää oman ikkunansa
     */
    private static final String ANDROID_PACKAGE =
            "android";

    private boolean hearthstoneActive = false;

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
         * Hearthstone avautuu / tulee aktiiviseksi.
         *
         * Tämä on ainoa tilanne jossa asetamme
         * Hearthstone aktiiviseksi.
         */
        if (HEARTHSTONE_PACKAGE.equals(
                packageNameString
        )) {

            if (!hearthstoneActive) {

                hearthstoneActive = true;

                CaptureService
                        .setHearthstoneActive(true);

                Log.d(
                        TAG,
                        "Hearthstone avattu"
                );
            }
        }

        /*
         * Jos tapahtuma tulee Androidin omasta
         * käyttöliittymästä, EI sammuteta overlayta.
         *
         * Tämä korjaa tilanteen jossa overlay
         * katoaa ilmoitusverhoa käytettäessä.
         */
        if (ANDROID_PACKAGE.equals(
                packageNameString
        )) {

            return;
        }

        /*
         * Jos tapahtuma tulee jostain muusta
         * sovelluksesta, Hearthstone ei enää ole
         * aktiivinen.
         *
         * Tyhjä packageName jätetään huomiotta,
         * koska Android voi lähettää sellaisia
         * tapahtumia ikkunoiden vaihtuessa.
         */
        if (!packageNameString.isEmpty()
                &&
                !HEARTHSTONE_PACKAGE.equals(
                        packageNameString
                )) {

            if (hearthstoneActive
                    &&
                    (type ==
                            AccessibilityEvent
                                    .TYPE_WINDOW_STATE_CHANGED
                            ||
                     type ==
                            AccessibilityEvent
                                    .TYPE_WINDOWS_CHANGED)) {

                hearthstoneActive = false;

                CaptureService
                        .setHearthstoneActive(false);

                Log.d(
                        TAG,
                        "Hearthstone poistuttu: "
                                + packageNameString
                );
            }

            return;
        }

        /*
         * Kaikki muu kuin Hearthstone ei saa
         * käsitellä korttiklikkauksia.
         */
        if (!HEARTHSTONE_PACKAGE.equals(
                packageNameString
        )) {

            return;
        }

        /*
         * TYPE_VIEW_CLICKED on edelleen
         * korttivalinnan tärkein tapahtuma.
         */
        if (type !=
                AccessibilityEvent.TYPE_VIEW_CLICKED) {

            return;
        }

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

        if (description != null) {

            return description.toString();
        }

        return "";
    }

    @Override
    public void onInterrupt() {

        /*
         * AccessibilityService keskeytettiin oikeasti,
         * joten tässä tapauksessa overlay voidaan
         * piilottaa.
         */
        hearthstoneActive = false;

        CaptureService.setHearthstoneActive(
                false
        );

        Log.d(
                TAG,
                "AccessibilityService interrupted"
        );
    }
}
