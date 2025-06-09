package com.example.myapplication.screens.feature.learn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Vocabulary;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiLivePronunciationActivity extends AppCompatActivity {

    private static final String TAG = "PronunciationAct";
    private static final int SAMPLE_RATE = 16000;
    private static final int PERMISSION_REQUEST = 2001;

    private ArrayList<Vocabulary> vocabList;
    private int currentIndex = 0;

    private TextView tvWord;
    private Button btnListen, btnSpeak;

    // Audio
    private AudioRecord recorder;
    private boolean isRecording;
    private ByteArrayOutputStream userBuffer;

    // HTTP
    private OkHttpClient http;
    private Gson gson;
    private static final String SPEECH_API =
            "https://speech.googleapis.com/v1p1beta1/speech:recognize?key=AIzaSyA5O1HTYscF4EVCXoIzsv1ca4_NbU_0N";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gemini_live_pronunciation);
        tvWord = findViewById(R.id.tvWord);
        btnListen = findViewById(R.id.btnListen);
        btnSpeak = findViewById(R.id.btnSpeak);

        http = new OkHttpClient();
        gson = new Gson();

        vocabList = getIntent().getParcelableArrayListExtra("vocabList");
        if (vocabList == null || vocabList.isEmpty()) {
            Toast.makeText(this, "Danh sách từ trống.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        showCurrentWord();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST);
        }

        btnListen.setOnClickListener(v -> playTTS());
        btnSpeak.setOnClickListener(v -> {
            if (!isRecording) startRecording();
            else stopRecordingAndCompare();
        });
    }

    private void showCurrentWord() {
        String word = vocabList.get(currentIndex).getWord();
        tvWord.setText(word);
        btnSpeak.setText("Speak");
    }

    private void playTTS() {
        String text = vocabList.get(currentIndex).getWord();
        JsonObject payload = new JsonObject();
        JsonObject input = new JsonObject(); input.addProperty("text", text);
        JsonObject voice = new JsonObject(); voice.addProperty("languageCode", "en-US");
        JsonObject audioConfig = new JsonObject(); audioConfig.addProperty("audioEncoding", "LINEAR16");
        payload.add("input", input);
        payload.add("voice", voice);
        payload.add("audioConfig", audioConfig);
        RequestBody body = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"),
                gson.toJson(payload)
        );
        Request req = new Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=YOUR_API_KEY")
                .post(body)
                .build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(GeminiLivePronunciationActivity.this,
                        "TTS error", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) {
                    onFailure(call, new IOException("HTTP " + res.code())); return;
                }
                String json = res.body().string();
                String audioBase64 = gson.fromJson(json, JsonObject.class)
                        .get("audioContent").getAsString();
                byte[] data = Base64.decode(audioBase64, Base64.DEFAULT);
                playAudio(data, SAMPLE_RATE);
            }
        });
    }

    private void playAudio(byte[] pcm, int sampleRate) {
        new Thread(() -> {
            AudioTrack track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    pcm.length,
                    AudioTrack.MODE_STATIC
            );
            track.write(pcm, 0, pcm.length);
            track.play();
        }).start();
    }

    private void startRecording() {
        int bufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);
        recorder.startRecording();
        isRecording = true;
        userBuffer = new ByteArrayOutputStream();
        btnSpeak.setText("Stop");
        new Thread(() -> {
            byte[] buf = new byte[bufSize];
            while (isRecording) {
                int r = recorder.read(buf, 0, buf.length);
                if (r > 0) userBuffer.write(buf, 0, r);
            }
        }).start();
    }

    private void stopRecordingAndCompare() {
        isRecording = false;
        recorder.stop(); recorder.release();
        btnSpeak.setText("Speak");
        byte[] userAudio = userBuffer.toByteArray();
        recognizeSpeech(userAudio);
    }

    private void recognizeSpeech(byte[] pcmData) {
        new Thread(() -> {
            JsonObject config = new JsonObject();
            config.addProperty("encoding", "LINEAR16");
            config.addProperty("sampleRateHertz", SAMPLE_RATE);
            config.addProperty("languageCode", "en-US");
            JsonObject audio = new JsonObject();
            audio.addProperty("content", Base64.encodeToString(pcmData, Base64.NO_WRAP));
            JsonObject reqJson = new JsonObject();
            reqJson.add("config", config);
            reqJson.add("audio", audio);
            RequestBody body = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    gson.toJson(reqJson)
            );
            Request req = new Request.Builder()
                    .url(SPEECH_API)
                    .post(body)
                    .build();
            http.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(GeminiLivePronunciationActivity.this,
                                    "STT error", Toast.LENGTH_SHORT).show());
                }
                @Override public void onResponse(Call call, Response res) throws IOException {
                    if (!res.isSuccessful()) {
                        onFailure(call, new IOException("HTTP " + res.code())); return;
                    }
                    String resp = res.body().string();
                    String transcript = gson.fromJson(resp, JsonObject.class)
                            .getAsJsonArray("results")
                            .get(0).getAsJsonObject()
                            .getAsJsonArray("alternatives")
                            .get(0).getAsJsonObject()
                            .get("transcript").getAsString();
                    int score = transcript.trim().equalsIgnoreCase(
                            vocabList.get(currentIndex).getWord()) ? 100 : 0;
                    runOnUiThread(() -> {
                        Toast.makeText(GeminiLivePronunciationActivity.this,
                                "Score: " + score + "%", Toast.LENGTH_SHORT).show();
                        nextWord();
                    });
                }
            });
        }).start();
    }

    private void nextWord() {
        currentIndex++;
        if (currentIndex < vocabList.size()) showCurrentWord();
        else {
            Toast.makeText(this, "Hoàn thành.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
