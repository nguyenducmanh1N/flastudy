package com.example.myapplication.screens.feature.learn;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuizActivity extends AppCompatActivity {

    private TextView tvWord;
    private Button[] answerButtons = new Button[4];

    // Danh sách từ vựng gốc (để reset lại nếu làm lại wrongAnswers)
    private List<Vocabulary> originalList;
    // Danh sách hiện tại đang quiz
    private List<Vocabulary> vocabularyList;
    private List<Vocabulary> wrongAnswers = new ArrayList<>();

    private int currentQuestionIndex = 0;
    private Vocabulary correctVocabulary;
    private Random random = new Random();

    // Cờ xem đang ở chế độ làm lại wrongAnswers hay không
    private boolean isReviewMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvWord = findViewById(R.id.tvQuizWord);
        answerButtons[0] = findViewById(R.id.btnAnswer1);
        answerButtons[1] = findViewById(R.id.btnAnswer2);
        answerButtons[2] = findViewById(R.id.btnAnswer3);
        answerButtons[3] = findViewById(R.id.btnAnswer4);

        // Nhận danh sách từ vựng từ Intent
        originalList = getIntent().getParcelableArrayListExtra("vocabList");
        if (originalList == null || originalList.size() < 4) {
            Toast.makeText(this,
                    "Không đủ từ vựng để làm trắc nghiệm!",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Tạo bản sao để quiz lần đầu
        vocabularyList = new ArrayList<>(originalList);
        Collections.shuffle(vocabularyList);

        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (currentQuestionIndex >= vocabularyList.size()) {
            // Hết câu hỏi lượt này
            if (!isReviewMode && !wrongAnswers.isEmpty()) {
                // Nếu có từ sai và chưa ở chế độ review => hỏi có làm lại không
                showRetryDialog();
            } else {
                // Đóng Activity (hoặc bạn có thể hiển thị summary)
                Toast.makeText(this,
                        "Kết thúc trắc nghiệm!",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        // Hiển thị câu hỏi mới
        correctVocabulary = vocabularyList.get(currentQuestionIndex);
        tvWord.setText(correctVocabulary.getWord());

        // Tạo 3 nghĩa sai ngẫu nhiên
        List<String> options = new ArrayList<>();
        options.add(correctVocabulary.getMeaning());

        while (options.size() < 4) {
            int randIndex = random.nextInt(vocabularyList.size());
            String distractor = vocabularyList.get(randIndex).getMeaning();
            if (!options.contains(distractor)) {
                options.add(distractor);
            }
        }

        Collections.shuffle(options);
        for (int i = 0; i < 4; i++) {
            Button btn = answerButtons[i];
            String answerText = options.get(i);
            btn.setText(answerText);
            btn.setBackgroundColor(getResources().getColor(R.color.teal_700));
            btn.setEnabled(true);
            btn.setOnClickListener(v -> checkAnswer(btn, answerText));
        }
    }

    private void checkAnswer(Button selectedButton, String selectedAnswer) {
        boolean isCorrect = selectedAnswer.equals(correctVocabulary.getMeaning());

        if (isCorrect) {
            selectedButton.setBackgroundColor(getResources().getColor(R.color.green));
            Toast.makeText(this, "Đúng rồi!", Toast.LENGTH_SHORT).show();
        } else {
            selectedButton.setBackgroundColor(getResources().getColor(R.color.red));
            Toast.makeText(this, "Sai rồi!", Toast.LENGTH_SHORT).show();

            if (!wrongAnswers.contains(correctVocabulary)) {
                wrongAnswers.add(correctVocabulary);
            }
            // Tô màu đáp án đúng
            for (Button btn : answerButtons) {
                if (btn.getText().equals(correctVocabulary.getMeaning())) {
                    btn.setBackgroundColor(getResources().getColor(R.color.green));
                    break;
                }
            }
        }


        for (Button btn : answerButtons) {
            btn.setEnabled(false);
        }


        new Handler().postDelayed(() -> {
            currentQuestionIndex++;
            loadNextQuestion();
        }, 2000);
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Làm lại những từ đã sai?")
                .setMessage("Bạn có muốn làm lại " +
                        wrongAnswers.size() + " từ đã chọn sai không?")
                .setPositiveButton("Làm lại", (dialog, which) -> {
                    isReviewMode = true;

                    vocabularyList = new ArrayList<>(wrongAnswers);
                    wrongAnswers.clear();
                    currentQuestionIndex = 0;
                    loadNextQuestion();
                })
                .setNegativeButton("Không", (dialog, which) -> {
                    // Kết thúc
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
