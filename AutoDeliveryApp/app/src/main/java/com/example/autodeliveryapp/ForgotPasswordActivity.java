package com.example.autodeliveryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etCode, etNewPassword, etConfirmPassword;
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;

    // Phase 1: mã giả để test UI
    private static final String MOCK_CODE = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail           = findViewById(R.id.etEmail);
        etCode            = findViewById(R.id.etCode);
        etNewPassword     = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);

        // Back → về ProfileActivity
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bước 1: Gửi mã
        findViewById(R.id.btnSendCode).setOnClickListener(v -> {
            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Vui lòng nhập email");
                return;
            }
            // Phase 2: FirebaseAuth.sendPasswordResetEmail(email)
            Toast.makeText(this,
                    "Mã xác nhận đã gửi!\n(Phase 1 - dùng mã: " + MOCK_CODE + ")",
                    Toast.LENGTH_LONG).show();
            layoutStep1.setVisibility(View.GONE);
            layoutStep2.setVisibility(View.VISIBLE);
        });

        // Bước 2: Xác nhận mã
        findViewById(R.id.btnVerifyCode).setOnClickListener(v -> {
            String code = etCode.getText() != null
                    ? etCode.getText().toString().trim() : "";
            if (!code.equals(MOCK_CODE)) {
                etCode.setError("Mã không đúng");
                return;
            }
            Toast.makeText(this, "Mã hợp lệ!", Toast.LENGTH_SHORT).show();
            layoutStep2.setVisibility(View.GONE);
            layoutStep3.setVisibility(View.VISIBLE);
        });

        // Bước 3: Đổi mật khẩu
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            String newPass  = etNewPassword.getText() != null
                    ? etNewPassword.getText().toString() : "";
            String confirm  = etConfirmPassword.getText() != null
                    ? etConfirmPassword.getText().toString() : "";

            if (newPass.length() < 6) {
                etNewPassword.setError("Ít nhất 6 ký tự");
                return;
            }
            if (!newPass.equals(confirm)) {
                etConfirmPassword.setError("Mật khẩu không khớp");
                return;
            }
            // Phase 2: cập nhật password qua Firebase
            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}