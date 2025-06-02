package com.example.myapplication.screens.feature.learn;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FillActivity extends AppCompatActivity {

    private TextView tvPrompt, tvCorrect;
    private EditText etAnswer;
    private Button btnCheck, btnNext;
    private ImageView btnBack;

    private List<Vocabulary> originalList;
    private List<Vocabulary> quizList;
    private List<Vocabulary> wrongAnswers = new ArrayList<>();

    private int index = 0;
    private boolean isReviewMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fill);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        tvPrompt  = findViewById(R.id.tvPrompt);
        etAnswer  = findViewById(R.id.etAnswer);
        tvCorrect = findViewById(R.id.tvCorrect);
        btnCheck  = findViewById(R.id.btnCheck);
        btnNext   = findViewById(R.id.btnNext);
        btnBack   = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        originalList = getIntent().getParcelableArrayListExtra("vocabList");
        if (originalList == null || originalList.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để quiz!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        quizList = new ArrayList<>(originalList);
        Collections.shuffle(quizList);

        showQuestion();

        btnCheck.setOnClickListener(v -> checkAnswer());
        btnNext.setOnClickListener(v -> {
            btnNext.setVisibility(View.GONE);
            tvCorrect.setVisibility(View.GONE);
            index++;
            showQuestion();
        });
    }

    private void showQuestion() {
        if (index >= quizList.size()) {
            endQuiz();
            return;
        }
        Vocabulary vocab = quizList.get(index);
        tvPrompt.setText(vocab.getMeaning());

        etAnswer.setEnabled(true);
        etAnswer.setText("");
        etAnswer.setBackgroundResource(android.R.drawable.edit_text);

        btnCheck.setEnabled(true);
        btnNext.setVisibility(View.GONE);
        tvCorrect.setVisibility(View.GONE);
    }

    private void checkAnswer() {
        Vocabulary vocab = quizList.get(index);
        String input = etAnswer.getText().toString().trim();
        if (input.equalsIgnoreCase(vocab.getWord())) {

            etAnswer.setBackgroundResource(R.drawable.input_correct);
            Toast.makeText(this, "Đúng rồi!", Toast.LENGTH_SHORT).show();
            etAnswer.setEnabled(false);
            btnCheck.setEnabled(false);
            new Handler().postDelayed(() -> {
                index++;
                showQuestion();
            }, 1000);
        } else {
            etAnswer.setBackgroundResource(R.drawable.input_wrong);
            tvCorrect.setText("Từ đúng: " + vocab.getWord());
            tvCorrect.setVisibility(View.VISIBLE);

            Toast.makeText(this,
                    "Sai rồi! Hãy xem từ đúng bên dưới.",
                    Toast.LENGTH_LONG).show();
            wrongAnswers.add(vocab);
            etAnswer.setEnabled(false);
            btnCheck.setEnabled(false);
            btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void endQuiz() {
        if (!isReviewMode && !wrongAnswers.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Làm lại từ sai?")
                    .setMessage("Bạn có muốn làm lại " + wrongAnswers.size() + " từ đã sai không?")
                    .setPositiveButton("Làm lại", (d, w) -> {
                        isReviewMode = true;
                        quizList = new ArrayList<>(wrongAnswers);
                        wrongAnswers.clear();
                        index = 0;
                        showQuestion();
                    })
                    .setNegativeButton("Kết thúc", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            Toast.makeText(this, "Quiz kết thúc!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
