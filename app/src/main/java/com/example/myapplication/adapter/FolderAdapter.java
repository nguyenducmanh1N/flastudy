package com.example.myapplication.adapter;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Folder;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<Folder> folderList;
    private OnFolderClickListener listener;

    public FolderAdapter(List<Folder> folderList) {
        this.folderList = folderList;
    }
    public void setOnFolderClickListener(OnFolderClickListener listener) {
        this.listener = listener;
    }
    public FolderAdapter(List<Folder> folderList, OnFolderClickListener listener) {
        this.folderList = folderList;
        this.listener = listener;
    }
    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }



    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_card, parent, false);
        return new FolderViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folderList.get(position);
        holder.folderNameTextView.setText(folder.getName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderNameTextView , folderCountTextView;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderName);
            folderCountTextView = itemView.findViewById(R.id.folderCount);
        }
    }
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderNameTextView);
        }

        public void bind(Folder folder) {
            folderName.setText(folder.getName());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });
        }
    }
}
