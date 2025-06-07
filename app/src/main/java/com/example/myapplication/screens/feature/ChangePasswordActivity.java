package com.example.myapplication.screens.feature;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {
    private EditText editCurrentPassword, editNewPassword, editConfirmPassword;
    private ImageView imgShowCurrentPassword, imgShowNewPassword, imgShowConfirmPassword;
    private Button btnSavePassword;
    private FirebaseAuth mAuth;
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        imgShowCurrentPassword = findViewById(R.id.imgShowCurrentPassword);
        imgShowNewPassword = findViewById(R.id.imgShowNewPassword);
        imgShowConfirmPassword = findViewById(R.id.imgShowConfirmPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        // Xử lý nút quay lại
        findViewById(R.id.imgBackChangePassword).setOnClickListener(view -> finish());

        // Xử lý hiện/ẩn mật khẩu
        setupPasswordVisibility();

        // Xử lý nút lưu mật khẩu
        btnSavePassword.setOnClickListener(view -> changePassword());
    }

    private void setupPasswordVisibility() {
        imgShowCurrentPassword.setOnClickListener(v -> {
            isCurrentPasswordVisible = !isCurrentPasswordVisible;
            editCurrentPassword.setInputType(
                    isCurrentPasswordVisible ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            editCurrentPassword.setSelection(editCurrentPassword.getText().length());
        });

        imgShowNewPassword.setOnClickListener(v -> {
            isNewPasswordVisible = !isNewPasswordVisible;
            editNewPassword.setInputType(
                    isNewPasswordVisible ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            editNewPassword.setSelection(editNewPassword.getText().length());
        });

        imgShowConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            editConfirmPassword.setInputType(
                    isConfirmPasswordVisible ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            editConfirmPassword.setSelection(editConfirmPassword.getText().length());
        });
    }

    private void changePassword() {
        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        // Validate input
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu mới và xác nhận mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị dialog loading
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setMessage("Đang đổi mật khẩu...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Xác thực mật khẩu hiện tại
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Đổi mật khẩu mới
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        loadingDialog.dismiss();
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Không thể đổi mật khẩu: " + updateTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            loadingDialog.dismiss();
            Toast.makeText(this, "Không thể xác thực người dùng", Toast.LENGTH_SHORT).show();
        }
    }
}