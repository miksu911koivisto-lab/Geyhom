package com.blessedmike.arenahelper;

import android.app.Activity;
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
        overlay.setText("1. Salli näytön päällä oleva ikkuna");
        layout.addView(overlay);
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        });

        Button start = new Button(this);
        start.setText("2. Käynnistä Arena Helper");
        layout.addView(start);
        start.setOnClickListener(v -> requestCapture());

        TextView info = new TextView(this);
        info.setText("\nKun sovellus on käynnissä, pieni Arena Helper -ikkuna näkyy Hearthstonen päällä.");
        info.setTextSize(16);
        layout.addView(info);

        setContentView(layout);
    }

    private void requestCapture() {
        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, CaptureService.class);
            service.putExtra("resultCode", resultCode);
            service.putExtra("data", data);
            startForegroundService(service);
        }
    }
}
