package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Class;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassVH> {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnItemClickListener {
        void onClick(Class cls);
    }
    private List<Class> data;
    private OnItemClickListener listener;

    public ClassAdapter(List<Class> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull @Override
    public ClassVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_card, parent, false);
        return new ClassVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassVH holder, int pos) {
        Class cls = data.get(pos);
        String classId = cls.getId(); // Đảm bảo model Class có getId()

        holder.name.setText(cls.getName());
        holder.creater.setText("Người tạo: " + cls.getCreater());

        // Lấy số lượng folders
        db.collection("classes")
                .document(classId)
                .collection("folders")
                .get()
                .addOnSuccessListener(folders -> {
                    holder.fcount.setText(folders.size() + " thư mục");
                })
                .addOnFailureListener(e -> {
                    holder.fcount.setText("Lỗi");
                });

        // Lấy số lượng courses
        db.collection("classes")
                .document(classId)
                .collection("courses")
                .get()
                .addOnSuccessListener(courses -> {
                    holder.ccount.setText(courses.size() + " học phần");
                })
                .addOnFailureListener(e -> {
                    holder.ccount.setText("Lỗi");
                });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(cls);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class ClassVH extends RecyclerView.ViewHolder {
        TextView name, fcount, ccount, creater;
        public ClassVH(@NonNull View itemView) {
            super(itemView);
            name  = itemView.findViewById(R.id.classTitle);
            fcount  = itemView.findViewById(R.id.classFolderCount);
            ccount = itemView.findViewById(R.id.classCourseCount);
            creater = itemView.findViewById(R.id.classCreater);

        }
    }
}
