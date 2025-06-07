package com.example.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myapplication.model.Message;
import com.example.myapplication.screens.feature.ClassDetailActivity;
import com.example.myapplication.screens.feature.FolderDetailActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GeminiFunctionCalling {
    private final FirestoreRepository firestoreRepository = new FirestoreRepository();
    private final String apiKey;
    private final Context context;
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiFunctionCalling(String apiKey, Context context) {
        this.apiKey = apiKey;
        this.context = context;
    }

    private JsonObject getFolderNamesFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "get_folder_names");
        functionDecl.addProperty("description", "Lấy thông tin về tên/danh sách các thư mục hiện có của người dùng.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject getClassNamesFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "get_class_names");
        functionDecl.addProperty("description", "Lấy thông tin về tên/danh sách các lớp học hiện có của người dùng.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject navigateToFolderDetailActivityByNameFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject nameParam = new JsonObject();
        nameParam.addProperty("type", "string");
        nameParam.addProperty("description", "Tên thư mục cần điều hướng, ví dụ: 'Học bài'");
        properties.add("name", nameParam);

        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("name");
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "navigate_to_folder_detail_activity_by_name");
        functionDecl.addProperty("description", "Điều hướng/Đi đến đến màn hình chi tiết thư mục dựa trên tên thư mục.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject navigateToClassDetailActivityByNameFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject nameParam = new JsonObject();
        nameParam.addProperty("type", "string");
        nameParam.addProperty("description", "Tên lớp học cần điều hướng, ví dụ: '21CN3'");
        properties.add("name", nameParam);

        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("name");
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "navigate_to_class_detail_activity_by_name");
        functionDecl.addProperty("description", "Điều hướng/Đi đến đến màn hình chi tiết lớp học dựa trên tên lớp học.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject translateEnglishToVietnameseFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject englishTextParam = new JsonObject();
        englishTextParam.addProperty("type", "string");
        englishTextParam.addProperty("description", "Văn bản tiếng việt cần dịch, ví dụ: 'Tiger'");
        properties.add("englishText", englishTextParam);

        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("englishText");
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "translate_english_to_vietnamese");
        functionDecl.addProperty("description", "Dịch văn bản tiếng anh sang tiếng việt.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject createFolderFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject folderNameParam = new JsonObject();
        folderNameParam.addProperty("type", "string");
        folderNameParam.addProperty("description", "Tên thư mục cần tạo, ví dụ: 'Toán'");
        properties.add("folderName", folderNameParam);

        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("folderName");
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "create_folder");
        functionDecl.addProperty("description", "Tạo thư mục mới.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    private JsonObject createClassFunctionDeclaration() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject classNameParam = new JsonObject();
        classNameParam.addProperty("type", "string");
        classNameParam.addProperty("description", "Tên lớp học cần tạo, ví dụ: 'Cấu trúc dữ liệu và giải thuật'");
        properties.add("className", classNameParam);

        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("className");
        parameters.add("required", required);

        JsonObject functionDecl = new JsonObject();
        functionDecl.addProperty("name", "create_class");
        functionDecl.addProperty("description", "Tạo lớp học mới.");
        functionDecl.add("parameters", parameters);

        return functionDecl;
    }

    public String callGeminiWithFunction(List<Message> messageList) throws IOException {
        JsonArray contents = new JsonArray();
        for (Message message : messageList) {
            JsonObject messageContent = new JsonObject();
            messageContent.addProperty("role", message.isUser() ? "user" : "model");

            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", message.getMessage());
            parts.add(textPart);
            messageContent.add("parts", parts);

            contents.add(messageContent);
        }

        JsonArray tools = new JsonArray();
        JsonObject tool = new JsonObject();
        JsonArray functionDeclarations = new JsonArray();
        functionDeclarations.add(getFolderNamesFunctionDeclaration());
        functionDeclarations.add(createFolderFunctionDeclaration());
        functionDeclarations.add(getClassNamesFunctionDeclaration());
        functionDeclarations.add(createClassFunctionDeclaration());
        functionDeclarations.add(navigateToFolderDetailActivityByNameFunctionDeclaration());
        functionDeclarations.add(navigateToClassDetailActivityByNameFunctionDeclaration());
        functionDeclarations.add(translateEnglishToVietnameseFunctionDeclaration());
        tool.add("functionDeclarations", functionDeclarations);
        tools.add(tool);

        JsonObject payload = new JsonObject();
        payload.add("contents", contents);
        payload.add("tools", tools);
        Log.d("GeminiFunctionCalling", "Body JSON: " + payload.toString());

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(GEMINI_URL + "?key=" + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("API call failed: " + response.code());

            String responseBody = response.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            Log.d("GeminiFunctionCalling", "Response JSON: " + responseJson.toString());

            if (responseJson.has("candidates")) {
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (!candidates.isEmpty()) {
                    JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray partsResponse = content.getAsJsonArray("parts");
                        if (!partsResponse.isEmpty()) {
                            JsonObject part = partsResponse.get(0).getAsJsonObject();
                            if (part.has("functionCall")) {
                                JsonObject functionCall = part.getAsJsonObject("functionCall");
                                String functionName = functionCall.get("name").getAsString();
                                JsonObject args = functionCall.getAsJsonObject("args");

                                String result = handleFunctionCall(functionName, args);

//                                if ("get_weather".equals(functionName)) {
//                                    String location = args.get("location").getAsString();
//                                    result = fetchWeatherFromAPI(location);
//                                } else if ("get_folder_names".equals(functionName)) {
//
//                                }else {
//                                    result = "Không xác định được hàm.";
//                                }

                                JsonObject functionResponsePart = new JsonObject();
                                functionResponsePart.addProperty("name", functionName);
                                JsonObject responsePart = new JsonObject();
                                responsePart.addProperty("result", result);
                                functionResponsePart.add("response", responsePart);

                                contents.add(createModelMessageWithFunctionCall(functionCall));
                                contents.add(createUserMessageWithFunctionResponse(functionResponsePart));

                                return callGeminiSecondTime(contents);
                            } else if (part.has("text")) {
                                return part.get("text").getAsString();
                            }
                        }
                    }
                }
            }

            return "Không có phản hồi phù hợp từ mô hình.";
        }
    }

    private String handleFunctionCall(String functionName, JsonObject args) {
        switch (functionName) {
            case "get_folder_names":
                List<String> folderNames = firestoreRepository.getFolderNamesBlocking();
                return "Danh sách thư mục hiện có của người dùng gồm: " + String.join(", ", folderNames);
            case "create_folder":
                return firestoreRepository.createFolderBlocking(args.get("folderName").getAsString());
            case "get_class_names":
                List<String> classNames = firestoreRepository.getClassNamesBlocking();
                return "Danh sách lớp học hiện có của người dùng gồm: " + String.join(", ", classNames);
            case "create_class":
                return firestoreRepository.createClassBlocking(args.get("className").getAsString());
            case "navigate_to_folder_detail_activity_by_name":
                String folderName = args.get("name").getAsString();
                String folderId = firestoreRepository.findFolderIdByNameBlocking(folderName);
                context.startActivity(new Intent(context, FolderDetailActivity.class).putExtra("folderId", folderId));
                return "Đã điều hướng đến màn hình chi tiết thư mục " + folderName + ".";
            case "navigate_to_class_detail_activity_by_name":
                String className = args.get("name").getAsString();
                String classId = firestoreRepository.findClassIdByNameBlocking(className);
                context.startActivity(new Intent(context, ClassDetailActivity.class).putExtra("classId", classId));
                return "Đã điều hướng đến màn hình chi tiết lớp học " + className + ".";
            case "translate_english_to_vietnamese":
                String englishText = args.get("englishText").getAsString();
                return translateEnglishToVietnameseSync(englishText);
            default:
                return "Không xác định được hàm.";
        }
    }

    private String callGeminiSecondTime(JsonArray contents) throws IOException {
        JsonObject payload = new JsonObject();
        payload.add("contents", contents);
        Log.d("GeminiFunctionCalling", "Body JSON Second Time: " + payload.toString());

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(GEMINI_URL + "?key=" + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Lỗi khi gọi lại Gemini: " + response.code();
            }

            String responseBody = response.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

            return responseJson.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }
    }

    private JsonObject createModelMessageWithFunctionCall(JsonObject functionCall) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "model");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.add("functionCall", functionCall);
        parts.add(part);
        message.add("parts", parts);
        return message;
    }

    private JsonObject createUserMessageWithFunctionResponse(JsonObject functionResponsePart) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.add("functionResponse", functionResponsePart);
        parts.add(part);
        message.add("parts", parts);
        return message;
    }

    private String translateEnglishToVietnameseSync(String englishText) {
        OkHttpClient client = new OkHttpClient();
        String encodedText;

        try {
            encodedText = URLEncoder.encode(englishText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedText = englishText;
        }

        String url = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=en|vi";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Lỗi dịch vụ: " + response.code();
            }

            JSONObject jsonResponse = new JSONObject(response.body().string());
            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            String mainTranslation = responseData.getString("translatedText").trim();

            List<String> translations = new ArrayList<>();
            if (!mainTranslation.isEmpty()) {
                translations.add(mainTranslation);
            }

            // Thêm các bản dịch từ matches[]
            if (jsonResponse.has("matches")) {
                JSONArray matches = jsonResponse.getJSONArray("matches");
                for (int i = 0; i < matches.length(); i++) {
                    JSONObject match = matches.getJSONObject(i);
                    String translation = match.getString("translation").trim();
                    if (!translations.contains(translation)) {
                        translations.add(translation);
                    }
                }
            }

            return "Nghĩa của từ '" + englishText + "' là: " + String.join(", ", translations);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return "Lỗi khi dịch văn bản.";
        }
    }



}