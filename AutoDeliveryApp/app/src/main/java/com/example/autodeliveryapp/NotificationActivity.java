package com.example.autodeliveryapp;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autodeliveryapp.data.NotificationItem;
import com.example.autodeliveryapp.list_managers.NotificationManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

public class NotificationActivity extends BottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        RecyclerView rv = findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new NotificationManager(this, getMockNotifications()));

        // Bottom Navigation — tab hiện tại: Thông báo
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        setupBottomNav(nav, R.id.nav_notification);
    }

    /**
     * Phase 1: Dữ liệu mẫu cứng
     * Phase 2: Đọc từ Firebase Cloud Messaging / Realtime Database
     */
    private List<NotificationItem> getMockNotifications() {
        return Arrays.asList(
                new NotificationItem("🤖", "Đơn hàng đã được đặt",
                        "Đơn #RBT-9042 đã xác nhận, robot đang chuẩn bị", "10:15"),
                new NotificationItem("🚀", "Đơn hàng đang vận chuyển",
                        "Robot đang trên đường giao đơn #RBT-9042",        "10:30"),
                new NotificationItem("✅", "Đơn hàng giao thành công",
                        "Đơn #RBT-9041 đã giao thành công lúc 09:45",     "09:45"),
                new NotificationItem("❌", "Đơn hàng đã bị hủy",
                        "Đơn #RBT-9039 đã hủy theo yêu cầu",              "Hôm qua"),
                new NotificationItem("📦", "Đơn hàng mới",
                        "Bạn có đơn #RBT-9038 đang chờ xử lý",            "Hôm qua")
        );
    }
}