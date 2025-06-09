package com.example.myapplication.screens.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.model.Class;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Folder;
import com.example.myapplication.screens.feature.ClassDetailActivity;
import com.example.myapplication.screens.feature.CourseDetailActivity;
import com.example.myapplication.screens.feature.FolderDetailActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FolderFragment extends Fragment {
    private TabLayout tabLayout;
    private EditText etFilter;
    private ScrollView coursesWrapper, foldersWrapper, classesWrapper;
    private LinearLayout courseContainer, folderContainer, classContainer;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String userId;
    private String currentFilter = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folder, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        etFilter = view.findViewById(R.id.etFilter);
        coursesWrapper = view.findViewById(R.id.coursesWrapper);
        foldersWrapper = view.findViewById(R.id.foldersWrapper);
        classesWrapper = view.findViewById(R.id.classesWrapper);
        courseContainer = view.findViewById(R.id.courseContainer);
        folderContainer = view.findViewById(R.id.folderContainer);
        classContainer = view.findViewById(R.id.classContainer);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userId = currentUser != null ? currentUser.getUid() : null;

        setupFilterListener();

        // default tab
        loadCourses();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                coursesWrapper.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                foldersWrapper.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                classesWrapper.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
                if (pos == 0) loadCourses();
                else if (pos == 1) loadFolders();
                else loadClasses();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void setupFilterListener() {
        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentFilter = s.toString().trim().toLowerCase();
                int pos = tabLayout.getSelectedTabPosition();
                if (pos == 0) loadCourses();
                else if (pos == 1) loadFolders();
                else loadClasses();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCourses() {
        courseContainer.removeAllViews();
        if (userId == null) return;

        db.collection("users").document(userId)
                .collection("folders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(folderSnapshot -> {
                    for (DocumentSnapshot folderDoc : folderSnapshot.getDocuments()) {
                        String folderId = folderDoc.getId();
                        db.collection("users").document(userId)
                                .collection("folders").document(folderId)
                                .collection("courses")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(courseSnapshot -> {
                                    for (DocumentSnapshot courseDoc : courseSnapshot.getDocuments()) {
                                        Course c = courseDoc.toObject(Course.class);
                                        if (c == null) continue;
                                        if (!c.getTitle().toLowerCase().contains(currentFilter)) continue;

                                        View item = LayoutInflater.from(getContext())
                                                .inflate(R.layout.item_course_vertical, courseContainer, false);
                                        TextView tvTitle = item.findViewById(R.id.courseTitle);
                                        TextView tvCount = item.findViewById(R.id.courseTermCount);
                                        TextView tvCreater = item.findViewById(R.id.courseCreater);

                                        tvTitle.setText(c.getTitle());
                                        int cnt = c.getVocabularyList() != null ? c.getVocabularyList().size() : 0;
                                        tvCount.setText(cnt + " thuật ngữ");
                                        tvCreater.setText("Người tạo: " + c.getCreater());

                                        item.setOnClickListener(v -> {
                                            Intent i = new Intent(getContext(), CourseDetailActivity.class);
                                            i.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, courseDoc.getId());
                                            i.putExtra(CourseDetailActivity.EXTRA_FOLDER_ID, folderId);
                                            startActivity(i);
                                        });

                                        courseContainer.addView(item);
                                    }
                                });
                    }
                    if (courseContainer.getChildCount() == 0) {
                        Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFolders() {
        folderContainer.removeAllViews();
        if (userId == null) return;

        db.collection("users").document(userId)
                .collection("folders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        String folderId = doc.getId();

                        if (f == null) continue;
                        if (!f.getName().toLowerCase().contains(currentFilter)) continue;

                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_folder_vertical_2, folderContainer, false);
                        TextView tvTitle = item.findViewById(R.id.tvTitle);
                        TextView tvCount = item.findViewById(R.id.tvCount);
                        TextView tvCreater = item.findViewById(R.id.tvCreater);

                        tvTitle.setText(f.getName());
                        tvCreater.setText("Người tạo: " + f.getCreater());
                        tvCount.setText("Đang tải...");

                        item.setOnClickListener(v -> {
                            Intent i = new Intent(getContext(), FolderDetailActivity.class);
                            i.putExtra("folderId", folderId);
                            startActivity(i);
                        });

                        folderContainer.addView(item);

                        // update count
                        db.collection("users").document(userId)
                                .collection("folders").document(doc.getId())
                                .collection("courses").get()
                                .addOnSuccessListener(cSnap ->
                                        tvCount.setText(cSnap.size() + " mục"));
                    }
                    if (folderContainer.getChildCount() == 0) {
                        Toast.makeText(getContext(), "Không tìm thấy thư mục.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadClasses() {
        classContainer.removeAllViews();
        String email = currentUser != null ? currentUser.getEmail() : null;
        if (email == null) return;

        db.collection("classes")
                .whereArrayContains("members", email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Class cls = doc.toObject(Class.class);
                        if (cls == null) continue;
                        if (!cls.getName().toLowerCase().contains(currentFilter)) continue;

                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_class_vertical, classContainer, false);
                        TextView tvTitle = item.findViewById(R.id.classTitle);
                        TextView tvFCount = item.findViewById(R.id.classFolderCount);
                        TextView tvCCount = item.findViewById(R.id.classCourseCount);
                        TextView tvCreater = item.findViewById(R.id.classCreater);

                        tvTitle.setText(cls.getName());
                        tvCreater.setText("Người tạo: " + cls.getCreater());

                        // counts
                        db.collection("classes").document(doc.getId())
                                .collection("folders").get()
                                .addOnSuccessListener(fs ->
                                        tvFCount.setText(fs.size() + " mục"));
                        db.collection("classes").document(doc.getId())
                                .collection("courses").get()
                                .addOnSuccessListener(cs ->
                                        tvCCount.setText(cs.size() + " mục"));

                        item.setOnClickListener(v -> {
                            Intent i = new Intent(getContext(), ClassDetailActivity.class);
                            i.putExtra("classId", doc.getId());
                            startActivity(i);
                        });


                        classContainer.addView(item);
                    }
                    if (classContainer.getChildCount() == 0) {
                        Toast.makeText(getContext(), "Không tìm thấy lớp học.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresh when returning
        int pos = tabLayout.getSelectedTabPosition();
        if (pos == 0) loadCourses();
        else if (pos == 1) loadFolders();
        else loadClasses();
    }
}
