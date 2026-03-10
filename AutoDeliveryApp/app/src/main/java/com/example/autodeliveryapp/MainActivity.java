package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserName;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserName = findViewById(R.id.tvUserName);
        bottomNav  = findViewById(R.id.bottomNav);

        // Lấy tên người dùng từ SharedPreferences
        // TODO Giai đoạn 2: Lấy từ Firebase Realtime Database
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", "Người dùng");
        tvUserName.setText(userName);

        // Nút Bắt đầu → CreateTaskActivity
        findViewById(R.id.btnStart).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));

        // Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_notifications) {
                // TODO: NotificationActivity
            } else if (id == R.id.nav_activity) {
                // TODO: ActivityListActivity
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}
