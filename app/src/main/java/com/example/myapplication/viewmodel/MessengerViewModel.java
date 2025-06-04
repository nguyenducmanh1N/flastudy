package com.example.myapplication.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessengerViewModel extends ViewModel {
    public List<Message> messageList = new ArrayList<>();
}