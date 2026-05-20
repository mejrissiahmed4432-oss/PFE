package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.User;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    // Views
    private View layoutSecurityMenu, layoutPasswordChangeForm, layoutEmailChangeForm;
    private View btnTriggerPasswordForm, btnTriggerEmailForm;
    
    // Password Views
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private ImageButton btnToggleCurrentPassword, btnToggleNewPassword, btnToggleConfirmPassword;
    private View layoutStrengthMeter, strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private TextView tvStrengthLabel;
    private View btnSavePassword, btnCancelPasswordChange;
    private boolean isCurrentVisible = false, isNewVisible = false, isConfirmVisible = false;

    // Email Views
    private EditText etNewEmail, etConfirmEmail, etEmailConfirmPassword;
    private ImageButton btnToggleEmailConfirmPassword;
    private View btnSaveEmail, btnCancelEmailChange;
    private boolean isEmailConfirmVisible = false;
    private TextView tvCurrentEmailLabel;

    // Preference Views
    private SwitchCompat switchEmailNotifications, switchSecurityAlerts, switchMarketingEmails;
    private View btnSavePreferences;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);

        // Bind layouts
        layoutSecurityMenu = view.findViewById(R.id.layoutSecurityMenu);
        layoutPasswordChangeForm = view.findViewById(R.id.layoutPasswordChangeForm);
        layoutEmailChangeForm = view.findViewById(R.id.layoutEmailChangeForm);
        
        btnTriggerPasswordForm = view.findViewById(R.id.btnTriggerPasswordForm);
        btnTriggerEmailForm = view.findViewById(R.id.btnTriggerEmailForm);

        // Bind Passwords
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnToggleCurrentPassword = view.findViewById(R.id.btnToggleCurrentPassword);
        btnToggleNewPassword = view.findViewById(R.id.btnToggleNewPassword);
        btnToggleConfirmPassword = view.findViewById(R.id.btnToggleConfirmPassword);
        layoutStrengthMeter = view.findViewById(R.id.layoutStrengthMeter);
        strengthBar1 = view.findViewById(R.id.strengthBar1);
        strengthBar2 = view.findViewById(R.id.strengthBar2);
        strengthBar3 = view.findViewById(R.id.strengthBar3);
        strengthBar4 = view.findViewById(R.id.strengthBar4);
        tvStrengthLabel = view.findViewById(R.id.tvStrengthLabel);
        btnSavePassword = view.findViewById(R.id.btnSavePassword);
        btnCancelPasswordChange = view.findViewById(R.id.btnCancelPasswordChange);

        // Bind Emails
        etNewEmail = view.findViewById(R.id.etNewEmail);
        etConfirmEmail = view.findViewById(R.id.etConfirmEmail);
        etEmailConfirmPassword = view.findViewById(R.id.etEmailConfirmPassword);
        btnToggleEmailConfirmPassword = view.findViewById(R.id.btnToggleEmailConfirmPassword);
        btnSaveEmail = view.findViewById(R.id.btnSaveEmail);
        btnCancelEmailChange = view.findViewById(R.id.btnCancelEmailChange);
        tvCurrentEmailLabel = view.findViewById(R.id.tvCurrentEmailLabel);

        // Bind Preferences
        switchEmailNotifications = view.findViewById(R.id.switchEmailNotifications);
        switchSecurityAlerts = view.findViewById(R.id.switchSecurityAlerts);
        switchMarketingEmails = view.findViewById(R.id.switchMarketingEmails);
        btnSavePreferences = view.findViewById(R.id.btnSavePreferences);

        // Init triggers
        btnTriggerPasswordForm.setOnClickListener(v -> {
            layoutSecurityMenu.setVisibility(View.GONE);
            layoutPasswordChangeForm.setVisibility(View.VISIBLE);
        });
        btnTriggerEmailForm.setOnClickListener(v -> {
            layoutSecurityMenu.setVisibility(View.GONE);
            layoutEmailChangeForm.setVisibility(View.VISIBLE);
        });

        // Cancel buttons
        btnCancelPasswordChange.setOnClickListener(v -> resetPasswordForm());
        btnCancelEmailChange.setOnClickListener(v -> resetEmailForm());

        // Password reveal actions
        btnToggleCurrentPassword.setOnClickListener(v -> {
            isCurrentVisible = !isCurrentVisible;
            togglePasswordVisibility(etCurrentPassword, btnToggleCurrentPassword, isCurrentVisible);
        });
        btnToggleNewPassword.setOnClickListener(v -> {
            isNewVisible = !isNewVisible;
            togglePasswordVisibility(etNewPassword, btnToggleNewPassword, isNewVisible);
        });
        btnToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmVisible = !isConfirmVisible;
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmVisible);
        });
        btnToggleEmailConfirmPassword.setOnClickListener(v -> {
            isEmailConfirmVisible = !isEmailConfirmVisible;
            togglePasswordVisibility(etEmailConfirmPassword, btnToggleEmailConfirmPassword, isEmailConfirmVisible);
        });

        // Password strength meter watcher
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Action Buttons click
        btnSavePassword.setOnClickListener(v -> handlePasswordSave());
        btnSaveEmail.setOnClickListener(v -> handleEmailSave());
        btnSavePreferences.setOnClickListener(v -> handlePreferencesSave());

        // Load settings state
        loadSettingsState();

        return view;
    }

    private void togglePasswordVisibility(EditText editText, ImageButton button, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            button.setImageResource(R.drawable.ic_eye);
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            button.setImageResource(R.drawable.ic_eye_off);
        }
        editText.setSelection(editText.getText().length());
    }

    private void updateStrengthMeter(String password) {
        if (password.isEmpty()) {
            layoutStrengthMeter.setVisibility(View.GONE);
            return;
        }

        layoutStrengthMeter.setVisibility(View.VISIBLE);
        int score = 0;

        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*") || password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?].*")) score++;

        // Reset all bars to empty gray
        strengthBar1.setBackgroundColor(Color.parseColor("#e2e8f0"));
        strengthBar2.setBackgroundColor(Color.parseColor("#e2e8f0"));
        strengthBar3.setBackgroundColor(Color.parseColor("#e2e8f0"));
        strengthBar4.setBackgroundColor(Color.parseColor("#e2e8f0"));

        if (score == 1) {
            strengthBar1.setBackgroundColor(Color.parseColor("#ef4444")); // Red
            tvStrengthLabel.setText("Weak");
            tvStrengthLabel.setTextColor(Color.parseColor("#ef4444"));
        } else if (score == 2) {
            strengthBar1.setBackgroundColor(Color.parseColor("#f59e0b")); // Orange
            strengthBar2.setBackgroundColor(Color.parseColor("#f59e0b"));
            tvStrengthLabel.setText("Fair");
            tvStrengthLabel.setTextColor(Color.parseColor("#f59e0b"));
        } else if (score == 3) {
            strengthBar1.setBackgroundColor(Color.parseColor("#06b6d4")); // Blue
            strengthBar2.setBackgroundColor(Color.parseColor("#06b6d4"));
            strengthBar3.setBackgroundColor(Color.parseColor("#06b6d4"));
            tvStrengthLabel.setText("Good");
            tvStrengthLabel.setTextColor(Color.parseColor("#06b6d4"));
        } else if (score >= 4) {
            strengthBar1.setBackgroundColor(Color.parseColor("#22c55e")); // Green
            strengthBar2.setBackgroundColor(Color.parseColor("#22c55e"));
            strengthBar3.setBackgroundColor(Color.parseColor("#22c55e"));
            strengthBar4.setBackgroundColor(Color.parseColor("#22c55e"));
            tvStrengthLabel.setText("Strong");
            tvStrengthLabel.setTextColor(Color.parseColor("#22c55e"));
        }
    }

    private void resetPasswordForm() {
        etCurrentPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
        layoutPasswordChangeForm.setVisibility(View.GONE);
        layoutSecurityMenu.setVisibility(View.VISIBLE);
        layoutStrengthMeter.setVisibility(View.GONE);
    }

    private void resetEmailForm() {
        etNewEmail.setText("");
        etConfirmEmail.setText("");
        etEmailConfirmPassword.setText("");
        layoutEmailChangeForm.setVisibility(View.GONE);
        layoutSecurityMenu.setVisibility(View.VISIBLE);
    }

    private void loadSettingsState() {
        String email = prefs.getString("user_email", "technician@company.com");
        tvCurrentEmailLabel.setText(email);

        switchEmailNotifications.setChecked(prefs.getBoolean("email_notifs", true));
        switchSecurityAlerts.setChecked(prefs.getBoolean("security_alerts", true));
        switchMarketingEmails.setChecked(prefs.getBoolean("marketing_emails", false));
    }

    private void handlePasswordSave() {
        String currentPass = etCurrentPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();

        if (currentPass.isEmpty()) {
            Toast.makeText(getContext(), "Current password is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 8) {
            Toast.makeText(getContext(), "New password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Confirm password does not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", currentPass);
        request.put("newPassword", newPass);

        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().changePassword(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    resetPasswordForm();
                } else {
                    Toast.makeText(getContext(), "Failed: Incorrect current password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error updating password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleEmailSave() {
        String newEmail = etNewEmail.getText().toString().trim();
        String confirmEmail = etConfirmEmail.getText().toString().trim();
        String password = etEmailConfirmPassword.getText().toString();

        if (newEmail.isEmpty()) {
            Toast.makeText(getContext(), "New email address is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newEmail.equals(confirmEmail)) {
            Toast.makeText(getContext(), "Confirm email does not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(getContext(), "Enter your current password to authorize this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("newEmail", newEmail);
        request.put("password", password);

        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().changeEmail(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Email updated successfully!", Toast.LENGTH_SHORT).show();
                    prefs.edit().putString("user_email", newEmail).apply();
                    tvCurrentEmailLabel.setText(newEmail);
                    resetEmailForm();
                } else {
                    Toast.makeText(getContext(), "Failed: Incorrect password or email already in use", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error updating email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePreferencesSave() {
        boolean notifs = switchEmailNotifications.isChecked();
        boolean alerts = switchSecurityAlerts.isChecked();
        boolean marketing = switchMarketingEmails.isChecked();

        prefs.edit()
                .putBoolean("email_notifs", notifs)
                .putBoolean("security_alerts", alerts)
                .putBoolean("marketing_emails", marketing)
                .apply();

        Toast.makeText(getContext(), "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
    }
}
