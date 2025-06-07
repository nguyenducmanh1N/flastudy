package com.example.myapplication.utils;

import android.util.Log;

import com.example.myapplication.model.Folder;
import com.example.myapplication.model.Class;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

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

    public String createFolderBlocking(String folderName) {
        if (currentUser == null) {
            return "Lỗi: Người dùng chưa đăng nhập";
        }

        String uid = currentUser.getUid();
        String creater = currentUser.getEmail();

        String folderId = db.collection("users")
                .document(uid)
                .collection("folders")
                .document()
                .getId();

        Folder folder = new Folder(folderName, System.currentTimeMillis(), 0, creater);
        folder.setId(folderId);

        try {
            // Tasks.await trả về Void khi thành công
            Tasks.await(db.collection("users")
                    .document(uid)
                    .collection("folders")
                    .document(folderId)
                    .set(folder, SetOptions.merge()));  // SetOptions.merge() để tránh ghi đè toàn bộ document nếu cần

            return "Tạo thư mục " + folderName + " thành công"; // trả về id folder khi thành công
        } catch (Exception e) {
            Log.e("Firestore", "Lỗi khi tạo thư mục: ", e);
            return "Tạo thư mục " + folderName + " thất bại:";
        }
    }

    public String createClassBlocking(String name) {
        if (currentUser == null) {
            return "Lỗi: Bạn chưa đăng nhập";
        }

        String creator = currentUser.getEmail();

        DocumentReference classRef = db.collection("classes").document();
        String classId = classRef.getId();

        // Tạo đối tượng Class
        Class cls = new Class();
        cls.setId(classId);
        cls.setName(name);
        cls.setDescription(""); // fallback
        cls.setCreater(creator);
        cls.setAllowMembersToAdd(true);

        ArrayList<String> members = new ArrayList<>();
        members.add(creator);
        cls.setMembers(members);

        cls.setFolderIds(new ArrayList<>());
        cls.setCourseIds(new ArrayList<>());

        try {
            Tasks.await(classRef.set(cls));
            return "Tạo lớp " + name + " thành công";
        } catch (Exception e) {
            Log.e("Firestore", "Lỗi tạo lớp học: ", e);
            return "Tạo lớp " + name + " thất bại:";
        }
    }



}