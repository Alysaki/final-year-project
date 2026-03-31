package com.example.autodeliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Nhận dữ liệu từ CreateTaskActivity
        Intent intent   = getIntent();
        String orderId  = intent.getStringExtra("orderId");
        String pickup   = intent.getStringExtra("pickup");
        String dropoff  = intent.getStringExtra("dropoff");
        double distance = intent.getDoubleExtra("distance", 1.2);

        // Hiển thị thông tin lên UI
        ((TextView) findViewById(R.id.tvMissionId))
                .setText("Mission #" + orderId);
        ((TextView) findViewById(R.id.tvOrderId))
                .setText("Mã đơn hàng: #" + orderId);
        ((TextView) findViewById(R.id.tvDistanceRemaining))
                .setText(distance + " km");

        // ETA: ước tính 5 phút/km
        int etaMinutes = (int) Math.ceil(distance * 5);
        ((TextView) findViewById(R.id.tvETA)).setText(etaMinutes + " phút");

        // Thời gian nhận đơn
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        ((TextView) findViewById(R.id.tvReceivedTime))
                .setText(now + " • " + pickup);
        ((TextView) findViewById(R.id.tvDeliveryStatus))
                .setText("Đang di chuyển đến " + dropoff);

        // BottomSheet: kéo lên/xuống
        BottomSheetBehavior<android.view.View> bsb =
                BottomSheetBehavior.from(findViewById(R.id.bottomSheet));
        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Back → MainActivity
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}