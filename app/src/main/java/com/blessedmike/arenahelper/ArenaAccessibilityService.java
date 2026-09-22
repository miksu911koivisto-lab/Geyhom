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

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        CharSequence packageName =
                event.getPackageName();

        if (packageName == null ||
                !HEARTHSTONE_PACKAGE.equals(
                        packageName.toString()
                )) {

            return;
        }

        int type =
                event.getEventType();

        /*
         * TYPE_VIEW_CLICKED on tärkein.
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

        Log.d(
                TAG,
                "AccessibilityService interrupted"
        );
    }
}
