package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateFolderActivity extends AppCompatActivity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_folder);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }

    private EditText editFolderName;
    private Button addFolderButton;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_folder);

        editFolderName = findViewById(R.id.editText);
        addFolderButton = findViewById(R.id.addFolderButton);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        addFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFolder();
            }
        });
    }

    private void createFolder() {
        String folderName = editFolderName.getText().toString().trim();

        if (folderName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên thư mục", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Dữ liệu thư mục
        Map<String, Object> folder = new HashMap<>();
        folder.put("name", folderName);
        folder.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .collection("folders")
                .add(folder)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Tạo thư mục thành công", Toast.LENGTH_SHORT).show();
                    editFolderName.setText(""); // clear text
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}