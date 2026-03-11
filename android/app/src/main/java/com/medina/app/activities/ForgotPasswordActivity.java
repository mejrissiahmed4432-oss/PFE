package com.medina.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.ForgotPasswordRequest;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendReset;
    private TextView tvBackToLogin, tvError, tvSuccess;
    private LinearLayout errorLayout, successLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        errorLayout = findViewById(R.id.errorLayout);
        tvError = findViewById(R.id.tvError);
        successLayout = findViewById(R.id.successLayout);
        tvSuccess = findViewById(R.id.tvSuccess);
        progressBar = findViewById(R.id.progressBar);

        btnSendReset.setOnClickListener(v -> sendResetLink());

        tvBackToLogin.setOnClickListener(v -> finish());

        // Staggered entry animation
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

    private void sendResetLink() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        hideMessages();
        setLoading(true);

        ApiClient.getApiService().forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            String msg = response.body().get("message");
                            showSuccess(msg != null ? msg : "Reset link sent successfully.");
                        } else {
                            // Parse error body
                            String errorMsg = "An error occurred.";
                            try {
                                if (response.errorBody() != null) {
                                    JSONObject json = new JSONObject(response.errorBody().string());
                                    errorMsg = json.optString("message", errorMsg);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            showError(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        setLoading(false);
                        showError("Cannot connect to server. Please check your connection.");
                    }
                });
    }

    private void showError(String message) {
        errorLayout.setVisibility(View.VISIBLE);
        tvError.setText(message);
        successLayout.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        successLayout.setVisibility(View.VISIBLE);
        tvSuccess.setText(message);
        errorLayout.setVisibility(View.GONE);
    }

    private void hideMessages() {
        errorLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!loading);
        btnSendReset.setAlpha(loading ? 0.7f : 1.0f);
    }
}
