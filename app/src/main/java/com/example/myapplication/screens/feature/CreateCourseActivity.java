package com.example.myapplication.screens.feature;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Vocabulary;
import com.example.myapplication.screens.feature.act.CameraActivity;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateCourseActivity extends AppCompatActivity {

    public static final String EXTRA_FOLDER_ID = "folderId";
    public static final String EXTRA_IS_EDIT = "isEdit";
    public static final String EXTRA_COURSE_ID = "courseId";
    private static final Executor aiExecutor = Executors.newSingleThreadExecutor();

    private LinearLayout containerTerms;
    private ImageButton btnAdd;
    private EditText inputCourseTitle;
    private ImageView btnScan, btnSave, btnClose, aiGenerate;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Handler uiHandler;
    private String folderId;

    private boolean isEditMode;
    private String editCourseId;

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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);


        inputCourseTitle = findViewById(R.id.inputTitle);
        containerTerms = findViewById(R.id.containerTerms);
        btnAdd = findViewById(R.id.btnAdd);
        btnScan = findViewById(R.id.btnScan);
        btnSave = findViewById(R.id.btnSave);
        btnClose = findViewById(R.id.btnCloseCreateCourse);
        aiGenerate = findViewById(R.id.aiGenerate);


        uiHandler = new Handler(Looper.getMainLooper());
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        isEditMode = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);
        editCourseId = getIntent().getStringExtra(EXTRA_COURSE_ID);


        if (folderId == null) {
            Toast.makeText(this, "Không xác định được thư mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnScan.setOnClickListener(v -> {
            startActivity(new Intent(this, CameraActivity.class));
        });

        btnAdd.setOnClickListener(v -> addTermLayout());

        btnClose.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveCourse());

        if (isEditMode && editCourseId != null) {
            btnSave.setImageResource(android.R.drawable.ic_menu_edit);
            containerTerms.removeAllViews();
            loadCourseForEdit();
        } else {
            addTermLayout();
        }

        aiGenerate.setOnClickListener(v -> {
            String topic = inputCourseTitle.getText().toString().trim();
            if (topic.isEmpty()) {
                Toast.makeText(CreateCourseActivity.this,
                        "Nhập chủ đề để AI tạo từ vựng", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(CreateCourseActivity.this,
                    "Đang tạo từ vựng bằng AI...", Toast.LENGTH_SHORT).show();

            generateVocabFromTopic(topic, vocabListFromAI -> {
                runOnUiThread(() -> {

                    if (vocabListFromAI.isEmpty()) {
                        Toast.makeText(CreateCourseActivity.this,
                                "AI không tạo được từ nào.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    containerTerms.removeAllViews();

                    for (Vocabulary aiVocab : vocabListFromAI) {

                        View termView = LayoutInflater.from(CreateCourseActivity.this)
                                .inflate(R.layout.item_term, containerTerms, false);
                        AutoCompleteTextView edtTerm = termView.findViewById(R.id.edtTerm);
                        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
                        ImageButton btnRemove = termView.findViewById(R.id.btnRemoveTerm);
                        ImageView btnRead = termView.findViewById(R.id.btnRead);

                        edtTerm.setText(aiVocab.getWord());
                        edtDefinition.setText(aiVocab.getMeaning());

                        DictionaryData dictData = new DictionaryData();
                        dictData.example = "";
                        dictData.audio = "";
                        dictData.phonetic = "";
                        termView.setTag(R.id.tag_definition_data, dictData);

                        fetchDefinitionsDictionary(aiVocab.getWord(), data -> {
                            dictData.meanings = data.meanings;
                            dictData.example = data.example;
                            dictData.audio = data.audio;
                            dictData.phonetic = data.phonetic;
                            termView.setTag(R.id.tag_definition_data, dictData);
                        });

                        btnRemove.setOnClickListener(rem -> containerTerms.removeView(termView));

                        btnRead.setOnClickListener(r -> {
                            Object tagObj = termView.getTag(R.id.tag_definition_data);
                            if (!(tagObj instanceof DictionaryData)) {
                                Toast.makeText(CreateCourseActivity.this, "Chưa có audio", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String audioUrl = ((DictionaryData) tagObj).audio;
                            if (audioUrl == null || audioUrl.isEmpty()) {
                                Toast.makeText(CreateCourseActivity.this, "Audio rỗng", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            MediaPlayer mp = new MediaPlayer();
                            try {
                                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mp.setDataSource(audioUrl);
                                mp.prepareAsync();
                                mp.setOnPreparedListener(MediaPlayer::start);
                                mp.setOnCompletionListener(MediaPlayer::release);
                            } catch (IOException e) {
                                Toast.makeText(CreateCourseActivity.this, "Không thể phát audio", Toast.LENGTH_SHORT).show();
                                mp.release();
                            }
                        });

                        containerTerms.addView(termView);
                    }

                    Toast.makeText(CreateCourseActivity.this,
                            "AI đã tạo xong " + vocabListFromAI.size() + " từ.", Toast.LENGTH_SHORT).show();
                });
            });
        });

    }

    private void loadCourseForEdit() {
        String uid = currentUser.getUid();
        DocumentReference ref = db.collection("users").document(uid)
                .collection("folders").document(folderId)
                .collection("courses").document(editCourseId);

        ref.get().addOnSuccessListener(doc -> {
            Course course = doc.toObject(Course.class);
            if (course == null) return;
            inputCourseTitle.setText(course.getTitle());
            for (Vocabulary v : course.getVocabularyList()) {
                addTermLayout();
                View termView = containerTerms.getChildAt(containerTerms.getChildCount() - 1);
                AutoCompleteTextView edtTerm = termView.findViewById(R.id.edtTerm);
                AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);

                edtTerm.setText(v.getWord());
                edtDefinition.setText(v.getMeaning());

                DictionaryData dict = new DictionaryData();
                dict.example = v.getExample();
                dict.audio = v.getAudio();
                dict.phonetic = v.getPhonetic();
                termView.setTag(R.id.tag_definition_data, dict);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi load Course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void addTermLayout() {
        View termView = LayoutInflater.from(this)
                .inflate(R.layout.item_term, containerTerms, false);
        AutoCompleteTextView edtTerm = termView.findViewById(R.id.edtTerm);
        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
        ImageButton btnRemove = termView.findViewById(R.id.btnRemoveTerm);
        ImageView btnRead = termView.findViewById(R.id.btnRead);

        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        edtTerm.setAdapter(termAdapter);
        edtTerm.setThreshold(1);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                String q = s.toString().trim();
                if (q.length() >= 1) {
                    fetchSuggestions(q, suggestions -> uiHandler.post(() -> {
                        termAdapter.clear();
                        termAdapter.addAll(suggestions);
                        termAdapter.notifyDataSetChanged();
                        if (!suggestions.isEmpty()) edtTerm.showDropDown();
                    }));
                }
            }
        };
        edtTerm.addTextChangedListener(watcher);

        ArrayAdapter<String> defAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        edtDefinition.setAdapter(defAdapter);
        edtDefinition.setThreshold(1);

        edtTerm.setOnItemClickListener((p, v, pos, id) -> {
            String word = termAdapter.getItem(pos);

            fetchDefinitionsDictionary(word, dict -> uiHandler.post(() ->
                    termView.setTag(R.id.tag_definition_data, dict)
            ));

            fetchTranslationSuggestions(word, defs -> uiHandler.post(() -> {
                defAdapter.clear();
                defAdapter.addAll(defs);
                defAdapter.notifyDataSetChanged();
                if (!defs.isEmpty()) {
                    edtDefinition.setText(defs.get(0));
                    edtDefinition.setSelection(defs.get(0).length());
                    edtDefinition.showDropDown();
                }
            }));

            edtTerm.removeTextChangedListener(watcher);
            edtTerm.setAdapter(null);
            edtTerm.dismissDropDown();
        });


        btnRead.setOnClickListener(v -> {
            Object tag = termView.getTag(R.id.tag_definition_data);
            if (!(tag instanceof DictionaryData)) {
                Toast.makeText(this, "Chưa có audio", Toast.LENGTH_SHORT).show();
                return;
            }
            String audioUrl = ((DictionaryData) tag).audio;
            if (audioUrl == null || audioUrl.isEmpty()) {
                Toast.makeText(this, "Audio rỗng", Toast.LENGTH_SHORT).show();
                return;
            }
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setDataSource(audioUrl);
                mp.prepareAsync();
                mp.setOnPreparedListener(MediaPlayer::start);
                mp.setOnCompletionListener(MediaPlayer::release);
            } catch (IOException e) {
                Toast.makeText(this, "Không thể phát audio", Toast.LENGTH_SHORT).show();
                mp.release();
            }
        });

        btnRemove.setOnClickListener(v -> containerTerms.removeView(termView));

        containerTerms.addView(termView);
    }

    private void saveCourse() {
        String title = inputCourseTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Nhập tiêu đề Course", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Vocabulary> vocabList = new ArrayList<>();
        for (int i = 0; i < containerTerms.getChildCount(); i++) {
            View v = containerTerms.getChildAt(i);
            String w = ((EditText) v.findViewById(R.id.edtTerm))
                    .getText().toString().trim();
            String m = ((EditText) v.findViewById(R.id.edtDefinition))
                    .getText().toString().trim();
            if (!w.isEmpty() && !m.isEmpty()) {
                Vocabulary vocab = new Vocabulary(w, m, "", "", "");
                Object tag = v.getTag(R.id.tag_definition_data);
                if (tag instanceof DictionaryData) {
                    DictionaryData d = (DictionaryData) tag;
                    vocab.setExample(d.example);
                    vocab.setAudio(d.audio);
                    vocab.setPhonetic(d.phonetic);
                }
                vocabList.add(vocab);
            }
        }
        if (vocabList.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 thuật ngữ", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference courseRef;
        if (isEditMode) {
            courseRef = db.collection("users").document(uid)
                    .collection("folders").document(folderId)
                    .collection("courses").document(editCourseId);

            courseRef.update("title", title, "createdAt", System.currentTimeMillis())
                    .addOnSuccessListener(a -> {

                        courseRef.collection("vocabularies").get()
                                .addOnSuccessListener(qs -> {
                                    WriteBatch batch = db.batch();
                                    for (DocumentSnapshot ds : qs) batch.delete(ds.getReference());
                                    for (Vocabulary vv : vocabList) {
                                        DocumentReference vr = courseRef
                                                .collection("vocabularies").document();
                                        vv.setId(vr.getId());
                                        batch.set(vr, vv);
                                    }
                                    batch.commit()
                                            .addOnSuccessListener(a2 -> {
                                                Toast.makeText(this,
                                                        "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Lỗi lưu từ vựng: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                                });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Lỗi update Course: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        } else {
            String newId = db.collection("users").document(uid)
                    .collection("folders").document(folderId)
                    .collection("courses").document().getId();
            Course course = new Course(title, System.currentTimeMillis(),
                    vocabList, folderId, currentUser.getEmail());
            course.setId(newId);
            courseRef = db.collection("users").document(uid)
                    .collection("folders").document(folderId)
                    .collection("courses").document(newId);
            courseRef.set(course)
                    .addOnSuccessListener(a -> commitVocabBatch(courseRef, vocabList))
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Lỗi lưu Course: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void commitVocabBatch(DocumentReference ref, List<Vocabulary> list) {
        WriteBatch batch = db.batch();
        for (Vocabulary v : list) {
            DocumentReference vr = ref.collection("vocabularies").document();
            v.setId(vr.getId());
            batch.set(vr, v);
        }
        batch.commit()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Tạo Course và lưu từ vựng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Lỗi lưu từ vựng: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }


    private void fetchSuggestions(String input, Consumer<List<String>> callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.datamuse.com/sug?s=" + input;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                List<String> list = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    for (int i = 0; i < arr.length() && i < 5; i++) {
                        list.add(arr.getJSONObject(i).getString("word"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                DictionaryData err = new DictionaryData();
                err.meanings = Collections.singletonList("Không có kết nối");
                err.example = "";
                err.audio = "";
                err.phonetic = "";
                callback.accept(err);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

                    data.phonetic = entry.optString("phonetic", "");

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
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                callback.accept(Collections.singletonList("Không có kết nối"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                callback.accept(Collections.singletonList("Không có kết nối"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

    private void generateVocabFromTopic(String topic, Consumer<List<Vocabulary>> callback) {

        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that generates a list of 10 English vocabulary words related to the topic: \"")
                .append(topic).append("\".\n")
                .append("For each word, provide its meaning in concise Vietnamese (not English).\n\n")
                .append("IMPORTANT: DO NOT wrap your JSON in ```json or any markdown code fences. ONLY output the pure JSON array.\n")
                .append("Return a JSON array of exactly 15 objects, each with keys:\n")
                .append("- \"word\": the English vocabulary word (string)\n")
                .append("- \"meaning\": a short Vietnamese explanation of that word (string)\n\n")
                .append("Example format:\n")
                .append("[\n")
                .append("  { \"word\": \"ecosystem\", \"meaning\": \"Một cộng đồng sinh vật cùng tương tác với môi trường xung quanh.\" },\n")
                .append("  { \"word\": \"biodiversity\", \"meaning\": \"Sự đa dạng sinh học của các loài trong một khu vực.\" },\n")
                .append("  ... exactly 10 items ...\n")
                .append("]\n\n")
                .append("Now generate the JSON array for topic: \"").append(topic).append("\". ");

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

                raw = raw.trim();

                android.util.Log.d("CreateCourseAI", "RAW_RESPONSE=\n" + raw);

                if (raw.startsWith("```")) {
                    int idx = raw.indexOf('\n');
                    if (idx != -1) raw = raw.substring(idx + 1).trim();
                }

                if (raw.endsWith("```")) {
                    int idx = raw.lastIndexOf("```");
                    if (idx != -1) raw = raw.substring(0, idx).trim();
                }

                raw = raw.trim();

                int startIdx = raw.indexOf('[');
                int endIdx = raw.lastIndexOf(']');
                if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                    android.util.Log.e("CreateCourseAI",
                            "Cannot find JSON array boundaries. startIdx=" + startIdx + ", endIdx=" + endIdx);
                    callback.accept(Collections.emptyList());
                    return;
                }
                String jsonArray = raw.substring(startIdx, endIdx + 1);

                android.util.Log.d("CreateCourseAI", "EXTRACTED_JSON_ARRAY=\n" + jsonArray);

                try {
                    JSONArray arr = new JSONArray(jsonArray);
                    List<Vocabulary> result = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String word = obj.optString("word", "").trim();
                        String meaning = obj.optString("meaning", "").trim();
                        if (!word.isEmpty() && !meaning.isEmpty()) {
                            result.add(new Vocabulary(word, meaning, "", "", ""));
                        }
                    }
                    callback.accept(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    android.util.Log.e("CreateCourseAI", "Parse exception: " + e.getMessage());
                    callback.accept(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                android.util.Log.e("CreateCourseAI", "AI call failure: " + t.getMessage());
                callback.accept(Collections.emptyList());
            }
        }, aiExecutor);

    }
}