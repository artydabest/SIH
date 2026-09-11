package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceId = getIntent().getStringExtra("deviceId");
        String eventId = getIntent().getStringExtra("eventId");
        String name = getIntent().getStringExtra("name");
        String phone = getIntent().getStringExtra("phone");
        String location = getIntent().getStringExtra("location");
        String timestamp = getIntent().getStringExtra("timestamp");
        String signal = getIntent().getStringExtra("signal");
        String status = getIntent().getStringExtra("status");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);
        layout.setBackgroundColor(Color.rgb(248, 249, 252));

        TextView title = new TextView(this);
        title.setText("Device Details");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 35, 50));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        addText(layout, "📱 " + deviceId, 23, true);
        addText(layout, "👤 Name: " + name, 17, false);
        addText(layout, "📞 Phone: " + phone, 17, false);
        addText(layout, "🚨 Status: " + status, 17, false);
        addText(layout, "🆔 Event: " + eventId, 17, false);
        addText(layout, "📍 Location: " + location, 17, false);
        addText(layout, "📶 Signal: " + signal, 17, false);
        addText(layout, "🕒 Time: " + timestamp, 17, false);

        Button contactButton = new Button(this);
        contactButton.setText("📞 CONTACT " + name);
        contactButton.setTextSize(16);

        contactButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + phone)
            );
            startActivity(intent);
        });

        layout.addView(contactButton);

        Button backButton = new Button(this);
        backButton.setText("← BACK");

        backButton.setOnClickListener(v -> finish());

        layout.addView(backButton);

        setContentView(layout);
    }

    private void addText(
            LinearLayout layout,
            String text,
            int size,
            boolean bold) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(50, 55, 70));
        view.setPadding(0, 12, 0, 12);

        if (bold) {
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        layout.addView(view);
    }
}