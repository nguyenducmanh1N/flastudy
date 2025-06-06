package com.example.myapplication.screens.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.model.Message;
import com.example.myapplication.utils.GeminiFunctionCalling;
import com.example.myapplication.viewmodel.MessengerViewModel;

import java.io.IOException;
import java.util.List;

public class MessengerFragment extends Fragment {

    private List<Message> messageList;
    private GeminiFunctionCalling gemini;
    private MessageAdapter adapter;

    private Button sendButton;
    private EditText editMessage;
    private RecyclerView recyclerView;
    private ImageView newChatImageView;

    private MessengerViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messenger, container, false);

        gemini = new GeminiFunctionCalling(BuildConfig.GEMINI_API_KEY, getContext());

        viewModel = new ViewModelProvider(requireActivity()).get(MessengerViewModel.class);
        messageList = viewModel.messageList;

        sendButton = view.findViewById(R.id.button_send);
        editMessage = view.findViewById(R.id.edit_message);
        recyclerView = view.findViewById(R.id.recycler_view);
        newChatImageView = view.findViewById(R.id.new_chat_imgView);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        newChatImageView.setOnClickListener(v -> {
            messageList.clear();
            editMessage.setText("");
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(0);
        });

        sendButton.setOnClickListener(v -> sendMessage());

        editMessage.setOnEditorActionListener((v1, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                sendMessage();  // Gửi tin nhắn khi nhấn Enter
                return true; // Đã xử lý
            }
            return false; // Không xử lý, để hệ thống xử lý tiếp
        });

        return view;
    }

    private void sendMessage() {
        String userMsg = editMessage.getText().toString().trim();
        if (!userMsg.isEmpty()) {
            // Ẩn bàn phím khi nhấn gửi
            View currentFocus = requireActivity().getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }

            messageList.add(new Message(userMsg, true));
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            editMessage.setText("");

            new Thread(() -> {
                try {
                    String response = gemini.callGeminiWithFunction(messageList);
                    requireActivity().runOnUiThread(() -> {
                        messageList.add(new Message(response, false));
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        Log.d("GeminiFunctionCalling", "Message List: " + messageList);
                    });
                } catch (IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        messageList.remove(messageList.size() - 1);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    });
                }
            }).start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        recyclerView.scrollToPosition(messageList.size() - 1);
    }
}