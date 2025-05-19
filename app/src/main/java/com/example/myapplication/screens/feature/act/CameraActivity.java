package com.example.myapplication.screens.feature.act;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 10;
    private Uri photoUri;
    private ImageView imgPreview;
    private TextView tvOcrResult;


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

        imgPreview  = findViewById(R.id.imgPreview);
        tvOcrResult = findViewById(R.id.tvOcrResult);
        Button btn   = findViewById(R.id.btnTakePhoto);

        btn.setOnClickListener(v -> {
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
    }

    // Kiểm tra đã có quyền chưa
    private boolean allPermissionsGranted() {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Kết quả xin quyền
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
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

    // Khởi camera intent, lưu file tạm trong cache
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

    // OCR với ML Kit
    private void runTextRecognition(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String fullText = visionText.getText();
                        tvOcrResult.setText(fullText);

                        List<String> words = extractEnglishWords(fullText);
                        tvOcrResult.append("\n\n>> Từ tìm được:\n" + words);
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

    // Tách từ tiếng Anh
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
}
