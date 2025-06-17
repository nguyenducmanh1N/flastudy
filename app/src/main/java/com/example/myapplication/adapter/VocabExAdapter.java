package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.widget.Toast;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;

import java.util.List;

public class VocabExAdapter extends RecyclerView.Adapter<VocabExAdapter.VocabVH> {

    private List<Vocabulary> vocabList;
    private Context context;

    public VocabExAdapter(Context context, List<Vocabulary> vocabList) {
        this.context = context;
        this.vocabList = vocabList;
    }

    @NonNull
    @Override
    public VocabVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vocab, parent, false);
        return new VocabVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VocabVH holder, int position) {
        Vocabulary v = vocabList.get(position);

        holder.tvTerm.setText(v.getWord());
        holder.tvDefinition.setText(v.getMeaning());
        holder.tvPhonetic.setText(v.getPhonetic());
        holder.tvExample.setText(v.getExample());

        holder.expandableLayout.setVisibility(View.GONE);
        holder.ivExpandToggle.setImageResource(R.drawable.ic_expand_more);

        holder.ivExpandToggle.setOnClickListener(view -> {
            boolean isExpanded = holder.expandableLayout.getVisibility() == View.VISIBLE;
            holder.expandableLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.ivExpandToggle.setImageResource(
                    isExpanded ? R.drawable.ic_expand_more : R.drawable.ic_expand_less
            );
        });

        holder.hear.setOnClickListener(view -> {
            String url = v.getAudio();
            if (url == null || url.isEmpty()) {
                Toast.makeText(context, "Không có âm thanh", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, "Không thể phát âm thanh", Toast.LENGTH_SHORT).show();
                mp.release();
            }
        });
    }


    @Override
    public int getItemCount() {
        return vocabList != null ? vocabList.size() : 0;
    }

    public static class VocabVH extends RecyclerView.ViewHolder {
        TextView tvTerm, tvDefinition, tvPhonetic, tvExample;
        ImageView ivExpandToggle,hear;
        ViewGroup expandableLayout;

        public VocabVH(@NonNull View itemView) {
            super(itemView);
            tvTerm = itemView.findViewById(R.id.tvTerm);
            tvDefinition = itemView.findViewById(R.id.tvDefinition);
            tvPhonetic = itemView.findViewById(R.id.tvPhonetic);
            tvExample = itemView.findViewById(R.id.tvExample);
            ivExpandToggle = itemView.findViewById(R.id.ivExpandToggle);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            hear= itemView.findViewById(R.id.hear);
        }
    }

}
