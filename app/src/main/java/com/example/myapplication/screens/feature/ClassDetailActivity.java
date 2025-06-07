package com.example.myapplication.screens.feature;

import static com.example.myapplication.screens.feature.CreateCourseActivity.EXTRA_IS_EDIT;
import static com.example.myapplication.screens.feature.CreateCourseActivity.EXTRA_FOLDER_ID;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.example.myapplication.adapter.CourseSelectAdapter;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvCourseCount, tvOwner;
    private ImageView btnClose, btnMenu;
    private TabLayout tabLayout;

    private ScrollView folderWrapper, coursesWrapper, memberWrapper;
    private LinearLayout folderContainer, courseContainer;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser;
    private String classId;
    private Class cls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_class_detail);

        tvTitle       = findViewById(R.id.titleText);
        tvCourseCount = findViewById(R.id.subTitleText);
        tvOwner       = findViewById(R.id.subTitleText2);
        btnClose      = findViewById(R.id.btnExit);
        btnMenu       = findViewById(R.id.btnMenu);
        tabLayout     = findViewById(R.id.tvTabLayout);
        folderWrapper = findViewById(R.id.containerFolders);
        coursesWrapper= findViewById(R.id.containerCoursesWrapper);
        memberWrapper = findViewById(R.id.containerMembers);
        folderContainer = findViewById(R.id.folderContainer);
        courseContainer = findViewById(R.id.courseContainer);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.detail_class_layout),
                (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                }
        );

        btnClose.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> showOptions());

        classId = getIntent().getStringExtra("classId");
        if (classId == null) {
            Toast.makeText(this, "Không tìm thấy lớp", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                folderWrapper.setVisibility(View.GONE);
                coursesWrapper.setVisibility(View.GONE);
                memberWrapper.setVisibility(View.GONE);
                switch (tab.getPosition()) {
                    case 0:
                        folderWrapper.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        coursesWrapper.setVisibility(View.VISIBLE);
                        loadCourses();
                        break;
                    case 2:
                        memberWrapper.setVisibility(View.VISIBLE);
                        loadMembers();
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        folderWrapper.setVisibility(View.VISIBLE);
        coursesWrapper.setVisibility(View.GONE);
        memberWrapper.setVisibility(View.GONE);

        loadClassDetail();
    }

    private void loadClassDetail() {
        db.collection("classes").document(classId).get()
                .addOnSuccessListener(doc -> {
                    cls = doc.toObject(Class.class);
                    if (cls == null) {
                        Toast.makeText(this, "Lớp không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    tvTitle.setText(cls.getName());
                    tvOwner.setText("Người tạo: " + cls.getCreater());
                    int count = cls.getCourseIds() != null ? cls.getCourseIds().size() : 0;
                    tvCourseCount.setText(count + " học phần");

                    loadFolders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadFolders() {
        folderContainer.removeAllViews();

        db.collection("classes")
                .document(classId)
                .collection("folders")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String fid = doc.getId();
                        String name     = doc.getString("name");
                        String creater  = doc.getString("creater");

                        List<Map<String,Object>> courses =
                                (List<Map<String,Object>>) doc.get("courses");
                        int count = courses != null ? courses.size() : 0;

                        View item = LayoutInflater.from(this)
                                .inflate(R.layout.item_folder_vertical_2, folderContainer, false);
                        TextView tvTitle   = item.findViewById(R.id.tvTitle);
                        TextView tvCount   = item.findViewById(R.id.tvCount);
                        TextView tvCreater = item.findViewById(R.id.tvCreater);
                        tvTitle.setText(name);
                        tvCount.setText(count + " mục");
                        tvCreater.setText("Người tạo: " + creater);

                        item.setOnClickListener(v -> {
                            Intent i = new Intent(this, FolderDetailActivity.class);
                            i.putExtra("folderId", fid);
                            startActivity(i);
                        });

                        folderContainer.addView(item);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi load folders: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void loadCourses() {
        courseContainer.removeAllViews();

        db.collection("classes")
                .document(classId)
                .collection("courses")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String cid      = doc.getId();
                        String title    = doc.getString("title");
                        String creater  = doc.getString("creater");

                        List<Map<String,Object>> vocs =
                                (List<Map<String,Object>>) doc.get("vocabularies");
                        int termCount = vocs != null ? vocs.size() : 0;

                        View item = LayoutInflater.from(this)
                                .inflate(R.layout.item_course_vertical, courseContainer, false);
                        TextView tvTitle   = item.findViewById(R.id.courseTitle);
                        TextView tvCount   = item.findViewById(R.id.courseTermCount);
                        TextView tvCreater = item.findViewById(R.id.courseCreater);

                        tvTitle.setText(title);
                        tvCount.setText(termCount + " thuật ngữ");
                        tvCreater.setText("Người tạo: " + creater);

                        item.setOnClickListener(v -> {
                            Intent i = new Intent(this, CourseDetailActivity.class);
                            i.putExtra("courseId", cid);

                            i.putExtra("folderId", doc.getString("folderId"));
                            startActivity(i);
                        });

                        courseContainer.addView(item);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi load courses: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
            bs.dismiss(); showAddFoldersToClass();
        });
        view.findViewById(R.id.addCourse).setOnClickListener(v -> {
            bs.dismiss(); showAddCoursesToClass();
        });
        view.findViewById(R.id.addMember).setOnClickListener(v -> {
            bs.dismiss(); showAddMemberDialog();
        });
        bs.show();
    }

    private void showAddMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mời thành viên vào lớp");
        final EditText input = new EditText(this);
        input.setHint("Email thành viên");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Gửi lời mời", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }


            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(q -> {
                        if (q.isEmpty()) {
                            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot udoc = q.getDocuments().get(0);
                        String uid = udoc.getId();

                        long now = System.currentTimeMillis();

                        Map<String, Object> invite = new HashMap<>();
                        invite.put("classId",   classId);
                        invite.put("email",     email);
                        invite.put("from",      currentUser.getEmail());
                        invite.put("timestamp", now);
                        invite.put("accepted",  false);

                        db.collection("classes")
                                .document(classId)
                                .collection("invitations")
                                .add(invite)
                                .addOnSuccessListener(invRef -> {

                                    Map<String, Object> note = new HashMap<>(invite);
                                    note.put("type", "invite");

                                    db.collection("users")
                                            .document(uid)
                                            .collection("notifications")
                                            .add(note)
                                            .addOnSuccessListener(nRef -> {
                                                Toast.makeText(this, "Đã gửi lời mời thành công", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Lỗi gửi notification: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi tạo lời mời: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi tìm tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }


    private void loadMembers() {

        LinearLayout memContainer = memberWrapper.findViewById(R.id.containerMembers);
        memContainer.removeAllViews();
        List<String> emails = cls.getMembers();
        if (emails == null) return;
        for (String email : emails) {
            TextView tv = new TextView(this);
            tv.setText(email);
            tv.setPadding(8,8,8,8);
            memContainer.addView(tv);
        }
    }

    private void showAddFoldersToClass() {
        View sheet = getLayoutInflater().inflate(R.layout.bs_add_folders, null);
        BottomSheetDialog bsDialog = new BottomSheetDialog(this);
        bsDialog.setContentView(sheet);
        bsDialog.setCancelable(true);
        RecyclerView rv = sheet.findViewById(R.id.rvFolders);
        Button btn = sheet.findViewById(R.id.btnAddSelected);
        List<Folder> list = new ArrayList<>();
        FolderSelectAdapter adapter = new FolderSelectAdapter(list, ids -> btn.setEnabled(!ids.isEmpty()));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        db.collection("users").document(currentUser.getUid())
                .collection("folders").get()
                .addOnSuccessListener(q -> {
                    list.clear();
                    for (DocumentSnapshot d : q) {
                        Folder f = d.toObject(Folder.class);
                        f.setId(d.getId()); list.add(f);
                    }
                    adapter.notifyDataSetChanged();
                });
        btn.setOnClickListener(v -> {
            List<String> toAdd = new ArrayList<>(adapter.getSelectedIds());
            if (toAdd.isEmpty()) return;

            String uid = currentUser.getUid();

            for (String fid : toAdd) {
                DocumentReference srcFolderRef = db.collection("users")
                        .document(uid)
                        .collection("folders")
                        .document(fid);
                srcFolderRef.get().addOnSuccessListener(folderDoc -> {
                    Folder folder = folderDoc.toObject(Folder.class);
                    if (folder == null) return;


                    Map<String, Object> folderMap = new HashMap<>();
                    folderMap.put("id", fid);
                    folderMap.put("name", folder.getName());
                    folderMap.put("creater", folder.getCreater());
                    folderMap.put("createdAt", folder.getCreatedAt());

                    srcFolderRef.collection("courses")
                            .get().addOnSuccessListener(courseSnap -> {
                                List<Map<String,Object>> courseList = new ArrayList<>();

                                List<Task<?>> tasks = new ArrayList<>();
                                for (DocumentSnapshot cDoc : courseSnap.getDocuments()) {
                                    Course course = cDoc.toObject(Course.class);
                                    if (course == null) continue;
                                    String cid = cDoc.getId();


                                    Map<String,Object> courseMap = new HashMap<>();
                                    courseMap.put("id", cid);
                                    courseMap.put("title", course.getTitle());
                                    courseMap.put("creater", course.getCreater());

                                    Task<?> t = srcFolderRef
                                            .collection("courses").document(cid)
                                            .collection("vocabularies")
                                            .get().addOnSuccessListener(vSnap -> {
                                                List<Map<String,Object>> vocs = new ArrayList<>();
                                                for (DocumentSnapshot vDoc : vSnap) {
                                                    Map<String,Object> vMap = vDoc.getData();
                                                    vMap.put("id", vDoc.getId());
                                                    vocs.add(vMap);
                                                }
                                                courseMap.put("vocabularies", vocs);
                                            });
                                    tasks.add(t);
                                    courseList.add(courseMap);
                                }

                                Tasks.whenAllComplete(tasks)
                                        .addOnSuccessListener(__ -> {
                                            folderMap.put("courses", courseList);

                                            db.collection("classes")
                                                    .document(classId)
                                                    .collection("folders")
                                                    .document(fid)
                                                    .set(folderMap)
                                                    .addOnSuccessListener(a -> {
                                                        Toast.makeText(this, "Đã thêm folder "+fid, Toast.LENGTH_SHORT).show();
                                                        bsDialog.dismiss();
                                                        loadFolders();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Lỗi ghi folder vào lớp: "+e.getMessage(), Toast.LENGTH_SHORT).show()
                                                    );
                                        });
                            });
                });
            }
        });

        bsDialog.show();
    }

    private void showAddCoursesToClass() {
        View sheet = getLayoutInflater().inflate(R.layout.bs_add_courses, null);
        BottomSheetDialog bsDialog = new BottomSheetDialog(this);
        bsDialog.setContentView(sheet);
        bsDialog.setCancelable(true);

        EditText etSearch = sheet.findViewById(R.id.etSearchCourses);
        RecyclerView rvCourses = sheet.findViewById(R.id.rvCourses);
        Button btnAdd = sheet.findViewById(R.id.btnAddSelected);
        btnAdd.setEnabled(false);

        List<Course> allCourses = new ArrayList<>();
        CourseSelectAdapter adapter = new CourseSelectAdapter(allCourses, selectedIds ->
                btnAdd.setEnabled(!selectedIds.isEmpty())
        );
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(adapter);


        String uid = currentUser.getUid();
        db.collection("users")
                .document(uid)
                .collection("folders")
                .get()
                .addOnSuccessListener(folderSnap -> {
                    List<Task<?>> tasks = new ArrayList<>();
                    for (DocumentSnapshot fDoc : folderSnap.getDocuments()) {
                        String fid = fDoc.getId();

                        Task<?> t = db.collection("users")
                                .document(uid)
                                .collection("folders")
                                .document(fid)
                                .collection("courses")
                                .get()
                                .addOnSuccessListener(courseSnap -> {
                                    for (DocumentSnapshot cDoc : courseSnap.getDocuments()) {
                                        Course c = cDoc.toObject(Course.class);
                                        c.setId(cDoc.getId());
                                        c.setFolderId(fid);
                                        allCourses.add(c);
                                    }
                                });
                        tasks.add(t);
                    }

                    Tasks.whenAllComplete(tasks)
                            .addOnCompleteListener(__ -> {
                                adapter.setCourses(allCourses);
                                adapter.notifyDataSetChanged();

                                etSearch.addTextChangedListener(new TextWatcher() {
                                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        adapter.filter(s.toString());
                                    }
                                    @Override public void afterTextChanged(Editable s) {}
                                });
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thư mục: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        btnAdd.setOnClickListener(v -> {

            Set<String> toAddIds = adapter.getSelectedIds();
            if (toAddIds.isEmpty()) return;




            List<Course> selectedCourses = new ArrayList<>();
            for (Course c : allCourses) {
                if (toAddIds.contains(c.getId())) {
                    selectedCourses.add(c);
                }
            }


            for (Course course : selectedCourses) {
                String cid = course.getId();
                String fid = course.getFolderId();
                DocumentReference srcCourseRef = db.collection("users")
                        .document(uid)
                        .collection("folders")
                        .document(fid)
                        .collection("courses")
                        .document(cid);


                srcCourseRef.get().addOnSuccessListener(courseDoc -> {
                    Course fullCourse = courseDoc.toObject(Course.class);
                    if (fullCourse == null) return;


                    Map<String,Object> courseMap = new HashMap<>();
                    courseMap.put("id", cid);
                    courseMap.put("title", fullCourse.getTitle());
                    courseMap.put("creater", fullCourse.getCreater());



                    srcCourseRef.collection("vocabularies")
                            .get().addOnSuccessListener(vSnap -> {
                                List<Map<String,Object>> vocs = new ArrayList<>();
                                for (DocumentSnapshot vDoc : vSnap.getDocuments()) {
                                    Map<String,Object> vMap = vDoc.getData();
                                    vMap.put("id", vDoc.getId());
                                    vocs.add(vMap);
                                }
                                courseMap.put("vocabularies", vocs);


                                db.collection("classes")
                                        .document(classId)
                                        .collection("courses")
                                        .document(cid)
                                        .set(courseMap)
                                        .addOnSuccessListener(a -> {

                                            Toast.makeText(this, "Đã thêm học phần "+ fullCourse.getTitle(), Toast.LENGTH_SHORT).show();
                                            bsDialog.dismiss();
                                            loadCourses();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Lỗi ghi course: "+e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            });
                });
            }
        });



        bsDialog.show();
    }


}
