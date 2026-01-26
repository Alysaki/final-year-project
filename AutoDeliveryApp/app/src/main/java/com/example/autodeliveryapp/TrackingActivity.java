package com.example.autodeliveryapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TrackingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Ví dụ: Bấm vào nút "Delivering" để mô phỏng việc xem chi tiết
        // Lưu ý: Bạn cần thêm ID cho các Button trong XML nếu muốn bắt sự kiện cụ thể.
        // Ở đây tôi chỉ để hiển thị giao diện tĩnh.
    }

    // Nếu bạn muốn xử lý nút Back trên điện thoại để về thẳng Trang chủ
    // thay vì về trang Tạo Task (nếu chưa finish ở trên)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Code mặc định là quay lại trang trước đó
    }
}