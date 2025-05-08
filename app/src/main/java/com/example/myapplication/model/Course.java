package com.example.myapplication.model;

import java.util.List;

public class Course {
    private String id;
    private String title;
    private long createdAt;
    private List<Vocabulary> vocabularyList;

    public Course() {}

    public Course(String title, long createdAt, List<Vocabulary> vocabularyList) {
        this.title = title;
        this.createdAt = createdAt;
        this.vocabularyList = vocabularyList;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<Vocabulary> getVocabularyList() {
        return vocabularyList;
    }

    public void setVocabularyList(List<Vocabulary> vocabularyList) {
        this.vocabularyList = vocabularyList;
    }
}
