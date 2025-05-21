package com.example.myapplication.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.Message;
import com.google.ai.client.generativeai.type.Content;

import java.util.ArrayList;
import java.util.List;

public class MessengerViewModel extends ViewModel {
    public List<Message> messageList = new ArrayList<>();
    public List<Content> history = new ArrayList<>();
}