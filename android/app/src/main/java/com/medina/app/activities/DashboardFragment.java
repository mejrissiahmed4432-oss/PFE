package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Task;
import com.medina.app.model.Ticket;
import com.medina.app.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    // Technician profile
    private TextView tvDashAvatar, tvDashName, tvDashRole, tvDashEmail;

    // Summary counters (header card)
    private TextView tvDashTotalTickets, tvDashTotalTasks;

    // Ticket stats
    private TextView statOpen, statInProgress, statWaiting;

    // Task stats
    private TextView tvTaskTodo, tvTaskInProgress, tvTaskDone;

    // Important info
    private TextView tvInfoOverdue, tvOverdueCount;
    private TextView tvInfoHighPriority, tvHighPriorityCount;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        ApiClient.authToken = prefs.getString("auth_token", null);

        // Bind views
        tvDashAvatar        = view.findViewById(R.id.tvDashAvatar);
        tvDashName          = view.findViewById(R.id.tvDashName);
        tvDashRole          = view.findViewById(R.id.tvDashRole);
        tvDashEmail         = view.findViewById(R.id.tvDashEmail);

        tvDashTotalTickets  = view.findViewById(R.id.tvDashTotalTickets);
        tvDashTotalTasks    = view.findViewById(R.id.tvDashTotalTasks);

        statOpen            = view.findViewById(R.id.statOpen);
        statInProgress      = view.findViewById(R.id.statInProgress);
        statWaiting         = view.findViewById(R.id.statWaiting);

        tvTaskTodo          = view.findViewById(R.id.tvTaskTodo);
        tvTaskInProgress    = view.findViewById(R.id.tvTaskInProgress);
        tvTaskDone          = view.findViewById(R.id.tvTaskDone);

        tvInfoOverdue       = view.findViewById(R.id.tvInfoOverdue);
        tvOverdueCount      = view.findViewById(R.id.tvOverdueCount);
        tvInfoHighPriority  = view.findViewById(R.id.tvInfoHighPriority);
        tvHighPriorityCount = view.findViewById(R.id.tvHighPriorityCount);

        // Populate from prefs first for instant display
        populateUserFromPrefs();

        // Bind and set redirection listeners for the stats blocks
        view.findViewById(R.id.layoutDashTotalTickets).setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).selectMenuItem(5);
            }
        });
        view.findViewById(R.id.layoutDashTotalTasks).setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).selectMenuItem(1);
            }
        });
        // Then fetch fresh data
        loadCurrentUser();
        loadTickets();
        loadTasks();

        return view;
    }

    private void populateUserFromPrefs() {
        String name  = prefs.getString("user_name", "Technician");
        String email = prefs.getString("user_email", "—");
        String role  = prefs.getString("user_role", "TECHNICIAN");

        tvDashName.setText(name);
        tvDashEmail.setText(email);
        tvDashRole.setText(formatRole(role));

        // Avatar initials
        String[] parts = name.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) initials.append(p.charAt(0));
            if (initials.length() == 2) break;
        }
        tvDashAvatar.setText(initials.length() > 0 ? initials.toString().toUpperCase() : "TN");
    }

    private void loadCurrentUser() {
        ApiClient.getApiService().getCurrentUser().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null && getContext() != null) {
                    User u = response.body();
                    String fullName = (u.getFirstName() != null ? u.getFirstName() : "") + " "
                            + (u.getLastName() != null ? u.getLastName() : "");
                    fullName = fullName.trim();
                    if (fullName.isEmpty()) fullName = prefs.getString("user_name", "Technician");

                    tvDashName.setText(fullName);
                    if (u.getEmail() != null) tvDashEmail.setText(u.getEmail());
                    if (u.getRole() != null) tvDashRole.setText(formatRole(u.getRole()));

                    String[] ps = fullName.split(" ");
                    StringBuilder ini = new StringBuilder();
                    for (String p : ps) {
                        if (!p.isEmpty()) ini.append(p.charAt(0));
                        if (ini.length() == 2) break;
                    }
                    tvDashAvatar.setText(ini.length() > 0 ? ini.toString().toUpperCase() : "TN");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void loadTickets() {
        String userId = prefs.getString("user_id", null);
        if (userId == null) return;

        ApiClient.getApiService().getTicketsForTechnician(userId).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && getContext() != null) {
                    List<Ticket> tickets = response.body();
                    int open = 0, inProgress = 0, waiting = 0, overdue = 0;

                    for (Ticket t : tickets) {
                        String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                        switch (status) {
                            case "OPEN":        open++;       break;
                            case "IN_PROGRESS": inProgress++; break;
                            case "WAITING":
                            case "ON_HOLD":     waiting++;    break;
                        }
                        // Count overdue: open/in-progress tickets (simplified check)
                        if (!status.equals("RESOLVED") && !status.equals("CLOSED")) {
                            overdue++; // we'll refine label below
                        }
                    }

                    final int totalTickets = tickets.size();
                    final int overdueCount = open + waiting; // tickets that need immediate attention

                    tvDashTotalTickets.setText(String.valueOf(totalTickets));
                    statOpen.setText(String.valueOf(open));
                    statInProgress.setText(String.valueOf(inProgress));
                    statWaiting.setText(String.valueOf(waiting));

                    tvOverdueCount.setText(String.valueOf(overdueCount));
                    tvInfoOverdue.setText(overdueCount > 0
                            ? overdueCount + " ticket(s) need your attention"
                            : "All tickets are on track");
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                if (getContext() != null) {
                    tvInfoOverdue.setText("Could not load ticket data");
                }
            }
        });
    }

    private void loadTasks() {
        String userId = prefs.getString("user_id", null);
        if (userId == null) return;

        ApiClient.getApiService().getTasksByUser(userId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null && getContext() != null) {
                    List<Task> tasks = response.body();
                    int todo = 0, inProg = 0, done = 0, highPriority = 0;

                    for (Task t : tasks) {
                        String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                        String priority = t.getPriority() != null ? t.getPriority().toUpperCase() : "";

                        if (status.equals("TODO") || status.equals("PENDING")) todo++;
                        else if (status.equals("IN_PROGRESS")) inProg++;
                        else if (status.equals("DONE") || status.equals("COMPLETED")) done++;

                        if (priority.equals("HIGH") || priority.equals("URGENT") || priority.equals("CRITICAL")) {
                            highPriority++;
                        }
                    }

                    tvDashTotalTasks.setText(String.valueOf(tasks.size()));
                    tvTaskTodo.setText(String.valueOf(todo));
                    tvTaskInProgress.setText(String.valueOf(inProg));
                    tvTaskDone.setText(String.valueOf(done));

                    tvHighPriorityCount.setText(String.valueOf(highPriority));
                    tvInfoHighPriority.setText(highPriority > 0
                            ? highPriority + " high-priority task(s) pending"
                            : "No urgent tasks right now");
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                if (getContext() != null) {
                    tvInfoHighPriority.setText("Could not load task data");
                }
            }
        });
    }

    private String formatRole(String role) {
        if (role == null) return "Technician";
        switch (role.toUpperCase()) {
            case "TECHNICIAN":    return "IT Technician";
            case "STOCK_MANAGER": return "Stock Manager";
            case "HR":            return "HR Manager";
            case "IT_MANAGER":    return "IT Manager";
            case "EMPLOYEE":      return "Employee";
            default:              return role;
        }
    }
}
