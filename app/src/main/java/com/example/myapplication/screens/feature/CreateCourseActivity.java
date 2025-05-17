package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Vocabulary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateCourseActivity extends AppCompatActivity {

    public static final String EXTRA_FOLDER_ID = "folderId";
    private LinearLayout containerTerms;
    private ImageButton btnAdd;
    private EditText inputCourseTitle;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Handler uiHandler;
    private String folderId;

    private static class DictionaryData {
        List<String> meanings;
        String example;
        String audio;
        String phonetic;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        inputCourseTitle = findViewById(R.id.inputTitle);
        containerTerms = findViewById(R.id.containerTerms);
        btnAdd = findViewById(R.id.btnAdd);
        uiHandler = new Handler(Looper.getMainLooper());

        btnAdd.setOnClickListener(v -> addTermLayout());
        addTermLayout();

        findViewById(R.id.btnCloseCreateCourse).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveCourse());

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        if (folderId == null) {
            Toast.makeText(this, "Không xác định được thư mục", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void addTermLayout() {
        View termView = LayoutInflater.from(this)
                .inflate(R.layout.item_term, containerTerms, false);
        AutoCompleteTextView edtTerm = termView.findViewById(R.id.edtTerm);
        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
        ImageButton btnRemove = termView.findViewById(R.id.btnRemoveTerm);

        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>());
        edtTerm.setAdapter(termAdapter);
        edtTerm.setThreshold(1);

        ArrayAdapter<String> defAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>());
        edtDefinition.setAdapter(defAdapter);
        edtDefinition.setThreshold(1);

        edtTerm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                String input = s.toString().trim();
                if (input.length() >= 1) {

                    fetchSuggestions(input, suggestions -> uiHandler.post(() -> {
                        termAdapter.clear();
                        termAdapter.addAll(suggestions);
                        termAdapter.notifyDataSetChanged();
                        if (!suggestions.isEmpty()) edtTerm.showDropDown();
                    }));
                }
            }
        });

        edtTerm.setOnItemClickListener((parent, view, pos, id) -> {
            String selected = termAdapter.getItem(pos);

            fetchTranslationSuggestions(selected, defs -> uiHandler.post(() -> {

                defAdapter.clear();
                defAdapter.addAll(defs);
                defAdapter.notifyDataSetChanged();
                if (!defs.isEmpty()) {
                    edtDefinition.setText(defs.get(0));
                    edtDefinition.setSelection(defs.get(0).length());
                    edtDefinition.showDropDown();
                }
            }));

        });

//        edtTerm.setOnItemClickListener((parent, view, pos, id) -> {
//            String selected = termAdapter.getItem(pos);
//            fetchDefinitionsDictionary(selected, dict -> uiHandler.post(() -> {
//                // fill adapter nghĩa
//                defAdapter.clear();
//                defAdapter.addAll(dict.meanings);
//                defAdapter.notifyDataSetChanged();
//                if (!dict.meanings.isEmpty()) {
//                    edtDefinition.setText(dict.meanings.get(0));
//                    edtDefinition.setSelection(dict.meanings.get(0).length());
//                    edtDefinition.showDropDown();
//                }
//                // lưu DictionaryData vào termView để dùng khi save
//                termView.setTag(R.id.tag_definition_data, dict);
//            }));
//        });

        btnRemove.setOnClickListener(v -> containerTerms.removeView(termView));
        containerTerms.addView(termView);
    }

    private void saveCourse() {
        String title = inputCourseTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Nhập tiêu đề Course", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Vocabulary> vocabList = new ArrayList<>();
        for (int i = 0; i < containerTerms.getChildCount(); i++) {
            View v = containerTerms.getChildAt(i);
            String word = ((EditText) v.findViewById(R.id.edtTerm))
                    .getText().toString().trim();
            String meaning = ((EditText) v.findViewById(R.id.edtDefinition))
                    .getText().toString().trim();

            if (!word.isEmpty() && !meaning.isEmpty()) {
                Vocabulary vocab = new Vocabulary(word, meaning, "","","");
                // lấy DictionaryData lưu trước đó (nếu có)
                Object tag = v.getTag(R.id.tag_definition_data);
                if (tag instanceof DictionaryData) {
                    DictionaryData dict = (DictionaryData) tag;
                    vocab.setExample(dict.example);
                    vocab.setAudio(dict.audio);
                    vocab.setPhonetic(dict.phonetic);
                }
                vocabList.add(vocab);
            }
        }
        if (vocabList.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 thuật ngữ", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        String courseId = db.collection("users").document(uid)
                .collection("folders").document(folderId)
                .collection("courses").document().getId();
        Course course = new Course(title, System.currentTimeMillis(),
                vocabList, folderId, currentUser.getEmail());
        course.setId(courseId);

        DocumentReference courseRef = db.collection("users").document(uid)
                .collection("folders").document(folderId)
                .collection("courses").document(courseId);

        courseRef.set(course)
                .addOnSuccessListener(a -> commitVocabBatch(courseRef, vocabList))
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Lỗi lưu Course: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void commitVocabBatch(DocumentReference courseRef, List<Vocabulary> vocabList) {
        WriteBatch batch = db.batch();
        CollectionReference vocabCol = courseRef.collection("vocabularies");
        for (Vocabulary vocab : vocabList) {
            DocumentReference vocabRef = vocabCol.document();
            vocab.setId(vocabRef.getId());
            batch.set(vocabRef, vocab);
        }
        batch.commit()
                .addOnSuccessListener(a2 -> Toast.makeText(this,
                        "Tạo Course và lưu từ vựng thành công", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Lỗi lưu từ vựng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void fetchSuggestions(String input, Consumer<List<String>> callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.datamuse.com/sug?s=" + input;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                List<String> list = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    for (int i = 0; i < arr.length() && i < 5; i++) {
                        list.add(arr.getJSONObject(i).getString("word"));
                    }
                } catch (JSONException e) { e.printStackTrace(); }
                callback.accept(list);
            }
        });
    }


//    private void fetchDefinitionsDictionary(String term, Consumer<List<String>> callback) {
//        OkHttpClient client = new OkHttpClient();
//        String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + term;
//        Request request = new Request.Builder()
//                .url(url)
//                .addHeader("Accept", "application/json")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//                callback.accept(Collections.singletonList("Không có kết nối"));
//            }
//
//            @Override public void onResponse(Call call, Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    callback.accept(Collections.singletonList("Lỗi server: " + response.code()));
//                    return;
//                }
//                List<String> defs = new ArrayList<>();
//                try {
//                    JSONArray arr = new JSONArray(response.body().string());
//                    JSONObject entry = arr.getJSONObject(0);
//                    JSONArray meanings = entry.getJSONArray("meanings");
//                    for (int i = 0; i < meanings.length() && defs.size() < 5; i++) {
//                        JSONObject m = meanings.getJSONObject(i);
//                        JSONArray definitions = m.getJSONArray("definitions");
//                        for (int j = 0; j < definitions.length() && defs.size() < 5; j++) {
//                            defs.add(definitions.getJSONObject(j).getString("definition"));
//                        }
//                    }
//                    if (defs.isEmpty()) defs.add("Chưa có định nghĩa");
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    defs = Collections.singletonList("Lỗi phân tích dữ liệu");
//                }
//                callback.accept(defs);
//            }
//        });
//    }
private void fetchDefinitionsDictionary(String term, Consumer<DictionaryData> callback) {
    OkHttpClient client = new OkHttpClient();
    String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + term;
    Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build();

    client.newCall(request).enqueue(new Callback() {
        @Override public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            DictionaryData err = new DictionaryData();
            err.meanings = Collections.singletonList("Không có kết nối");
            err.example = "";
            err.audio = "";
            err.phonetic = "";
            callback.accept(err);
        }

        @Override public void onResponse(Call call, Response response) throws IOException {
            DictionaryData data = new DictionaryData();
            data.meanings = new ArrayList<>();
            data.example = "";
            data.audio = "";
            data.phonetic = "";

            if (!response.isSuccessful()) {
                data.meanings = Collections.singletonList("Lỗi server: " + response.code());
                callback.accept(data);
                return;
            }

            try {
                JSONArray arr = new JSONArray(response.body().string());
                JSONObject entry = arr.getJSONObject(0);

                // phonetic chung
                data.phonetic = entry.optString("phonetic", "");

                // lấy audio đầu tiên có link
                JSONArray phonetics = entry.optJSONArray("phonetics");
                if (phonetics != null) {
                    for (int i = 0; i < phonetics.length(); i++) {
                        String audioUrl = phonetics.getJSONObject(i).optString("audio", "");
                        if (!audioUrl.isEmpty()) {
                            data.audio = audioUrl;
                            break;
                        }
                    }
                }

                // meanings + example đầu tiên
                JSONArray meaningsArr = entry.getJSONArray("meanings");
                outer:
                for (int i = 0; i < meaningsArr.length() && data.meanings.size() < 5; i++) {
                    JSONObject m = meaningsArr.getJSONObject(i);
                    JSONArray defs = m.getJSONArray("definitions");
                    for (int j = 0; j < defs.length() && data.meanings.size() < 5; j++) {
                        JSONObject defObj = defs.getJSONObject(j);
                        data.meanings.add(defObj.getString("definition"));
                        if (data.example.isEmpty() && defObj.has("example")) {
                            data.example = defObj.getString("example");
                        }
                        if (!data.example.isEmpty() && data.meanings.size() >= 5) {
                            break outer;
                        }
                    }
                }
                if (data.meanings.isEmpty()) {
                    data.meanings.add("Chưa có định nghĩa");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                data.meanings = Collections.singletonList("Lỗi phân tích dữ liệu");
            }
            callback.accept(data);
        }
    });
}




    private void fetchDefinitions(String term, Consumer<List<String>> callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + term;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                callback.accept(Collections.singletonList("Không có kết nối"));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.accept(Collections.singletonList("Lỗi server: " + response.code()));
                    return;
                }
                List<String> defs = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    JSONObject entry = arr.getJSONObject(0);
                    JSONArray meanings = entry.getJSONArray("meanings");
                    for (int i = 0; i < meanings.length() && defs.size() < 5; i++) {
                        JSONObject m = meanings.getJSONObject(i);
                        JSONArray definitions = m.getJSONArray("definitions");
                        for (int j = 0; j < definitions.length() && defs.size() < 5; j++) {
                            defs.add(definitions.getJSONObject(j).getString("definition"));
                        }
                    }
                    if (defs.isEmpty()) defs.add("Chưa có định nghĩa");
                } catch (JSONException e) {
                    e.printStackTrace();
                    defs = Collections.singletonList("Lỗi phân tích dữ liệu");
                }
                callback.accept(defs);
            }
        });
    }



    private void fetchTranslationSuggestions(String term, Consumer<List<String>> callback) {
        OkHttpClient client = new OkHttpClient();
        String encoded;
        try {

            encoded = URLEncoder.encode(term, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encoded = term;
        }
        String url = "https://api.mymemory.translated.net/get"
                + "?q=" + encoded
                + "&langpair=en|vi";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                callback.accept(Collections.singletonList("Không có kết nối"));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                List<String> results = new ArrayList<>();
                if (!response.isSuccessful()) {
                    results.add("Lỗi server: " + response.code());
                } else {
                    try {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject respData = root.getJSONObject("responseData");
                        String main = respData.optString("translatedText");
                        if (!main.isEmpty()) results.add(main);
                        JSONArray matches = root.optJSONArray("matches");
                        if (matches != null) {
                            for (int i = 0; i < matches.length() && results.size() < 5; i++) {
                                String txt = matches.getJSONObject(i)
                                        .optString("translation");
                                if (!results.contains(txt) && !txt.isEmpty()) {
                                    results.add(txt);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        results.clear();
                        results.add("Lỗi phân tích dữ liệu");
                    }
                }
                if (results.isEmpty()) results.add("Chưa có định nghĩa");
                callback.accept(results);
            }
        });
    }
}