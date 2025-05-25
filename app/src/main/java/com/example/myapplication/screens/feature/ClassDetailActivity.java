package com.example.myapplication.screens.feature;

import static com.example.myapplication.screens.feature.CreateCourseActivity.EXTRA_FOLDER_ID;
import static com.example.myapplication.screens.feature.CreateCourseActivity.EXTRA_IS_EDIT;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.example.myapplication.adapter.CourseSelectAdapter;
import com.example.myapplication.adapter.FolderAdapter;
import com.example.myapplication.adapter.FolderSelectAdapter;
import com.example.myapplication.model.Class;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Folder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ClassDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvCourseCount, tvOwner, tvDate;
    private RecyclerView rvItems;
    private TabLayout tabLayout;
    private ImageView btnClose, btnMenu;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser;
    private String classId;
    private Class cls;

    private List<Course> courseList = new ArrayList<>();
    private CourseAdapter courseAdapter;

    private List<Folder> folderList = new ArrayList<>();
    private FolderAdapter folderAdapter;
    private LinearLayout folderContainer;
    private LinearLayout courseContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_class_detail);

        folderContainer = findViewById(R.id.folderContainer);


        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.detail_class_layout),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                }
        );


        tvTitle       = findViewById(R.id.titleText);
        tvCourseCount = findViewById(R.id.subTitleText);
        tvOwner       = findViewById(R.id.subTitleText2);
        tvDate        = findViewById(R.id.textViewDate);
        //rvItems       = findViewById(R.id.recyclerView);
        tabLayout     = findViewById(R.id.tvTabLayout);
        btnClose      = findViewById(R.id.btnExit);
        btnMenu       = findViewById(R.id.btnMenu);
        courseContainer = findViewById(R.id.courseContainer);


        btnClose.setOnClickListener(v -> finish());


        classId = getIntent().getStringExtra("classId");
        if (classId == null) {
            Toast.makeText(this, "Không tìm thấy lớp", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


//        rvItems.setLayoutManager(
//                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        );
        courseAdapter = new CourseAdapter(courseList, c -> {

        });
        folderAdapter = new FolderAdapter(folderList);


//        rvItems.setAdapter(folderAdapter);

        btnMenu.setOnClickListener(v -> showOptions());

        TabLayout tabs = findViewById(R.id.tvTabLayout);
        ScrollView folderWrapper = findViewById(R.id.containerFolders);

        View memberWrapper = findViewById(R.id.containerMembers);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                folderWrapper.setVisibility(View.GONE);
                courseContainer.setVisibility(View.GONE);
                memberWrapper.setVisibility(View.GONE);


                switch (tab.getPosition()) {
                    case 0:
                        folderWrapper.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        courseContainer.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        memberWrapper.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        loadClassDetail();
    }

    private void loadClassDetail() {
        db.collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener(doc -> {
                    cls = doc.toObject(Class.class);
                    if (cls == null) {
                        Toast.makeText(this, "Lớp không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    tvTitle.setText(cls.getName());
                    tvOwner.setText("Người tạo: " + cls.getCreater());
                    int count = cls.getCourseIds()!=null?cls.getCourseIds().size():0;
                    tvCourseCount.setText(count + " học phần");

                    loadCourses();
                    loadFolders();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải lớp: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCourses() {

        courseContainer.removeAllViews();


        List<String> courseIds = cls.getCourseIds();
        List<String> folderIds = cls.getFolderIds();
        if (courseIds == null || courseIds.isEmpty()
                || folderIds == null || folderIds.isEmpty()) {
            return;
        }

        String uid = currentUser.getUid();
        // 3. Với mỗi folderId và mỗi courseId, fetch từ users/{uid}/folders/{fid}/courses/{cid}
        for (String fid : folderIds) {
            for (String cid : courseIds) {
                db.collection("users")
                        .document(uid)
                        .collection("folders")
                        .document(fid)
                        .collection("courses")
                        .document(cid)
                        .get()
                        .addOnSuccessListener(d -> {
                            Course c = d.toObject(Course.class);
                            if (c == null) return;

                            // 4. Inflate layout item
                            View item = LayoutInflater.from(this)
                                    .inflate(R.layout.item_course_vertical, courseContainer, false);

                            TextView tvTitle   = item.findViewById(R.id.courseTitle);
                            TextView tvCount   = item.findViewById(R.id.courseTermCount);
                            TextView tvCreater = item.findViewById(R.id.courseCreater);

                            tvTitle.setText(c.getTitle());
                            tvCount.setText(c.getVocabularyList().size() + " thuật ngữ");
                            tvCreater.setText("Người tạo: " + c.getCreater());

                            // 5. Click vào chuyển trang detail
                            item.setOnClickListener(v -> {
                                Intent i = new Intent(this, CourseDetailActivity.class);
                                i.putExtra("folderId", fid);
                                i.putExtra("courseId", cid);
                                startActivity(i);
                            });

                            // 6. Thêm vào container
                            courseContainer.addView(item);
                        })
                        .addOnFailureListener(e -> {
                            // có thể log hoặc show toast tuỳ ý
                        });
            }
        }
    }



    private void loadFolders() {
        folderContainer.removeAllViews();
        if (cls.getFolderIds() == null) return;

        for (String fid : cls.getFolderIds()) {

            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("folders")
                    .document(fid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        Folder f = doc.toObject(Folder.class);
                        if (f == null) return;

                        View item = getLayoutInflater()
                                .inflate(R.layout.item_folder_vertical, folderContainer, false);

                        TextView tvTitle   = item.findViewById(R.id.tvTitle);
                        TextView tvCount   = item.findViewById(R.id.tvCount);
                        TextView tvCreater = item.findViewById(R.id.tvCreater);
                        CheckBox cbSelect  = item.findViewById(R.id.cbSelect);

                        tvTitle.setText(f.getName());
                        // Giả sử f.getTermCount() trả về số thuật ngữ
                        //tvCount.setText(f.getTermCount() + " thuật ngữ");
                        tvCreater.setText("Người tạo: " + f.getCreater());
                        cbSelect.setVisibility(View.GONE);

                        // Bắt sự kiện click
                        item.setOnClickListener(v -> {
                            Intent i = new Intent(this, FolderDetailActivity.class);
                            i.putExtra("folderId", f.getId());
                            startActivity(i);
                        });

                        folderContainer.addView(item);
                    });
        }
    }


    private void showOptions() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(
                R.layout.class_bottom_sheet_add_options, null);
        bs.setContentView(view);

        view.findViewById(R.id.edit).setOnClickListener(v -> {
            bs.dismiss();

            Intent i = new Intent(this, CreateClassActivity.class);
            i.putExtra("classId", classId);
            i.putExtra(EXTRA_IS_EDIT, true);
            startActivity(i);
        });
        view.findViewById(R.id.addFolder).setOnClickListener(v -> {
            bs.dismiss();
            showAddFoldersToClass();
        });
        view.findViewById(R.id.addCourse).setOnClickListener(v -> {
            bs.dismiss();
            showAddCoursesToClass();
        });
        view.findViewById(R.id.addMember).setOnClickListener(v -> {
            bs.dismiss();

        });

        bs.show();
    }

    private void showAddFoldersToClass() {
        View sheet = getLayoutInflater().inflate(R.layout.bs_add_folders, null);
        BottomSheetDialog bsDialog = new BottomSheetDialog(this);

        bsDialog.setContentView(sheet);
        bsDialog.setCancelable(true);

        RecyclerView rv = sheet.findViewById(R.id.rvFolders);
        Button btnAdd = sheet.findViewById(R.id.btnAddSelected);

        List<Folder> allFolders = new ArrayList<>();
        FolderSelectAdapter adapter = new FolderSelectAdapter(allFolders, selectedIds -> {
            btnAdd.setEnabled(!selectedIds.isEmpty());
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);


        db.collection("users")
                .document(currentUser.getUid())
                .collection("folders")
                .get()
                .addOnSuccessListener(q -> {
                    allFolders.clear();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        Folder f = d.toObject(Folder.class);
                        f.setId(d.getId());
                        allFolders.add(f);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thư mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });





        btnAdd.setOnClickListener(v -> {
            List<String> toAdd = new ArrayList<>(adapter.getSelectedIds());
            if (toAdd.isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất 1 thư mục", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentReference clsRef = db.collection("classes").document(classId);
            clsRef.update("folderIds", FieldValue.arrayUnion(toAdd.toArray()))
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Đã thêm thư mục", Toast.LENGTH_SHORT).show();
                        bsDialog.dismiss();
                        loadFolders();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            bsDialog.dismiss();
        });


        bsDialog.setContentView(sheet);
        bsDialog.show();
    }

    private void showAddCoursesToClass() {

        View sheet = getLayoutInflater().inflate(R.layout.bs_add_courses, null);
        BottomSheetDialog bsDialog = new BottomSheetDialog(this);
        bsDialog.setContentView(sheet);
        bsDialog.setCancelable(true);


        EditText etSearch      = sheet.findViewById(R.id.etSearchCourses);
        RecyclerView rv        = sheet.findViewById(R.id.rvCourses);
        Button btnAddSelected  = sheet.findViewById(R.id.btnAddSelected);
        btnAddSelected.setEnabled(false);


        List<Course> allCourses = new ArrayList<>();
        CourseSelectAdapter adapter = new CourseSelectAdapter(allCourses, selectedIds -> {
            btnAddSelected.setEnabled(!selectedIds.isEmpty());
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);


        if (cls.getFolderIds() != null && !cls.getFolderIds().isEmpty()) {
            List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
            for (String fid : cls.getFolderIds()) {
                com.google.android.gms.tasks.Task<?> t = db
                        .collection("users")
                        .document(currentUser.getUid())
                        .collection("folders")
                        .document(fid)
                        .collection("courses")
                        .get()
                        .addOnSuccessListener(qSnap -> {
                            for (DocumentSnapshot d : qSnap.getDocuments()) {
                                Course c = d.toObject(Course.class);
                                if (c != null) {
                                    c.setId(d.getId());
                                    c.setFolderId(fid);
                                    allCourses.add(c);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this,
                                    "Lỗi load courses từ folder " + fid + ": " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                tasks.add(t);
            }

            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                    .addOnCompleteListener(task -> {
                        adapter.setCourses(allCourses);
                    });
        }


        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 6. Bắt sự kiện nút "Thêm"
        btnAddSelected.setOnClickListener(v -> {
            List<String> toAdd = new ArrayList<>(adapter.getSelectedIds());
            if (toAdd.isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất 1 học phần", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentReference clsRef = db.collection("classes").document(classId);
            clsRef.update("courseIds", FieldValue.arrayUnion(toAdd.toArray()))
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Đã thêm học phần vào lớp", Toast.LENGTH_SHORT).show();
                        bsDialog.dismiss();
                        loadCourses();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Lỗi thêm học phần: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // 7. Show Bottom Sheet
        bsDialog.show();
    }




}
