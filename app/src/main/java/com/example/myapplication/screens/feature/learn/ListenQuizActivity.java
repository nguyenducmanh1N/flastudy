package com.example.myapplication.screens.feature.learn;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ListenQuizActivity extends AppCompatActivity {

    private ImageButton btnPlayAudio , btnBack;
    private Button[] opts = new Button[4];
    private List<Vocabulary> quizList;
    private Vocabulary correctVocab;
    private Random rnd = new Random();
    private int idx = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_quiz);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        opts[0] = findViewById(R.id.btnAnswer1);
        opts[1] = findViewById(R.id.btnAnswer2);
        opts[2] = findViewById(R.id.btnAnswer3);
        opts[3] = findViewById(R.id.btnAnswer4);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        List<Vocabulary> original = getIntent().getParcelableArrayListExtra("vocabList");
        if (original == null || original.size() < 4) {
            Toast.makeText(this, "Không đủ dữ liệu để quiz!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        quizList = new ArrayList<>(original);
        Collections.shuffle(quizList);

        loadQuestion();

        btnPlayAudio.setOnClickListener(v -> playAudio());
    }

    private void loadQuestion() {
        if (idx >= quizList.size()) {
            Toast.makeText(this, "Hoàn thành quiz!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        correctVocab = quizList.get(idx);
        List<String> texts = new ArrayList<>();
        texts.add(correctVocab.getWord());
        while (texts.size() < 4) {
            String w = quizList.get(rnd.nextInt(quizList.size())).getWord();
            if (!texts.contains(w)) texts.add(w);
        }
        Collections.shuffle(texts);

        for (int i = 0; i < 4; i++) {
            Button b = opts[i];
            String t = texts.get(i);
            b.setText(t);
            b.setBackgroundResource(R.drawable.input_correct);
            b.setEnabled(true);
            b.setOnClickListener(v -> checkAnswer(b, t));
        }
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

    private void checkAnswer(Button selected, String text) {
        boolean ok = text.equals(correctVocab.getWord());
        if (ok) {
            selected.setBackgroundColor(getResources().getColor(R.color.green));
            Toast.makeText(this, "Đúng rồi!", Toast.LENGTH_SHORT).show();
        } else {
            selected.setBackgroundColor(getResources().getColor(R.color.red));
            Toast.makeText(this, "Sai rồi!", Toast.LENGTH_SHORT).show();
            for (Button b : opts) {
                if (b.getText().equals(correctVocab.getWord())) {
                    b.setBackgroundColor(getResources().getColor(R.color.green));
                    break;
                }
            }
        }
        for (Button b : opts) b.setEnabled(false);
        handler.postDelayed(() -> {
            idx++;
            loadQuestion();
        }, 1500);
    }
}