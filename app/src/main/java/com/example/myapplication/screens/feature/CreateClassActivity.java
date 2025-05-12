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
import com.example.myapplication.model.ClassModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CreateClassActivity extends AppCompatActivity {

    private EditText edtClassName, edtDescription;
    private Switch  switchAllowMembers;
    private Button  btnSave;

    private FirebaseUser   currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_class);

        // xử lý inset
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_class_layout),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        // UI
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        edtClassName     = findViewById(R.id.class_name);
        edtDescription   = findViewById(R.id.class_description);
        switchAllowMembers = findViewById(R.id.class_switch);
        btnSave          = findViewById(R.id.addClassButton);

        // Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db          = FirebaseFirestore.getInstance();

        btnSave.setOnClickListener(v -> createClass());
    }

    private void createClass() {
        String name        = edtClassName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        boolean allowAdd   = switchAllowMembers.isChecked();

        if (name.isEmpty()) {
            edtClassName.setError("Tên lớp không được để trống");
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String creator = currentUser.getEmail();

        // 1. Tạo document ref để lấy ID trước
        DocumentReference classRef = db.collection("classes").document();
        String classId = classRef.getId();

        // 2. Khởi tạo model với folderIds & courseIds rỗng
        ClassModel cls = new ClassModel();
        cls.setId(classId);
        cls.setName(name);
        cls.setDescription(description);
        cls.setCreater(creator);
        cls.setAllowMembersToAdd(allowAdd);

        // Ban đầu, chỉ creator là member
        ArrayList<String> members   = new ArrayList<>();
        members.add(creator);
        cls.setMembers(members);

        // Hai list này khởi tạo trống, sẽ thêm sau
        cls.setFolderIds(new ArrayList<>());
        cls.setCourseIds(new ArrayList<>());

        // 3. Lưu model
        classRef.set(cls)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
                    // Truyền ID qua Intent
                    Intent intent = new Intent(this, ClassDetailActivity.class);
                    intent.putExtra("classId", classId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo lớp: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
