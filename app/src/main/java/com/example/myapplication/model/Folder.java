package com.example.myapplication.model;

import java.util.List;

public class Folder {
    private String id;
    private String name;
    private int count;
    private long createdAt;
    private String creater;
    private List<Course> courses;

    public Folder() {}
    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public Folder(String name, long createdAt, int count, String creater) {
        this.name = name;
        this.createdAt = createdAt;
        this.count = count;
        this.creater = creater;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public String getCreater() {
        return creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }
}
