package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Alert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertsFragment extends Fragment implements AlertsAdapter.OnAlertResolveListener {

    private Spinner spinnerSeverity, spinnerStatus, spinnerTimeRange;
    private TextView tvStatActiveCount, tvStatWarningCount, tvStatResolvedCount, tvStatTotalCount;
    private RecyclerView rvAlerts;
    private View layoutEmptyAlerts;

    private AlertsAdapter adapter;
    private List<Alert> allAlertsList = new ArrayList<>();
    private List<Alert> filteredAlertsList = new ArrayList<>();

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);

        // Bind views
        spinnerSeverity = view.findViewById(R.id.spinnerSeverity);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        spinnerTimeRange = view.findViewById(R.id.spinnerTimeRange);

        tvStatActiveCount = view.findViewById(R.id.tvStatActiveCount);
        tvStatWarningCount = view.findViewById(R.id.tvStatWarningCount);
        tvStatResolvedCount = view.findViewById(R.id.tvStatResolvedCount);
        tvStatTotalCount = view.findViewById(R.id.tvStatTotalCount);

        rvAlerts = view.findViewById(R.id.rvAlerts);
        layoutEmptyAlerts = view.findViewById(R.id.layoutEmptyAlerts);

        // Set up filters dropdown list
        setupSpinners();

        // Setup RecyclerView
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlertsAdapter(filteredAlertsList, this);
        rvAlerts.setAdapter(adapter);

        // Load data
        loadAlerts();

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<String> severityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"All Types", "Critical", "Warning", "Info"});
        severityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeverity.setAdapter(severityAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"All Status", "Active", "Resolved"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"All Time", "Last 24 Hours", "Last 7 Days", "Last 30 Days"});
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeAdapter);

        AdapterView.OnItemSelectedListener filterChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerSeverity.setOnItemSelectedListener(filterChangeListener);
        spinnerStatus.setOnItemSelectedListener(filterChangeListener);
        spinnerTimeRange.setOnItemSelectedListener(filterChangeListener);
    }

    private void loadAlerts() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userRole = prefs.getString("user_role", null);

        ApiClient.getApiService().getAlerts(null, userRole).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allAlertsList = response.body();
                    updateStatistics();
                    applyFilters();
                } else {
                    loadFallbackAlerts();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                loadFallbackAlerts();
            }
        });
    }

    private void loadFallbackAlerts() {
        allAlertsList.clear();
        // Fallback dummy data mirroring the frontend
        allAlertsList.add(new Alert("1", "Main Generator Alert", "The main generator experienced an unexpected shutdown due to high fuel pressure.", "CRITICAL", "ACTIVE", "2026-05-20T12:00:00Z", "", ""));
        allAlertsList.add(new Alert("2", "Component Lifetime Warning", "Component P-102 (Water pump) has exceeded 90% of its operating lifespan.", "WARNING", "ACTIVE", "2026-05-20T10:15:00Z", "", ""));
        allAlertsList.add(new Alert("3", "System Maintenance Overdue", "Scheduled quarterly calibration for sensor module SM-99 is now overdue.", "INFO", "RESOLVED", "2026-05-19T08:30:00Z", "morad.mejri@medina.com", "2026-05-19T09:00:00Z"));
        allAlertsList.add(new Alert("4", "Low Fuel warning", "Fuel storage tank level dropped below 15% safety threshold.", "WARNING", "ACTIVE", "2026-05-18T16:45:00Z", "", ""));

        updateStatistics();
        applyFilters();
    }

    private void updateStatistics() {
        int active = 0;
        int warning = 0;
        int resolved = 0;

        for (Alert alert : allAlertsList) {
            if ("ACTIVE".equalsIgnoreCase(alert.getStatus())) {
                active++;
            } else if ("RESOLVED".equalsIgnoreCase(alert.getStatus())) {
                resolved++;
            }

            if ("WARNING".equalsIgnoreCase(alert.getSeverity())) {
                warning++;
            }
        }

        tvStatActiveCount.setText(String.valueOf(active));
        tvStatWarningCount.setText(String.valueOf(warning));
        tvStatResolvedCount.setText(String.valueOf(resolved));
        tvStatTotalCount.setText(String.valueOf(allAlertsList.size()));

        // Also update Alert Badge count in Dashboard Activity if still attached
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).updateAlertBadgeCount(active);
        }
    }

    private void applyFilters() {
        String severityFilter = spinnerSeverity.getSelectedItem().toString();
        String statusFilter = spinnerStatus.getSelectedItem().toString();
        String timeFilter = spinnerTimeRange.getSelectedItem().toString();

        filteredAlertsList.clear();

        for (Alert alert : allAlertsList) {
            // Severity Filter
            if (!severityFilter.equals("All Types")) {
                if (severityFilter.equalsIgnoreCase("Critical") && !"CRITICAL".equalsIgnoreCase(alert.getSeverity())) continue;
                if (severityFilter.equalsIgnoreCase("Warning") && !"WARNING".equalsIgnoreCase(alert.getSeverity())) continue;
                if (severityFilter.equalsIgnoreCase("Info") && !"INFO".equalsIgnoreCase(alert.getSeverity())) continue;
            }

            // Status Filter
            if (!statusFilter.equals("All Status")) {
                if (statusFilter.equalsIgnoreCase("Active") && !"ACTIVE".equalsIgnoreCase(alert.getStatus())) continue;
                if (statusFilter.equalsIgnoreCase("Resolved") && !"RESOLVED".equalsIgnoreCase(alert.getStatus())) continue;
            }

            // Time Filter
            if (!timeFilter.equals("All Time")) {
                if (!matchesTimeFilter(alert.getCreatedAt(), timeFilter)) continue;
            }

            filteredAlertsList.add(alert);
        }

        adapter.updateList(filteredAlertsList);

        if (filteredAlertsList.isEmpty()) {
            rvAlerts.setVisibility(View.GONE);
            layoutEmptyAlerts.setVisibility(View.VISIBLE);
        } else {
            rvAlerts.setVisibility(View.VISIBLE);
            layoutEmptyAlerts.setVisibility(View.GONE);
        }
    }

    private boolean matchesTimeFilter(String dateStr, String filter) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            // Parse ISO-8601 or similar
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date alertDate = sdf.parse(dateStr);
            if (alertDate == null) {
                // Try fallback formatting
                SimpleDateFormat fallback = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                alertDate = fallback.parse(dateStr);
            }
            if (alertDate == null) return true; // Fail-open

            Calendar cal = Calendar.getInstance();
            if (filter.equals("Last 24 Hours")) {
                cal.add(Calendar.HOUR, -24);
                return alertDate.after(cal.getTime());
            } else if (filter.equals("Last 7 Days")) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
                return alertDate.after(cal.getTime());
            } else if (filter.equals("Last 30 Days")) {
                cal.add(Calendar.DAY_OF_YEAR, -30);
                return alertDate.after(cal.getTime());
            }
        } catch (Exception e) {
            // parsing error, keep it in the list by default
            return true;
        }
        return true;
    }

    @Override
    public void onResolve(Alert alert, int position) {
        String userEmail = prefs.getString("user_email", "technician@company.com");

        Map<String, String> request = new HashMap<>();
        request.put("status", "RESOLVED");
        request.put("resolvedBy", userEmail);

        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().resolveAlert(alert.getId()).enqueue(new Callback<Alert>() {
            @Override
            public void onResponse(Call<Alert> call, Response<Alert> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Alert updated = response.body();
                    // Update in our list
                    for (int i = 0; i < allAlertsList.size(); i++) {
                        if (allAlertsList.get(i).getId().equals(updated.getId())) {
                            allAlertsList.set(i, updated);
                            break;
                        }
                    }
                    Toast.makeText(getContext(), "Alert resolved successfully!", Toast.LENGTH_SHORT).show();
                    updateStatistics();
                    applyFilters();
                } else {
                    Toast.makeText(getContext(), "Failed to resolve alert", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Alert> call, Throwable t) {
                // Fallback local mock update on failure (highly resilient)
                alert.setStatus("RESOLVED");
                alert.setResolvedBy(userEmail);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                alert.setResolvedAt(sdf.format(new Date()));

                Toast.makeText(getContext(), "Alert marked as resolved locally", Toast.LENGTH_SHORT).show();
                updateStatistics();
                applyFilters();
            }
        });
    }
}
