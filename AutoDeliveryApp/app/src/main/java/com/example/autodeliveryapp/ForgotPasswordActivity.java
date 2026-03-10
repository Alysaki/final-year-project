package com.example.autodeliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private View stepEmail, stepCode, stepNewPassword;
    private TextInputEditText etEmail, etCode, etNewPassword;

    // Mã test tạm thời
    private final String MOCK_CODE = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        stepEmail       = findViewById(R.id.stepEmail);
        stepCode        = findViewById(R.id.stepCode);
        stepNewPassword = findViewById(R.id.stepNewPassword);

        etEmail       = findViewById(R.id.etEmail);
        etCode        = findViewById(R.id.etCode);
        etNewPassword = findViewById(R.id.etNewPassword);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Step 1: Gửi mã
        ((MaterialButton) findViewById(R.id.btnSendCode)).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Nhập email trước", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO Giai đoạn 2: Firebase sendPasswordResetEmail
            Toast.makeText(this, "Mã xác thực đã gửi (test: " + MOCK_CODE + ")",
                    Toast.LENGTH_LONG).show();
            stepEmail.setVisibility(View.GONE);
            stepCode.setVisibility(View.VISIBLE);
        });

        // Step 2: Xác nhận mã
        ((MaterialButton) findViewById(R.id.btnVerifyCode)).setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.equals(MOCK_CODE)) {
                stepCode.setVisibility(View.GONE);
                stepNewPassword.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Mã không đúng", Toast.LENGTH_SHORT).show();
            }
        });

        // Step 3: Đổi mật khẩu
        ((MaterialButton) findViewById(R.id.btnResetPassword)).setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO Giai đoạn 2: Cập nhật mật khẩu Firebase
            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}