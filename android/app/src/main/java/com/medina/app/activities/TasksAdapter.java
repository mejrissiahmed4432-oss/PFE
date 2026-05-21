package com.medina.app.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.Task;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskEditClick(Task task);
    }

    private List<Task> tasks;
    private OnTaskClickListener listener;

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void updateList(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskDesc.setText(task.getDescription());
        holder.tvTaskCategory.setText(task.getType() != null ? task.getType() : "General");
        
        String dueDateStr = task.getDueDate() != null ? task.getDueDate() : "No Date";
        holder.tvTaskDueDate.setText("📅 " + dueDateStr);

        // Status accent line colors
        String status = task.getStatus() != null ? task.getStatus() : "Pending";
        int accentColor;
        switch (status) {
            case "In Progress":
                accentColor = Color.parseColor("#f59e0b"); // Amber
                break;
            case "Completed":
                accentColor = Color.parseColor("#10b981"); // Mint
                break;
            case "History":
                accentColor = Color.parseColor("#64748b"); // Slate
                break;
            case "Pending":
            default:
                accentColor = Color.parseColor("#3b82f6"); // Blue
                break;
        }
        holder.viewStatusAccent.setBackgroundColor(accentColor);

        // Priority Badge styling
        String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
        holder.tvTaskPriority.setText(priority);
        if ("HIGH".equals(priority)) {
            holder.tvTaskPriority.setTextColor(Color.parseColor("#ef4444"));
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else if ("MEDIUM".equals(priority)) {
            holder.tvTaskPriority.setTextColor(Color.parseColor("#f59e0b"));
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else {
            holder.tvTaskPriority.setTextColor(Color.parseColor("#10b981"));
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_badge_available);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        holder.btnTaskOptions.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEditClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStatusAccent;
        TextView tvTaskTitle, tvTaskPriority, tvTaskDesc, tvTaskCategory, tvTaskDueDate;
        ImageButton btnTaskOptions;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusAccent = itemView.findViewById(R.id.viewStatusAccent);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvTaskDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvTaskCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvTaskDueDate = itemView.findViewById(R.id.tvTaskDueDate);
            btnTaskOptions = itemView.findViewById(R.id.btnTaskOptions);
        }
    }
}
