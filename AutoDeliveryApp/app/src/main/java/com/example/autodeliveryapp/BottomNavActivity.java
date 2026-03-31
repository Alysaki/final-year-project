package com.example.autodeliveryapp;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity gốc cho tất cả màn hình có Bottom Navigation.
 * Các Activity con kế thừa và gọi setupBottomNav() với tab ID hiện tại.
 */
public abstract class BottomNavActivity extends AppCompatActivity {

    protected void setupBottomNav(BottomNavigationView nav, int selectedItemId) {
        nav.setSelectedItemId(selectedItemId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return false; // Đang ở tab này

            Intent intent = null;
            if      (id == R.id.nav_home)         intent = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_history)      intent = new Intent(this, HistoryActivity.class);
            else if (id == R.id.nav_notification) intent = new Intent(this, NotificationActivity.class);
            else if (id == R.id.nav_profile)      intent = new Intent(this, ProfileActivity.class);

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0); // Không animation khi chuyển tab
            }
            return true;
        });
    }
}