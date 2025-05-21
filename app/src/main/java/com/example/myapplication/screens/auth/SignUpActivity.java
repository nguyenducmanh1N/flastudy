package com.example.myapplication.screens.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private FirebaseFirestore db;
    private EditText edtFullName, signupEmail, signupPassword;
    private Button signupButton;
    private ImageButton btnBack;
    private CheckBox chkRemember;
    private TextView txtForgotPassword, txtSignUp;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // hoặc tên file XML bạn đã tạo

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        edtFullName = findViewById(R.id.edtFullName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        signupButton = findViewById(R.id.signupButton);
        btnBack = findViewById(R.id.btnBack_From_SignUp);
        chkRemember = findViewById(R.id.chkRemember);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtSignUp = findViewById(R.id.txtSignUp);

        // Xử lý nút đăng ký
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = signupEmail.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();
                String fullName = edtFullName.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Đăng ký với Firebase
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    String uid = user.getUid();
                                    Log.d("SIGN_UP", "UID: " + uid);

                                    User userData = new User(uid, fullName, email);

                                    db.collection("users").document(uid)
                                            .set(userData)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("SIGN_UP", "User saved to Firestore");
                                                Toast.makeText(SignUpActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("SIGN_UP", "Firestore write failed", e);
                                                Toast.makeText(SignUpActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Log.e("SIGN_UP", "FirebaseUser is null");
                                }
                            }

                        });
            }
        });


        // Quay về
        btnBack.setOnClickListener(v -> finish());

        // Quên mật khẩu
        txtForgotPassword.setOnClickListener(v -> {
            // Mở màn hình quên mật khẩu (nếu có)
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });

        // Chuyển sang màn hình login nếu đã có tài khoản
        txtSignUp.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }
}
