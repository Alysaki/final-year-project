package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nếu đã đăng nhập → bỏ qua
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> handleLogin());

        ((TextView) findViewById(R.id.tvGoRegister)).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        ((TextView) findViewById(R.id.tvForgotPassword)).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void handleLogin() {
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null
                ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(email)) { etEmail.setError("Vui lòng nhập email"); return; }
        if (TextUtils.isEmpty(pass))  { etPassword.setError("Vui lòng nhập mật khẩu"); return; }

        // ── Phase 1: Mock login ──────────────────────────────────────
        // Phase 2: Thay bằng FirebaseAuth.signInWithEmailAndPassword()
        String username = email.contains("@") ? email.split("@")[0] : email;

        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                .putString("email",       email)
                .putString("username",    username)
                .putString("phone",       "")
                .putBoolean("is_logged_in", true)
                .apply();

        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}