package com.example.myapplication.screens.feature;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UpdateInfoActivity extends AppCompatActivity {

    private EditText editChangeUsername;
    private Button btnSaveInfo;
    private ImageView imgBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        // Ánh xạ view
        editChangeUsername = findViewById(R.id.editChangeUsername);
        btnSaveInfo = findViewById(R.id.btnSaveInfo);
        imgBack = findViewById(R.id.imgBackUpdateInfo);

        imgBack.setOnClickListener(v -> finish());
        btnSaveInfo.setOnClickListener(v -> saveUserInfo());

        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setMessage("Đang tải dữ liệu...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        editChangeUsername.setText(username != null ? username : "");
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserInfo() {
        String newUsername = editChangeUsername.getText().toString().trim();

        if (newUsername.isEmpty()) {
            editChangeUsername.setError("Vui lòng nhập tên người dùng");
            return;
        }

        if (currentUser == null) return;
        String uid = currentUser.getUid();

        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setMessage("Đang lưu thông tin...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        db.collection("users").document(uid)
                .update("username", newUsername)
                .addOnSuccessListener(unused -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}