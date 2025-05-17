package com.example.myapplication.screens.feature.learn;


import android.os.Bundle;
import android.service.credentials.Action;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.adapter.FlashCardPagerAdapter;
import com.example.myapplication.model.Vocabulary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class FlashCardActivity extends AppCompatActivity {
    private ViewPager2 vp;
    private List<Vocabulary> vocabList;
    private TextView tvCounter, tvMastered, tvNotMastered;

    private List<String> masteredIds = new ArrayList<>();
    private List<String> notMasteredIds = new ArrayList<>();

    private int previousPosition = 0;
    private Deque<Action> actionStack = new ArrayDeque<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_card);


        vp = findViewById(R.id.vpFlashcards);
        tvCounter = findViewById(R.id.tvCounter);
        tvMastered = findViewById(R.id.masteredIds);
        tvNotMastered = findViewById(R.id.notMasteredIds);

        vocabList = getIntent().getParcelableArrayListExtra("vocabList");
        if (vocabList == null) vocabList = new ArrayList<>();

        vp.setAdapter(new FlashCardPagerAdapter(vocabList));
        updateCounter(0);
        vp.setOffscreenPageLimit(3);

// Tắt clipping để page “nhìn xuyên” sang hai bên
        ViewGroup recyclerView = (ViewGroup) vp.getChildAt(0);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);

// Padding để lộ card bên dưới
        vp.setPadding(48, 0, 48, 0);
        vp.setClipToPadding(false);
        vp.setClipChildren(false);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer((page, position) -> {
            // position: -1 (trái) … 0 (giữa) … +1 (phải)
            float absPos = Math.abs(position);

            // 1) Xếp chồng: dịch chuyển theo chiều ngang
            float offsetPx = page.getWidth() * 0.3f;
            page.setTranslationX(-position * offsetPx);

            // 2) Scale nhỏ dần để tạo chiều sâu
            float scale = 1f - 0.1f * absPos;
            page.setScaleY(scale);

            // 3) (Tuỳ chọn) Alpha mờ dần
            page.setAlpha(1f - 0.3f * absPos);
        });

        vp.setPageTransformer(transformer);

        vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                handleSwipe(position);
                updateCounter(position);
                previousPosition = position;
            }
        });
        ImageButton btnNotMastered = findViewById(R.id.btnNotMastered);
        ImageButton btnMastered    = findViewById(R.id.btnMastered);

        btnNotMastered.setOnClickListener(v -> markCurrent(false));
        btnMastered.setOnClickListener(v -> markCurrent(true));

//        ImageButton btnUndo = findViewById(R.id.btnUndo);
//        btnUndo.setOnClickListener(v -> {
//            if (!actionStack.isEmpty()) {
//                Action a = actionStack.pop();
//                String id = a.vocab.getId();
//                if (a.mastered)   masteredIds.remove(id);
//                else               notMasteredIds.remove(id);
//
//                updateMasteredCount();
//                updateNotMasteredCount();
//                vp.setCurrentItem(a.position, true);
//            }
//        });

    }

    private void markCurrent(boolean mastered) {
        int pos = vp.getCurrentItem();
        Vocabulary vocab = vocabList.get(pos);
        String id = vocab.getId();

        if (mastered) {
            if (!masteredIds.contains(id)) {
                masteredIds.add(id);
                updateMasteredCount();
                actionStack.push(new Action(vocab, pos, true));
            }
        } else {
            if (!notMasteredIds.contains(id)) {
                notMasteredIds.add(id);
                updateNotMasteredCount();
                actionStack.push(new Action(vocab, pos, false));
            }
        }

        // chuyển sang từ tiếp theo (tuỳ chọn)
        if (pos < vocabList.size() - 1) {
            vp.setCurrentItem(pos + 1, true);
        }
    }


    private void updateCounter(int position) {
        int total = vocabList.size();
        String text = (position + 1) + " / " + total;
        tvCounter.setText(text);
    }

    private void handleSwipe(int currentPosition) {
        if (currentPosition == previousPosition) return;

        Vocabulary previousVocab = vocabList.get(previousPosition);
        String vocabId = previousVocab.getId();

        if (currentPosition > previousPosition) {

            if (!notMasteredIds.contains(vocabId)) {
                notMasteredIds.add(vocabId);
                updateNotMasteredCount();
            }
        } else {

            if (!masteredIds.contains(vocabId)) {
                masteredIds.add(vocabId);
                updateMasteredCount();
            }
        }
    }
    private void updateMasteredCount() {
        tvMastered.setText(String.valueOf(masteredIds.size()));
    }

    private void updateNotMasteredCount() {
        tvNotMastered.setText(String.valueOf(notMasteredIds.size()));
    }

    private static class Action {
        final Vocabulary vocab;
        final int position;
        final boolean mastered;
        Action(Vocabulary v, int pos, boolean mastered) {
            this.vocab = v; this.position = pos; this.mastered = mastered;
        }
    }


}

