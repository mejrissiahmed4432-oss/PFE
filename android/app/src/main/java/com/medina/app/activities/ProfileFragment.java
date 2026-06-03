package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.User;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private View btnEditProfile, layoutEditActions, btnCancelProfile, btnSaveProfile;
    private TextView tvProfileInitials, tvProfileFullName, tvProfileRole;
    private TextView tvProfileMetaEmail, tvProfileMetaDept, tvProfileMetaJoined;
    private EditText etProfileFirstName, etProfileLastName, etProfileEmail, etProfilePhone, etProfileDept;
    private TextView tvRequiredFirstName, tvRequiredLastName, tvRequiredPhone;
    private View layoutFirstNameContainer, layoutLastNameContainer, layoutPhoneContainer;

    private SharedPreferences prefs;
    private User currentUser;
    private boolean isEditing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);

        // Bind layouts
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        layoutEditActions = view.findViewById(R.id.layoutEditActions);
        btnCancelProfile = view.findViewById(R.id.btnCancelProfile);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);

        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);

        tvProfileMetaEmail = view.findViewById(R.id.tvProfileMetaEmail);
        tvProfileMetaDept = view.findViewById(R.id.tvProfileMetaDept);
        tvProfileMetaJoined = view.findViewById(R.id.tvProfileMetaJoined);

        etProfileFirstName = view.findViewById(R.id.etProfileFirstName);
        etProfileLastName = view.findViewById(R.id.etProfileLastName);
        etProfileEmail = view.findViewById(R.id.etProfileEmail);
        etProfilePhone = view.findViewById(R.id.etProfilePhone);
        etProfileDept = view.findViewById(R.id.etProfileDept);

        tvRequiredFirstName = view.findViewById(R.id.tvRequiredFirstName);
        tvRequiredLastName = view.findViewById(R.id.tvRequiredLastName);
        tvRequiredPhone = view.findViewById(R.id.tvRequiredPhone);

        layoutFirstNameContainer = view.findViewById(R.id.layoutFirstNameContainer);
        layoutLastNameContainer = view.findViewById(R.id.layoutLastNameContainer);
        layoutPhoneContainer = view.findViewById(R.id.layoutPhoneContainer);

        // Filters and limitations
        etProfilePhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

        // Setup actions
        btnEditProfile.setOnClickListener(v -> toggleEditMode(true));
        btnCancelProfile.setOnClickListener(v -> {
            toggleEditMode(false);
            populateFields();
        });
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        // Initial loading
        loadProfileData();

        return view;
    }

    private void toggleEditMode(boolean edit) {
        isEditing = edit;
        btnEditProfile.setVisibility(edit ? View.GONE : View.VISIBLE);
        layoutEditActions.setVisibility(edit ? View.VISIBLE : View.GONE);

        etProfileFirstName.setEnabled(edit);
        etProfileLastName.setEnabled(edit);
        etProfilePhone.setEnabled(edit);

        int visibility = edit ? View.VISIBLE : View.GONE;
        tvRequiredFirstName.setVisibility(visibility);
        tvRequiredLastName.setVisibility(visibility);
        tvRequiredPhone.setVisibility(visibility);

        // Background drawables update to reflect edit states visually
        int bgRes = edit ? R.drawable.bg_input_field : R.drawable.bg_input_field; // we keep same background but can highlight focus
        if (edit) {
            etProfileFirstName.requestFocus();
        }
    }

    private void loadProfileData() {
        // Sync token in ApiClient first
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().getCurrentUser().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    populateFields();
                    
                    // Update locally cached SharedPreferences
                    prefs.edit()
                            .putString("user_id", currentUser.getId())
                            .putString("user_name", currentUser.getFirstName() + " " + currentUser.getLastName())
                            .putString("user_email", currentUser.getEmail())
                            .putString("user_role", currentUser.getRole())
                            .putString("user_phone", currentUser.getPhoneNumber())
                            .putString("user_department", currentUser.getDepartment())
                            .apply();

                    // Update Topbar initials in activity if still attached
                    if (getActivity() instanceof DashboardActivity) {
                        ((DashboardActivity) getActivity()).updateUserAvatarInitials(currentUser.getFirstName() + " " + currentUser.getLastName());
                    }
                } else {
                    // Load fallback local preferences
                    loadFallbackLocal();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                loadFallbackLocal();
            }
        });
    }

    private void loadFallbackLocal() {
        String name = prefs.getString("user_name", "Morad Mejri");
        String email = prefs.getString("user_email", "morad.mejri@medina.com");
        String role = prefs.getString("user_role", "Stock Manager");
        String phone = prefs.getString("user_phone", "55123456");
        String dept = prefs.getString("user_department", "IT Management");

        currentUser = new User("local", "Morad", "Mejri", email, role, phone, dept, "March 2026");
        String[] parts = name.split(" ");
        if (parts.length > 0) currentUser.setFirstName(parts[0]);
        if (parts.length > 1) currentUser.setLastName(parts[1]);

        populateFields();
    }

    private void populateFields() {
        if (currentUser == null) return;

        String fName = currentUser.getFirstName();
        String lName = currentUser.getLastName();
        String initials = "";
        if (fName != null && !fName.isEmpty()) initials += fName.substring(0, 1).toUpperCase();
        if (lName != null && !lName.isEmpty()) initials += lName.substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "MM";

        tvProfileInitials.setText(initials);
        tvProfileFullName.setText(fName + " " + lName);
        tvProfileRole.setText(currentUser.getRole());

        tvProfileMetaEmail.setText(currentUser.getEmail());
        tvProfileMetaDept.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "IT Management");
        tvProfileMetaJoined.setText("Joined " + (currentUser.getJoinedDate() != null ? currentUser.getJoinedDate() : "March 2026"));

        etProfileFirstName.setText(fName);
        etProfileLastName.setText(lName);
        etProfileEmail.setText(currentUser.getEmail());
        etProfilePhone.setText(currentUser.getPhoneNumber());
        etProfileDept.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "IT Management");
    }

    private void saveProfileChanges() {
        String firstName = etProfileFirstName.getText().toString().trim();
        String lastName = etProfileLastName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();

        if (firstName.isEmpty()) {
            Toast.makeText(getContext(), "First name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastName.isEmpty()) {
            Toast.makeText(getContext(), "Last name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() != 8) {
            Toast.makeText(getContext(), "Phone number must be exactly 8 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("firstName", firstName);
        request.put("lastName", lastName);
        request.put("phoneNumber", phone);

        // Sync token in ApiClient
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().updateProfile(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    currentUser.setFirstName(firstName);
                    currentUser.setLastName(lastName);
                    currentUser.setPhoneNumber(phone);

                    // Update cached preferences
                    prefs.edit()
                            .putString("user_name", firstName + " " + lastName)
                            .putString("user_phone", phone)
                            .apply();

                    populateFields();
                    toggleEditMode(false);

                    // Update Topbar initials
                    if (getActivity() instanceof DashboardActivity) {
                        ((DashboardActivity) getActivity()).updateUserAvatarInitials(firstName + " " + lastName);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error updating profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
