package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvGoRegister;

    // Tài khoản test tạm thời (Giai đoạn 1 - chưa Firebase)
    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_PASSWORD = "123456";
    private static final String TEST_USERNAME = "Minh Nguyễn";
    private static final String TEST_PHONE = "0901234567";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nếu đã đăng nhập → vào thẳng MainActivity
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO (Giai đoạn 2): Thay bằng Firebase Authentication
        if (email.equals(TEST_EMAIL) && password.equals(TEST_PASSWORD)) {
            // Lưu session
            SharedPreferences.Editor editor =
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putString("userName", TEST_USERNAME);
            editor.putString("userEmail", email);
            editor.putString("userPhone", TEST_PHONE);
            editor.apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
        }
    }
}