package com.blessedmike.arenahelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQUEST_CAPTURE = 1001;

    private boolean permissionCheckInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("Hearthstone Arena Helper");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);

        layout.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button overlay = new Button(this);
        overlay.setText("Luvat / asetukset");
        layout.addView(overlay);

        overlay.setOnClickListener(v -> checkPermissions());

        Button start = new Button(this);
        start.setText("Käynnistä Arena Helper");
        layout.addView(start);

        start.setOnClickListener(v -> {

            if (!hasOverlayPermission()) {
                checkPermissions();
                return;
            }

            if (!isAccessibilityServiceEnabled()) {
                checkPermissions();
                return;
            }

            requestCapture();
        });

        TextView info = new TextView(this);
        info.setText(
                "\nArena Helper tarkistaa tarvittavat luvat " +
                "automaattisesti sovelluksen käynnistyessä.\n\n" +
                "Kun luvat ovat kunnossa, Arena Helper -ikkuna " +
                "näkyy Hearthstonen päällä."
        );
        info.setTextSize(16);
        layout.addView(info);

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tarkistetaan luvat aina kun käyttäjä palaa
        // asetuksista takaisin sovellukseen.
        if (!permissionCheckInProgress) {
            checkPermissions();
        }
    }

    private void checkPermissions() {

        if (permissionCheckInProgress) {
            return;
        }

        // 1. Overlay-lupa
        if (!hasOverlayPermission()) {

            permissionCheckInProgress = true;

            new AlertDialog.Builder(this)
                    .setTitle("Näytön päällä oleva ikkuna")
                    .setMessage(
                            "Arena Helper tarvitsee luvan näyttää " +
                            "analyysi-ikkunan Hearthstonen päällä.\n\n" +
                            "Avataanko asetukset nyt?"
                    )
                    .setPositiveButton("Kyllä", (dialog, which) -> {

                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())
                        );

                        startActivity(intent);
                    })
                    .setNegativeButton("Myöhemmin", (dialog, which) -> {
                        permissionCheckInProgress = false;
                    })
                    .setOnDismissListener(dialog -> {
                        // Jos asetuksia ei avattu, sallitaan uusi tarkistus.
                        if (!hasOverlayPermission()) {
                            permissionCheckInProgress = false;
                        }
                    })
                    .show();

            return;
        }

        // 2. AccessibilityService-lupa
        if (!isAccessibilityServiceEnabled()) {

            permissionCheckInProgress = true;

            new AlertDialog.Builder(this)
                    .setTitle("Accessibility-lupa")
                    .setMessage(
                            "Arena Helper tarvitsee Accessibility-luvan " +
                            "tunnistaakseen korttien valinnan ja seuratakseen " +
                            "Hearthstonea.\n\n" +
                            "Avataanko Accessibility-asetukset nyt?"
                    )
                    .setPositiveButton("Kyllä", (dialog, which) -> {

                        Intent intent = new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                        );

                        startActivity(intent);
                    })
                    .setNegativeButton("Myöhemmin", (dialog, which) -> {
                        permissionCheckInProgress = false;
                    })
                    .setOnDismissListener(dialog -> {
                        if (!isAccessibilityServiceEnabled()) {
                            permissionCheckInProgress = false;
                        }
                    })
                    .show();

            return;
        }

        // Molemmat luvat ovat kunnossa.
        permissionCheckInProgress = false;
    }

    private boolean hasOverlayPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }

        return true;
    }

    private boolean isAccessibilityServiceEnabled() {

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) {
            return false;
        }

        ComponentName expectedService = new ComponentName(
                this,
                ArenaAccessibilityService.class
        );

        String expectedServiceName = expectedService.flattenToString();

        String[] services = enabledServices.split(":");

        for (String service : services) {
            if (service.equalsIgnoreCase(expectedServiceName)) {
                return true;
            }
        }

        return false;
    }

    private void requestCapture() {

        MediaProjectionManager mgr =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        if (mgr != null) {

            startActivityForResult(
                    mgr.createScreenCaptureIntent(),
                    REQUEST_CAPTURE
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == REQUEST_CAPTURE
                && resultCode == RESULT_OK
                && data != null) {

            // Välitetään MediaProjection-data suoraan CaptureServicelle
            CaptureService.setProjectionData(
                    resultCode,
                    data
            );

            Intent serviceIntent =
                    new Intent(
                            this,
                            CaptureService.class
                    );

            startForegroundService(serviceIntent);
        }
    }
}
