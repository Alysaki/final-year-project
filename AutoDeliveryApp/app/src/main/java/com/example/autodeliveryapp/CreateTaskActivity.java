package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class CreateTaskActivity extends AppCompatActivity {

    // Cước phí Phase 1 (đồng)
    private static final int BASE_STANDARD  = 15_000;
    private static final int RATE_STANDARD  = 5_000;   // mỗi km
    private static final int BASE_FAST      = 25_000;
    private static final int RATE_FAST      = 8_000;   // mỗi km

    private TextInputEditText etSenderName, etSenderPhone;
    private EditText          etPickup, etDropoff;
    private Spinner           spinnerService;
    private RadioGroup        rgPayment;
    private TextView          tvEstimatedPrice, tvDistance;

    private double simulatedDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        bindViews();
        prefillUserInfo();
        setupServiceSpinner();
        setupAddressWatcher();

        // Nút Xác nhận
        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirmTask());
    }

    private void bindViews() {
        etSenderName     = findViewById(R.id.etSenderName);
        etSenderPhone    = findViewById(R.id.etSenderPhone);
        etPickup         = findViewById(R.id.etPickup);
        etDropoff        = findViewById(R.id.etDropoff);
        spinnerService   = findViewById(R.id.spinnerService);
        rgPayment        = findViewById(R.id.rgPayment);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        tvDistance       = findViewById(R.id.tvDistance);
    }

    private void prefillUserInfo() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        etSenderName.setText(prefs.getString("username", ""));
        etSenderPhone.setText(prefs.getString("phone", ""));
    }

    private void setupServiceSpinner() {
        String[] services = {
                "📦 Giao hàng tiêu chuẩn (30-60 phút)",
                "🚀 Giao hàng nhanh (15-30 phút)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, services);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(adapter);

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, android.view.View v,
                                                 int pos, long id) { updatePrice(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupAddressWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String pickup  = etPickup.getText().toString().trim();
                String dropoff = etDropoff.getText().toString().trim();
                if (!pickup.isEmpty() && !dropoff.isEmpty()) {
                    // Phase 1: khoảng cách ngẫu nhiên 0.5 – 8.0 km
                    // Phase 3: thay bằng khoảng cách thực từ MapLibre/OSM
                    simulatedDistance = Math.round(
                            (0.5 + new Random().nextDouble() * 7.5) * 10.0) / 10.0;
                    updatePrice();
                } else {
                    simulatedDistance = 0;
                    tvDistance.setText("- km");
                    tvEstimatedPrice.setText("---.---đ");
                }
            }
        };
        etPickup.addTextChangedListener(watcher);
        etDropoff.addTextChangedListener(watcher);
    }

    private void updatePrice() {
        if (simulatedDistance == 0) return;
        boolean isFast    = spinnerService.getSelectedItemPosition() == 1;
        int     base      = isFast ? BASE_FAST    : BASE_STANDARD;
        int     ratePerKm = isFast ? RATE_FAST    : RATE_STANDARD;
        int     total     = (int) (base + ratePerKm * simulatedDistance);

        tvDistance.setText(simulatedDistance + " km");
        tvEstimatedPrice.setText(
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(total) + "đ");
    }

    private void confirmTask() {
        String senderName  = etSenderName.getText() != null
                ? etSenderName.getText().toString().trim() : "";
        String senderPhone = etSenderPhone.getText() != null
                ? etSenderPhone.getText().toString().trim() : "";
        String pickup      = etPickup.getText().toString().trim();
        String dropoff     = etDropoff.getText().toString().trim();

        if (senderName.isEmpty())  { Toast.makeText(this, "Nhập tên người gửi",   Toast.LENGTH_SHORT).show(); return; }
        if (senderPhone.isEmpty()) { Toast.makeText(this, "Nhập số điện thoại",   Toast.LENGTH_SHORT).show(); return; }
        if (pickup.isEmpty())      { Toast.makeText(this, "Nhập điểm lấy hàng",   Toast.LENGTH_SHORT).show(); return; }
        if (dropoff.isEmpty())     { Toast.makeText(this, "Nhập điểm giao hàng",  Toast.LENGTH_SHORT).show(); return; }
        if (simulatedDistance == 0){ Toast.makeText(this, "Vui lòng nhập đủ địa chỉ", Toast.LENGTH_SHORT).show(); return; }


        // random orderId
        String orderId = "RBT-" + (1000 + new Random().nextInt(9000));

        // Phase 2: Gửi dữ liệu lên Firebase Realtime Database ở đây

        Toast.makeText(this, "✅ Đã tạo nhiệm vụ thành công!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra("orderId",  orderId);
        intent.putExtra("pickup",   pickup);
        intent.putExtra("dropoff",  dropoff);
        intent.putExtra("distance", simulatedDistance);
        startActivity(intent);
        finish();
    }
}