package com.example.myapplication.model;

import java.util.List;

public class Course {
    private String id;
    private String title;
    private long createdAt;



    private String creater;
    private List<Vocabulary> vocabularyList;
    private String folderId;
    private List<AIQuestion> questionList;

    public Course() {}

    public Course(String title, long createdAt, List<Vocabulary> vocabularyList, String folderId,String creater) {
        this.title = title;
        this.createdAt = createdAt;
        this.vocabularyList = vocabularyList;
        this.folderId = folderId;
        this.creater= creater;
    }

    public List<AIQuestion> getQuestionList() {
        return questionList;
    }

    public void setQuestionList(List<AIQuestion> questionList) {
        this.questionList = questionList;
    }

    public String getCreater() {
        return creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }
    public String getFolderId() {
        return folderId;
    }
    public void setFolderId(String folderId) {
        this.folderId = folderId;
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
