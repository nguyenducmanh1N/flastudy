package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Folder;
import com.example.myapplication.model.Vocabulary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateCourseActivity extends AppCompatActivity {

    public static final String EXTRA_FOLDER_ID = "folderId";
    private LinearLayout containerTerms;
    private ImageButton btnAdd;
    private EditText inputCourseTitle;
    private Button addFolderButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String folderId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        inputCourseTitle = findViewById(R.id.inputTitle);



        containerTerms = findViewById(R.id.containerTerms);

        btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> addTermLayout());
        addTermLayout(); // khởi tạo item đầu tiên

        ImageView btnBack = findViewById(R.id.btnCloseCreateCourse);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveCourse());

        // Firestore + Auth
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        folderId = getIntent().getStringExtra("folderId");
        if (folderId == null) {
            Toast.makeText(this, "Không xác định được thư mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


    }

//    private void addTermLayout() {
//        View termView = LayoutInflater.from(this).inflate(R.layout.item_term, containerTerms, false);
//        containerTerms.addView(termView);
//    }
    private void addTermLayout() {
        View termView = LayoutInflater.from(this)
                .inflate(R.layout.item_term, containerTerms, false);
        ImageButton btnRemove = termView.findViewById(R.id.btnRemoveTerm);
        btnRemove.setOnClickListener(v -> containerTerms.removeView(termView));
        containerTerms.addView(termView);
    }


    private void saveCourse() {
        String title = inputCourseTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Nhập tiêu đề Course", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tập hợp vocabulary
        List<Vocabulary> vocabList = new ArrayList<>();
        for (int i = 0; i < containerTerms.getChildCount(); i++) {
            View view = containerTerms.getChildAt(i);
            EditText edtTerm       = view.findViewById(R.id.edtTerm);
            EditText edtDefinition = view.findViewById(R.id.edtDefinition);

            String word    = edtTerm.getText().toString().trim();
            String meaning = edtDefinition.getText().toString().trim();
            if (!word.isEmpty() && !meaning.isEmpty()) {
                vocabList.add(new Vocabulary(word, meaning, ""));
            }
        }
        if (vocabList.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 thuật ngữ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo Course object
        long createdAt = System.currentTimeMillis();
        Course course = new Course(title, createdAt, vocabList);

        String uid      = currentUser.getUid();
        String courseId = db.collection("users").document(uid)
                .collection("folders")
                .document(folderId)
                .collection("courses")
                .document()
                .getId();
        course.setId(courseId);

        // Path gốc đến course
        DocumentReference courseRef = db.collection("users").document(uid)
                .collection("folders")
                .document(folderId)
                .collection("courses")
                .document(courseId);

        // 1. Ghi Course
        courseRef.set(course)
                .addOnSuccessListener(aVoid -> {
                    // 2. Nếu thành công, bắt đầu ghi batch cho các Vocabulary
                    WriteBatch batch = db.batch();
                    CollectionReference vocabCol = courseRef.collection("vocabularies");
                    for (Vocabulary vocab : vocabList) {
                        DocumentReference vocabRef = vocabCol.document();
                        vocab.setId(vocabRef.getId());
                        batch.set(vocabRef, vocab);
                    }
                    // 3. Commit batch
                    batch.commit()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Tạo Course và lưu từ vựng thành công", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Lỗi khi lưu từ vựng: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu Course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void fetchSuggestions(String input, Consumer<List<String>> callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.datamuse.com/sug?s=" + input;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONArray jsonArray = new JSONArray(response.body().string());
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        suggestions.add(jsonArray.getJSONObject(i).getString("word"));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(suggestions));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });




    }


}
