package com.example.myapplication.screens.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ClassAdapter;
import com.example.myapplication.adapter.CourseAdapter;
import com.example.myapplication.adapter.FolderAdapter;
import com.example.myapplication.model.ClassModel;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView folderRecyclerView, courseRecyclerView, classRecyclerView;
    private FolderAdapter folderAdapter;
    private CourseAdapter courseAdapter;
    private ClassAdapter classAdapter;

    private TextView tvEmptyFolders, tvEmptyCourses, tvEmptyClasses;

    private List<Folder> folderList = new ArrayList<>();
    private List<Course> courseList = new ArrayList<>();
    private List<ClassModel> classList = new ArrayList<>();

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Init Firestore & User
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Empty state TextViews
        tvEmptyFolders = view.findViewById(R.id.tvEmptyFolders);
        tvEmptyCourses = view.findViewById(R.id.tvEmptyCourses);
        tvEmptyClasses = view.findViewById(R.id.tvEmptyClasses);

        // Folder RecyclerView
        folderRecyclerView = view.findViewById(R.id.folderRecyclerView);
        folderRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        folderAdapter = new FolderAdapter(folderList);
        folderAdapter.setOnFolderClickListener(folder -> {
            Intent intent = new Intent(getContext(), FolderDetailActivity.class);
            intent.putExtra("folderId", folder.getId());
            startActivity(intent);
        });
        folderRecyclerView.setAdapter(folderAdapter);

        // Course RecyclerView
        courseRecyclerView = view.findViewById(R.id.courseRecyclerView);
        courseRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        courseAdapter = new CourseAdapter(courseList, course -> {
            Intent intent = new Intent(getContext(), CourseDetailActivity.class);
            intent.putExtra("folderId", course.getFolderId());
            intent.putExtra("courseId", course.getId());
            startActivity(intent);
        });

        courseRecyclerView.setAdapter(courseAdapter);

        // Class RecyclerView
        classRecyclerView = view.findViewById(R.id.classRecyclerView);
        classRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        classAdapter = new ClassAdapter(classList, cls -> {
            Intent i = new Intent(getContext(), ClassDetailActivity.class);
            i.putExtra("classId", cls.getId());
            startActivity(i);
        });
        classRecyclerView.setAdapter(classAdapter);

        loadData();
        return view;
    }

    private void loadData() {
        if (currentUser == null) return;

        // Load folders
        db.collection("users")
                .document(currentUser.getUid())
                .collection("folders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    folderList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Folder f = doc.toObject(Folder.class);
                        folderList.add(f);
                    }
                    folderAdapter.notifyDataSetChanged();

                    // Show/hide empty view
                    if (folderList.isEmpty()) {
                        tvEmptyFolders.setVisibility(View.VISIBLE);
                        folderRecyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyFolders.setVisibility(View.GONE);
                        folderRecyclerView.setVisibility(View.VISIBLE);
                    }

                    // Load courses for each folder
                    courseList.clear();
                    for (Folder f : folderList) {
                        db.collection("users")
                                .document(currentUser.getUid())
                                .collection("folders")
                                .document(f.getId())
                                .collection("courses")
                                .get()
                                .addOnSuccessListener(csnap -> {
                                    int courseCount = csnap.size();
                                    f.setCount(courseCount);      // dùng trường 'count' đã có sẵn trong model
                                    folderAdapter.notifyItemChanged(folderList.indexOf(f));
                                    for (DocumentSnapshot cDoc : csnap) {
                                        Course c = cDoc.toObject(Course.class);
                                        c.setFolderId(f.getId());
                                        courseList.add(c);
                                    }

                                    courseAdapter.notifyDataSetChanged();

                                    // Show/hide courses empty view
                                    if (courseList.isEmpty()) {
                                        tvEmptyCourses.setVisibility(View.VISIBLE);
                                        courseRecyclerView.setVisibility(View.GONE);


                                    } else {
                                        tvEmptyCourses.setVisibility(View.GONE);
                                        courseRecyclerView.setVisibility(View.VISIBLE);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi load folders: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Load classes
        db.collection("classes")
                .whereArrayContains("members", currentUser.getEmail())
                .get()
                .addOnSuccessListener(docs -> {
                    classList.clear();
                    for (DocumentSnapshot doc : docs) {
                        ClassModel cls = doc.toObject(ClassModel.class);
                        cls.setId(doc.getId());
                        classList.add(cls);
                    }
                    classAdapter.notifyDataSetChanged();

                    // Show/hide classes empty view
                    if (classList.isEmpty()) {
                        tvEmptyClasses.setVisibility(View.VISIBLE);
                        classRecyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyClasses.setVisibility(View.GONE);
                        classRecyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi load lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
