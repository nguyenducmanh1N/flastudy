package com.example.myapplication.service;

import com.example.myapplication.model.AIQuestion;
import com.example.myapplication.model.Vocabulary;
import com.example.myapplication.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiService {

    public interface GeminiCallback {
        void onSuccess(List<AIQuestion> questions);
        void onFailure(String errorMessage);
    }


    private static final Executor executor = Executors.newSingleThreadExecutor();


    public static void generateQuestionsWithGemini(List<Vocabulary> vocabList, GeminiCallback callback) {
        GenerativeModel gm = new GenerativeModel(
                /* modelName */ "gemini-1.5-flash",
                /* apiKey    */ BuildConfig.GEMINI_API_KEY
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);


        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that creates multiple-choice fill-in-the-blank questions for learning vocabulary. ")
                .append("For each vocabulary word provided, generate 3 questions:\n")
                .append("1. A 'meaning' question: choose the correct meaning of the word in context.\n")
                .append("2. A 'tense' question: choose the correct verb tense/form of the word in a sentence.\n")
                .append("3. A 'fill' question: choose the correct vocabulary word to fill in the blank from 4 options ")
                .append("(all options must be distinct words taken from the provided vocabulary list, including the target word itself).\n\n")
                .append("Each question object must have exactly these keys:\n")
                .append("- \"word\": the original vocabulary word (string)\n")
                .append("- \"type\": one of \"meaning\", \"tense\", or \"fill\"\n")
                .append("- \"question\": the sentence with a blank and instruction (string)\n")
                .append("- \"options\": an array of exactly 4 choices (list of strings)\n")
                .append("- \"correctAnswer\": the exact correct choice text (string)\n")
                .append("- \"explanation_en\": a short explanation in English why that choice is correct (string)\n")
                .append("- \"explanation_vi\": a short explanation in Vietnamese why that choice is correct (string)\n\n")
                .append("Return a JSON array of question objects and nothing else. Do not include any extra fields, commentary, or markdown. ")
                .append("Example format:\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"word\": \"run\",\n")
                .append("    \"type\": \"meaning\",\n")
                .append("    \"question\": \"What does 'run' mean in the sentence: 'I like to ____ every morning.'?\",\n")
                .append("    \"options\": [\"to move quickly on feet\", \"to cook food\", \"to build\", \"to read\"],\n")
                .append("    \"correctAnswer\": \"to move quickly on feet\",\n")
                .append("    \"explanation_en\": \"In this sentence, 'run' means 'to move quickly on feet.'\",\n")
                .append("    \"explanation_vi\": \"Trong câu này, 'run' nghĩa là 'chạy nhanh trên đôi chân.'\"\n")
                .append("  },\n")
                .append("  {\n")
                .append("    \"word\": \"run\",\n")
                .append("    \"type\": \"tense\",\n")
                .append("    \"question\": \"Choose the correct form of 'run' in the past tense for: 'I ____ fast yesterday.'\",\n")
                .append("    \"options\": [\"ran\", \"runs\", \"running\", \"runned\"],\n")
                .append("    \"correctAnswer\": \"ran\",\n")
                .append("    \"explanation_en\": \"The past tense of 'run' is 'ran.'\",\n")
                .append("    \"explanation_vi\": \"Quá khứ của 'run' là 'ran.'\"\n")
                .append("  },\n")
                .append("  {\n")
                .append("    \"word\": \"run\",\n")
                .append("    \"type\": \"fill\",\n")
                .append("    \"question\": \"Select the correct word to fill in: 'She loves to ____ in the park every evening.'\",\n")
                .append("    \"options\": [\"run\", \"eat\", \"sleep\", \"write\"],\n")
                .append("    \"correctAnswer\": \"run\",\n")
                .append("    \"explanation_en\": \"In this context, 'run' means to move quickly on feet, which fits the sentence about a daily activity.\",\n")
                .append("    \"explanation_vi\": \"Trong ngữ cảnh này, 'run' nghĩa là 'chạy nhanh trên đôi chân,' phù hợp với câu nói về hoạt động hằng ngày.\"\n")
                .append("  }\n")
                .append("]\n\n")
                .append("Now generate questions for these vocabularies (only output JSON array!):\n");
        for (Vocabulary v : vocabList) {
            prompt.append("- Word: ").append(v.getWord())
                    .append(", Meaning: ").append(v.getMeaning())
                    .append("\n");
        }


        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();


        List<Content> history = new ArrayList<>();
        history.add(userContent);
        ChatFutures chat = model.startChat(history);

        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String raw = response.getText();

                android.util.Log.d("GeminiService", "RAW_AI_RESPONSE=\n" + raw);
                if (raw.startsWith("```")) {
                    int firstNewline = raw.indexOf('\n');
                    if (firstNewline != -1) {
                        raw = raw.substring(firstNewline + 1);
                    }
                }
                if (raw.endsWith("```")) {
                    int fenceIdx = raw.lastIndexOf("```");
                    if (fenceIdx != -1) {
                        raw = raw.substring(0, fenceIdx);
                    }
                }
                int startIdx = raw.indexOf('[');
                int endIdx   = raw.lastIndexOf(']');
                if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                    callback.onFailure("Không tìm thấy JSON array trong phản hồi AI.");
                    return;
                }
                String jsonArrayOnly = raw.substring(startIdx, endIdx + 1);
                try {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<AIQuestion>>() {}.getType();
                    List<AIQuestion> questions = gson.fromJson(jsonArrayOnly, listType);
                    if (questions == null) {
                        callback.onFailure("Không parse được JSON (list null).");
                    } else {
                        callback.onSuccess(questions);
                    }
                } catch (Exception e) {
                    callback.onFailure("Lỗi parse JSON: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure("Lỗi khi gọi Gemini: " + t.getMessage());
            }
        }, executor);
    }

}
