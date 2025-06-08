package com.example.myapplication.screens.feature;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.example.myapplication.screens.feature.learn.AIQuizActivity;
import com.example.myapplication.screens.feature.learn.FillActivity;
import com.example.myapplication.screens.feature.learn.FlashCardActivity;
import com.example.myapplication.screens.feature.learn.ListenFillActivity;
import com.example.myapplication.screens.feature.learn.ListenQuizActivity;
import com.example.myapplication.screens.feature.learn.QuizActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import androidx.appcompat.app.AlertDialog;

public class CourseDetailActivity extends AppCompatActivity {
    public static final String EXTRA_FOLDER_ID = "folderId";
    public static final String EXTRA_COURSE_ID = "courseId";
    public static final String EXTRA_IS_EDIT = "isEdit";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String folderId, courseId, from, classId;

    private TextView tvTitle, tvCreatedAt;

    private ImageView btnBack,btnAdd,btnMenu;
    private ViewPager2 vpCards;
    private VocabAdapter vocabAdapter;
    private List<Vocabulary> vocabList = new ArrayList<>();
    private LinearLayout dotsContainer ,flashCard,learn,graft,listen,d;
    private ImageView[] dots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        from = getIntent().getStringExtra("from");
        classId = getIntent().getStringExtra("classId");

        tvTitle = findViewById(R.id.tvCourseName);
        tvCreatedAt = findViewById(R.id.tvCourseDate);
        vpCards = findViewById(R.id.vpCards);
        btnMenu = findViewById(R.id.btnMenu);



        btnMenu.setOnClickListener(v -> showAddOptionsCourse());
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

        flashCard = findViewById(R.id.btnFlashCard);
        flashCard.setOnClickListener(v -> {
            Intent intent = new Intent(CourseDetailActivity.this, FlashCardActivity.class);
            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            intent.putExtra("courseId", courseId);
            startActivity(intent);
        });


        if ("class".equals(from)) {
            btnMenu.setVisibility(View.GONE);
            loadCourseDetailFromClass();
        } else {
            loadCourseDetail();
        }


        LinearLayout learn = findViewById(R.id.learn);
        learn.setOnClickListener(v -> showAddOptionsLearn());


//        learn.setOnClickListener(v -> {
//            Intent intent = new Intent(CourseDetailActivity.this, QuizActivity.class);
//            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
//            startActivity(intent);
//        });

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

    private void loadCourseDetailFromClass() {
        db.collection("classes")
                .document(classId)
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

    private void showAddOptionsCourse() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.course_bottom_sheet_add_options, null);
        bottomSheetDialog.setContentView(view);

        TextView edit = view.findViewById(R.id.edit);
        TextView addToFolder = view.findViewById(R.id.addToFolder);
        TextView addToClass = view.findViewById(R.id.addToClass);
        TextView share = view.findViewById(R.id.share);
        TextView delete = view.findViewById(R.id.delete);

        edit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateCourseActivity.class);
            intent.putExtra(CreateCourseActivity.EXTRA_FOLDER_ID, folderId);
            intent.putExtra(CreateCourseActivity.EXTRA_COURSE_ID, courseId);
            intent.putExtra(CreateCourseActivity.EXTRA_IS_EDIT, true);
            startActivity(intent);
        });

        addToFolder.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateFolderActivity.class);
            startActivity(intent);
        });

        addToClass.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateClassActivity.class);
            startActivity(intent);
        });
        share.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateClassActivity.class);
            startActivity(intent);
        });
        delete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa khóa học này không? Hành động này không thể hoàn tác.")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        deleteCourse();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        bottomSheetDialog.show();
    }

    private void deleteCourse() {
        String uid = currentUser.getUid();
        if (uid == null || folderId == null || courseId == null) {
            Toast.makeText(this, "Không thể xác định course để xóa.", Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference courseRef = db.collection("users")
                .document(uid)
                .collection("folders")
                .document(folderId)
                .collection("courses")
                .document(courseId);
        courseRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Xóa khóa học thành công.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showAddOptionsLearn() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.learn_bottom_sheet_add_options, null);
        bottomSheetDialog.setContentView(view);

        TextView quiz = view.findViewById(R.id.quiz);
        TextView fill = view.findViewById(R.id.fill);
        TextView hear = view.findViewById(R.id.hear);
        TextView aiQuiz = view.findViewById(R.id.aiQuiz);
        TextView hearandfill = view.findViewById(R.id.hearandfill);
        //TextView delete = view.findViewById(R.id.test);

        quiz.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, QuizActivity.class);

            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            startActivity(intent);
        });

        fill.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, FillActivity.class);
            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            startActivity(intent);
        });

        hear.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, ListenQuizActivity.class);
            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            startActivity(intent);
        });

        hearandfill.setOnClickListener(v ->{
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, ListenFillActivity.class);
            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            startActivity(intent);
        } );

        aiQuiz.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, AIQuizActivity.class);
            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
            intent.putExtra("courseId", courseId);
            intent.putExtra("folderId",folderId);
            startActivity(intent);
        });
//        delete.setOnClickListener(v -> {
//            bottomSheetDialog.dismiss();
//            Intent intent = new Intent(this, CreateClassActivity.class);
//            intent.putParcelableArrayListExtra("vocabList", new ArrayList<>(vocabList));
//            startActivity(intent);
//        });


        bottomSheetDialog.show();
    }
}