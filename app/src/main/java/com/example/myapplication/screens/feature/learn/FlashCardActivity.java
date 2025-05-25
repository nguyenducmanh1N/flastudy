package com.example.myapplication.screens.feature.learn;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.adapter.FlashCardPagerAdapter;
import com.example.myapplication.model.Vocabulary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlashCardActivity extends AppCompatActivity {
    private ViewPager2 vp;
    private TextView tvCounter, tvMastered, tvNotMastered;
    private List<Vocabulary> vocabList;
    private List<String> masteredIds   = new ArrayList<>();
    private List<String> notMasteredIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_card);

        vp            = findViewById(R.id.vpFlashcards);
        tvCounter     = findViewById(R.id.tvCounter);
        tvMastered    = findViewById(R.id.masteredIds);
        tvNotMastered = findViewById(R.id.notMasteredIds);

        ImageButton btnNotMastered = findViewById(R.id.btnNotMastered);
        ImageButton btnMastered    = findViewById(R.id.btnMastered);
        ImageButton btnHeadCard    = findViewById(R.id.btnHeadCard);
        ImageView   btnAutoScroll  = findViewById(R.id.btnAutoScroll);

        vocabList = getIntent().getParcelableArrayListExtra("vocabList");
        if (vocabList == null) vocabList = new ArrayList<>();

        vp.setAdapter(new FlashCardPagerAdapter(vocabList));
        setupViewPager();

        btnNotMastered.setOnClickListener(v -> markNotMastered());
        btnMastered   .setOnClickListener(v -> markMastered());
        btnHeadCard   .setOnClickListener(v -> playCurrentAudio());
        btnAutoScroll .setOnClickListener(v -> startAutoScroll(btnAutoScroll));
    }

    private void setupViewPager() {
        updateCounter(0);

        vp.setOffscreenPageLimit(1);
        vp.setClipToPadding(true);
        vp.setClipChildren(true);
        vp.setPadding(0, 0, 0, 0);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer((page, position) -> {
            float abs = Math.abs(position);
            page.setScaleY(1f - 0.1f * abs);
            page.setAlpha(1f - 0.3f * abs);
        });
        vp.setPageTransformer(transformer);

        vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                updateCounter(pos);
            }
        });
    }


    private void updateCounter(int pos) {
        tvCounter.setText((pos + 1) + " / " + vocabList.size());
        updateMasteredCount();
        updateNotMasteredCount();
    }

    private void markNotMastered() {
        int pos = vp.getCurrentItem();
        Vocabulary v = vocabList.get(pos);
        if (!notMasteredIds.contains(v.getId())) {
            notMasteredIds.add(v.getId());
            updateNotMasteredCount();
            Toast.makeText(this, "Chưa thuộc: " + v.getWord(), Toast.LENGTH_SHORT).show();
        }
    }

    private void markMastered() {
        int pos = vp.getCurrentItem();
        Vocabulary v = vocabList.get(pos);
        if (!masteredIds.contains(v.getId())) {
            masteredIds.add(v.getId());
            updateMasteredCount();
            Toast.makeText(this, "Đã thuộc: " + v.getWord(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMasteredCount() {
        tvMastered.setText(String.valueOf(masteredIds.size()));
    }

    private void updateNotMasteredCount() {
        tvNotMastered.setText(String.valueOf(notMasteredIds.size()));
    }

    private void playCurrentAudio() {
        int pos = vp.getCurrentItem();
        Vocabulary v = vocabList.get(pos);
        String url = v.getAudio();
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Không có âm thanh", Toast.LENGTH_SHORT).show();
            return;
        }
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(url);
            mp.prepareAsync();
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(MediaPlayer::release);
        } catch (IOException e) {
            Toast.makeText(this, "Không thể phát âm thanh", Toast.LENGTH_SHORT).show();
            mp.release();
        }
    }

    private void startAutoScroll(ImageView btn) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable task = new Runnable() {
            @Override public void run() {

                vp.getChildAt(0).performClick();

                int next = (vp.getCurrentItem() + 1) % vocabList.size();
                vp.setCurrentItem(next, true);
                handler.postDelayed(this, 3000);
            }
        };
        Toast.makeText(this, "Bắt đầu tự động lướt", Toast.LENGTH_SHORT).show();
        handler.post(task);
        btn.setEnabled(false);
    }
}
