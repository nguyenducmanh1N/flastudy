package com.example.myapplication.screens.feature;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

public class FolderDetailActivity extends AppCompatActivity {
    private RecyclerView courseRecycler;
    private CourseAdapter courseAdapter;
    private List<Course> courseList = new ArrayList<>();


    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String folderId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);


        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();


        folderId = getIntent().getStringExtra("folderId");
        if (folderId == null) {
            Toast.makeText(this, "Không xác định được thư mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        TextView textViewName = findViewById(R.id.folderNameTextView);
        TextView textViewDate = findViewById(R.id.folderDateTextView);
        ImageView btnBack     = findViewById(R.id.btnBack);
        ImageView btnAdd      = findViewById(R.id.btnAdd);
        courseRecycler        = findViewById(R.id.courseRecyclerView);


        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateCourseActivity.class);
            i.putExtra("folderId", folderId);
            startActivity(i);
        });

        courseAdapter = new CourseAdapter(
                courseList,
                course -> {
                    Intent i = new Intent(FolderDetailActivity.this, CourseDetailActivity.class);
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

        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.folder_menu, popup.getMenu());


            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    showEditNameDialog();
                    return true;
                } else if (id == R.id.action_share) {
                    shareFolder();
                    return true;
                } else if (id == R.id.action_delete) {
                    confirmAndDeleteFolder();
                    return true;
                }
                return false;
            });

            popup.show();
        });


        loadFolderInfo(textViewName, textViewDate);
        loadCourses();

    }
    private void loadFolderInfo(TextView tvName, TextView tvDate) {
        db.collection("users")
                .document(currentUser.getUid())
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

    private void loadCourses() {
        db.collection("users")
                .document(currentUser.getUid())
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
    private void showEditNameDialog() {
        final EditText input = new EditText(this);
        input.setText(((TextView)findViewById(R.id.folderNameTextView)).getText());
        new AlertDialog.Builder(this)
                .setTitle("Sửa tên thư mục")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("folders")
                            .document(folderId)
                            .update("name", newName)
                            .addOnSuccessListener(aVoid -> {
                                ((TextView)findViewById(R.id.folderNameTextView)).setText(newName);
                                Toast.makeText(this, "Đổi tên thành công", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void shareFolder() {

        String shareText = "Mình chia sẻ thư mục: "
                + ((TextView)findViewById(R.id.folderNameTextView)).getText()
                + "\nID: " + folderId;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ thư mục");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }

    private void confirmAndDeleteFolder() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thư mục")
                .setMessage("Bạn có chắc muốn xóa thư mục này? Toàn bộ khóa học sẽ bị xóa theo.")
                .setPositiveButton("Xóa", (dialog, which) -> {

                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("folders")
                            .document(folderId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa thư mục", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(this, HomeActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }




}