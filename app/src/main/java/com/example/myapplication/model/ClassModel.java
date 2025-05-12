package com.example.myapplication.model;

import java.util.List;

public class ClassModel {
    private String id;
    private String name;
    private String description;
    private String creater;
    private boolean allowMembersToAdd;
    private List<String> members;
    private List<String> folderIds;
    private List<String> courseIds;
    public ClassModel() { }

    public ClassModel(String name,
                      String description,
                      String creater,
                      boolean allowMembersToAdd,
                      List<String> members,
                      List<String> folderIds,
                      List<String> courseIds) {
        this.name = name;
        this.description = description;
        this.creater = creater;
        this.allowMembersToAdd = allowMembersToAdd;
        this.members = members;
        this.folderIds = folderIds;
        this.courseIds = courseIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreater() {
        return creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAllowMembersToAdd() {
        return allowMembersToAdd;
    }

    public void setAllowMembersToAdd(boolean allowMembersToAdd) {
        this.allowMembersToAdd = allowMembersToAdd;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<String> getFolderIds() {
        return folderIds;
    }

    public void setFolderIds(List<String> folderIds) {
        this.folderIds = folderIds;
    }

    public List<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<String> courseIds) {
        this.courseIds = courseIds;
    }
}
