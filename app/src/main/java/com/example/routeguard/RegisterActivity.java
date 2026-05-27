package com.example.routeguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.routeguard.data.model.User;
import com.example.routeguard.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etEmail,
            etPassword, etConfirmPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etFirstName       = findViewById(R.id.etFirstName);
        etLastName        = findViewById(R.id.etLastName);
        etUsername        = findViewById(R.id.etUsername);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        findViewById(R.id.btnSignUp).setOnClickListener(v -> register());

        findViewById(R.id.tvSignIn).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        etFirstName.setHintTextColor(
                getResources().getColor(R.color.text_hint, null));
        etLastName.setHintTextColor(
                getResources().getColor(R.color.text_hint, null));
        etEmail.setHintTextColor(
                getResources().getColor(R.color.text_hint, null));
        etPassword.setHintTextColor(
                getResources().getColor(R.color.text_hint, null));
        etConfirmPassword.setHintTextColor(
                getResources().getColor(R.color.text_hint, null));
    }

    private void register() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String username  = etUsername.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();
        String confirm   = etConfirmPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser != null) {
                        // 1. Update Firebase Profile with Full Name
                        String fullName = firstName + " " + lastName;
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();

                        firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    // 2. Sync with MongoDB via Retrofit
                                    syncUsernameWithBackend(username, fullName);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Registration failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void syncUsernameWithBackend(String username, String displayName) {
        User userUpdate = new User();
        userUpdate.setUsername(username);
        userUpdate.setDisplayName(displayName);

        RetrofitClient.getInstance().getApiService()
                .updateUserProfile(userUpdate)
                .enqueue(new retrofit2.Callback<User>() {
                    @Override
                    public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                        // Regardless of sync success, we proceed to MainActivity
                        // In a real app, we might check for 400 (username taken)
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<User> call, Throwable t) {
                        // Proceed anyway, user is still created in Firebase
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }
}