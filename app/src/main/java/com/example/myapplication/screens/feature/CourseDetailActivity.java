package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.adapter.VocabAdapter;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Vocabulary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseDetailActivity extends AppCompatActivity {
    public static final String EXTRA_FOLDER_ID = "folderId";
    public static final String EXTRA_COURSE_ID = "courseId";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String folderId, courseId;

    private TextView tvTitle, tvCreatedAt;

    private ImageView btnBack,btnAdd,btnMenu;
    private ViewPager2 vpCards;
    private VocabAdapter vocabAdapter;
    private List<Vocabulary> vocabList = new ArrayList<>();
    private LinearLayout dotsContainer;
    private ImageView[] dots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);

        tvTitle = findViewById(R.id.tvCourseName);
        tvCreatedAt = findViewById(R.id.tvCourseDate);
        vpCards = findViewById(R.id.vpCards);

        vocabAdapter = new VocabAdapter(this, vocabList);
        vpCards.setAdapter(vocabAdapter);

        dotsContainer = findViewById(R.id.dotsContainer);
//        setupDots(vocabList.size());
//        vpCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                updateDots(position);
//            }
//        });


        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadCourseDetail();
    }

    private void loadCourseDetail() {
        db.collection("users")
                .document(currentUser.getUid())
                .collection("folders")
                .document(folderId)
                .collection("courses")
                .document(courseId)
                .get()
                .addOnSuccessListener(doc -> {
                    Course c = doc.toObject(Course.class);
                    if (c != null) {
                        tvTitle.setText(c.getTitle());
                        String date = new SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", Locale.getDefault()
                        ).format(new Date(c.getCreatedAt()));
                        tvCreatedAt.setText(date);

                        vocabList.clear();
                        vocabList.addAll(c.getVocabularyList());
                        vocabAdapter.notifyDataSetChanged();
                        setupDots(vocabList.size());
                        vpCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                updateDots(position);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }
    private void setupDots(int count) {
        dots = new ImageView[count];
        dotsContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(getDrawable(R.drawable.dot_unselected));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(8, 0, 8, 0);
            dotsContainer.addView(dots[i], lp);
        }
        // highlight dot đầu
        if (count > 0) {
            dots[0].setImageDrawable(getDrawable(R.drawable.dot_selected));
        }
    }

    private void updateDots(int currentPosition) {
        for (int i = 0; i < dots.length; i++) {
            int res = (i == currentPosition
                    ? R.drawable.dot_selected
                    : R.drawable.dot_unselected);
            dots[i].setImageDrawable(getDrawable(res));
        }
    }
}