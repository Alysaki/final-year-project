package com.example.autodeliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreateTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Tìm nút xác nhận (Đảm bảo bạn đã đặt ID này trong XML)
        // Nếu trong XML chưa có ID, hãy thêm android:id="@+id/btnConfirmTask" vào Button cuối cùng
        Button btnConfirmTask = findViewById(R.id.btnConfirmTask);

        // Nếu trong code XML trước đó tôi quên đặt ID cho nút này,
        // bạn có thể tạm thời dùng findViewById bằng cách thêm ID vào XML trước nhé.
        if (btnConfirmTask == null) {
            // Đây là đoạn code phòng hờ nếu bạn chưa thêm ID
            // Bạn hãy thêm dòng này vào Button trong XML: android:id="@+id/btnConfirmTask"
        } else {
            btnConfirmTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 1. Hiển thị thông báo nhỏ
                    Toast.makeText(CreateTaskActivity.this, "Đang tạo nhiệm vụ...", Toast.LENGTH_SHORT).show();

                    // 2. Chuyển sang màn hình theo dõi (TrackingActivity)
                    Intent intent = new Intent(CreateTaskActivity.this, TrackingActivity.class);
                    startActivity(intent);

                    // 3. (Tùy chọn) Đóng màn hình tạo task này lại để không back về được
                    finish();
                }
            });
        }
    }
}