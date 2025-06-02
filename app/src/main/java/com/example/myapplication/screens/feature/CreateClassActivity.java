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
import com.example.myapplication.model.Class;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class  CreateClassActivity extends AppCompatActivity {

    private EditText edtClassName, edtDescription;
    private Switch  switchAllowMembers;
    private Button  btnSave;
    private FirebaseUser  currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_class);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_class_layout),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        edtClassName     = findViewById(R.id.class_name);
        edtDescription   = findViewById(R.id.class_description);
        switchAllowMembers = findViewById(R.id.class_switch);
        btnSave          = findViewById(R.id.addClassButton);

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

        DocumentReference classRef = db.collection("classes").document();
        String classId = classRef.getId();

        Class cls = new Class();
        cls.setId(classId);
        cls.setName(name);
        cls.setDescription(description);
        cls.setCreater(creator);
        cls.setAllowMembersToAdd(allowAdd);

        ArrayList<String> members   = new ArrayList<>();
        members.add(creator);
        cls.setMembers(members);

        cls.setFolderIds(new ArrayList<>());
        cls.setCourseIds(new ArrayList<>());

        classRef.set(cls)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
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
