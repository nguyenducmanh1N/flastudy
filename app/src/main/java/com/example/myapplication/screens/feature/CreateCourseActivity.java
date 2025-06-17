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
import java.lang.StringBuilder;
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
    public static final String EXTRA_IS_EDIT   = "isEdit";
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
        containerTerms    = findViewById(R.id.containerTerms);
        btnAdd            = findViewById(R.id.btnAdd);
        btnScan           = findViewById(R.id.btnScan);
        btnSave           = findViewById(R.id.btnSave);
        btnClose          = findViewById(R.id.btnCloseCreateCourse);
        aiGenerate        = findViewById(R.id.aiGenerate);

        uiHandler   = new Handler(Looper.getMainLooper());
        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        folderId    = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        isEditMode  = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);
        editCourseId= getIntent().getStringExtra(EXTRA_COURSE_ID);

        if (folderId == null || currentUser == null) {
            Toast.makeText(this,
                    folderId == null ? "Không xác định được thư mục" : "Bạn chưa đăng nhập",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_FOLDER_ID, folderId);
            startActivity(intent);
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
                Toast.makeText(this, "Nhập chủ đề để AI tạo từ vựng", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đang tạo từ vựng bằng AI...", Toast.LENGTH_SHORT).show();
            generateVocabFromTopic(topic, vocabList -> {
                runOnUiThread(() -> {
                    if (vocabList.isEmpty()) {
                        Toast.makeText(this, "AI không tạo được từ nào.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    containerTerms.removeAllViews();
                    for (Vocabulary aiV : vocabList) {
                        View termView = LayoutInflater.from(this)
                                .inflate(R.layout.item_term, containerTerms, false);
                        AutoCompleteTextView edtTerm       = termView.findViewById(R.id.edtTerm);
                        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
                        ImageButton btnRemove              = termView.findViewById(R.id.btnRemoveTerm);
                        ImageView   btnRead                = termView.findViewById(R.id.btnRead);

                        edtTerm.setText(aiV.getWord());
                        edtDefinition.setText(aiV.getMeaning());
                        edtDefinition.setVisibility(View.VISIBLE);

                        DictionaryData dictData = new DictionaryData();
                        dictData.example  = aiV.getExample();
                        dictData.phonetic = aiV.getPhonetic();
                        dictData.audio    = "";
                        dictData.meanings = new ArrayList<>();
                        termView.setTag(R.id.tag_definition_data, dictData);

                        fetchAudioFromDictionary(aiV.getWord(), audioUrl -> {
                            uiHandler.post(() -> {
                                dictData.audio = audioUrl;
                                termView.setTag(R.id.tag_definition_data, dictData);
                            });
                        });

                        btnRemove.setOnClickListener(rem ->
                                containerTerms.removeView(termView)
                        );
                        btnRead.setOnClickListener(r ->
                                playAudioFromTag(termView)
                        );

                        containerTerms.addView(termView);
                    }
                    Toast.makeText(this, "AI đã tạo xong " + vocabList.size() + " từ.", Toast.LENGTH_SHORT).show();
                });
            });
        });


        ArrayList<Vocabulary> aiList =
                getIntent().getParcelableArrayListExtra("aiGeneratedVocabList");
        if (aiList != null && !aiList.isEmpty()) {
            containerTerms.removeAllViews();
            for (Vocabulary aiV : aiList) {
                View termView = LayoutInflater.from(this)
                        .inflate(R.layout.item_term, containerTerms, false);
                AutoCompleteTextView edtTerm       = termView.findViewById(R.id.edtTerm);
                AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
                ImageButton btnRemove              = termView.findViewById(R.id.btnRemoveTerm);
                ImageView   btnRead                = termView.findViewById(R.id.btnRead);

                edtTerm.setText(aiV.getWord());
                edtDefinition.setText(aiV.getMeaning());
                edtDefinition.setVisibility(View.VISIBLE);

                DictionaryData dictData = new DictionaryData();
                dictData.meanings = new ArrayList<>();
                dictData.example  = "";
                dictData.audio    = "";
                dictData.phonetic = "";
                termView.setTag(R.id.tag_definition_data, dictData);

                fetchDefinitionsByAI(aiV.getWord(), data -> {
                    dictData.meanings = data.meanings;
                    dictData.example  = data.example;
                    dictData.audio    = data.audio;
                    dictData.phonetic = data.phonetic;
                    termView.setTag(R.id.tag_definition_data, dictData);
                });
                fetchAudioFromDictionary(aiV.getWord(), audioUrl -> {
                    uiHandler.post(() -> {
                        dictData.audio = audioUrl;
                        termView.setTag(R.id.tag_definition_data, dictData);
                    });
                });

                btnRemove.setOnClickListener(rem -> containerTerms.removeView(termView));
                btnRead.setOnClickListener(r -> {
                    Object tagObj = termView.getTag(R.id.tag_definition_data);
                    if (!(tagObj instanceof DictionaryData)) {
                        Toast.makeText(this, "Chưa có audio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String audioUrl = ((DictionaryData) tagObj).audio;
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

                containerTerms.addView(termView);
            }

        } else {
            if (isEditMode && editCourseId != null) {
                btnSave.setImageResource(android.R.drawable.ic_menu_edit);
                containerTerms.removeAllViews();
                loadCourseForEdit();
            } else {
                addTermLayout();
            }
        }
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
            containerTerms.removeAllViews();
            for (Vocabulary v : course.getVocabularyList()) {
                addTermLayout();
                View termView = containerTerms.getChildAt(containerTerms.getChildCount() - 1);
                ((AutoCompleteTextView)termView.findViewById(R.id.edtTerm)).setText(v.getWord());
                ((AutoCompleteTextView)termView.findViewById(R.id.edtDefinition)).setText(v.getMeaning());

                DictionaryData dict = new DictionaryData();
                dict.meanings = new ArrayList<>();
                dict.example  = v.getExample();
                dict.audio    = v.getAudio();
                dict.phonetic = v.getPhonetic();
                termView.setTag(R.id.tag_definition_data, dict);

                termView.findViewById(R.id.btnRead).setOnClickListener(r -> playAudioFromTag(termView));
                termView.findViewById(R.id.btnRemoveTerm).setOnClickListener(rem -> containerTerms.removeView(termView));
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi load Course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void addTermLayout() {
        View termView = LayoutInflater.from(this)
                .inflate(R.layout.item_term, containerTerms, false);
        AutoCompleteTextView edtTerm       = termView.findViewById(R.id.edtTerm);
        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
        ImageButton btnRemove              = termView.findViewById(R.id.btnRemoveTerm);
        ImageView   btnRead                = termView.findViewById(R.id.btnRead);
        ImageView   btnAI                  = termView.findViewById(R.id.btnAI);

        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        edtTerm.setAdapter(termAdapter);
        edtTerm.setThreshold(1);
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
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

        ArrayAdapter<String> defAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        edtDefinition.setAdapter(defAdapter);
        edtDefinition.setThreshold(1);

        edtTerm.setOnItemClickListener((p, v, pos, id) -> {
            String word = termAdapter.getItem(pos);

            fetchAudioFromDictionary(word, audioUrl -> {
                DictionaryData dict = new DictionaryData();
                dict.audio = audioUrl;
                uiHandler.post(() -> termView.setTag(R.id.tag_definition_data, dict));
            });

            fetchTranslationSuggestions(word, defs -> uiHandler.post(() -> {
                List<String> stringDefs = new ArrayList<>();
                for (DictionaryData d : defs) {
                    stringDefs.add("(" + d.phonetic + ") " + d.meanings.get(0));
                }
                defAdapter.clear();
                defAdapter.addAll(stringDefs);
                defAdapter.notifyDataSetChanged();
                if (!stringDefs.isEmpty()) {
                    edtDefinition.setText(stringDefs.get(0));
                    edtDefinition.setSelection(stringDefs.get(0).length());
                    edtDefinition.showDropDown();
                }
            }));
            edtTerm.removeTextChangedListener(watcher);
            edtTerm.setAdapter(null);
            edtTerm.dismissDropDown();
        });

        btnAI.setOnClickListener(v -> {
            String term = edtTerm.getText().toString().trim();
            if (term.isEmpty()) {
                Toast.makeText(this, "Nhập thuật ngữ trước khi lấy nghĩa", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đang lấy gợi ý định nghĩa...", Toast.LENGTH_SHORT).show();
            fetchTranslationSuggestions(term, defs -> uiHandler.post(() -> {
                defAdapter.clear();
                for (DictionaryData d : defs) {
                    defAdapter.add("(" + d.phonetic + ") " + d.meanings.get(0));
                }
                defAdapter.notifyDataSetChanged();
                if (!defs.isEmpty()) {

                    edtDefinition.setText(defAdapter.getItem(0));
                    edtDefinition.setSelection(edtDefinition.getText().length());
                    edtDefinition.showDropDown();
                } else {
                    Toast.makeText(this, "Không tìm được định nghĩa", Toast.LENGTH_SHORT).show();
                }
            }));
        });

        btnRead.setOnClickListener(v -> playAudioFromTag(termView));
        btnRemove.setOnClickListener(v -> containerTerms.removeView(termView));
        containerTerms.addView(termView);
    }

    private void playAudioFromTag(View termView) {
        Object tagObj = termView.getTag(R.id.tag_definition_data);
        if (!(tagObj instanceof DictionaryData)) {
            Toast.makeText(this, "Chưa có audio", Toast.LENGTH_SHORT).show();
            return;
        }
        String audioUrl = ((DictionaryData) tagObj).audio;
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
            String w = ((EditText)v.findViewById(R.id.edtTerm)).getText().toString().trim();
            String m = ((EditText)v.findViewById(R.id.edtDefinition)).getText().toString().trim();
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
                                        DocumentReference vr = courseRef.collection("vocabularies").document();
                                        vv.setId(vr.getId());
                                        batch.set(vr, vv);
                                    }
                                    batch.commit().addOnSuccessListener(a2 -> {
                                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }).addOnFailureListener(e ->
                                            Toast.makeText(this, "Lỗi lưu từ vựng: "+e.getMessage(), Toast.LENGTH_SHORT).show());
                                });
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi update Course: "+e.getMessage(), Toast.LENGTH_SHORT).show());
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
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi lưu Course: "+e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void commitVocabBatch(DocumentReference ref, List<Vocabulary> list) {
        WriteBatch batch = db.batch();
        for (Vocabulary v : list) {
            DocumentReference vr = ref.collection("vocabularies").document();
            v.setId(vr.getId());
            batch.set(vr, v);
        }
        batch.commit().addOnSuccessListener(a -> {
            Toast.makeText(this, "Tạo Course và lưu từ vựng thành công", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi lưu từ vựng: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchSuggestions(String input, Consumer<List<String>> callback) {

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = String.format(
                "You are an AI that suggests up to 5 English words that start with or are related to the prefix \"%s\". " +
                        "Return a pure JSON array of strings, each being a single word. " +
                        "Example output:\n[\"apple\", \"application\", \"appoint\"]\n\n" +
                        "Now suggest for prefix: \"%s\"",
                input, input
        );

        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();

        ChatFutures chat = model.startChat(Collections.singletonList(userContent));
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String raw = response.getText().trim();

                if (raw.startsWith("```")) {
                    raw = raw.substring(raw.indexOf('\n') + 1).trim();
                }
                if (raw.endsWith("```")) {
                    raw = raw.substring(0, raw.lastIndexOf("```")).trim();
                }
                int start = raw.indexOf('['), end = raw.lastIndexOf(']');
                if (start < 0 || end < 0) {
                    callback.accept(Collections.emptyList());
                    return;
                }
                String json = raw.substring(start, end + 1);
                try {
                    JSONArray arr = new JSONArray(json);
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < arr.length() && i < 5; i++) {
                        suggestions.add(arr.getString(i));
                    }
                    callback.accept(suggestions);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.accept(Collections.emptyList());
                }
            }
            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                callback.accept(Collections.emptyList());
            }
        }, aiExecutor);
    }

    private void fetchDefinitionsByAI(String term, Consumer<DictionaryData> callback) {
        String prompt = String.format(
                "You are an AI that provides:\n" +
                        "- \"meanings\": an array of up to 5 concise Vietnamese definitions.\n" +
                        "- \"example\": one example sentence in English.\n" +
                        "- \"phonetic\": the IPA transcription.\n\n" +
                        "Return ONLY a JSON object. Example:\n" +
                        "{ \"meanings\": [\"...\"], \"example\": \"...\", \"phonetic\": \"/.../\" }\n\n" +
                        "Now: \"%s\".", term
        );
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();

        ChatFutures chat = model.startChat(Collections.singletonList(userContent));
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse response) {
                String raw = response.getText().trim();
                if (raw.startsWith("```")) raw = raw.substring(raw.indexOf('\n')+1).trim();
                if (raw.endsWith("```")) raw = raw.substring(0, raw.lastIndexOf("```")).trim();
                try {
                    JSONObject o = new JSONObject(raw);
                    DictionaryData d = new DictionaryData();
                    d.meanings = new ArrayList<>();
                    JSONArray ma = o.optJSONArray("meanings");
                    if (ma!=null) for(int i=0;i<ma.length();i++) d.meanings.add(ma.getString(i));
                    d.example = o.optString("example","");
                    d.phonetic= o.optString("phonetic","");
                    d.audio   = "";
                    callback.accept(d);
                } catch (Exception e) {
                    e.printStackTrace(); callback.accept(new DictionaryData());
                }
            }
            @Override public void onFailure(Throwable t) { t.printStackTrace(); callback.accept(new DictionaryData()); }
        }, aiExecutor);
    }

    private void fetchAudioFromDictionary(String term, Consumer<String> callback) {
        OkHttpClient client = new OkHttpClient();
        String url;
        try {
            url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(term, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + term;
        }
        Request request = new Request.Builder().url(url).addHeader("Accept","application/json").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.accept(""); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) { callback.accept(""); return; }
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    JSONArray phon = arr.getJSONObject(0).optJSONArray("phonetics");
                    if (phon!=null) for(int i=0;i<phon.length();i++) {
                        String a = phon.getJSONObject(i).optString("audio","");
                        if (!a.isEmpty()) { callback.accept(a); return; }
                    }
                } catch (Exception ignored) {}
                callback.accept("");
            }
        });
    }
//    private void fetchTranslationSuggestions(String term, Consumer<List<DictionaryData>> callback) {
//        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
//        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
//
//        StringBuilder prompt = new StringBuilder();
//        prompt.append("You are an AI that provides up to 3 concise Vietnamese definitions for a single English word, ")
//                .append("each with its part of speech tag (e.g., \"v\" for verb, \"n\" for noun).\n")
//                .append("Return a pure JSON array of objects. Each object must have:\n")
//                .append("  - \"type\": one-letter POS tag as string, e.g. \"v\", \"n\", \"adj\"...\n")
//                .append("  - \"meaning\": short Vietnamese definition (string)\n\n")
//                .append("Example:\n")
//                .append("[\n")
//                .append("  { \"type\": \"v\", \"meaning\": \"nói\" },\n")
//                .append("  { \"type\": \"n\", \"meaning\": \"lời nói\" }\n")
//                .append("]\n\n")
//                .append("Now generate up to 3 entries for the word \"").append(term).append("\".");
//
//        Content.Builder contentBuilder = new Content.Builder();
//        contentBuilder.setRole("user");
//        contentBuilder.addText(prompt.toString());
//        Content userContent = contentBuilder.build();
//
//        ChatFutures chat = model.startChat(Collections.singletonList(userContent));
//        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);
//
//        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
//            @Override
//            public void onSuccess(GenerateContentResponse response) {
//                String raw = response.getText().trim();
//                if (raw.startsWith("```")) raw = raw.substring(raw.indexOf('\n') + 1).trim();
//                if (raw.endsWith("```"))  raw = raw.substring(0, raw.lastIndexOf("```")).trim();
//
//                int start = raw.indexOf('['), end = raw.lastIndexOf(']');
//                if (start < 0 || end < 0 || end <= start) {
//                    callback.accept(Collections.emptyList());
//                    return;
//                }
//                String json = raw.substring(start, end + 1);
//                try {
//                    JSONArray arr = new JSONArray(json);
//                    List<DictionaryData> list = new ArrayList<>();
//                    for (int i = 0; i < arr.length() && list.size() < 3; i++) {
//                        JSONObject obj = arr.getJSONObject(i);
//                        String type    = obj.optString("type", "").trim();
//                        String meaning = obj.optString("meaning", "").trim();
//                        if (!type.isEmpty() && !meaning.isEmpty()) {
//                            DictionaryData d = new DictionaryData();
//                            d.meanings = Collections.singletonList(meaning);
//                            d.example  = "";
//                            d.audio    = "";
//                            d.phonetic = type;
//                            list.add(d);
//                        }
//                    }
//                    callback.accept(list);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    callback.accept(Collections.emptyList());
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                t.printStackTrace();
//                callback.accept(Collections.emptyList());
//            }
//        }, aiExecutor);
//    }

    private void fetchTranslationSuggestions(String term, Consumer<List<DictionaryData>> callback) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that provides up to 3 concise Vietnamese definitions for a single English word or phrase, ")
                .append("each with its part of speech tag (e.g., \"v\" for verb, \"n\" for noun, \"phr\" for phrase).\n")
                .append("Return a pure JSON array of objects. Each object must have:\n")
                .append("  - \"type\": one-letter or three-letter POS tag as string, e.g. \"v\", \"n\", \"adj\", \"phr\"...\n")
                .append("  - \"meaning\": short Vietnamese definition (string)\n\n")
                .append("Example for phrase \"get along with\":\n")
                .append("[\n")
                .append("  { \"type\": \"phr\", \"meaning\": \"hòa hợp với\" }\n")
                .append("]\n\n")
                .append("Now generate up to 3 entries for the term: \"")
                .append(term.replace("\"", "\\\""))
                .append("\".");

        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();

        List<Content> history = Collections.singletonList(userContent);
        ChatFutures chat = model.startChat(history);
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String raw = response.getText().trim();
                android.util.Log.d("GeminiService", "RAW_AI_RESPONSE=\n" + raw);
                if (raw.startsWith("```")) raw = raw.substring(raw.indexOf('\n') + 1).trim();
                if (raw.endsWith("```"))  raw = raw.substring(0, raw.lastIndexOf("```")).trim();

                int start = raw.indexOf('['), end = raw.lastIndexOf(']');
                if (start < 0 || end < 0 || end <= start) {
                    callback.accept(Collections.emptyList());
                    return;
                }
                String json = raw.substring(start, end + 1);
                try {
                    JSONArray arr = new JSONArray(json);
                    List<DictionaryData> list = new ArrayList<>();
                    for (int i = 0; i < arr.length() && list.size() < 3; i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String type    = obj.optString("type", "").trim();
                        String meaning = obj.optString("meaning", "").trim();
                        if (!type.isEmpty() && !meaning.isEmpty()) {
                            DictionaryData d = new DictionaryData();
                            d.phonetic = type;
                            d.meanings = Collections.singletonList(meaning);
                            d.example  = "";
                            d.audio    = "";
                            list.add(d);
                        }
                    }
                    callback.accept(list);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.accept(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                callback.accept(Collections.emptyList());
            }
        }, aiExecutor);
    }

    private void generateVocabFromTopic(String topic, Consumer<List<Vocabulary>> callback) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that generates a list of 15 English vocabulary words related to the topic: \"")
                .append(topic).append("\".\n")
                .append("For each word, provide:\n")
                .append("  - \"word\": English word\n")
                .append("  - \"meaning\": concise Vietnamese meaning\n")
                .append("  - \"example\": an example sentence in English\n")
                .append("  - \"phonetic\": phonetic transcription in IPA\n\n")
                .append("IMPORTANT: ONLY output the pure JSON array of exactly 15 objects (no markdown).\n")
                .append("Example:\n[\n")
                .append("  { \"word\": \"ecosystem\", \"meaning\": \"Một cộng đồng...\", ")
                .append("\"example\": \"The rainforest ecosystem is fragile.\", ")
                .append("\"phonetic\": \"/ˈiːkoʊˌsɪstəm/\" }, ...\n]\n\n")
                .append("Generate for: \"").append(topic).append("\".");

        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(prompt.toString());
        Content userContent = contentBuilder.build();

        ChatFutures chat = model.startChat(Collections.singletonList(userContent));
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse response) {
                String raw = response.getText().trim();

                android.util.Log.d("GeminiService", "RAW_AI_RESPONSE=\n" + raw);
                if (raw.startsWith("```")) raw = raw.substring(raw.indexOf('\n') + 1).trim();
                if (raw.endsWith("```")) raw = raw.substring(0, raw.lastIndexOf("```")).trim();
                int start = raw.indexOf('['), end = raw.lastIndexOf(']');
                if (start < 0 || end < 0) {
                    callback.accept(Collections.emptyList());
                    return;
                }
                String json = raw.substring(start, end + 1);
                try {
                    JSONArray arr = new JSONArray(json);
                    List<Vocabulary> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String w  = o.optString("word","").trim();
                        String m  = o.optString("meaning","").trim();
                        String ex = o.optString("example","").trim();
                        String ph = o.optString("phonetic","").trim();
                        if (!w.isEmpty() && !m.isEmpty()) {
                            Vocabulary v = new Vocabulary(w, m, ex, "", ph);
                            list.add(v);
                        }
                    }
                    callback.accept(list);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.accept(Collections.emptyList());
                }
            }
            @Override public void onFailure(Throwable t) {
                t.printStackTrace();
                callback.accept(Collections.emptyList());
            }
        }, aiExecutor);
    }

    private void addTermViewWithVocabulary(Vocabulary aiV) {
        View termView = LayoutInflater.from(this)
                .inflate(R.layout.item_term, containerTerms, false);
        AutoCompleteTextView edtTerm       = termView.findViewById(R.id.edtTerm);
        AutoCompleteTextView edtDefinition = termView.findViewById(R.id.edtDefinition);
        ImageButton btnRemove              = termView.findViewById(R.id.btnRemoveTerm);
        ImageView   btnRead                = termView.findViewById(R.id.btnRead);
        edtTerm.setText(aiV.getWord());
        edtDefinition.setText(aiV.getMeaning());
        edtDefinition.setVisibility(View.VISIBLE);
        DictionaryData dictData = new DictionaryData();
        dictData.example  = aiV.getExample();
        dictData.phonetic = aiV.getPhonetic();
        dictData.audio    = aiV.getAudio();
        dictData.meanings = new ArrayList<>();
        termView.setTag(R.id.tag_definition_data, dictData);
        btnRemove.setOnClickListener(rem -> containerTerms.removeView(termView));
        btnRead.setOnClickListener(r -> playAudioFromTag(termView));
        containerTerms.addView(termView);
    }

}
