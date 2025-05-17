package com.example.myapplication.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.util.List;

public class FlashCardPagerAdapter extends RecyclerView.Adapter<FlashCardPagerAdapter.VH> {
    private final List<Vocabulary> list;

    public FlashCardPagerAdapter(List<Vocabulary> list) {
        this.list = list;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard_main, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Vocabulary vocab = list.get(position);
        holder.tvTerm.setText(vocab.getWord());
        holder.tvDefinition.setText(vocab.getMeaning());
        holder.bindFlip();

    }

    @Override public int getItemCount() { return list.size(); }

    class VH extends RecyclerView.ViewHolder {
        View front, back;
        TextView tvTerm, tvDefinition;

        VH(View itemView) {
            super(itemView);
            front = itemView.findViewById(R.id.card_front);
            back  = itemView.findViewById(R.id.card_back);
            tvTerm = itemView.findViewById(R.id.tvTerm);
            tvDefinition = itemView.findViewById(R.id.tvDefinition);
            itemView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            float px = front.getWidth() / 2f;
                            float py = front.getHeight() / 2f;
                            front.setPivotX(px); front.setPivotY(py);
                            back .setPivotX(px);  back .setPivotY(py);
                            float scale = itemView.getResources()
                                    .getDisplayMetrics().density;
                            int dist = 12000;
                            front.setCameraDistance(scale * dist);
                            back .setCameraDistance(scale * dist);
                        }
                    });
        }

        void bindFlip() {
            front.setVisibility(View.VISIBLE);
            back.setVisibility(View.GONE);
            itemView.setOnClickListener(v -> {
                View visible = front.getVisibility()==View.VISIBLE ? front : back;
                View hidden  = visible==front ? back : front;
                ObjectAnimator out = ObjectAnimator.ofFloat(
                        visible, "rotationY", 0f, 90f);
                out.setDuration(250);
                ObjectAnimator in = ObjectAnimator.ofFloat(
                        hidden,  "rotationY", -90f,0f);
                in.setDuration(250);
                out.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator anim) {
                        visible.setVisibility(View.GONE);
                        hidden.setVisibility(View.VISIBLE);
                        in.start();
                    }
                });
                out.start();
            });
        }
    }
    public Vocabulary removeAt(int position) {
        Vocabulary removed = list.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    // Thêm một item vào vị trí position, thông báo cho ViewPager
    public void addAt(int position, Vocabulary vocab) {
        list.add(position, vocab);
        notifyItemInserted(position);
    }


}
