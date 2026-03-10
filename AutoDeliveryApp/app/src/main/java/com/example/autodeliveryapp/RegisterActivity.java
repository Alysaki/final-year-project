package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword, etPhone;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone    = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO (Giai đoạn 2): Thay bằng Firebase Auth createUserWithEmailAndPassword
        SharedPreferences.Editor editor =
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userName", username);
        editor.putString("userEmail", email);
        editor.putString("userPhone", phone);
        editor.apply();

        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}