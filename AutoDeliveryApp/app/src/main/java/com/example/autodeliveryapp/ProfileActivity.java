package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Load thông tin từ SharedPreferences
        // TODO Giai đoạn 2: Lấy từ Firebase Auth + Realtime Database
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String name  = prefs.getString("userName", "Người dùng");
        String email = prefs.getString("userEmail", "—");
        String phone = prefs.getString("userPhone", "—");

        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvProfileEmail)).setText(email);
        ((TextView) findViewById(R.id.tvProfilePhone)).setText(phone);

        // Back → MainActivity
        findViewById(R.id.btnBack).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    private void logout() {
        // TODO Giai đoạn 2: Firebase Auth signOut()
        SharedPreferences.Editor editor =
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}