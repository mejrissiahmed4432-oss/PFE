package com.medina.app.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertsFragment extends Fragment implements AlertsAdapter.OnAlertResolveListener {

    private Spinner spinnerSeverity, spinnerStatus, spinnerTimeRange;
    private TextView tvStatActiveCount, tvStatWarningCount, tvStatResolvedCount, tvStatTotalCount;
    private RecyclerView rvAlerts;
    private View layoutEmptyAlerts;
    private CheckBox cbSelectAllAlerts;
    private Button btnDeleteSelectedAlerts;

    private AlertsAdapter adapter;
    private List<Alert> allAlertsList = new ArrayList<>();
    private List<Alert> filteredAlertsList = new ArrayList<>();

    private SharedPreferences prefs;

    // Track if spinner listeners should suppress filter calls on init
    private boolean spinnerReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);

        // Bind views
        spinnerSeverity  = view.findViewById(R.id.spinnerSeverity);
        spinnerStatus    = view.findViewById(R.id.spinnerStatus);
        spinnerTimeRange = view.findViewById(R.id.spinnerTimeRange);

        tvStatActiveCount   = view.findViewById(R.id.tvStatActiveCount);
        tvStatWarningCount  = view.findViewById(R.id.tvStatWarningCount);
        tvStatResolvedCount = view.findViewById(R.id.tvStatResolvedCount);
        tvStatTotalCount    = view.findViewById(R.id.tvStatTotalCount);

        rvAlerts          = view.findViewById(R.id.rvAlerts);
        layoutEmptyAlerts = view.findViewById(R.id.layoutEmptyAlerts);
        cbSelectAllAlerts        = view.findViewById(R.id.cbSelectAllAlerts);
        btnDeleteSelectedAlerts  = view.findViewById(R.id.btnDeleteSelectedAlerts);

        setupSpinners();
        setupRecyclerView();
        setupSelectionBar();
        loadAlerts();

        return view;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPINNERS
    // ─────────────────────────────────────────────────────────────────────────
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

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerReady) applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerSeverity.setOnItemSelectedListener(filterListener);
        spinnerStatus.setOnItemSelectedListener(filterListener);
        spinnerTimeRange.setOnItemSelectedListener(filterListener);

        spinnerReady = true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RECYCLER VIEW
    // ─────────────────────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlertsAdapter(filteredAlertsList, this);
        adapter.setOnSelectionChangedListener(selectedCount -> {
            // Keep "Select All" checkbox in sync
            boolean allSelected = adapter.isAllSelected();
            cbSelectAllAlerts.setOnCheckedChangeListener(null);
            cbSelectAllAlerts.setChecked(allSelected);
            cbSelectAllAlerts.setOnCheckedChangeListener((cb, checked) -> onSelectAllToggled(checked));

            // Enable/disable delete button
            boolean hasSelection = selectedCount > 0;
            btnDeleteSelectedAlerts.setEnabled(hasSelection);
            btnDeleteSelectedAlerts.setAlpha(hasSelection ? 1f : 0.5f);
            btnDeleteSelectedAlerts.setText(selectedCount > 0 ? "Delete (" + selectedCount + ")" : "Delete Selected");
        });
        rvAlerts.setAdapter(adapter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SELECTION BAR
    // ─────────────────────────────────────────────────────────────────────────
    private void setupSelectionBar() {
        cbSelectAllAlerts.setOnCheckedChangeListener((cb, checked) -> onSelectAllToggled(checked));

        btnDeleteSelectedAlerts.setOnClickListener(v -> {
            List<Alert> selected = adapter.getSelectedAlerts();
            if (selected.isEmpty()) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Alerts")
                    .setMessage("Delete " + selected.size() + " selected alert(s)? This cannot be undone.")
                    .setPositiveButton("Delete All", (d, w) -> deleteSelectedAlerts(selected))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void onSelectAllToggled(boolean checked) {
        if (checked) {
            adapter.setSelectionMode(true);
            adapter.selectAll();
        } else {
            adapter.clearAll();
        }
        boolean hasSelection = adapter.getSelectedCount() > 0;
        btnDeleteSelectedAlerts.setEnabled(hasSelection);
        btnDeleteSelectedAlerts.setAlpha(hasSelection ? 1f : 0.5f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD ALERTS (user-specific)
    // ─────────────────────────────────────────────────────────────────────────
    private void loadAlerts() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userId   = prefs.getString("user_id", null);
        String userRole = prefs.getString("user_role", null);

        // Enable selection mode always so checkboxes are accessible
        adapter.setSelectionMode(true);

        ApiClient.getApiService().getAlerts(userId, userRole).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    allAlertsList = response.body();
                    updateStatistics();
                    applyFilters();
                } else {
                    // Show empty state, not fallback dummy data
                    allAlertsList.clear();
                    updateStatistics();
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                if (!isAdded()) return;
                allAlertsList.clear();
                updateStatistics();
                applyFilters();
                Toast.makeText(getContext(), "Could not connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE SELECTED ALERTS
    // ─────────────────────────────────────────────────────────────────────────
    private void deleteSelectedAlerts(List<Alert> selected) {
        ApiClient.authToken = prefs.getString("auth_token", null);

        final int[] remaining = {selected.size()};

        for (Alert alert : selected) {
            ApiClient.getApiService().deleteAlert(alert.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    remaining[0]--;
                    allAlertsList.remove(alert);
                    if (remaining[0] == 0) {
                        if (!isAdded()) return;
                        adapter.clearAll();
                        cbSelectAllAlerts.setChecked(false);
                        btnDeleteSelectedAlerts.setEnabled(false);
                        btnDeleteSelectedAlerts.setAlpha(0.5f);
                        btnDeleteSelectedAlerts.setText("Delete Selected");
                        updateStatistics();
                        applyFilters();
                        Toast.makeText(getContext(), "Alerts deleted successfully", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    remaining[0]--;
                    // Remove locally even if API fails
                    allAlertsList.remove(alert);
                    if (remaining[0] == 0) {
                        if (!isAdded()) return;
                        adapter.clearAll();
                        updateStatistics();
                        applyFilters();
                        Toast.makeText(getContext(), "Deleted locally (offline)", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATISTICS
    // ─────────────────────────────────────────────────────────────────────────
    private void updateStatistics() {
        int active = 0, warning = 0, resolved = 0;
        for (Alert alert : allAlertsList) {
            if ("ACTIVE".equalsIgnoreCase(alert.getStatus())) active++;
            else if ("RESOLVED".equalsIgnoreCase(alert.getStatus())) resolved++;
            if ("WARNING".equalsIgnoreCase(alert.getSeverity())) warning++;
        }
        if (tvStatActiveCount   != null) tvStatActiveCount.setText(String.valueOf(active));
        if (tvStatWarningCount  != null) tvStatWarningCount.setText(String.valueOf(warning));
        if (tvStatResolvedCount != null) tvStatResolvedCount.setText(String.valueOf(resolved));
        if (tvStatTotalCount    != null) tvStatTotalCount.setText(String.valueOf(allAlertsList.size()));

        if (getActivity() instanceof DashboardActivity)
            ((DashboardActivity) getActivity()).updateAlertBadgeCount(active);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTERS
    // ─────────────────────────────────────────────────────────────────────────
    private void applyFilters() {
        String severityFilter = spinnerSeverity.getSelectedItem() != null ? spinnerSeverity.getSelectedItem().toString() : "All Types";
        String statusFilter   = spinnerStatus.getSelectedItem()   != null ? spinnerStatus.getSelectedItem().toString()   : "All Status";
        String timeFilter     = spinnerTimeRange.getSelectedItem() != null ? spinnerTimeRange.getSelectedItem().toString() : "All Time";

        filteredAlertsList.clear();
        for (Alert alert : allAlertsList) {
            if (!severityFilter.equals("All Types")) {
                if (severityFilter.equalsIgnoreCase("Critical") && !"CRITICAL".equalsIgnoreCase(alert.getSeverity())) continue;
                if (severityFilter.equalsIgnoreCase("Warning")  && !"WARNING".equalsIgnoreCase(alert.getSeverity()))  continue;
                if (severityFilter.equalsIgnoreCase("Info")     && !"INFO".equalsIgnoreCase(alert.getSeverity()))     continue;
            }
            if (!statusFilter.equals("All Status")) {
                if (statusFilter.equalsIgnoreCase("Active")   && !"ACTIVE".equalsIgnoreCase(alert.getStatus()))   continue;
                if (statusFilter.equalsIgnoreCase("Resolved") && !"RESOLVED".equalsIgnoreCase(alert.getStatus())) continue;
            }
            if (!timeFilter.equals("All Time")) {
                if (!matchesTimeFilter(alert.getCreatedAt(), timeFilter)) continue;
            }
            filteredAlertsList.add(alert);
        }

        adapter.updateList(filteredAlertsList);

        boolean empty = filteredAlertsList.isEmpty();
        rvAlerts.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyAlerts.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private boolean matchesTimeFilter(String dateStr, String filter) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            Date alertDate = null;
            for (String fmt : new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"}) {
                try {
                    alertDate = new SimpleDateFormat(fmt, Locale.US).parse(dateStr);
                    if (alertDate != null) break;
                } catch (Exception ignored) {}
            }
            if (alertDate == null) return true;
            Calendar cal = Calendar.getInstance();
            if (filter.equals("Last 24 Hours")) { cal.add(Calendar.HOUR, -24); return alertDate.after(cal.getTime()); }
            if (filter.equals("Last 7 Days"))   { cal.add(Calendar.DAY_OF_YEAR, -7); return alertDate.after(cal.getTime()); }
            if (filter.equals("Last 30 Days"))  { cal.add(Calendar.DAY_OF_YEAR, -30); return alertDate.after(cal.getTime()); }
        } catch (Exception ignored) {}
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESOLVE (from adapter callback)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onResolve(Alert alert, int position) {
        String userEmail = prefs.getString("user_email", "user@company.com");
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().resolveAlert(alert.getId()).enqueue(new Callback<Alert>() {
            @Override
            public void onResponse(Call<Alert> call, Response<Alert> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Alert updated = response.body();
                    for (int i = 0; i < allAlertsList.size(); i++) {
                        if (allAlertsList.get(i).getId().equals(updated.getId())) {
                            allAlertsList.set(i, updated);
                            break;
                        }
                    }
                    Toast.makeText(getContext(), "Alert resolved!", Toast.LENGTH_SHORT).show();
                } else {
                    // Local fallback
                    alert.setStatus("RESOLVED");
                    alert.setResolvedBy(userEmail);
                    alert.setResolvedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
                }
                updateStatistics();
                applyFilters();
            }

            @Override
            public void onFailure(Call<Alert> call, Throwable t) {
                if (!isAdded()) return;
                alert.setStatus("RESOLVED");
                alert.setResolvedBy(userEmail);
                alert.setResolvedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
                Toast.makeText(getContext(), "Marked as resolved locally", Toast.LENGTH_SHORT).show();
                updateStatistics();
                applyFilters();
            }
        });
    }
}
