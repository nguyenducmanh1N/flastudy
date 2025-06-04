package com.example.myapplication.screens.feature.act;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;
import com.example.myapplication.screens.feature.CreateCourseActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CameraActivity extends AppCompatActivity {
    public static final String EXTRA_FOLDER_ID = "folderId";

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private String folderId;
    private static final int REQUEST_PERMISSIONS = 10;
    private Uri photoUri;

    private ImageView imgPreview;
    private TextView tvOcrResult;
    private Button btnTakePhoto;

    private LinearLayout containerWords;
    private Button btnAddToCourse;

    private List<String> extractedWords = new ArrayList<>();
    private List<Vocabulary> fetchedVocabList = new ArrayList<>();

    private final Executor aiExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && photoUri != null) {
                            imgPreview.setImageURI(photoUri);
                            runTextRecognition(photoUri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camere);

        imgPreview        = findViewById(R.id.imgPreview);
        tvOcrResult       = findViewById(R.id.tvOcrResult);
        btnTakePhoto      = findViewById(R.id.btnTakePhoto);
        containerWords    = findViewById(R.id.containerWords);
        btnAddToCourse    = findViewById(R.id.btnAddToCourse);

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        btnTakePhoto.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                dispatchTakePictureIntent();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        REQUEST_PERMISSIONS
                );
            }
        });

        btnAddToCourse.setOnClickListener(v -> {
            if (fetchedVocabList.isEmpty()) {
                Toast.makeText(this, "Chưa có từ vựng nào để thêm.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CameraActivity.this, CreateCourseActivity.class);
            intent.putParcelableArrayListExtra("aiGeneratedVocabList",
                    new ArrayList<>(fetchedVocabList));
            intent.putExtra(CreateCourseActivity.EXTRA_FOLDER_ID, folderId);
            startActivity(intent);
            finish();
        });
    }

    private boolean allPermissionsGranted() {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (allPermissionsGranted()) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this,
                        "Cần cấp quyền Camera và Storage để tiếp tục",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePic.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = File.createTempFile(
                        "OCR_", ".jpg",
                        getExternalCacheDir()
                );
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Không thể tạo file ảnh",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(takePic);
        }
    }

    private void runTextRecognition(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String fullText = visionText.getText();
                        extractedWords = extractEnglishWords(fullText);
                        if (extractedWords.isEmpty()) {
                            tvOcrResult.setText("Không tìm thấy từ tiếng Anh nào trên ảnh.");
                            return;
                        }

                        tvOcrResult.setText("Đang lấy nghĩa cho " + extractedWords.size() + " từ...");
                        fetchMeaningsForWords(extractedWords, resultList -> {
                            runOnUiThread(() -> {
                                if (resultList.isEmpty()) {
                                    tvOcrResult.setText("Không lấy được nghĩa từ AI.");
                                    return;
                                }
                                fetchedVocabList.clear();
                                fetchedVocabList.addAll(resultList);

                                containerWords.removeAllViews();
                                LayoutInflater inflater = LayoutInflater.from(this);
                                for (Vocabulary vbocab : fetchedVocabList) {
                                    View card = inflater.inflate(R.layout.item_term, containerWords, false);
                                    TextView edtTerm       = card.findViewById(R.id.edtTerm);
                                    TextView edtDefinition = card.findViewById(R.id.edtDefinition);
                                    View btnRead           = card.findViewById(R.id.btnRead);
                                    View btnRemove         = card.findViewById(R.id.btnRemoveTerm);

                                    edtTerm.setText(vbocab.getWord());
                                    edtDefinition.setText(vbocab.getMeaning());
                                    edtDefinition.setVisibility(View.VISIBLE);

                                    if (btnRead != null) btnRead.setVisibility(View.GONE);
                                    if (btnRemove != null) btnRemove.setVisibility(View.GONE);

                                    containerWords.addView(card);
                                }
                                tvOcrResult.setText("Hoàn thành lấy nghĩa.");
                                btnAddToCourse.setVisibility(View.VISIBLE);
                            });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "OCR thất bại: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> extractEnglishWords(String text) {
        List<String> result = new ArrayList<>();
        String cleaned = text.replaceAll("[^A-Za-z ]", " ");
        for (String tok : cleaned.split("\\s+")) {
            if (tok.length() >= 2) {
                result.add(tok.toLowerCase());
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(result));
    }

    private void fetchMeaningsForWords(List<String> words, Consumer<List<Vocabulary>> callback) {
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI that translates English words into concise Vietnamese definitions.\n")
                .append("Given the following list of English words, return a pure JSON array of objects:\n")
                .append("Each object must have keys:\n")
                .append("- \"word\": the English word (string)\n")
                .append("- \"meaning\": short Vietnamese definition (string)\n\n")
                .append("IMPORTANT: Only output the JSON array, no markdown or code fences.\n")
                .append("Example format:\n")
                .append("[\n")
                .append("  { \"word\": \"ecosystem\", \"meaning\": \"Một cộng đồng sinh vật cùng tương tác với môi trường xung quanh.\" },\n")
                .append("  { \"word\": \"biodiversity\", \"meaning\": \"Sự đa dạng sinh học của các loài trong một khu vực.\" }\n")
                .append("]\n\n")
                .append("Now translate these words into Vietnamese definitions:\n");
        for (String w : words) {
            prompt.append("- ").append(w).append("\n");
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
                String raw = response.getText().trim();
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
                int endIdx   = raw.lastIndexOf(']');
                if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                    callback.accept(Collections.emptyList());
                    return;
                }
                String jsonArray = raw.substring(startIdx, endIdx + 1);

                try {
                    JSONArray arr = new JSONArray(jsonArray);
                    List<Vocabulary> result = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String w       = obj.optString("word", "").trim();
                        String m       = obj.optString("meaning", "").trim();
                        if (!w.isEmpty() && !m.isEmpty()) {
                            result.add(new Vocabulary(w, m, "", "", ""));
                        }
                    }
                    callback.accept(result);
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
}
