package com.example.autodeliveryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autodeliveryapp.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeHelper.setLightStatusBar(this, true);
        EdgeToEdgeHelper.applySystemBarsPadding(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance(Constants.DB_URL)
                .getReference("users")
                .child(currentUser.getUid());

        loadCurrentProfile();

        binding.btnSave.setOnClickListener(v -> saveProfile(currentUser));
    }

    private void loadCurrentProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (binding == null || isFinishing() || isDestroyed()) return;

                String name = snapshot.child("username").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                if (name != null) binding.etName.setText(name);
                if (phone != null) binding.etPhone.setText(phone);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                android.util.Log.e("EditProfileDB",
                        "loadCurrentProfile cancelled - " + error.getMessage());
            }
        });
    }

    private void saveProfile(FirebaseUser currentUser) {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String newPassword = binding.etPassword.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError(getString(R.string.error_name_required));
            binding.etName.requestFocus();
            return;
        }

        // Disable button to prevent double-tap
        binding.btnSave.setEnabled(false);

        // Update name and phone in Realtime Database
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);
        updates.put("phone", phone);

        userRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (binding == null || isFinishing() || isDestroyed()) return;

                    // Handle optional password change
                    if (!TextUtils.isEmpty(newPassword)) {
                        if (newPassword.length() < 6) {
                            binding.etPassword.setError(
                                    getString(R.string.error_short_password));
                            binding.btnSave.setEnabled(true);
                            return;
                        }
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(v -> {
                                    if (binding == null) return;
                                    Toast.makeText(this,
                                            R.string.toast_password_updated,
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    if (binding == null) return;
                                    // Likely FirebaseAuthRecentLoginRequiredException
                                    Toast.makeText(this,
                                            "Please log out and log in again to change password",
                                            Toast.LENGTH_LONG).show();
                                    binding.btnSave.setEnabled(true);
                                });
                    } else {
                        Toast.makeText(this,
                                R.string.toast_profile_updated,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null || isFinishing() || isDestroyed()) return;
                    Toast.makeText(this,
                            getString(R.string.toast_error_prefix, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    binding.btnSave.setEnabled(true);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
