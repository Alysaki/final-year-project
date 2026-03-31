package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class MainActivity extends BottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lời chào theo giờ
        ((TextView) findViewById(R.id.tvGreeting)).setText(getGreeting());

        // Tên người dùng từ SharedPreferences
        // Phase 2: Thay bằng FirebaseDatabase / FirebaseAuth.getCurrentUser()
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        ((TextView) findViewById(R.id.tvUserName))
                .setText(prefs.getString("username", "Người dùng"));

        // Nút Bắt đầu → CreateTaskActivity
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));

        // Bottom Navigation — tab hiện tại: Trang chủ
        setupBottomNav(findViewById(R.id.bottomNavigation), R.id.nav_home);
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5  && hour < 12) return "Chào buổi sáng,";
        if (hour >= 12 && hour < 13) return "Chào buổi trưa,";
        if (hour >= 13 && hour < 18) return "Chào buổi chiều,";
        return "Chào buổi tối,";
    }
}