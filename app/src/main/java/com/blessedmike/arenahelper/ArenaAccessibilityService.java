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
         * Seurataan sovelluksen vaihtumista.
         *
         * Hearthstone -> overlay näkyviin
         * Muu sovellus -> overlay piiloon
         */
        if (type ==
                        AccessibilityEvent
                                .TYPE_WINDOW_STATE_CHANGED
                ||
                type ==
                        AccessibilityEvent
                                .TYPE_WINDOWS_CHANGED) {

            boolean active =
                    HEARTHSTONE_PACKAGE.equals(
                            packageNameString
                    );

            if (active != hearthstoneActive) {

                hearthstoneActive =
                        active;

                CaptureService
                        .setHearthstoneActive(
                                active
                        );

                Log.d(
                        TAG,
                        active
                                ? "Hearthstone avattu"
                                : "Hearthstone suljettu / poistuttu"
                );
            }
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
         * Jos AccessibilityService keskeytetään,
         * piilotetaan overlay varmuuden vuoksi.
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
