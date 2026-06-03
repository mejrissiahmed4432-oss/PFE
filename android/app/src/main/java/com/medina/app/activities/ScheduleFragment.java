package com.medina.app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.DragEvent;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Task;
import com.medina.app.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleFragment extends Fragment {

    private SharedPreferences prefs;
    private String userId;
    private String userName;

    // ── Stats ──
    private TextView tvStatTotalCount, tvStatPendingCount, tvStatProgressCount, tvStatCompletedCount;

    // ── Calendar ──
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvMonthLabel;
    private LinearLayout layoutCalendarGrid;

    // ── Tasks panel ──
    private View layoutNoSelection;
    private NestedScrollView layoutKanban;  // Kanban scrollable container
    private TextView tvTasksCountBadge;
    private NestedScrollView scrollParent; // Root scroll for drag interception

    // Kanban outer columns (for drag event receivers)
    private LinearLayout colTodo, colProgress, colDone;

    // Kanban columns
    private LinearLayout layoutTodoTasks, layoutProgressTasks, layoutDoneTasks;
    private TextView tvTodoCount, tvProgressCount, tvDoneCount;
    private TextView tvTodoEmpty, tvProgressEmpty, tvDoneEmpty;

    // ── History panel ──
    private NestedScrollView scrollHistory;  // History scrollable container
    private LinearLayout layoutHistoryTasks;
    private View layoutHistoryEmpty;
    private TextView tvHistoryCount;

    // ── Data ──
    private List<Task> allTasks = new ArrayList<>();
    private View.OnDragListener kanbanDragListener;

    // Calendar state
    private Calendar currentCalendar = Calendar.getInstance();
    private String selectedDateStr = null; // null = no selection

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        prefs    = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        userId   = prefs.getString("user_id", "");
        userName = prefs.getString("user_name", "Technician");

        Calendar todayCal = Calendar.getInstance();
        selectedDateStr = dateKey(todayCal.get(Calendar.YEAR),
                                  todayCal.get(Calendar.MONTH),
                                  todayCal.get(Calendar.DAY_OF_MONTH));

        // Stats
        tvStatTotalCount     = view.findViewById(R.id.tvStatTotalCount);
        tvStatPendingCount   = view.findViewById(R.id.tvStatPendingCount);
        tvStatProgressCount  = view.findViewById(R.id.tvStatProgressCount);
        tvStatCompletedCount = view.findViewById(R.id.tvStatCompletedCount);

        // Calendar
        btnPrevMonth      = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth      = view.findViewById(R.id.btnNextMonth);
        tvMonthLabel      = view.findViewById(R.id.tvMonthLabel);
        layoutCalendarGrid = view.findViewById(R.id.layoutCalendarGrid);

        // Tasks panel
        scrollParent       = view.findViewById(R.id.scrollParent);
        layoutNoSelection  = view.findViewById(R.id.layoutNoSelection);
        layoutKanban       = view.findViewById(R.id.layoutKanban);
        tvTasksCountBadge  = view.findViewById(R.id.tvTasksCountBadge);

        // Outer column containers (larger drag targets)
        colTodo     = view.findViewById(R.id.colTodo);
        colProgress = view.findViewById(R.id.colProgress);
        colDone     = view.findViewById(R.id.colDone);

        layoutTodoTasks     = view.findViewById(R.id.layoutTodoTasks);
        layoutProgressTasks = view.findViewById(R.id.layoutProgressTasks);
        layoutDoneTasks     = view.findViewById(R.id.layoutDoneTasks);

        tvTodoCount     = view.findViewById(R.id.tvTodoCount);
        tvProgressCount = view.findViewById(R.id.tvProgressCount);
        tvDoneCount     = view.findViewById(R.id.tvDoneCount);

        tvTodoEmpty     = view.findViewById(R.id.tvTodoEmpty);
        tvProgressEmpty = view.findViewById(R.id.tvProgressEmpty);
        tvDoneEmpty     = view.findViewById(R.id.tvDoneEmpty);

        // History panel
        scrollHistory      = view.findViewById(R.id.scrollHistory);
        layoutHistoryTasks = view.findViewById(R.id.layoutHistoryTasks);
        layoutHistoryEmpty = view.findViewById(R.id.layoutHistoryEmpty);
        tvHistoryCount     = view.findViewById(R.id.tvHistoryCount);

        // Month navigation
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        // Add Task buttons (header button)
        Button btnAddTask = view.findViewById(R.id.btnAddTask);
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Drag and drop listeners for Kanban outer column containers
        kanbanDragListener = (v, event) -> {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Prevent parent scroll and kanban scroll from intercepting the drag touch
                    if (scrollParent != null) {
                        scrollParent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (layoutKanban != null) {
                        layoutKanban.requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.7f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);
                    // Re-enable parent scrolls after drag ends
                    if (scrollParent != null) {
                        scrollParent.requestDisallowInterceptTouchEvent(false);
                    }
                    if (layoutKanban != null) {
                        layoutKanban.requestDisallowInterceptTouchEvent(false);
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f);
                    Task droppedTask = (Task) event.getLocalState();
                    if (droppedTask != null) {
                        String newStatus = "Pending";
                        int viewId = v.getId();
                        if (viewId == R.id.colTodo || viewId == R.id.layoutTodoTasks || viewId == R.id.tvTodoEmpty) {
                            newStatus = "Pending";
                        } else if (viewId == R.id.colProgress || viewId == R.id.layoutProgressTasks || viewId == R.id.tvProgressEmpty) {
                            newStatus = "In Progress";
                        } else if (viewId == R.id.colDone || viewId == R.id.layoutDoneTasks || viewId == R.id.tvDoneEmpty) {
                            newStatus = "Completed";
                        }

                        if (!newStatus.equalsIgnoreCase(droppedTask.getStatus())) {
                            updateTaskStatusOnDrop(droppedTask, newStatus);
                        }
                    }
                    // Re-enable parent scrolls after drop
                    if (scrollParent != null) {
                        scrollParent.requestDisallowInterceptTouchEvent(false);
                    }
                    if (layoutKanban != null) {
                        layoutKanban.requestDisallowInterceptTouchEvent(false);
                    }
                    return true;
            }
            return false;
        };

        // Attach drag listeners to the outer column containers (whole column area, not just task list)
        colTodo.setOnDragListener(kanbanDragListener);
        colProgress.setOnDragListener(kanbanDragListener);
        colDone.setOnDragListener(kanbanDragListener);

        // Also attach to inner task lists as fallback for precise drops
        layoutTodoTasks.setOnDragListener(kanbanDragListener);
        layoutProgressTasks.setOnDragListener(kanbanDragListener);
        layoutDoneTasks.setOnDragListener(kanbanDragListener);

        // And attach to the empty text views
        tvTodoEmpty.setOnDragListener(kanbanDragListener);
        tvProgressEmpty.setOnDragListener(kanbanDragListener);
        tvDoneEmpty.setOnDragListener(kanbanDragListener);

        loadTasks();
        return view;
    }

    // ─────────────────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────────────────

    private void loadTasks() {
        if (userId == null || userId.isEmpty()) {
            userId = prefs.getString("user_id", "");
        }
        if (userId == null || userId.isEmpty()) {
            ApiClient.getApiService().getCurrentUser().enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        userId = response.body().getId();
                        prefs.edit().putString("user_id", userId).apply();
                        loadTasks();
                    }
                }
                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        ApiClient.getApiService().getTasksByUser(userId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks = response.body();
                    updateStats();
                    renderCalendar();
                    renderHistory();
                    if (selectedDateStr != null) renderKanban();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────────────────

    private void updateStats() {
        int total = 0, pending = 0, progress = 0, completed = 0;
        for (Task t : allTasks) {
            String s = t.getStatus() != null ? t.getStatus() : "Pending";
            if (!"History".equalsIgnoreCase(s)) total++;
            if ("Pending".equalsIgnoreCase(s))           pending++;
            else if ("In Progress".equalsIgnoreCase(s))  progress++;
            else if ("Completed".equalsIgnoreCase(s))    completed++;
        }
        tvStatTotalCount.setText(String.valueOf(total));
        tvStatPendingCount.setText(String.valueOf(pending));
        tvStatProgressCount.setText(String.valueOf(progress));
        tvStatCompletedCount.setText(String.valueOf(completed));
    }

    // ─────────────────────────────────────────────────────────
    // CALENDAR
    // ─────────────────────────────────────────────────────────

    private void renderCalendar() {
        layoutCalendarGrid.removeAllViews();

        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthLabel.setText(fmt.format(currentCalendar.getTime()));

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow    = cal.get(Calendar.DAY_OF_WEEK) - 1;   // 0=Sun
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int numRows     = (int) Math.ceil((firstDow + daysInMonth) / 7.0);

        Calendar todayCal = Calendar.getInstance();
        String todayKey = dateKey(todayCal.get(Calendar.YEAR),
                                  todayCal.get(Calendar.MONTH),
                                  todayCal.get(Calendar.DAY_OF_MONTH));

        int dayCounter = 1;
        for (int r = 0; r < numRows; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                LinearLayout.LayoutParams cellParams =
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

                LinearLayout cell = new LinearLayout(getContext());
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(0, 10, 0, 10);
                cell.setLayoutParams(cellParams);

                TextView tvDay = new TextView(getContext());
                tvDay.setTextSize(13f);
                tvDay.setGravity(Gravity.CENTER);

                // Dot indicator
                View dot = new View(getContext());
                LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(8, 8);
                dotP.topMargin = 4;
                dot.setLayoutParams(dotP);
                dot.setVisibility(View.INVISIBLE);

                cell.addView(tvDay);
                cell.addView(dot);

                if (cellIndex >= firstDow && dayCounter <= daysInMonth) {
                    final int day = dayCounter;
                    final String cellDate = dateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day);

                    tvDay.setText(String.valueOf(day));

                    // Colour the dot by tasks present
                    boolean hasTodo = false, hasProgress = false, hasDone = false;
                    for (Task t : allTasks) {
                        String s = t.getStatus() != null ? t.getStatus() : "Pending";
                        if (cellDate.equals(t.getDueDate())) {
                            if ("In Progress".equalsIgnoreCase(s))         hasProgress = true;
                            else if ("Completed".equalsIgnoreCase(s))      hasDone     = true;
                            else if (!"History".equalsIgnoreCase(s))       hasTodo     = true;
                        }
                    }
                    if (hasProgress) {
                        dot.setBackgroundResource(R.drawable.bg_dot_progress);
                        dot.setVisibility(View.VISIBLE);
                    } else if (hasDone) {
                        dot.setBackgroundResource(R.drawable.bg_dot_done);
                        dot.setVisibility(View.VISIBLE);
                    } else if (hasTodo) {
                        dot.setBackgroundResource(R.drawable.bg_dot_todo);
                        dot.setVisibility(View.VISIBLE);
                    }

                    // Highlight selected / today
                    if (cellDate.equals(selectedDateStr)) {
                        tvDay.setTextColor(Color.WHITE);
                        cell.setBackgroundResource(R.drawable.bg_button_gradient);
                    } else if (cellDate.equals(todayKey)) {
                        tvDay.setTextColor(Color.parseColor("#3b82f6"));
                        tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tvDay.setTextColor(isNightMode()
                                ? Color.parseColor("#f8fafc")
                                : Color.parseColor("#0f172a"));
                    }

                    cell.setOnClickListener(v -> {
                        selectedDateStr = cellDate;
                        renderCalendar();
                        renderKanban();
                    });

                    dayCounter++;
                } else {
                    tvDay.setText("");
                }
                row.addView(cell);
            }
            layoutCalendarGrid.addView(row);
        }
    }

    // ─────────────────────────────────────────────────────────
    // KANBAN BOARD
    // ─────────────────────────────────────────────────────────

    private void renderKanban() {
        if (selectedDateStr == null) {
            layoutNoSelection.setVisibility(View.VISIBLE);
            layoutKanban.setVisibility(View.GONE);
            tvTasksCountBadge.setVisibility(View.GONE);
            return;
        }

        layoutNoSelection.setVisibility(View.GONE);
        layoutKanban.setVisibility(View.VISIBLE);
        tvTasksCountBadge.setVisibility(View.VISIBLE);
        // Drag listeners are persistently attached in onCreateView

        List<Task> todoList     = new ArrayList<>();
        List<Task> progressList = new ArrayList<>();
        List<Task> doneList     = new ArrayList<>();

        for (Task t : allTasks) {
            if (!selectedDateStr.equals(t.getDueDate())) continue;
            String s = t.getStatus() != null ? t.getStatus() : "Pending";
            if ("In Progress".equalsIgnoreCase(s))       progressList.add(t);
            else if ("Completed".equalsIgnoreCase(s))    doneList.add(t);
            else if (!"History".equalsIgnoreCase(s))     todoList.add(t);
        }

        int total = todoList.size() + progressList.size() + doneList.size();
        tvTasksCountBadge.setText(String.valueOf(total));

        tvTodoCount.setText(String.valueOf(todoList.size()));
        tvProgressCount.setText(String.valueOf(progressList.size()));
        tvDoneCount.setText(String.valueOf(doneList.size()));

        populateKanbanColumn(layoutTodoTasks,     tvTodoEmpty,     todoList,     "#3b82f6");
        populateKanbanColumn(layoutProgressTasks, tvProgressEmpty, progressList, "#f59e0b");
        populateKanbanColumn(layoutDoneTasks,     tvDoneEmpty,     doneList,     "#10b981");
    }

    private void populateKanbanColumn(LinearLayout container, TextView emptyView,
                                      List<Task> tasks, String accentHex) {
        container.removeAllViews();

        if (tasks.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);

        LayoutInflater inf = LayoutInflater.from(getContext());
        for (Task task : tasks) {
            View card = inf.inflate(R.layout.item_task, container, false);

            // Accent bar colour
            View accent = card.findViewById(R.id.viewStatusAccent);
            accent.setBackgroundColor(Color.parseColor(accentHex));

            TextView tvTitle    = card.findViewById(R.id.tvTaskTitle);
            TextView tvDesc     = card.findViewById(R.id.tvTaskDesc);
            TextView tvCategory = card.findViewById(R.id.tvTaskCategory);
            TextView tvDueDate  = card.findViewById(R.id.tvTaskDueDate);
            TextView tvPriority = card.findViewById(R.id.tvTaskPriority);
            ImageButton btnEdit = card.findViewById(R.id.btnTaskOptions);

            tvTitle.setText(task.getTitle());
            tvDesc.setText(task.getDescription() != null ? task.getDescription() : "");
            tvCategory.setText(task.getType() != null ? task.getType() : "General");
            tvDueDate.setText("📅 " + (task.getDueDate() != null ? task.getDueDate() : ""));

            String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
            tvPriority.setText(priority);
            applyPriorityBadge(tvPriority, priority);

            card.setOnClickListener(v -> showTaskDetailsDialog(task));
            btnEdit.setOnClickListener(v -> showTaskDetailsDialog(task));

            // Enable drag and drop on long click
            card.setOnLongClickListener(v -> {
                android.content.ClipData data = android.content.ClipData.newPlainText("taskId", task.getId());
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(card);
                v.startDragAndDrop(data, shadowBuilder, task, 0);
                return true;
            });

            container.addView(card);
        }
    }

    // ─────────────────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────────────────

    private void renderHistory() {
        layoutHistoryTasks.removeAllViews();

        List<Task> history = new ArrayList<>();
        for (Task t : allTasks) {
            if ("History".equalsIgnoreCase(t.getStatus())) history.add(t);
        }

        tvHistoryCount.setText(String.valueOf(history.size()));

        if (history.isEmpty()) {
            layoutHistoryEmpty.setVisibility(View.VISIBLE);
            scrollHistory.setVisibility(View.GONE);
            return;
        }

        layoutHistoryEmpty.setVisibility(View.GONE);
        scrollHistory.setVisibility(View.VISIBLE);

        LayoutInflater inf = LayoutInflater.from(getContext());
        for (Task task : history) {
            View card = inf.inflate(R.layout.item_task, layoutHistoryTasks, false);

            View accent = card.findViewById(R.id.viewStatusAccent);
            accent.setBackgroundColor(Color.parseColor("#94a3b8"));

            TextView tvTitle    = card.findViewById(R.id.tvTaskTitle);
            TextView tvDesc     = card.findViewById(R.id.tvTaskDesc);
            TextView tvCategory = card.findViewById(R.id.tvTaskCategory);
            TextView tvDueDate  = card.findViewById(R.id.tvTaskDueDate);
            TextView tvPriority = card.findViewById(R.id.tvTaskPriority);
            ImageButton btnEdit = card.findViewById(R.id.btnTaskOptions);

            tvTitle.setText(task.getTitle());
            tvDesc.setText(task.getDescription() != null ? task.getDescription() : "");
            tvCategory.setText("Archived");
            tvDueDate.setText("🕐 Done " + (task.getDueDate() != null ? task.getDueDate() : ""));

            tvPriority.setText("ARCHIVED");
            tvPriority.setTextColor(Color.parseColor("#64748b"));
            tvPriority.setBackgroundResource(R.drawable.bg_card);

            card.setOnClickListener(v -> showTaskDetailsDialog(task));
            btnEdit.setOnClickListener(v -> showTaskDetailsDialog(task));

            layoutHistoryTasks.addView(card);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DIALOGS
    // ─────────────────────────────────────────────────────────

    private void showTaskDetailsDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task_detail, null);
        builder.setView(v);
        AlertDialog dialog = builder.show();

        TextView tvHeader = v.findViewById(R.id.tvDetailHeader);
        EditText etTitle = v.findViewById(R.id.etDetailTitle);
        EditText etDesc = v.findViewById(R.id.etDetailDesc);
        Spinner spType = v.findViewById(R.id.spDetailType);
        Spinner spPriority = v.findViewById(R.id.spDetailPriority);
        TextView tvDueDateSelect = v.findViewById(R.id.tvDetailDueDateSelect);
        Spinner spStatus = v.findViewById(R.id.spDetailStatus);
        ImageButton btnDelete = v.findViewById(R.id.btnDeleteTask);
        Button btnClose = v.findViewById(R.id.btnDetailClose);
        Button btnSave = v.findViewById(R.id.btnDetailSave);

        // Pre-fill values
        etTitle.setText(task.getTitle());
        etDesc.setText(task.getDescription() != null ? task.getDescription() : "");
        tvDueDateSelect.setText(task.getDueDate() != null ? task.getDueDate() : "");

        // Category Type Spinner setup
        String[] types = {"General", "Equipment", "Maintenance", "Stock"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        String curType = task.getType() != null ? task.getType() : "General";
        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(curType)) { spType.setSelection(i); break; }
        }

        // Priority Spinner setup
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);
        String curPrio = task.getPriority() != null ? task.getPriority() : "Medium";
        if (curPrio.length() > 0) {
            curPrio = curPrio.substring(0, 1).toUpperCase() + curPrio.substring(1).toLowerCase();
        }
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equalsIgnoreCase(curPrio)) { spPriority.setSelection(i); break; }
        }

        // Status Spinner setup
        String[] statusOptions = {"Pending", "In Progress", "Completed", "History"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);
        String curStatus = task.getStatus() != null ? task.getStatus() : "Pending";
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equalsIgnoreCase(curStatus)) { spStatus.setSelection(i); break; }
        }

        // Lock editing for completed/history tasks (matching web behavior)
        boolean isLocked = "Completed".equalsIgnoreCase(curStatus) || "History".equalsIgnoreCase(curStatus);
        if (isLocked) {
            tvHeader.setText("Task Details (Locked)");
            etTitle.setEnabled(false);
            etDesc.setEnabled(false);
            spType.setEnabled(false);
            spPriority.setEnabled(false);
            tvDueDateSelect.setEnabled(false);
            spStatus.setEnabled(false);
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.4f);
            btnDelete.setVisibility(View.GONE);
        }

        // Date picker setup
        final Calendar dueCal = Calendar.getInstance();
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            try {
                String[] parts = task.getDueDate().split("-");
                dueCal.set(Calendar.YEAR,  Integer.parseInt(parts[0]));
                dueCal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                dueCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {}
        }
        tvDueDateSelect.setOnClickListener(view -> {
            DatePickerDialog dp = new DatePickerDialog(getContext(),
                    (picker, year, month, day) -> {
                        // Reject past dates
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);
                        Calendar sel = Calendar.getInstance();
                        sel.set(year, month, day, 0, 0, 0);
                        sel.set(Calendar.MILLISECOND, 0);
                        if (sel.before(today)) {
                            Toast.makeText(getContext(),
                                    "Due date cannot be in the past", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dueCal.set(year, month, day);
                        String key = dateKey(year, month, day);
                        tvDueDateSelect.setText(key);
                    },
                    dueCal.get(Calendar.YEAR),
                    dueCal.get(Calendar.MONTH),
                    dueCal.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnClose.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(view -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String type = spType.getSelectedItem().toString();
            String priority = spPriority.getSelectedItem().toString();
            String dueDate = tvDueDateSelect.getText().toString();
            String selectedStatus = spStatus.getSelectedItem().toString();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Task title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dueDate.isEmpty() || "Select target date".equals(dueDate)) {
                Toast.makeText(getContext(), "Please select a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            task.setTitle(title);
            task.setDescription(desc);
            task.setType(type);
            task.setPriority(priority);
            task.setDueDate(dueDate);
            task.setStatus(selectedStatus);

            ApiClient.getApiService().updateTask(task.getId(), task)
                    .enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> call, Response<Task> resp) {
                            if (resp.isSuccessful()) {
                                Toast.makeText(getContext(), "Task updated successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadTasks();
                            }
                        }
                        @Override
                        public void onFailure(Call<Task> call, Throwable t) {
                            Toast.makeText(getContext(), "Failed to save task", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnDelete.setOnClickListener(view ->
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task permanently?")
                    .setPositiveButton("Delete", (di, which) ->
                        ApiClient.getApiService().deleteTask(task.getId())
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> resp) {
                                        Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadTasks();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                                    }
                                })
                    )
                    .setNegativeButton("Cancel", null)
                    .show()
        );
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
        builder.setView(v);
        AlertDialog dialog = builder.show();

        EditText etTitle          = v.findViewById(R.id.etTaskTitle);
        EditText etDesc           = v.findViewById(R.id.etTaskDesc);
        Spinner  spType           = v.findViewById(R.id.spTaskType);
        Spinner  spPriority       = v.findViewById(R.id.spTaskPriority);
        TextView tvDueDateSelect  = v.findViewById(R.id.tvTaskDueDateSelect);
        Button   btnCancel        = v.findViewById(R.id.btnCancelTask);
        Button   btnSave          = v.findViewById(R.id.btnSaveTask);

        // Type spinner
        String[] types = {"General", "Equipment", "Maintenance", "Stock"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        // Priority spinner
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);
        spPriority.setSelection(1); // Default Medium

        // Pre-fill selected calendar date
        final Calendar dueCal = Calendar.getInstance();
        if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
            try {
                String[] parts = selectedDateStr.split("-");
                dueCal.set(Calendar.YEAR,  Integer.parseInt(parts[0]));
                dueCal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                dueCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                tvDueDateSelect.setText(selectedDateStr);
                tvDueDateSelect.setTextColor(isNightMode()
                        ? Color.parseColor("#f8fafc") : Color.parseColor("#0f172a"));
            } catch (Exception ignored) {}
        }

        // Date picker
        tvDueDateSelect.setOnClickListener(view -> {
            DatePickerDialog dp = new DatePickerDialog(getContext(),
                    (picker, year, month, day) -> {
                        // Reject past dates
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);
                        Calendar sel = Calendar.getInstance();
                        sel.set(year, month, day, 0, 0, 0);
                        sel.set(Calendar.MILLISECOND, 0);
                        if (sel.before(today)) {
                            Toast.makeText(getContext(),
                                    "Due date cannot be in the past", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dueCal.set(year, month, day);
                        String key = dateKey(year, month, day);
                        tvDueDateSelect.setText(key);
                        tvDueDateSelect.setTextColor(isNightMode()
                                ? Color.parseColor("#f8fafc") : Color.parseColor("#0f172a"));
                    },
                    dueCal.get(Calendar.YEAR),
                    dueCal.get(Calendar.MONTH),
                    dueCal.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(view -> {
            String title    = etTitle.getText().toString().trim();
            String desc     = etDesc.getText().toString().trim();
            String type     = spType.getSelectedItem().toString();
            String priority = spPriority.getSelectedItem().toString();
            String dueDate  = tvDueDateSelect.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Task title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("Select target date".equals(dueDate) || dueDate.isEmpty()) {
                Toast.makeText(getContext(), "Please select a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task();
            task.setTitle(title);
            task.setDescription(desc);
            task.setType(type);
            task.setPriority(priority);
            task.setDueDate(dueDate);
            task.setStatus("Pending");
            task.setUserId(userId);
            task.setAssignedTo(userName);

            ApiClient.getApiService().createTask(task).enqueue(new Callback<Task>() {
                @Override
                public void onResponse(Call<Task> call, Response<Task> resp) {
                    if (resp.isSuccessful()) {
                        Toast.makeText(getContext(), "Task created successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Auto-select the new task's date
                        selectedDateStr = dueDate;
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

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private void applyPriorityBadge(TextView tv, String priority) {
        if ("HIGH".equals(priority)) {
            tv.setTextColor(Color.parseColor("#ef4444"));
            tv.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else if ("MEDIUM".equals(priority)) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
            tv.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else {
            tv.setTextColor(Color.parseColor("#10b981"));
            tv.setBackgroundResource(R.drawable.bg_badge_available);
        }
    }

    private String dateKey(int year, int month, int day) {
        return String.format(Locale.US, "%d-%02d-%02d", year, month + 1, day);
    }

    private boolean isNightMode() {
        int uiMode = getResources().getConfiguration().uiMode;
        return (uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateTaskStatusOnDrop(Task task, String targetStatus) {
        final String oldStatus = task.getStatus();

        // Optimistic UI update
        task.setStatus(targetStatus);
        updateStats();
        renderCalendar();
        renderKanban();

        ApiClient.getApiService().updateTaskStatus(task.getId(), targetStatus).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Task moved to " + targetStatus, Toast.LENGTH_SHORT).show();
                    }
                    // Update task in local cache
                    for (int i = 0; i < allTasks.size(); i++) {
                        if (allTasks.get(i).getId().equals(task.getId())) {
                            allTasks.set(i, response.body());
                            break;
                        }
                    }
                    updateStats();
                    renderCalendar();
                    renderKanban();
                } else {
                    rollback();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                rollback();
            }

            private void rollback() {
                task.setStatus(oldStatus);
                updateStats();
                renderCalendar();
                renderKanban();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to move task", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
