package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.model.User;
import com.example.myapplication.screens.HomeActivity;
import com.example.myapplication.screens.auth.LoginActivity;
import com.example.myapplication.screens.auth.SignUpActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private CheckBox rememberMe;
    private SharedPreferences prefs;

    private static final String PREFS_NAME   = "login_prefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_PASS     = "password";

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loginEmail    = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton   = findViewById(R.id.loginButton);
        rememberMe    = findViewById(R.id.checkboxRemember);
        prefs         = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean rem = prefs.getBoolean(KEY_REMEMBER, false);
        if (rem) {
            loginEmail.setText(prefs.getString(KEY_EMAIL, ""));
            loginPassword.setText(prefs.getString(KEY_PASS, ""));
            rememberMe.setChecked(true);
        }

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String pass  = loginPassword.getText().toString().trim();
            if (!isValid(email, pass)) return;

            if (rememberMe.isChecked()) {
                prefs.edit()
                        .putBoolean(KEY_REMEMBER, true)
                        .putString(KEY_EMAIL, email)
                        .putString(KEY_PASS, pass)
                        .apply();
            } else {
                prefs.edit().clear().apply();
            }

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(res -> {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Login Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.googleSignInButton)
                .setOnClickListener(v -> {
                    googleSignInClient.signOut().addOnCompleteListener(t -> {
                        Intent intent = googleSignInClient.getSignInIntent();
                        startActivityForResult(intent, RC_SIGN_IN);
                    });
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btn_SignUp_screen).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private boolean isValid(String email, String pass) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Please enter a valid email");
            return false;
        }
        if (pass.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int rc, int resultCode, Intent data) {
        super.onActivityResult(rc, resultCode, data);
        if (rc == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acct = task.getResult(ApiException.class);
                if (acct != null) {
                    AuthCredential cred = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                    auth.signInWithCredential(cred)
                            .addOnCompleteListener(this, t -> {
                                if (t.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        saveUserToFirestore(user);
                                        Toast.makeText(this,
                                                "Welcome " + user.getDisplayName(),
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, HomeActivity.class));
                                        finish();
                                    }
                                } else {
                                    Toast.makeText(this,
                                            "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String uid = user.getUid();
        User userData = new User(uid, user.getDisplayName(), user.getEmail());
        db.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> { /* OK */ })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to save user to Firestore", Toast.LENGTH_SHORT).show());
    }
}