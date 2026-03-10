package com.example.autodeliveryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.UUID;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etSenderName, etSenderPhone, etPickupPoint, etDeliveryPoint;
    private AutoCompleteTextView spinnerService;
    private android.widget.TextView tvEstimatedCost, tvDistance;
    private MaterialButton btnConfirmTask;

    // Phương thức thanh toán được chọn
    private String selectedPayment = "wallet"; // "wallet" hoặc "cash"

    // Giá cước theo loại dịch vụ (VNĐ/km)
    private static final double PRICE_STANDARD = 5000; // 5.000đ/km
    private static final double PRICE_EXPRESS   = 9000; // 9.000đ/km

    // Khoảng cách mặc định giả lập (Giai đoạn 3 sẽ tính thật)
    private double estimatedDistanceKm = 2.4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        initViews();
        prefillUserData();
        setupServiceDropdown();
        setupPaymentSelection();
        calculatePrice();

        btnConfirmTask.setOnClickListener(v -> confirmTask());

        // Tính lại giá khi đổi loại dịch vụ
        spinnerService.setOnItemClickListener((parent, view, position, id) -> calculatePrice());

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }

    private void initViews() {
        etSenderName   = findViewById(R.id.etSenderName);
        etSenderPhone  = findViewById(R.id.etSenderPhone);
//        etPickupPoint  = findViewById(R.id.etPickupPoint);
//        etDeliveryPoint = findViewById(R.id.etDeliveryPoint);
        spinnerService  = findViewById(R.id.spinnerService);
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost);
        tvDistance      = findViewById(R.id.tvDistance);
        btnConfirmTask  = findViewById(R.id.btnConfirmTask);
    }

    /** Điền sẵn thông tin từ profile */
    private void prefillUserData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        etSenderName.setText(prefs.getString("userName", ""));
        etSenderPhone.setText(prefs.getString("userPhone", ""));
    }

    private void setupServiceDropdown() {
        String[] services = {
                "🚀 Giao nhanh (15-30 phút)",
                "📦 Giao tiêu chuẩn (1-2 giờ)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, services);
        spinnerService.setAdapter(adapter);
        spinnerService.setText(services[0], false);
    }

    private void setupPaymentSelection() {
        androidx.cardview.widget.CardView cardWallet = findViewById(R.id.cardWallet);
        androidx.cardview.widget.CardView cardCash   = findViewById(R.id.cardCash);

        cardWallet.setOnClickListener(v -> {
            selectedPayment = "wallet";
            cardWallet.setCardBackgroundColor(getColor(R.color.green_light));
            cardCash.setCardBackgroundColor(getColor(R.color.background));
        });
        cardCash.setOnClickListener(v -> {
            selectedPayment = "cash";
            cardCash.setCardBackgroundColor(getColor(R.color.green_light));
            cardWallet.setCardBackgroundColor(getColor(R.color.background));
        });
    }

    /** Tính giá ước tính dựa trên loại dịch vụ */
    private void calculatePrice() {
        String service = spinnerService.getText().toString();
        double pricePerKm = service.contains("nhanh") ? PRICE_EXPRESS : PRICE_STANDARD;
        long totalPrice = Math.round(estimatedDistanceKm * pricePerKm);

        // Format giá VNĐ
        String formattedPrice = String.format("%,dđ", totalPrice)
                .replace(",", ".");
        tvEstimatedCost.setText(formattedPrice);
        tvDistance.setText(String.format("%.1f km", estimatedDistanceKm));
    }

    private void confirmTask() {
        String senderName    = etSenderName.getText().toString().trim();
        String senderPhone   = etSenderPhone.getText().toString().trim();
        String pickupPoint   = etPickupPoint.getText().toString().trim();
        String deliveryPoint = etDeliveryPoint.getText().toString().trim();

        // Validate
        if (senderName.isEmpty() || senderPhone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập thông tin người gửi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pickupPoint.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập điểm lấy hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deliveryPoint.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập điểm giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo mã nhiệm vụ
        String taskId = "RBT-" + (1000 + (int)(Math.random() * 9000));
        String serviceType  = spinnerService.getText().toString();
        String costText     = tvEstimatedCost.getText().toString();

        // TODO Giai đoạn 2: Gửi lên Firebase Realtime Database
        // Cấu trúc node: /tasks/{taskId}
        // {
        //   senderName, senderPhone, pickupPoint, deliveryPoint,
        //   serviceType, payment, estimatedCost, status: "pending",
        //   timestamp: ServerValue.TIMESTAMP
        // }

        Toast.makeText(this, "Đã tạo nhiệm vụ thành công!", Toast.LENGTH_LONG).show();

        // Chuyển sang TrackingActivity
//        Intent intent = new Intent(this, TrackingActivity.class);
//        intent.putExtra("taskId", taskId);
//        intent.putExtra("deliveryPoint", deliveryPoint);
//        startActivity(intent);
//        finish();
    }
}