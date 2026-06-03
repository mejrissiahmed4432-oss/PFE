package com.medina.app.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.Ticket;

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private final List<Ticket> ticketList;
    private final OnTicketClickListener listener;

    public TicketAdapter(List<Ticket> ticketList, OnTicketClickListener listener) {
        this.ticketList = ticketList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);

        // Ticket number & title
        String ticketNum = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "#" + (position + 1);
        holder.tvTicketNumber.setText(ticketNum);
        holder.tvTitle.setText(ticket.getTitle() != null ? ticket.getTitle() : "Untitled Ticket");

        // Equipment info
        String equipInfo = ticket.getEquipmentName() != null ? "🖥 " + ticket.getEquipmentName() : "No equipment linked";
        holder.tvEquipment.setText(equipInfo);

        // Requester
        String requester = ticket.getUserName() != null ? "👤 " + ticket.getUserName() : "Unknown";
        holder.tvRequester.setText(requester);

        // Date
        String date = ticket.getCreatedAt();
        if (date != null && date.length() >= 10) {
            date = date.substring(0, 10);
        }
        holder.tvDate.setText(date != null ? date : "");

        // Status badge
        String status = ticket.getStatus() != null ? ticket.getStatus() : "Open";
        holder.tvStatus.setText(status.toUpperCase().replace("_", " "));
        applyStatusStyle(holder.tvStatus, status);

        // Priority badge
        String priority = ticket.getPriority() != null ? ticket.getPriority() : "medium";
        holder.tvPriority.setText(priority.toUpperCase());
        applyPriorityStyle(holder.tvPriority, priority);

        // Category icon tint
        String category = ticket.getCategory() != null ? ticket.getCategory().toLowerCase() : "";
        if (category.contains("hardware") || category.contains("equipment")) {
            holder.ivIcon.setImageResource(R.drawable.ic_laptop);
        } else if (category.contains("network") || category.contains("wifi")) {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_reports);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_tickets);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTicketClick(ticket);
            }
        });
    }

    private void applyStatusStyle(TextView tv, String status) {
        if (status.equalsIgnoreCase("open")) {
            tv.setTextColor(Color.parseColor("#ef4444"));
            tv.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else if (status.equalsIgnoreCase("in_progress") || status.equalsIgnoreCase("in progress")) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
            tv.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else if (status.equalsIgnoreCase("resolved") || status.equalsIgnoreCase("closed")) {
            tv.setTextColor(Color.parseColor("#22c55e"));
            tv.setBackgroundResource(R.drawable.bg_badge_available);
        } else {
            tv.setTextColor(Color.parseColor("#64748b"));
            tv.setBackgroundResource(R.drawable.bg_card);
        }
    }

    private void applyPriorityStyle(TextView tv, String priority) {
        if (priority.equalsIgnoreCase("high") || priority.equalsIgnoreCase("critical")) {
            tv.setTextColor(Color.parseColor("#ef4444"));
        } else if (priority.equalsIgnoreCase("medium")) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
        } else {
            tv.setTextColor(Color.parseColor("#22c55e"));
        }
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTicketNumber, tvTitle, tvEquipment, tvRequester, tvDate, tvStatus, tvPriority;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivTicketIcon);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvEquipment = itemView.findViewById(R.id.tvTicketEquipment);
            tvRequester = itemView.findViewById(R.id.tvTicketRequester);
            tvDate = itemView.findViewById(R.id.tvTicketDate);
            tvStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvPriority = itemView.findViewById(R.id.tvTicketPriority);
        }
    }
}
