package com.example.myapplication.screens.feature.learn;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListenFillActivity extends AppCompatActivity {

    private ImageButton btnPlayAudio;
    private ImageView btnBack;
    private EditText inputAnswer;
    private Button btnConfirm;
    private TextView tvResult;

    private List<Vocabulary> quizList;
    private Vocabulary correctVocab;
    private int idx = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_fill);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnBack = findViewById(R.id.btnBack);
        inputAnswer = findViewById(R.id.inputAnswer);
        tvResult = findViewById(R.id.tvResult);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnBack.setOnClickListener(v -> finish());

        List<Vocabulary> original = getIntent().getParcelableArrayListExtra("vocabList");
        if (original == null || original.size() < 1) {
            Toast.makeText(this, "Không đủ dữ liệu để quiz!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        quizList = new ArrayList<>(original);
        Collections.shuffle(quizList);

        loadQuestion();

        btnPlayAudio.setOnClickListener(v -> playAudio());
        btnConfirm.setOnClickListener(v -> checkAnswer());
    }

    private void loadQuestion() {
        if (idx >= quizList.size()) {
            Toast.makeText(this, "Hoàn thành quiz!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        correctVocab = quizList.get(idx);
        inputAnswer.setText("");
        tvResult.setText("");
        tvResult.setVisibility(View.GONE);
        inputAnswer.setBackgroundResource(R.drawable.input_background);
        tvResult.setBackgroundResource(R.drawable.input_correct);
    }

    private void playAudio() {
        String url = correctVocab.getAudio();
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Audio không có sẵn", Toast.LENGTH_SHORT).show();
            return;
        }
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(url);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể phát audio", Toast.LENGTH_SHORT).show();
            mp.release();
        }
    }

    private void checkAnswer() {
        String userAnswer = inputAnswer.getText().toString().trim();
        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userAnswer.equalsIgnoreCase(correctVocab.getWord())) {
            tvResult.setText("Đúng rồi!");
            inputAnswer.setBackgroundResource(R.drawable.input_correct);
        } else {
            inputAnswer.setBackgroundResource(R.drawable.input_wrong);
            tvResult.setText("Sai rồi! Đáp án đúng là: " + correctVocab.getWord());
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setBackgroundResource(R.drawable.input_correct);
        }
        handler.postDelayed(() -> {
            idx++;
            loadQuestion();
        }, 5000);
    }
}