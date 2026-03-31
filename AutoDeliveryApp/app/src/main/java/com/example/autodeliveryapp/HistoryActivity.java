package com.example.autodeliveryapp;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autodeliveryapp.data.HistoryItem;
import com.example.autodeliveryapp.list_managers.HistoryManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

public class HistoryActivity extends BottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new HistoryManager(this, getMockHistory()));

        // Bottom Navigation — tab hiện tại: Hoạt động
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        setupBottomNav(nav, R.id.nav_history);
    }

    /**
     * Phase 1: Dữ liệu mẫu cứng
     * Phase 2: Tải từ Firebase Realtime Database → node "tasks/{userId}"
     */
    private List<HistoryItem> getMockHistory() {  //data test
        return Arrays.asList(
                new HistoryItem("#RBT-9042", "Kho hàng trung tâm A",
                        "123 Lý Thường Kiệt, Q.10",    "Hoàn thành", "24/03/2025", "45.000đ"),
                new HistoryItem("#RBT-9041", "Kho hàng trung tâm A",
                        "456 Nguyễn Trãi, Q.5",         "Hoàn thành", "23/03/2025", "38.000đ"),
                new HistoryItem("#RBT-9040", "Kho hàng trung tâm B",
                        "789 Hai Bà Trưng, Q.3",        "Đang giao",  "23/03/2025", "62.000đ"),
                new HistoryItem("#RBT-9039", "Kho hàng trung tâm A",
                        "321 Võ Văn Tần, Q.3",          "Đã hủy",     "22/03/2025", "0đ"),
                new HistoryItem("#RBT-9038", "Kho hàng trung tâm B",
                        "654 Điện Biên Phủ, Bình Thạnh","Hoàn thành", "22/03/2025", "55.000đ")
        );
    }
}