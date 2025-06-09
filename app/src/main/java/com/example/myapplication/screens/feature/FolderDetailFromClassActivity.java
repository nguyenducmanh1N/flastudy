package com.example.myapplication.screens.feature;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.R;
import com.example.myapplication.adapter.CourseAdapter;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Folder;
import com.example.myapplication.screens.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FolderDetailFromClassActivity extends AppCompatActivity {
    private RecyclerView courseRecycler;
    private CourseAdapter courseAdapter;
    private List<Course> courseList = new ArrayList<>();
    private FirebaseFirestore db;
    private String folderId, classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail_from_class);

        db = FirebaseFirestore.getInstance();

        folderId = getIntent().getStringExtra("folderId");
        classId = getIntent().getStringExtra("classId");

        if (folderId == null) {
            Toast.makeText(this, "Không xác định được thư mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView textViewName = findViewById(R.id.folderNameTextView);
        TextView textViewDate = findViewById(R.id.folderDateTextView);
        ImageView btnBack = findViewById(R.id.btnBack);
        courseRecycler = findViewById(R.id.courseRecyclerView);

        btnBack.setOnClickListener(v -> finish());

        courseAdapter = new CourseAdapter(
                courseList,
                course -> {
                    Intent i = new Intent(FolderDetailFromClassActivity.this, CourseDetailFromClassActivity.class);
                    i.putExtra("classId", classId);
                    i.putExtra("folderId", folderId);
                    i.putExtra("courseId", course.getId());
                    startActivity(i);
                },
                R.layout.item_course_vertical
        );
        LinearLayoutManager lm = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        courseRecycler.setLayoutManager(lm);
        courseRecycler.setAdapter(courseAdapter);

        loadFolderInfoFromClass(textViewName, textViewDate);
        loadCoursesFromClass();


    }

    private void loadFolderInfoFromClass(TextView tvName, TextView tvDate) {
        db.collection("classes")
                .document(classId)
                .collection("folders")
                .document(folderId)
                .get()
                .addOnSuccessListener(doc -> {
                    Folder f = doc.toObject(Folder.class);
                    if (f != null) {
                        tvName.setText(f.getName());
                        String date = new SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(new Date(f.getCreatedAt()));
                        tvDate.setText(date);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lấy folder: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void loadCoursesFromClass() {
        db.collection("classes")
                .document(classId)
                .collection("folders")
                .document(folderId)
                .collection("courses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qsnap -> {
                    courseList.clear();
                    for (DocumentSnapshot ds : qsnap.getDocuments()) {
                        Course c = ds.toObject(Course.class);
                        if (c != null) {

                            courseList.add(c);
                        }
                    }
                    courseAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lấy courses: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCoursesFromClass();
    }
}