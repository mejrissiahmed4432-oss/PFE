package com.medina.app.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.Alert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    public interface OnAlertResolveListener {
        void onResolve(Alert alert, int position);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<Alert> alerts;
    private OnAlertResolveListener resolveListener;
    private OnSelectionChangedListener selectionChangedListener;
    private Set<String> selectedIds = new HashSet<>();
    private boolean selectionModeActive = false;

    public AlertsAdapter(List<Alert> alerts, OnAlertResolveListener resolveListener) {
        this.alerts = alerts;
        this.resolveListener = resolveListener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void updateList(List<Alert> newAlerts) {
        this.alerts = newAlerts;
        // Remove any selections for items no longer in the list
        Set<String> validIds = new HashSet<>();
        for (Alert a : newAlerts) if (a.getId() != null) validIds.add(a.getId());
        selectedIds.retainAll(validIds);
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean active) {
        this.selectionModeActive = active;
        if (!active) selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionModeActive() {
        return selectionModeActive;
    }

    public void selectAll() {
        selectedIds.clear();
        for (Alert a : alerts) if (a.getId() != null) selectedIds.add(a.getId());
        if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    public void clearAll() {
        selectedIds.clear();
        if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(0);
        notifyDataSetChanged();
    }

    public List<Alert> getSelectedAlerts() {
        List<Alert> selected = new ArrayList<>();
        for (Alert a : alerts) {
            if (a.getId() != null && selectedIds.contains(a.getId())) selected.add(a);
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public boolean isAllSelected() {
        return !alerts.isEmpty() && selectedIds.size() == alerts.size();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alerts.get(position);

        holder.tvAlertTitle.setText(alert.getTitle());
        holder.tvAlertMessage.setText(alert.getMessage());
        // Format date nicely
        String rawDate = alert.getCreatedAt();
        if (rawDate != null && rawDate.length() >= 10) {
            holder.tvAlertTime.setText(rawDate.substring(0, 10));
        } else {
            holder.tvAlertTime.setText(rawDate != null ? rawDate : "—");
        }

        // Severity badge
        String severity = alert.getSeverity() != null ? alert.getSeverity().toUpperCase() : "INFO";
        holder.tvAlertSeverityBadge.setText(severity);

        int severityColor;
        int severityBg;
        switch (severity) {
            case "CRITICAL":
                severityColor = Color.parseColor("#ef4444");
                severityBg = Color.parseColor("#fef2f2");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_alert);
                break;
            case "WARNING":
                severityColor = Color.parseColor("#f59e0b");
                severityBg = Color.parseColor("#fffbeb");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_alert);
                break;
            default: // INFO
                severityColor = Color.parseColor("#3b82f6");
                severityBg = Color.parseColor("#eff6ff");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_notification);
                break;
        }

        holder.viewSeverityBar.setBackgroundColor(severityColor);
        holder.ivAlertIcon.setColorFilter(severityColor);
        holder.tvAlertSeverityBadge.setTextColor(severityColor);
        try {
            holder.tvAlertSeverityBadge.getBackground().setTint(severityBg);
        } catch (Exception ignored) {}

        // Resolved state
        boolean isResolved = "RESOLVED".equalsIgnoreCase(alert.getStatus());
        if (isResolved) {
            holder.btnResolveAlert.setVisibility(View.GONE);
            holder.tvAlertResolvedDetails.setVisibility(View.VISIBLE);
            StringBuilder resolvedText = new StringBuilder("Resolved");
            if (alert.getResolvedBy() != null && !alert.getResolvedBy().isEmpty())
                resolvedText.append(" by ").append(alert.getResolvedBy());
            if (alert.getResolvedAt() != null && !alert.getResolvedAt().isEmpty())
                resolvedText.append(" at ").append(alert.getResolvedAt());
            holder.tvAlertResolvedDetails.setText(resolvedText.toString());
        } else {
            holder.btnResolveAlert.setVisibility(View.VISIBLE);
            holder.tvAlertResolvedDetails.setVisibility(View.GONE);
            holder.btnResolveAlert.setOnClickListener(v -> {
                if (resolveListener != null) resolveListener.onResolve(alert, holder.getAdapterPosition());
            });
        }

        // Checkbox (selection mode)
        if (selectionModeActive) {
            holder.cbAlertSelect.setVisibility(View.VISIBLE);
            holder.cbAlertSelect.setOnCheckedChangeListener(null); // reset before setting
            holder.cbAlertSelect.setChecked(alert.getId() != null && selectedIds.contains(alert.getId()));
            holder.cbAlertSelect.setOnCheckedChangeListener((cb, checked) -> {
                if (alert.getId() != null) {
                    if (checked) selectedIds.add(alert.getId());
                    else selectedIds.remove(alert.getId());
                }
                if (selectionChangedListener != null) selectionChangedListener.onSelectionChanged(selectedIds.size());
            });
        } else {
            holder.cbAlertSelect.setVisibility(View.GONE);
            holder.cbAlertSelect.setOnCheckedChangeListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return alerts != null ? alerts.size() : 0;
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbAlertSelect;
        View viewSeverityBar;
        ImageView ivAlertIcon;
        TextView tvAlertTitle;
        TextView tvAlertSeverityBadge;
        TextView tvAlertMessage;
        TextView tvAlertTime;
        TextView tvAlertResolvedDetails;
        Button btnResolveAlert;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            cbAlertSelect = itemView.findViewById(R.id.cbAlertSelect);
            viewSeverityBar = itemView.findViewById(R.id.viewSeverityBar);
            ivAlertIcon = itemView.findViewById(R.id.ivAlertIcon);
            tvAlertTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvAlertSeverityBadge = itemView.findViewById(R.id.tvAlertSeverityBadge);
            tvAlertMessage = itemView.findViewById(R.id.tvAlertMessage);
            tvAlertTime = itemView.findViewById(R.id.tvAlertTime);
            tvAlertResolvedDetails = itemView.findViewById(R.id.tvAlertResolvedDetails);
            btnResolveAlert = itemView.findViewById(R.id.btnResolveAlert);
        }
    }
}
