package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.FolderSelectAdapter;
import com.example.myapplication.model.Folder;
import com.example.myapplication.screens.feature.CreateClassActivity;
import com.example.myapplication.screens.feature.CreateCourseActivity;
import com.example.myapplication.screens.feature.CreateFolderActivity;
import com.example.myapplication.screens.fragment.FolderFragment;
import com.example.myapplication.screens.fragment.HomeFragment;
import com.example.myapplication.screens.fragment.MessengerFragment;
import com.example.myapplication.screens.fragment.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        EdgeToEdge.enable(this);

        bottomNavigationView = findViewById(R.id.bottom_navigation);


        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_folder) {
                selectedFragment = new FolderFragment();
            } else if (itemId == R.id.navigation_add) {
                showAddOptions();
                return false;
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
            showSelectFolderForCourse();
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

    private void showSelectFolderForCourse() {
        BottomSheetDialog dlg = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bs_add_folders, null);
        dlg.setContentView(view);

        TextView addFolder = view.findViewById(R.id.addFolder);

        addFolder.setOnClickListener(v -> {
            dlg.dismiss();
            startActivity(new Intent(this, CreateFolderActivity.class));
        });

        RecyclerView rv = view.findViewById(R.id.rvFolders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("folders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    List<Folder> list = new ArrayList<>();
                    for (DocumentSnapshot d : q) {
                        Folder f = d.toObject(Folder.class);
                        f.setId(d.getId());
                        list.add(f);
                    }
                    FolderSelectAdapter adapter = new FolderSelectAdapter(list, selectedIds -> {
                        if (!selectedIds.isEmpty()) {
                            String folderId = selectedIds.iterator().next();
                            Intent i = new Intent(HomeActivity.this, CreateCourseActivity.class);
                            i.putExtra(CreateCourseActivity.EXTRA_FOLDER_ID, folderId);
                            startActivity(i);
                            dlg.dismiss();
                        }
                    });
                    rv.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi load thư mục: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        dlg.show();
    }

}
