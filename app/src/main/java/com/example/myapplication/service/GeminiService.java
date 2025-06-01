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

/**
 * GeminiService phiên bản mới (dùng GenerativeModelFutures) –
 * chỉ khác là đổi addText(...) thành setText(...)
 */
public class GeminiService {

    public interface GeminiCallback {
        void onSuccess(List<AIQuestion> questions);
        void onFailure(String errorMessage);
    }

    // Một executor đơn giản để chạy callback của Future
    private static final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Tạo 2 câu hỏi (meaning + tense) cho mỗi vocabulary.
     * Sử dụng GenerativeModelFutures thay cho OkHttp/REST.
     */
    public static void generateQuestionsWithGemini(List<Vocabulary> vocabList, GeminiCallback callback) {
        // 1. Khởi tạo GenerativeModel (model name và API key)
        GenerativeModel gm = new GenerativeModel(
                /* modelName */ "gemini-1.5-flash",
                /* apiKey    */ BuildConfig.GEMINI_API_KEY
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // 2. Xây dựng prompt giống như trước, nhưng dùng Content(role="user")
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that creates multiple-choice fill-in-the-blank questions. ")
                .append("For each vocabulary word provided, generate 2 questions:\n")
                .append("1. A question to choose the correct meaning of the word in context.\n")
                .append("2. A question to choose the correct verb tense/form of the word in a sentence.\n\n")
                .append("Each question must have exactly these keys:\n")
                .append("- \"word\": the original vocabulary word (string)\n")
                .append("- \"type\": either \"meaning\" or \"tense\"\n")
                .append("- \"question\": the sentence with a blank and instruction (string)\n")
                .append("- \"options\": an array of exactly 4 choices (list of strings)\n")
                .append("- \"correctAnswer\": the exact correct choice text (string)\n")
                .append("- \"explanation\": a short explanation why that choice is correct (string)\n\n")
                .append("Return a JSON array of question objects. No extra fields, no commentary, no markdown. ")
                .append("Example format:\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"word\": \"run\",\n")
                .append("    \"type\": \"meaning\",\n")
                .append("    \"question\": \"What does 'run' mean in the sentence: 'I like to ____ every morning.'?\",\n")
                .append("    \"options\": [\"to move quickly on feet\", \"to cook food\", \"to build\", \"to read\"],\n")
                .append("    \"correctAnswer\": \"to move quickly on feet\",\n")
                .append("    \"explanation\": \"In this sentence, 'run' means 'to move quickly on feet'.\"\n")
                .append("  },\n")
                .append("  {\n")
                .append("    \"word\": \"run\",\n")
                .append("    \"type\": \"tense\",\n")
                .append("    \"question\": \"Choose the correct form of 'run' in the past tense for: 'I ____ fast yesterday.'\",\n")
                .append("    \"options\": [\"ran\", \"runs\", \"running\", \"runned\"],\n")
                .append("    \"correctAnswer\": \"ran\",\n")
                .append("    \"explanation\": \"The past tense of 'run' is 'ran'.\"\n")
                .append("  }\n")
                .append("  // ... and so on for each word\n")
                .append("]\n\n")
                .append("Now generate questions for these vocabularies:\n");
        for (Vocabulary v : vocabList) {
            prompt.append("- Word: ").append(v.getWord())
                    .append(", Meaning: ").append(v.getMeaning())
                    .append("\n");
        }

        // 3. Đóng gói prompt vào Content(role="user")
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();
        // 4. Tạo history và bắt đầu chat
        List<Content> history = new ArrayList<>();
        history.add(userContent);
        ChatFutures chat = model.startChat(history);

        // 5. Gửi prompt tới Gemini
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        // 6. Khi có phản hồi, parse JSON rồi trả callback
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String json = response.getText();

                String raw = response.getText();
                android.util.Log.d("GeminiService", "RAW_AI_RESPONSE=\n" + json);
                // Parse JSON thành List<AIQuestion>
                // 6.2. Nếu có code fence ``` hoặc ```json, chúng ta phải loại bỏ
                //     Ví dụ raw có thể bắt đầu bằng "```json\n[...]" và kết thúc bằng "```"
                //     Ta sẽ strip các ký tự fence ở đầu và cuối.
                if (raw.startsWith("```")) {
                    // Xóa phần fence mở đầu, ví dụ "```json\n"
                    int firstNewline = raw.indexOf('\n');
                    if (firstNewline != -1) {
                        raw = raw.substring(firstNewline + 1);
                    }
                }
                // Bây giờ raw có thể đang kết thúc bằng "```"
                if (raw.endsWith("```")) {
                    // Xóa toàn bộ phần fence đóng ở cuối
                    int fenceIdx = raw.lastIndexOf("```");
                    if (fenceIdx != -1) {
                        raw = raw.substring(0, fenceIdx);
                    }
                }

                // 6.3. Tiếp tục tìm đến JSON array: từ dấu '[' đầu tiên đến ']' cuối cùng
                int startIdx = raw.indexOf('[');
                int endIdx   = raw.lastIndexOf(']');
                if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                    callback.onFailure("Không tìm thấy JSON array trong phản hồi AI.");
                    return;
                }
                String jsonArrayOnly = raw.substring(startIdx, endIdx + 1);

                // 6.4. Parse JSON array thành List<AIQuestion>
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
