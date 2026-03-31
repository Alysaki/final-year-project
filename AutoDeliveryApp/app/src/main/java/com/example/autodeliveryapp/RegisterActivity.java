package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone    = findViewById(R.id.etPhone);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRegister).setOnClickListener(v -> handleRegister());
        ((TextView) findViewById(R.id.tvGoLogin)).setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String username = etUsername.getText() != null
                ? etUsername.getText().toString().trim() : "";
        String email    = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";
        String pass     = etPassword.getText() != null
                ? etPassword.getText().toString() : "";
        String phone    = etPhone.getText() != null
                ? etPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username)) { etUsername.setError("Vui lòng nhập tên"); return; }
        if (TextUtils.isEmpty(email))    { etEmail.setError("Vui lòng nhập email"); return; }
        if (pass.length() < 6)          { etPassword.setError("Mật khẩu ít nhất 6 ký tự"); return; }

        // ── Phase 1: Lưu vào SharedPreferences ──────────────────────
        // Phase 2: Thay bằng FirebaseAuth.createUserWithEmailAndPassword()
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                .putString("username",    username)
                .putString("email",       email)
                .putString("phone",       phone)
                .putBoolean("is_logged_in", true)
                .apply();

        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }
}