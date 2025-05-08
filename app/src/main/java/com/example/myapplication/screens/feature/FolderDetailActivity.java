package com.example.myapplication.screens.feature;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Folder;
import com.example.myapplication.screens.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FolderDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);
        TextView textViewName = findViewById(R.id.folderNameTextView);
        TextView textViewDate = findViewById(R.id.folderDateTextView);

        String folderId = getIntent().getStringExtra("folderId");
        if (folderId != null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("folders")
                    .document(folderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Folder folder = documentSnapshot.toObject(Folder.class);
                        if (folder != null) {
                            textViewName.setText(folder.getName());
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            String dateString = sdf.format(new Date(folder.getCreatedAt()));
                            textViewDate.setText(dateString);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi lấy thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnMenu = findViewById(R.id.btnMenu);

        ImageView btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            // Tạo Intent và đính kèm folderId
            Intent intent = new Intent(
                    FolderDetailActivity.this,
                    CreateCourseActivity.class
            );
            intent.putExtra("folderId", folderId);
            startActivity(intent);
        });



    }
}