package com.example.myapplication.service;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class GeminiApi {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta2/models/chat-bison-001:generateMessage";
    private static final String API_KEY = "AIzaSyA5O1HTYscF4EVCXoIzsv1ca4_NbU_0N-U";


    public interface GeminiCallback {
        void onResponse(String json);
        void onFailure(String error);
    }

    public static void sendPrompt(String prompt, GeminiCallback cb) {
        OkHttpClient client = new OkHttpClient();


        String jsonPayload = "{\"prompt\": " +
                new com.google.gson.Gson().toJson(prompt) +
                ", \"temperature\":0.7}";

        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.get("application/json; charset=utf-8")
        );

        Request req = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onFailure(e.getMessage());
            }
            @Override public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) {
                    cb.onFailure("HTTP " + res.code());
                } else {
                    String respBody = res.body().string();
                    cb.onResponse(respBody);
                }
            }
        });
    }
}
