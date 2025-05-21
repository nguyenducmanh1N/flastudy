package com.example.myapplication.screens.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.model.Message;
import com.example.myapplication.viewmodel.MessengerViewModel;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessengerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessengerFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private List<Message> messageList;

    private List<Content> history;

    ChatFutures chat;
    Executor executor = Executors.newSingleThreadExecutor();
    private MessageAdapter adapter;

    Button sendButton;
    EditText editMessage;
    RecyclerView recyclerView;

    ImageView newChatImageView;

    MessengerViewModel viewModel;

    public MessengerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MessengerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MessengerFragment newInstance(String param1, String param2) {
        MessengerFragment fragment = new MessengerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messenger, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MessengerViewModel.class);

        messageList = viewModel.messageList;
        history = viewModel.history;

        // The Gemini 1.5 models are versatile and work with multi-turn conversations (like chat)
        GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-1.5-flash",
// Access your API key as a Build Configuration variable (see "Set up your API key" above)
                /* apiKey */ BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        sendButton = view.findViewById(R.id.button_send);
        editMessage = view.findViewById(R.id.edit_message);
        recyclerView = view.findViewById(R.id.recycler_view);
        newChatImageView = view.findViewById(R.id.new_chat_imgView);

        // Khởi tạo adapter và RecyclerView
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        newChatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageList.clear();
                history.clear();

                adapter.notifyDataSetChanged();

                chat = model.startChat(history);

                recyclerView.scrollToPosition(0);
            }
        });

        sendButton.setOnClickListener(v -> {
            chat = model.startChat(history);

            String userMsg = editMessage.getText().toString().trim();
            if (!userMsg.isEmpty()) {

                Content.Builder userMessageBuilder = new Content.Builder();
                userMessageBuilder.setRole("user");
                userMessageBuilder.addText(userMsg);
                Content userMessage = userMessageBuilder.build();
                history.add(userMessage);
                messageList.add(new Message(userMsg, true));

                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
                editMessage.setText("");

                ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userMessage);

                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String resultText = result.getText();
                        System.out.println(resultText);

                        Content.Builder modelMessageBuilder = new Content.Builder();
                        modelMessageBuilder.setRole("model");
                        modelMessageBuilder.addText(resultText);
                        Content modelMessage = modelMessageBuilder.build();
                        history.add(modelMessage);
                        messageList.add(new Message(resultText, false));

                        chat = model.startChat(history);

                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }
                }, executor);
            }
        });

        return view;
    }
}