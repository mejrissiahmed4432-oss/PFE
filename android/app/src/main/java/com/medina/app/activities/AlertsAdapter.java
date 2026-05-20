package com.medina.app.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.Alert;

import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    public interface OnAlertResolveListener {
        void onResolve(Alert alert, int position);
    }

    private List<Alert> alerts;
    private OnAlertResolveListener resolveListener;

    public AlertsAdapter(List<Alert> alerts, OnAlertResolveListener resolveListener) {
        this.alerts = alerts;
        this.resolveListener = resolveListener;
    }

    public void updateList(List<Alert> newAlerts) {
        this.alerts = newAlerts;
        notifyDataSetChanged();
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
        holder.tvAlertTime.setText(alert.getCreatedAt());

        // Setup Severity
        String severity = alert.getSeverity() != null ? alert.getSeverity().toUpperCase() : "INFO";
        holder.tvAlertSeverityBadge.setText(severity);

        int severityColor;
        int severityBg;
        switch (severity) {
            case "CRITICAL":
                severityColor = Color.parseColor("#ef4444"); // Red
                severityBg = Color.parseColor("#fef2f2");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_alert);
                break;
            case "WARNING":
                severityColor = Color.parseColor("#f59e0b"); // Orange
                severityBg = Color.parseColor("#fffbeb");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_alert);
                break;
            case "INFO":
            default:
                severityColor = Color.parseColor("#3b82f6"); // Blue
                severityBg = Color.parseColor("#eff6ff");
                holder.ivAlertIcon.setImageResource(R.drawable.ic_notification);
                break;
        }

        holder.viewSeverityBar.setBackgroundColor(severityColor);
        holder.ivAlertIcon.setColorFilter(severityColor);
        holder.tvAlertSeverityBadge.setTextColor(severityColor);
        // On modern devices we can dynamically set the background badge tinted color or fallback
        try {
            holder.tvAlertSeverityBadge.getBackground().setTint(severityBg);
        } catch (Exception e) {
            // ignore
        }

        // Setup Resolved State
        boolean isResolved = "RESOLVED".equalsIgnoreCase(alert.getStatus());
        if (isResolved) {
            holder.btnResolveAlert.setVisibility(View.GONE);
            holder.tvAlertResolvedDetails.setVisibility(View.VISIBLE);
            
            String resolvedText = "Resolved";
            if (alert.getResolvedBy() != null && !alert.getResolvedBy().isEmpty()) {
                resolvedText += " by " + alert.getResolvedBy();
            }
            if (alert.getResolvedAt() != null && !alert.getResolvedAt().isEmpty()) {
                resolvedText += " at " + alert.getResolvedAt();
            }
            holder.tvAlertResolvedDetails.setText(resolvedText);
        } else {
            holder.btnResolveAlert.setVisibility(View.VISIBLE);
            holder.tvAlertResolvedDetails.setVisibility(View.GONE);
            holder.btnResolveAlert.setOnClickListener(v -> {
                if (resolveListener != null) {
                    resolveListener.onResolve(alert, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return alerts != null ? alerts.size() : 0;
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
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
