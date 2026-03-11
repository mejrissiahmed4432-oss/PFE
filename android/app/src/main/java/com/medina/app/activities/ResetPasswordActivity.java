package com.medina.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.ResetPasswordRequest;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private ImageButton btnToggleNew, btnToggleConfirm;
    private View btnUpdatePassword;
    private TextView tvCancel, tvError, tvSuccess;
    private LinearLayout errorLayout, successLayout;
    private ProgressBar progressBar;
    private static final String TAG = "ResetPasswordActivity";

    private String resetToken;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;
    private boolean isSubmittedSuccessfully = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Get token from intent data (deep link) or extras
        Uri data = getIntent().getData();
        Log.d(TAG, "onCreate intent data: " + (data != null ? data.toString() : "null"));
        
        if (data != null && data.getQueryParameter("token") != null) {
            resetToken = data.getQueryParameter("token");
            Log.d(TAG, "Extracted token from deep link: " + resetToken);
            Toast.makeText(this, "Reset link detected", Toast.LENGTH_SHORT).show();
        } else {
            resetToken = getIntent().getStringExtra("reset_token");
        }

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnToggleNew = findViewById(R.id.btnToggleNew);
        btnToggleConfirm = findViewById(R.id.btnToggleConfirm);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        tvCancel = findViewById(R.id.tvCancel);
        errorLayout = findViewById(R.id.errorLayout);
        tvError = findViewById(R.id.tvError);
        successLayout = findViewById(R.id.successLayout);
        tvSuccess = findViewById(R.id.tvSuccess);
        progressBar = findViewById(R.id.progressBar);

        // Initial button state
        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setAlpha(0.5f);

        // Live validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordsLive();
            }
        };
        etNewPassword.addTextChangedListener(validationWatcher);
        etConfirmPassword.addTextChangedListener(validationWatcher);

        btnToggleNew.setOnClickListener(v -> {
            newPasswordVisible = !newPasswordVisible;
            if (newPasswordVisible) {
                etNewPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleNew.setImageResource(R.drawable.ic_eye_off);
            } else {
                etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleNew.setImageResource(R.drawable.ic_eye);
            }
            etNewPassword.setSelection(etNewPassword.length());
        });

        btnToggleConfirm.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            if (confirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleConfirm.setImageResource(R.drawable.ic_eye_off);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleConfirm.setImageResource(R.drawable.ic_eye);
            }
            etConfirmPassword.setSelection(etConfirmPassword.length());
        });

        btnUpdatePassword.setOnClickListener(v -> updatePassword());

        tvCancel.setOnClickListener(v -> finish());

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null && data.getQueryParameter("token") != null) {
            resetToken = data.getQueryParameter("token");
            Log.d(TAG, "New token received via onNewIntent: " + resetToken);
            Toast.makeText(this, "Reset link detected", Toast.LENGTH_SHORT).show();
            hideError();
            hideSuccess();
        } else {
            Log.d(TAG, "onNewIntent called but no token found in: " + (data != null ? data.toString() : "null"));
        }
    }

    private void validatePasswordsLive() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.isEmpty()) {
            hideError();
            return;
        }

        if (newPass.length() < 6) {
            showError("Password must be at least 6 characters.");
            setButtonEnabled(false);
        } else if (!newPass.matches(".*[a-z].*") || !newPass.matches(".*[A-Z].*")) {
            showError("Must contain uppercase and lowercase letters.");
            setButtonEnabled(false);
        } else if (!confirmPass.isEmpty() && !newPass.equals(confirmPass)) {
            showError("Passwords do not match.");
            setButtonEnabled(false);
        } else if (newPass.equals(confirmPass)) {
            hideError();
            setButtonEnabled(!isSubmittedSuccessfully);
        } else {
            setButtonEnabled(false);
        }
    }

    private void setButtonEnabled(boolean enabled) {
        btnUpdatePassword.setEnabled(enabled);
        btnUpdatePassword.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void updatePassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        // Validate password complexity (matches backend requirement)
        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!newPassword.matches(".*[a-z].*") || !newPassword.matches(".*[A-Z].*")) {
            showError("Password must contain both uppercase and lowercase letters.");
            return;
        }

        if (resetToken == null || resetToken.isEmpty()) {
            showError("Invalid reset token. Please request a new reset link.");
            return;
        }

        hideError();
        setLoading(true);

        ApiClient.getApiService().resetPassword(new ResetPasswordRequest(resetToken, newPassword))
                .enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            isSubmittedSuccessfully = true; // Lock the UI
                            setButtonEnabled(false);
                            tvCancel.setEnabled(false);
                            tvCancel.setAlpha(0.5f);
                            
                            String msg = response.body().get("message");
                            showSuccess(msg != null ? msg : "Password updated successfully!");
                            Toast.makeText(ResetPasswordActivity.this, "Password reset successful!", Toast.LENGTH_LONG).show();
                            
                            // Navigate back to login using CLEAR_TASK to ensure no backstack issues
                            etNewPassword.postDelayed(() -> {
                                if (!isFinishing()) {
                                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }, 1500);
                        } else {
                            String errorMsg = "Failed to update password. Link may be expired.";
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

    private void hideError() {
        errorLayout.setVisibility(View.GONE);
    }

    private void hideSuccess() {
        successLayout.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            validatePasswordsLive(); // Re-validate to set button state correctly
        } else {
            btnUpdatePassword.setEnabled(false);
            btnUpdatePassword.setAlpha(0.7f);
        }
    }
}
