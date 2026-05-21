package com.medina.app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.medina.app.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private SharedPreferences prefs;
    private String userId;
    private String userName;

    // UI elements
    private TextView tvStatTotalCount, tvStatPendingCount, tvStatProgressCount, tvStatCompletedCount;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvMonthLabel;
    private LinearLayout layoutCalendarGrid;
    private TextView btnFilterAll, btnFilterToday, btnFilterUpcoming, btnFilterCompleted;
    private RecyclerView rvTasks;
    private View layoutEmptyTasks;
    private View fabAddTask;

    private List<Task> allTasks = new ArrayList<>();
    private List<Task> displayedTasks = new ArrayList<>();
    private TasksAdapter adapter;

    // Calendar state
    private Calendar currentCalendar = Calendar.getInstance();
    private String selectedDateStr = ""; // YYYY-MM-DD format
    private String activeChipFilter = "All"; // All, Today, Upcoming, Completed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        userName = prefs.getString("user_name", "Technician");

        // Bind Views
        tvStatTotalCount = view.findViewById(R.id.tvStatTotalCount);
        tvStatPendingCount = view.findViewById(R.id.tvStatPendingCount);
        tvStatProgressCount = view.findViewById(R.id.tvStatProgressCount);
        tvStatCompletedCount = view.findViewById(R.id.tvStatCompletedCount);

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel);
        layoutCalendarGrid = view.findViewById(R.id.layoutCalendarGrid);

        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterToday = view.findViewById(R.id.btnFilterToday);
        btnFilterUpcoming = view.findViewById(R.id.btnFilterUpcoming);
        btnFilterCompleted = view.findViewById(R.id.btnFilterCompleted);

        rvTasks = view.findViewById(R.id.rvTasks);
        layoutEmptyTasks = view.findViewById(R.id.layoutEmptyTasks);
        fabAddTask = view.findViewById(R.id.fabAddTask);

        // RecyclerView setup
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TasksAdapter(displayedTasks, this);
        rvTasks.setAdapter(adapter);

        // Initialize state
        Calendar today = Calendar.getInstance();
        selectedDateStr = getFormattedDateKey(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        // Month listeners
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        // Filter chips listeners
        btnFilterAll.setOnClickListener(v -> changeChipFilter("All"));
        btnFilterToday.setOnClickListener(v -> changeChipFilter("Today"));
        btnFilterUpcoming.setOnClickListener(v -> changeChipFilter("Upcoming"));
        btnFilterCompleted.setOnClickListener(v -> changeChipFilter("Completed"));

        // Add Task FAB
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Load tasks and render calendar
        loadTasks();

        return view;
    }

    private void loadTasks() {
        if (userId == null || userId.isEmpty()) return;

        ApiClient.getApiService().getTasksByUser(userId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks = response.body();
                    updateStats();
                    renderCalendar();
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateStats() {
        int total = 0;
        int pending = 0;
        int progress = 0;
        int completed = 0;

        for (Task t : allTasks) {
            String status = t.getStatus() != null ? t.getStatus() : "Pending";
            if (!"History".equalsIgnoreCase(status)) {
                total++;
            }
            if ("Pending".equalsIgnoreCase(status)) {
                pending++;
            } else if ("In Progress".equalsIgnoreCase(status)) {
                progress++;
            } else if ("Completed".equalsIgnoreCase(status)) {
                completed++;
            }
        }

        tvStatTotalCount.setText(String.valueOf(total));
        tvStatPendingCount.setText(String.valueOf(pending));
        tvStatProgressCount.setText(String.valueOf(progress));
        tvStatCompletedCount.setText(String.valueOf(completed));
    }

    private void renderCalendar() {
        layoutCalendarGrid.removeAllViews();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthLabel.setText(monthFormat.format(currentCalendar.getTime()));

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0-indexed (0=Sun)
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = firstDayOfWeek + daysInMonth;
        int numRows = (int) Math.ceil((double) totalCells / 7);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        Calendar todayCal = Calendar.getInstance();
        String todayKey = getFormattedDateKey(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));

        int dayCounter = 1;
        for (int r = 0; r < numRows; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(rowParams);

            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

                // Build a custom layout dynamically for calendar cell
                LinearLayout cellContainer = new LinearLayout(getContext());
                cellContainer.setOrientation(LinearLayout.VERTICAL);
                cellContainer.setGravity(Gravity.CENTER);
                cellContainer.setPadding(0, 10, 0, 10);
                cellContainer.setLayoutParams(cellParams);

                TextView tvDayNumber = new TextView(getContext());
                tvDayNumber.setTextSize(13f);
                tvDayNumber.setGravity(Gravity.CENTER);

                View viewDot = new View(getContext());
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(12, 12);
                dotParams.topMargin = 6;
                viewDot.setLayoutParams(dotParams);
                viewDot.setBackgroundResource(R.drawable.bg_badge_dot);
                viewDot.setVisibility(View.INVISIBLE);

                cellContainer.addView(tvDayNumber);
                cellContainer.addView(viewDot);

                if (cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                    final int day = dayCounter;
                    final String cellDateStr = getFormattedDateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day);

                    tvDayNumber.setText(String.valueOf(day));

                    // Highlight selected day or today
                    if (cellDateStr.equals(selectedDateStr)) {
                        tvDayNumber.setTextColor(Color.WHITE);
                        cellContainer.setBackgroundResource(R.drawable.bg_button_gradient);
                    } else if (cellDateStr.equals(todayKey)) {
                        tvDayNumber.setTextColor(Color.parseColor("#3b82f6"));
                        tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tvDayNumber.setTextColor(Color.parseColor(isNightMode() ? "#f8fafc" : "#0f172a"));
                    }

                    // Task indicator dots
                    boolean hasTasks = false;
                    for (Task t : allTasks) {
                        if (cellDateStr.equals(t.getDueDate())) {
                            hasTasks = true;
                            break;
                        }
                    }
                    if (hasTasks) {
                        viewDot.setVisibility(View.VISIBLE);
                    }

                    cellContainer.setOnClickListener(v -> {
                        selectedDateStr = cellDateStr;
                        renderCalendar();
                        applyFilters();
                    });

                    dayCounter++;
                } else {
                    tvDayNumber.setText("");
                }

                row.addView(cellContainer);
            }
            layoutCalendarGrid.addView(row);
        }
    }

    private int spToPx(float sp, Context context) {
        return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity);
    }

    private float sp() {
        return 13.0f;
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void changeChipFilter(String filter) {
        activeChipFilter = filter;

        // Reset backgrounds
        resetChipStyle(btnFilterAll);
        resetChipStyle(btnFilterToday);
        resetChipStyle(btnFilterUpcoming);
        resetChipStyle(btnFilterCompleted);

        // Highlight selected
        if ("All".equals(filter)) {
            setActiveChipStyle(btnFilterAll);
        } else if ("Today".equals(filter)) {
            setActiveChipStyle(btnFilterToday);
        } else if ("Upcoming".equals(filter)) {
            setActiveChipStyle(btnFilterUpcoming);
        } else if ("Completed".equals(filter)) {
            setActiveChipStyle(btnFilterCompleted);
        }

        applyFilters();
    }

    private void resetChipStyle(TextView tv) {
        tv.setTextColor(Color.parseColor(isNightMode() ? "#94a3b8" : "#475569"));
        tv.setBackgroundResource(R.drawable.bg_card);
    }

    private void setActiveChipStyle(TextView tv) {
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundResource(R.drawable.bg_button_gradient);
    }

    private void applyFilters() {
        displayedTasks.clear();

        Calendar todayCal = Calendar.getInstance();
        String todayStr = getFormattedDateKey(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));

        for (Task t : allTasks) {
            String dueDate = t.getDueDate();
            String status = t.getStatus() != null ? t.getStatus() : "Pending";

            // Date selection filter overrides chips if selected date does not match the general chip query
            boolean dateMatch = true;
            if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
                dateMatch = selectedDateStr.equals(dueDate);
            }

            boolean filterMatch = true;
            switch (activeChipFilter) {
                case "Today":
                    filterMatch = todayStr.equals(dueDate);
                    break;
                case "Upcoming":
                    filterMatch = dueDate != null && dueDate.compareTo(todayStr) > 0 && !"Completed".equalsIgnoreCase(status) && !"History".equalsIgnoreCase(status);
                    break;
                case "Completed":
                    filterMatch = "Completed".equalsIgnoreCase(status) || "History".equalsIgnoreCase(status);
                    break;
                case "All":
                default:
                    filterMatch = true;
                    break;
            }

            if (dateMatch && filterMatch) {
                displayedTasks.add(t);
            }
        }

        adapter.updateList(displayedTasks);

        if (displayedTasks.isEmpty()) {
            layoutEmptyTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            layoutEmptyTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskClick(Task task) {
        showTaskDetailsDialog(task);
    }

    @Override
    public void onTaskEditClick(Task task) {
        showTaskDetailsDialog(task);
    }

    private void showTaskDetailsDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task_detail, null);
        builder.setView(v);

        AlertDialog dialog = builder.show();

        TextView tvTitle = v.findViewById(R.id.tvDetailTitle);
        TextView tvPriority = v.findViewById(R.id.tvDetailPriority);
        TextView tvCategory = v.findViewById(R.id.tvDetailCategory);
        TextView tvDueDate = v.findViewById(R.id.tvDetailDueDate);
        TextView tvDesc = v.findViewById(R.id.tvDetailDesc);
        Spinner spStatus = v.findViewById(R.id.spDetailStatus);
        ImageButton btnDelete = v.findViewById(R.id.btnDeleteTask);
        Button btnClose = v.findViewById(R.id.btnDetailClose);
        Button btnSave = v.findViewById(R.id.btnDetailSave);

        tvTitle.setText(task.getTitle());
        tvDesc.setText(task.getDescription() != null ? task.getDescription() : "No details provided.");
        tvCategory.setText(task.getType() != null ? task.getType() : "General");
        tvDueDate.setText("📅 " + (task.getDueDate() != null ? task.getDueDate() : "No Date"));

        String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
        tvPriority.setText(priority);
        if ("HIGH".equals(priority)) {
            tvPriority.setTextColor(Color.parseColor("#ef4444"));
            tvPriority.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else if ("MEDIUM".equals(priority)) {
            tvPriority.setTextColor(Color.parseColor("#f59e0b"));
            tvPriority.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else {
            tvPriority.setTextColor(Color.parseColor("#10b981"));
            tvPriority.setBackgroundResource(R.drawable.bg_badge_available);
        }

        // Set status spinner options
        String[] statusOptions = {"Pending", "In Progress", "Completed", "History"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);

        // Pre-select current status
        String curStatus = task.getStatus() != null ? task.getStatus() : "Pending";
        int selIndex = 0;
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equalsIgnoreCase(curStatus)) {
                selIndex = i;
                break;
            }
        }
        spStatus.setSelection(selIndex);

        // Listeners
        btnClose.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(view -> {
            String selectedStatus = spStatus.getSelectedItem().toString();
            task.setStatus(selectedStatus);

            ApiClient.getApiService().updateTask(task.getId(), task).enqueue(new Callback<Task>() {
                @Override
                public void onResponse(Call<Task> call, Response<Task> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Status updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadTasks();
                    }
                }

                @Override
                public void onFailure(Call<Task> call, Throwable t) {
                    Toast.makeText(getContext(), "Failed to save updates", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnDelete.setOnClickListener(view -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task permanently?")
                    .setPositiveButton("Delete", (dialogInterface, which) -> {
                        ApiClient.getApiService().deleteTask(task.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Toast.makeText(getContext(), "Task deleted successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadTasks();
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(getContext(), "Failed to delete task", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
        builder.setView(v);

        AlertDialog dialog = builder.show();

        EditText etTitle = v.findViewById(R.id.etTaskTitle);
        EditText etDesc = v.findViewById(R.id.etTaskDesc);
        Spinner spType = v.findViewById(R.id.spTaskType);
        Spinner spPriority = v.findViewById(R.id.spTaskPriority);
        TextView tvDueDateSelect = v.findViewById(R.id.tvTaskDueDateSelect);
        Button btnCancel = v.findViewById(R.id.btnCancelTask);
        Button btnSave = v.findViewById(R.id.btnSaveTask);

        // Spinners binding
        String[] types = {"General", "Equipment", "Maintenance", "Stock"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);

        // Pre-fill selected calendar date
        final Calendar dueCal = Calendar.getInstance();
        if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
            try {
                String[] p = selectedDateStr.split("-");
                dueCal.set(Calendar.YEAR, Integer.parseInt(p[0]));
                dueCal.set(Calendar.MONTH, Integer.parseInt(p[1]) - 1);
                dueCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(p[2]));
                tvDueDateSelect.setText(selectedDateStr);
                tvDueDateSelect.setTextColor(Color.parseColor(isNightMode() ? "#f8fafc" : "#0f172a"));
            } catch (Exception ignored) {}
        }

        // Date selection click listener
        tvDueDateSelect.setOnClickListener(view -> {
            DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String dateKey = getFormattedDateKey(year, month, dayOfMonth);
                        
                        // Validate: target date cannot be in the past
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);

                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);

                        if (selected.before(today)) {
                            Toast.makeText(getContext(), "Due date cannot be set in the past", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dueCal.set(year, month, dayOfMonth);
                        tvDueDateSelect.setText(dateKey);
                        tvDueDateSelect.setTextColor(Color.parseColor(isNightMode() ? "#f8fafc" : "#0f172a"));
                    },
                    dueCal.get(Calendar.YEAR),
                    dueCal.get(Calendar.MONTH),
                    dueCal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(view -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String type = spType.getSelectedItem().toString();
            String priority = spPriority.getSelectedItem().toString();
            String dueDateVal = tvDueDateSelect.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Task Title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("Select target date".equals(dueDateVal) || dueDateVal.isEmpty()) {
                Toast.makeText(getContext(), "Please select a target due date", Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task();
            task.setTitle(title);
            task.setDescription(desc);
            task.setType(type);
            task.setPriority(priority);
            task.setDueDate(dueDateVal);
            task.setStatus("Pending");
            task.setUserId(userId);
            task.setAssignedTo(userName);

            ApiClient.getApiService().createTask(task).enqueue(new Callback<Task>() {
                @Override
                public void onResponse(Call<Task> call, Response<Task> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Task scheduled successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadTasks();
                    }
                }

                @Override
                public void onFailure(Call<Task> call, Throwable t) {
                    Toast.makeText(getContext(), "Failed to create task", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getFormattedDateKey(int year, int month, int day) {
        return String.format(Locale.US, "%d-%02d-%02d", year, month + 1, day);
    }
}
