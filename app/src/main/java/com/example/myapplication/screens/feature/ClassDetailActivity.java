package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.CourseAdapter;
import com.example.myapplication.adapter.FolderAdapter;
import com.example.myapplication.model.ClassModel;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Folder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassDetailActivity extends AppCompatActivity {
    private TextView tvName, tvOwner, tvDate, tvCounts;
    private RecyclerView rvList;
    private TabLayout tabLayout;

    private FirebaseFirestore db;
    private String classId;

    private ClassModel cls;
    private List<Course> courseList = new ArrayList<>();
    private CourseAdapter courseAdapter;
    private List<Folder> folderList = new ArrayList<>();
    private FolderAdapter folderAdapter;
    private ImageView btnCloseClasDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_class_detail);

        // inset handling
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.detail_class_layout),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                }
        );

        // UI refs
        tvName   = findViewById(R.id.titleText);
        tvCounts = findViewById(R.id.subTitleText);
        tvOwner  = findViewById(R.id.subTitleText2);
        tvDate   = findViewById(R.id.textView3);
        rvList   = findViewById(R.id.recyclerView);
        tabLayout= findViewById(R.id.tabLayout);
        btnCloseClasDetail = findViewById(R.id.btnCloseClasDetail);

        btnCloseClasDetail.setOnClickListener(v -> {
            finish();
        });

        // init Firestore
        db = FirebaseFirestore.getInstance();
        // get classId
        classId = getIntent().getStringExtra("classId");
        if (classId == null) {
            Toast.makeText(this, "Không tìm thấy lớp", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // RecyclerView setup (dynamically switch adapter on tab)
        rvList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        // prepare both adapters
        courseAdapter = new CourseAdapter(courseList, course -> {
            // xử lý click course, ví dụ mở CourseDetailActivity
        });
        folderAdapter = new FolderAdapter(folderList);
        // mặc định hiển thị courses
        rvList.setAdapter(courseAdapter);

        // tab chọn Courses / Folders
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    rvList.setAdapter(courseAdapter);
                } else {
                    rvList.setAdapter(folderAdapter);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        loadClassDetail();
    }

    private void loadClassDetail() {
        db.collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener(doc -> {
                    cls = doc.toObject(ClassModel.class);
                    if (cls == null) {
                        Toast.makeText(this, "Lớp không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // bind info
                    tvName.setText(cls.getName());
                    tvOwner.setText("Người tạo: " + cls.getCreater());

                    // ngày khởi tạo
//                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                            .format(new Date(doc.getCreateTime().toDate().getTime()));
//                    tvDate.setText(date);

                    // count courses
                    int courseCount = cls.getCourseIds() != null ? cls.getCourseIds().size() : 0;
                    tvCounts.setText(courseCount + " học phần");

                    // load course list
                    loadCourses();

                    // load folder list
                    loadFolders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadCourses() {
        if (cls.getCourseIds() == null) return;
        courseList.clear();
        for (String cid : cls.getCourseIds()) {
            db.collection("classes")
                    .document(classId)
                    .collection("courses")
                    .document(cid)
                    .get()
                    .addOnSuccessListener(d -> {
                        Course c = d.toObject(Course.class);
                        if (c != null) {
                            c.setId(d.getId());
                            courseList.add(c);
                            courseAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void loadFolders() {
        if (cls.getFolderIds() == null) return;
        folderList.clear();
        for (String fid : cls.getFolderIds()) {
            db.collection("classes")
                    .document(classId)
                    .collection("folders")
                    .document(fid)
                    .get()
                    .addOnSuccessListener(d -> {
                        Folder f = d.toObject(Folder.class);
                        if (f != null) {
                            f.setId(d.getId());
                            folderList.add(f);
                            folderAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}