package com.example.myapplication.utils;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser currentUser = auth.getCurrentUser();

    public List<String> getFolderNamesBlocking() {
        List<String> folderNames = new ArrayList<>();
        try {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("folders")
                            .get()
            );
            for (DocumentSnapshot doc : snapshot) {
                String name = doc.getString("name");
                if (name != null) folderNames.add(name);
            }
        } catch (Exception e) {
            // Ghi log hoặc thêm thông báo lỗi vào danh sách nếu cần
            Log.e("Firestore", "Lỗi khi lấy danh sách thư mục: ", e);
            folderNames.add("Lỗi: " + e.getMessage());
        }
        return folderNames;
    }

    public List<String> getClassNamesBlocking() {
        List<String> classNames = new ArrayList<>();
        try {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("classes")
                            .whereArrayContains("members", currentUser.getEmail())
                            .get()
            );
            for (DocumentSnapshot doc : snapshot) {
                String name = doc.getString("name");
                if (name != null) classNames.add(name);
            }
        } catch (Exception e) {
            // Ghi log hoặc thêm thông báo lỗi vào danh sách nếu cần
            Log.e("Firestore", "Lỗi khi lấy danh sách lớp học: ", e);
            classNames.add("Lỗi: " + e.getMessage());

        }
        return classNames;
    }

    public String findFolderIdByNameBlocking(String folderName) {
        String folderId = null;
        try {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("folders")
                            .whereEqualTo("name", folderName)
                            .get()
            );
            for (DocumentSnapshot doc : snapshot) {
                folderId = doc.getId();
                break;
            }
        } catch (Exception e) {
            // Ghi log hoặc thêm thông báo lỗi vào danh sách nếu cần
            Log.e("Firestore", "Lỗi khi tìm kiếm thư mục: ", e);
            folderId = "Lỗi: " + e.getMessage();
        }
        return folderId;
    }

    public String findClassIdByNameBlocking(String className) {
        String classId = null;

        try {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("classes")
                            .whereArrayContains("members", currentUser.getEmail())
                            .whereEqualTo("name", className)
                            .get()
            );
            for (DocumentSnapshot doc : snapshot) {
                classId = doc.getId();
                break;
            }
        } catch (Exception e) {
            // Ghi log hoặc thêm thông báo lỗi vào danh sách nếu cần
            Log.e("Firestore", "Lỗi khi tìm kiếm lớp học: ", e);
            classId = "Lỗi: " + e.getMessage();

        }
        return classId;
    }

}
