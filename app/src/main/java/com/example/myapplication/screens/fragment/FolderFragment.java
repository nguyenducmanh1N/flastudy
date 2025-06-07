package com.example.myapplication.screens.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class FolderFragment extends Fragment {

    private TabLayout tabLayout;
    private ScrollView coursesWrapper, foldersWrapper, classesWrapper;
    private LinearLayout courseContainer, folderContainer, classContainer;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folder, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        coursesWrapper = view.findViewById(R.id.coursesWrapper);
        foldersWrapper = view.findViewById(R.id.foldersWrapper);
        classesWrapper = view.findViewById(R.id.classesWrapper);
        courseContainer = view.findViewById(R.id.courseContainer);
        folderContainer = view.findViewById(R.id.folderContainer);
        classContainer = view.findViewById(R.id.classContainer);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userId = currentUser.getUid();

        // default tab
        loadCourses();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                coursesWrapper.setVisibility(pos==0?View.VISIBLE:View.GONE);
                foldersWrapper.setVisibility(pos==1?View.VISIBLE:View.GONE);
                classesWrapper.setVisibility(pos==2?View.VISIBLE:View.GONE);
                switch(pos) {
                    case 0: loadCourses(); break;
                    case 1: loadFolders(); break;
                    case 2: loadClasses(); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadCourses() {
        courseContainer.removeAllViews();
        db.collection("users").document(userId)
                .collection("courses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q) {
                        Course c = doc.toObject(Course.class);
                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_course_vertical, courseContainer, false);
                        TextView tvTitle = item.findViewById(R.id.courseTitle);
                        TextView tvCount = item.findViewById(R.id.courseTermCount);
                        TextView tvCreater = item.findViewById(R.id.courseCreater);
                        tvTitle.setText(c.getTitle());
                        int cnt = c.getVocabularyList()!=null?c.getVocabularyList().size():0;
                        tvCount.setText(cnt+" thuật ngữ");
                        tvCreater.setText("Người tạo: "+c.getCreater());
                        item.setOnClickListener(v -> {
                            Intent i = new Intent(getContext(), CourseDetailActivity.class);
                            i.putExtra("courseId", doc.getId());
                            startActivity(i);
                        });
                        courseContainer.addView(item);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Lỗi load học phần: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFolders() {
        folderContainer.removeAllViews();
        db.collection("users").document(userId)
                .collection("folders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q) {
                        Folder f = doc.toObject(Folder.class);
                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_folder_vertical_2, folderContainer, false);
                        TextView tvTitle = item.findViewById(R.id.tvTitle);
                        TextView tvCount = item.findViewById(R.id.tvCount);
                        TextView tvCreater = item.findViewById(R.id.tvCreater);
                        tvTitle.setText(f.getName());
                        int cnt = f.getCount();
                        tvCount.setText(cnt+" mục");
                        tvCreater.setText("Người tạo: "+f.getCreater());
                        item.setOnClickListener(v -> {
                            Intent i = new Intent(getContext(), FolderDetailActivity.class);
                            i.putExtra("folderId", doc.getId());
                            startActivity(i);
                        });
                        folderContainer.addView(item);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Lỗi load thư mục: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadClasses() {
        classContainer.removeAllViews();
        db.collection("classes")
                .whereArrayContains("members", currentUser.getEmail())
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q) {
                        Class cls = doc.toObject(Class.class);
                        View item = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_class_vertical, classContainer, false);
                        // similar binding for class item...
                        TextView tvTitle = item.findViewById(R.id.classTitle);
                        TextView tvFCount = item.findViewById(R.id.classFolderCount);
                        TextView tvCCount = item.findViewById(R.id.classCourseCount);
                        TextView tvCreater = item.findViewById(R.id.classCreater);
                        tvTitle.setText(cls.getName());
                        int cnt = cls.getFolderIds().size();
                        int cct = cls.getCourseIds().size();
                        tvFCount.setText(cnt+" mục");
                        tvCCount.setText(cct+ " mucc");
                        tvCreater.setText("Người tạo: "+cls.getCreater());
                        item.setOnClickListener(v -> {
                            Intent i = new Intent(getContext(), ClassDetailActivity.class);
                            i.putExtra("classId", doc.getId());
                            startActivity(i);
                        });
                        classContainer.addView(item);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Lỗi load lớp học: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
