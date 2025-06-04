package com.example.myapplication.screens.feature.learn;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;
import com.example.myapplication.screens.feature.CourseDetailActivity;

import java.util.ArrayList;

public class AIQuizResultActivity extends AppCompatActivity {

    private TextView tvScore;
    private LinearLayout wrongListContainer;
    private Button btnFlashcards, btnCourseDetail;

    private ArrayList<Vocabulary> vocabList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aiquiz_result);

        tvScore = findViewById(R.id.tvScore);
        wrongListContainer = findViewById(R.id.wrongListContainer);
        btnFlashcards = findViewById(R.id.btnFlashcards);
        btnCourseDetail = findViewById(R.id.btnCourseDetail);

        int correctCount = getIntent().getIntExtra("correctCount", 0);
        int totalCount   = getIntent().getIntExtra("totalCount", 0);
        ArrayList<String> wrongWords = getIntent().getStringArrayListExtra("wrongWords");
        vocabList = getIntent().getParcelableArrayListExtra("vocabList");

        String folderId = getIntent().getStringExtra("folderId");
        String courseId = getIntent().getStringExtra("courseId");

        int percent = 0;
        if (totalCount > 0) {
            percent = (int) Math.round((correctCount * 100.0) / totalCount);
        }
        String scoreText = "Bạn đúng " + correctCount + "/" + totalCount +
                " (" + percent + "%)";
        tvScore.setText(scoreText);

        if (wrongWords != null && !wrongWords.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (String word : wrongWords) {

                View card = inflater.inflate(R.layout.item_term, wrongListContainer, false);
                TextView edtTerm = card.findViewById(R.id.edtTerm);
                TextView edtDefinition = card.findViewById(R.id.edtDefinition);
                View btnRead = card.findViewById(R.id.btnRead);
                View btnRemove = card.findViewById(R.id.btnRemoveTerm);

                edtTerm.setText(word);

                String meaning = "";
                if (vocabList != null) {
                    for (Vocabulary v : vocabList) {
                        if (word.equals(v.getWord())) {
                            meaning = v.getMeaning();
                            break;
                        }
                    }
                }

                edtDefinition.setText(meaning);
                edtDefinition.setVisibility(View.VISIBLE);

                if (btnRead != null) btnRead.setVisibility(View.GONE);
                if (btnRemove != null) btnRemove.setVisibility(View.GONE);

                wrongListContainer.addView(card);
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("Chúc mừng! Bạn làm đúng tất cả các câu hỏi.");
            tv.setTextSize(16f);
            tv.setPadding(16, 8, 16, 8);
            wrongListContainer.addView(tv);
        }

        btnFlashcards.setOnClickListener(v -> {
            if (vocabList == null || vocabList.isEmpty()) {
                Toast.makeText(this, "Không có từ vựng để hiển thị.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(AIQuizResultActivity.this, FlashCardActivity.class);
            intent.putParcelableArrayListExtra("vocabList", vocabList);
            startActivity(intent);
            finish();
        });

        btnCourseDetail.setOnClickListener(v -> {
            if (folderId == null || courseId == null) {
                Toast.makeText(this, "Không xác định được Course.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(AIQuizResultActivity.this, CourseDetailActivity.class);
            intent.putExtra(CourseDetailActivity.EXTRA_FOLDER_ID, folderId);
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
            finish();
        });
    }
}
