package com.example.myapplication.adapter;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.util.List;

public class VocabAdapter extends RecyclerView.Adapter<VocabAdapter.FlashcardVH> {

    private List<Vocabulary> vocabList;
    private LayoutInflater inflater;
    private AnimatorSet flipInAnimator;
    private AnimatorSet flipOutAnimator;

    public VocabAdapter(Context context, List<Vocabulary> vocabList) {
        this.vocabList = vocabList;
        this.inflater = LayoutInflater.from(context);
        // Load animator XML
        flipInAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.flip_in);
        flipOutAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.flip_out);
    }

    @NonNull
    @Override
    public FlashcardVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_flashcard, parent, false);
        return new FlashcardVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardVH holder, int position) {
        Vocabulary vocab = vocabList.get(position);
        holder.frontText.setText(vocab.getWord());
        holder.backMeaning.setText(vocab.getMeaning());

        // camera distance
        float scale = holder.container.getResources().getDisplayMetrics().density;
        holder.front.setCameraDistance(8000 * scale);
        holder.back.setCameraDistance(8000 * scale);

        // thiết lập mặt sau
        if (vocab.getExample() != null && !vocab.getExample().isEmpty()) {
            holder.backExample.setVisibility(View.VISIBLE);
            holder.backExample.setText(vocab.getExample());
        } else {
            holder.backExample.setVisibility(View.GONE);
        }

        holder.front.setVisibility(View.VISIBLE);
        holder.back.setVisibility(View.GONE);

        holder.container.setOnClickListener(v -> flipCard(holder));
    }


    @Override
    public int getItemCount() {
        return vocabList.size();
    }

    private void flipCard(FlashcardVH holder) {
        View front = holder.front;
        View back = holder.back;

        if (front.getVisibility() == View.VISIBLE) {
            flipOutAnimator.setTarget(front);
            flipInAnimator.setTarget(back);
            flipOutAnimator.start();
            flipInAnimator.start();
            front.setVisibility(View.GONE);
            back.setVisibility(View.VISIBLE);
        } else {
            flipOutAnimator.setTarget(back);
            flipInAnimator.setTarget(front);
            flipOutAnimator.start();
            flipInAnimator.start();
            back.setVisibility(View.GONE);
            front.setVisibility(View.VISIBLE);
        }
    }


    static class FlashcardVH extends RecyclerView.ViewHolder {
        FrameLayout container;
        View front, back;
        TextView frontText;
        TextView backMeaning;
        TextView backExample;

        public FlashcardVH(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.flashcard_container);
            front = itemView.findViewById(R.id.flashcard_front);
            back = itemView.findViewById(R.id.flashcard_back);
            frontText = itemView.findViewById(R.id.tvFrontWord);
            backMeaning = itemView.findViewById(R.id.tvBackMeaning);
            backExample = itemView.findViewById(R.id.tvBackExample);
        }
    }

}