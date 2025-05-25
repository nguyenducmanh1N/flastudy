package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CourseSelectAdapter
        extends RecyclerView.Adapter<CourseSelectAdapter.VH> {

    public interface OnSelectionChanged {
        void onSelectionChanged(List<String> selectedIds);
    }

    private final List<Course> originalCourses;
    private final List<Course> filteredCourses;
    private final Set<String> selected = new HashSet<>();
    private final OnSelectionChanged listener;

    public CourseSelectAdapter(List<Course> courses, OnSelectionChanged listener) {
        this.originalCourses = new ArrayList<>(courses);
        this.filteredCourses = new ArrayList<>(courses);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_vertical_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Course course = filteredCourses.get(position);
        holder.tvTitle.setText(course.getTitle());
        holder.tvCount.setText(course.getVocabularyList().size() + " thuật ngữ");
        holder.tvCreater.setText("Người tạo: " + course.getCreater());

        holder.check.setChecked(selected.contains(course.getId()));
        holder.itemView.setOnClickListener(v -> {
            if (selected.contains(course.getId())) {
                selected.remove(course.getId());
            } else {
                selected.add(course.getId());
            }
            notifyItemChanged(position);
            listener.onSelectionChanged(new ArrayList<>(selected));
        });
    }

    @Override
    public int getItemCount() {
        return filteredCourses.size();
    }


    public void setCourses(List<Course> courses) {
        originalCourses.clear();
        originalCourses.addAll(courses);
        filter("");
    }


    public void filter(String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        filteredCourses.clear();
        if (lower.isEmpty()) {
            filteredCourses.addAll(originalCourses);
        } else {
            for (Course c : originalCourses) {
                if (c.getTitle().toLowerCase().contains(lower)) {
                    filteredCourses.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedIds() {
        return selected;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCount, tvCreater;
        CheckBox check;

        VH(View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.courseTitle);
            tvCount   = itemView.findViewById(R.id.courseTermCount);
            tvCreater = itemView.findViewById(R.id.courseCreater);
            check     = itemView.findViewById(R.id.cbSelect2);
        }
    }
}
