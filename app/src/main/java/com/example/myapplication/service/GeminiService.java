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
        prompt.append("You are a vocabulary-quiz-generation AI. ")
                .append("Create multiple-choice questions that are: high-quality, varied, and shuffled so that no two questions for the same word appear consecutively. ")
                .append("Across all questions, interleave the three types (“meaning”, “tense”, “fill”) in random order. ")
                .append("Each question object must have EXACTLY these fields: ")
                .append("[\"word\", \"type\", \"question\", \"options\", \"correctAnswer\", \"explanation_en\", \"explanation_vi\"].\n\n")
                .append("Requirements:\n")
                .append("1. Do NOT reveal the correct answer in the question text or adjacent context.\n")
                .append("2. Shuffle the order of the four options randomly.\n")
                .append("3. Ensure no option list repeats any word across all questions.\n")
                .append("4. Avoid using the exact same distractors or sentence structures you generated previously.\n")
                .append("5. Output ONLY a single JSON array of question objects—no markdown, no commentary.\n\n")
                .append("Example output:\n")
                .append("[{ \"word\":\"run\",\"type\":\"meaning\",\"question\":\"In “She decided to ___ a marathon next month,” what does the blank mean?\",")
                .append("\"options\":[\"to move quickly\",\"to read books\",\"to build houses\",\"to cook food\"],")
                .append("\"correctAnswer\":\"to move quickly\",")
                .append("\"explanation_en\":\"Here, 'run' means 'to move quickly' in the context of a marathon.\",")
                .append("\"explanation_vi\":\"Trong ngữ cảnh chạy marathon, 'run' nghĩa là 'chạy nhanh'.\" }]\n\n")
                .append("Now generate for the following vocabularies. ")
                .append("Intermix types and words randomly, and do not group same-word questions together:\n");

        for (Vocabulary v : vocabList) {
            prompt.append("- \"").append(v.getWord())
                    .append("\" (meaning: ").append(v.getMeaning()).append(")\n");
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
