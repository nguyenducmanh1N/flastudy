package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Folder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateFolderActivity extends AppCompatActivity {

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

        ImageView btnBack = findViewById(R.id.btnCloseCreateFolder);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> createFolder());
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
        String creater = currentUser.getEmail();


        String folderId = db.collection("users").document(uid)
                .collection("folders").document().getId();

        Folder folder = new Folder(folderName, System.currentTimeMillis(), 0, creater);
        folder.setId(folderId);


        db.collection("users").document(uid)
                .collection("folders").document(folderId)
                .set(folder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo thư mục thành công", Toast.LENGTH_SHORT).show();
                    editFolderName.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}