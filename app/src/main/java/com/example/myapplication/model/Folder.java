package com.example.myapplication.model;

public class Folder {
    private String name;
    private int count;

    private long createdAt;

    public Folder() {}

    public Folder(String name, long createdAt, int count) {
        this.name = name;
        this.createdAt = createdAt;
        this.count = count;
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

    public long getCreatedAt() {
        return createdAt;
    }
}