package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Folder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderSelectAdapter
        extends RecyclerView.Adapter<FolderSelectAdapter.VH> {

    public interface OnSelectionChanged {
        void onSelectionChanged(List<String> selectedIds);
    }

    private final List<Folder> folders;
    private final Set<String> selected = new HashSet<>();
    private final OnSelectionChanged listener;

    public FolderSelectAdapter(List<Folder> folders, OnSelectionChanged listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder_vertical, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Folder folder = folders.get(position);
        holder.tvName.setText(folder.getName());

        holder.tvCount.setText(folder.getCount() + " mục");
        holder.tvCreater.setText("Người tạo: " + folder.getCreater());

        holder.check.setChecked(selected.contains(folder.getId()));
        holder.itemView.setOnClickListener(v -> {
            if (selected.contains(folder.getId())) {
                selected.remove(folder.getId());
            } else {
                selected.add(folder.getId());
            }
            notifyItemChanged(position);
            listener.onSelectionChanged(new ArrayList<>(selected));
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public Set<String> getSelectedIds() {
        return selected;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount, tvCreater;
        CheckBox check;

        VH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTitle);
            tvCount = itemView.findViewById(R.id.tvCount);
            tvCreater = itemView.findViewById(R.id.tvCreater);
            check = itemView.findViewById(R.id.cbSelect);
        }
    }
}
