package com.example.myapplication.screens.feature;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateClassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_class);
//
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        EditText edtClassName = findViewById(R.id.class_name);
        EditText edtDescription = findViewById(R.id.class_description);
        Switch switchAllowMembers = findViewById(R.id.class_switch);
        Button btnSave = findViewById(R.id.addClassButton);

        btnSave.setOnClickListener(v -> {
            String className = edtClassName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            boolean allowMembers = switchAllowMembers.isChecked();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            String userEmail = user.getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> classData = new HashMap<>();
            classData.put("name", className);
            classData.put("description", description);
            classData.put("createdBy", userEmail);
            classData.put("allowMembersToAdd", allowMembers);

            List<String> members = new ArrayList<>();
            members.add(userEmail);
            classData.put("members", members);

            db.collection("classes")
                    .add(classData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreateClassActivity.this, ClassDetailActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi tạo lớp: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        });
    }

}