package com.example.myapplication.screens.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.myapplication.R;
import com.example.myapplication.screens.auth.LoginActivity;
import com.example.myapplication.screens.feature.ChangePasswordActivity;
import com.example.myapplication.screens.feature.UpdateInfoActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView txtUsername, txtEmail;
    private Button btnLogout, btnChangePassword, btnUpdateInfo;
    private View layoutLoadingUserFragment;

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtUsername = view.findViewById(R.id.txtUsername);
        txtEmail = view.findViewById(R.id.txtEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnUpdateInfo = view.findViewById(R.id.btnUpdateInfo);
        layoutLoadingUserFragment = view.findViewById(R.id.loadingLayoutUserFragment);

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        auth.signOut();

                        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(
                                getContext(),
                                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(getString(R.string.web_client_id))
                                        .requestEmail()
                                        .build()
                        );
                        googleSignInClient.signOut().addOnCompleteListener(task -> {
                            Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getContext(), LoginActivity.class));
                            requireActivity().finish();
                        });
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ChangePasswordActivity.class));
        });

        btnUpdateInfo.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), UpdateInfoActivity.class));
        });

        loadData();

        return view;
    }

    private void loadData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        layoutLoadingUserFragment.setVisibility(View.VISIBLE);

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    layoutLoadingUserFragment.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        txtUsername.setText(documentSnapshot.getString("username"));
                        txtEmail.setText(documentSnapshot.getString("email"));
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    layoutLoadingUserFragment.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
