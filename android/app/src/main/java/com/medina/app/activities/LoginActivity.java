package com.medina.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.LoginRequest;
import com.medina.app.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private View btnSignIn;
    private TextView tvForgotPassword, tvError;
    private LinearLayout errorLayout;
    private ProgressBar progressBar;
    private ImageButton btnTogglePassword;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        errorLayout = findViewById(R.id.errorLayout);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if  (!passwordVisible) {

                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.length());
        });

        // Sign in
        btnSignIn.setOnClickListener(v -> performLogin());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Staggered entry animation on the form card
        View formCard = findViewById(R.id.formCard);
        if (formCard != null) {
            formCard.setAlpha(0f);
            formCard.setTranslationY(40f);
            formCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .start();
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        hideError();
        setLoading(true);

        ApiClient.getApiService().login(new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse user = response.body();
                            // Save token and user data to SharedPreferences
                            getSharedPreferences("medina_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("auth_token", user.getToken())
                                    .putString("user_email", user.getEmail())
                                    .putString("user_name", user.getFirstName() + " " + user.getLastName())
                                    .putString("user_role", user.getRole())
                                    .apply();
                            // TODO: Navigate to dashboard
                            showError("Login successful! Welcome " + user.getFirstName());
                        } else if (response.code() == 401) {
                            showError("Invalid email or password.");
                        } else {
                            showError("A server error occurred. Please try again later.");
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Cannot connect to server. Please check your connection.");
                    }
                });
    }

    private void showError(String message) {
        errorLayout.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    private void hideError() {
        errorLayout.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        btnSignIn.setAlpha(loading ? 0.7f : 1.0f);
    }
}
