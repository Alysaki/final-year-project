package com.example.autodeliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Tìm nút Logout theo ID đã đặt trong XML (btnLogout)
        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hiển thị thông báo
                Toast.makeText(ProfileActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

                // Logic đăng xuất: Quay về màn hình chính và xóa các màn hình cũ
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);

                // Cờ (Flag) này giúp xóa sạch các Activity đang mở để về lại tinh khôi
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish(); // Đóng Activity hiện tại
            }
        });
    }
}