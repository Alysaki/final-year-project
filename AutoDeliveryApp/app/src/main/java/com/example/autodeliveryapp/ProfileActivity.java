package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        ((TextView) findViewById(R.id.tvUserName))
                .setText(prefs.getString("username", "Người dùng"));
        ((TextView) findViewById(R.id.tvEmail))
                .setText(prefs.getString("email", "---"));
        ((TextView) findViewById(R.id.tvPhone))
                .setText(prefs.getString("phone", "Chưa cập nhật"));

        // Logout → xóa session → LoginActivity
        ((LinearLayout) findViewById(R.id.btnLogout)).setOnClickListener(v -> {
            prefs.edit().putBoolean("is_logged_in", false).apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Bottom Navigation — tab hiện tại: Cá nhân
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        setupBottomNav(nav, R.id.nav_profile);
    }
}