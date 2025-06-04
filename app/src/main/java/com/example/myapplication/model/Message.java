package com.example.myapplication.model;

public class Message {
    private String message;
    private boolean isUser;

    public Message(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", isUser=" + isUser +
                '}';
    }
}

