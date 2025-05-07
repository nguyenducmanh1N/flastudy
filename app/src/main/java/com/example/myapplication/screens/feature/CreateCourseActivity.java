package com.example.myapplication.screens.feature;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateCourseActivity extends AppCompatActivity {

    private LinearLayout containerTerms;
    private ImageButton btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        containerTerms = findViewById(R.id.containerTerms);
        btnAdd = findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(v -> addTermLayout());

        addTermLayout(); // khởi tạo item đầu tiên

        ImageView btnBack = findViewById(R.id.btnCloseCreateCourse);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnSave = findViewById(R.id.btnSave);
    }

    private void addTermLayout() {
        View termView = LayoutInflater.from(this).inflate(R.layout.item_term, containerTerms, false);
        containerTerms.addView(termView);
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

                try {
                    JSONArray jsonArray = new JSONArray(response.body().string());
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        suggestions.add(jsonArray.getJSONObject(i).getString("word"));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(suggestions));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}
