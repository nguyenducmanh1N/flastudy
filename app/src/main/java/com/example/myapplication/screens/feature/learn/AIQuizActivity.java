package com.example.myapplication.screens.feature.learn;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.AIQuestion;
import com.example.myapplication.model.Vocabulary;
import com.example.myapplication.service.GeminiService;

import java.util.ArrayList;
import java.util.List;

public class AIQuizActivity extends AppCompatActivity {

    private TextView tvQuestion;
    private RadioGroup optionsGroup;
    private Button btnSubmit, btnNext;
    private TextView tvExplanation;
    private ProgressBar progressBar;
    private ScrollView scrollContent;

    private List<Vocabulary> vocabList;
    private List<AIQuestion> questionList = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aiquiz);

        tvQuestion    = findViewById(R.id.tvQuestion);
        optionsGroup  = findViewById(R.id.optionsGroup);
        btnSubmit     = findViewById(R.id.btnSubmit);
        tvExplanation = findViewById(R.id.tvExplanation);
        btnNext       = findViewById(R.id.btnNext);
        progressBar   = findViewById(R.id.progressBar);
        scrollContent = findViewById(R.id.scrollContent);

        vocabList = getIntent().getParcelableArrayListExtra("vocabList");
        if (vocabList == null || vocabList.isEmpty()) {
            Toast.makeText(this, "Danh sách từ vựng trống!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scrollContent.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        GeminiService.generateQuestionsWithGemini(vocabList, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(List<AIQuestion> questions) {
                runOnUiThread(() -> {

                    progressBar.setVisibility(View.GONE);

                    scrollContent.setVisibility(View.VISIBLE);

                    if (questions == null || questions.isEmpty()) {
                        Toast.makeText(AIQuizActivity.this,
                                "Không có câu hỏi nào được tạo.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        questionList.addAll(questions);
                        displayCurrentQuestion();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(AIQuizActivity.this,
                            "Lỗi khi tạo câu hỏi: " + errorMessage, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });

        btnSubmit.setOnClickListener(v -> {
            int selectedId = optionsGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(AIQuizActivity.this,
                        "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRb = findViewById(selectedId);
            String chosenAnswer = selectedRb.getText().toString();
            AIQuestion currentQ = questionList.get(currentIndex);

            String en = currentQ.getExplanationEn();
            String vi = currentQ.getExplanationVi();
            tvExplanation.setVisibility(View.VISIBLE);
            if (chosenAnswer.equals(currentQ.getCorrectAnswer())) {
                tvExplanation.setText("Chính xác!\n\n" +
                        "Giải thích :"+"\n\n"+
                        "English: " + en + "\n\n" +
                        "Tiếng Việt: " + vi);
            } else {
                tvExplanation.setText("Sai rồi! Đáp án đúng: " +
                        currentQ.getCorrectAnswer() + "\n\n" +
                        "Giải thích :"+"\n\n"+
                        "English: " + en + "\n\n" +
                        "Tiếng Việt: " + vi);
            }


            for (int i = 0; i < optionsGroup.getChildCount(); i++) {
                optionsGroup.getChildAt(i).setEnabled(false);
            }

            btnSubmit.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
        });

        btnNext.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex < questionList.size()) {
                displayCurrentQuestion();
            } else {
                Toast.makeText(AIQuizActivity.this,
                        "Bạn đã hoàn thành tất cả câu hỏi!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayCurrentQuestion() {
        AIQuestion q = questionList.get(currentIndex);

        tvQuestion.setText(q.getQuestion());

        optionsGroup.removeAllViews();
        optionsGroup.clearCheck();

        for (String option : q.getOptions()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(option);
            rb.setTextSize(16f);
            rb.setPadding(8, 16, 8, 16);
            optionsGroup.addView(rb);
        }

        tvExplanation.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.VISIBLE);

        for (int i = 0; i < optionsGroup.getChildCount(); i++) {
            optionsGroup.getChildAt(i).setEnabled(true);
        }
    }
}
