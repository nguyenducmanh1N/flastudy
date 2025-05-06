package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.screens.feature.CreateClassActivity;
import com.example.myapplication.screens.feature.CreateCourseActivity;
import com.example.myapplication.screens.feature.CreateFolderActivity;
import com.example.myapplication.screens.fragment.FolderFragment;
import com.example.myapplication.screens.fragment.HomeFragment;
import com.example.myapplication.screens.fragment.MessengerFragment;
import com.example.myapplication.screens.fragment.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomeActivity extends AppCompatActivity {


//    private BottomNavigationView bottomNavigationView;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_home);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_page), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//
//    }
private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load màn hình mặc định
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_folder) {
                selectedFragment = new FolderFragment();
            } else if (itemId == R.id.navigation_add) {
                showAddOptions(); // 👉 Thay vì chuyển sang AddFragment
                return false;
//                selectedFragment = new AddFragment();
            } else if (itemId == R.id.navigation_messenger) {
                selectedFragment = new MessengerFragment();
            } else if (itemId == R.id.navigation_user) {
                selectedFragment = new UserFragment();
            }

            return loadFragment(selectedFragment);
        });

    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
    private void showAddOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_options, null);
        bottomSheetDialog.setContentView(view);

        TextView createSubject = view.findViewById(R.id.optionCreateSubject);
        TextView createFolder = view.findViewById(R.id.optionCreateFolder);
        TextView createClass = view.findViewById(R.id.optionCreateClass);

        createSubject.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateCourseActivity.class);
            startActivity(intent);
        });

        createFolder.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateFolderActivity.class);
            startActivity(intent);
        });

        createClass.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, CreateClassActivity.class);
            startActivity(intent);
        });

        bottomSheetDialog.show();
    }





}