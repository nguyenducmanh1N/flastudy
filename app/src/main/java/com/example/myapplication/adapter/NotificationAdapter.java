package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.NotificationItem;

import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnClick {
        void onClick(NotificationItem item);
    }

    private List<NotificationItem> items;
    private OnClick listener;

    public NotificationAdapter(List<NotificationItem> items, OnClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        NotificationItem n = items.get(pos);
        h.tvTitle.setText(
                n.getType().equals("invite")
                        ? "Lời mời vào lớp " + n.getClassId()
                        : "Thông báo mới"
        );

        h.btnAccept.setVisibility(
                n.getType().equals("invite") && !n.isAccepted()
                        ? View.VISIBLE
                        : View.GONE
        );
        h.btnAccept.setOnClickListener(v -> listener.onClick(n));
        h.itemView.setOnClickListener(v -> {

        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        Button btnAccept;
        VH(View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tvNotifTitle);
            btnAccept = v.findViewById(R.id.btnAcceptInvite);
        }
    }
}
