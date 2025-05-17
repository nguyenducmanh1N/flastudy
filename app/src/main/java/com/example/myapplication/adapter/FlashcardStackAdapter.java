package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.util.List;

public class FlashcardStackAdapter
        extends RecyclerView.Adapter<FlashcardStackAdapter.VH> {

    private final List<Vocabulary> list;
    public FlashcardStackAdapter(List<Vocabulary> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_flashcard_stack, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Vocabulary v = list.get(pos);
        h.tvTerm.setText(v.getWord());
        h.tvDefinition.setText(v.getMeaning());
        h.bindFlip();
    }

    @Override public int getItemCount() {
        return list.size();
    }

    public Vocabulary removeTop() {
        if (list.isEmpty()) return null;
        Vocabulary top = list.remove(0);
        notifyItemRemoved(0);
        return top;
    }

    class VH extends RecyclerView.ViewHolder {
        View front, back;
        TextView tvTerm, tvDefinition;
        VH(View item) {
            super(item);
            front = item.findViewById(R.id.card_front);
            back  = item.findViewById(R.id.card_back);
            tvTerm = item.findViewById(R.id.tvTerm);
            tvDefinition = item.findViewById(R.id.tvDefinition);
            // Thiết lập flip giống trước…
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
        void bindFlip() { /* … như trước … */ }
    }
}

