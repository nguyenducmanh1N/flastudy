package com.example.myapplication.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseVH> {

    public interface OnItemClickListener {
        void onClick(Course course);
    }

    private List<Course> data;
    private OnItemClickListener listener;
    private int layoutRes;

    public CourseAdapter(List<Course> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }
    // New constructor allows specifying custom layout
    public CourseAdapter(List<Course> data, OnItemClickListener listener, int layoutRes) {
        this.data = data;
        this.listener = listener;
        this.layoutRes = layoutRes;
    }

    @NonNull
    @Override
    public CourseVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (layoutRes == 0) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course_card, parent, false);
            return new CourseVH(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new CourseVH(v);

    }



    @Override
    public void onBindViewHolder(@NonNull CourseVH holder, int position) {
        Course c = data.get(position);
        holder.title.setText(c.getTitle());
        int count = c.getVocabularyList() != null
                ? c.getVocabularyList().size() : 0;
        holder.courseCreater.setText("Người tạo: " + c.getCreater()+ "  ");
        holder.termCount.setText(count + " thuật ngữ   ");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class CourseVH extends RecyclerView.ViewHolder {
        TextView title, termCount,courseCreater;
        public CourseVH(@NonNull View itemView) {
            super(itemView);
            title     = itemView.findViewById(R.id.courseTitle);
            termCount = itemView.findViewById(R.id.courseTermCount);
            courseCreater = itemView.findViewById(R.id.courseCreater);
        }
    }
}

