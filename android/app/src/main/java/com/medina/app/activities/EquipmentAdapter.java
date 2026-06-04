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
import com.medina.app.model.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    public interface OnEquipmentClickListener {
        void onEquipmentClick(Equipment equipment);
    }

    private final List<Equipment> equipmentList;
    private final OnEquipmentClickListener listener;

    public EquipmentAdapter(List<Equipment> equipmentList, OnEquipmentClickListener listener) {
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment eq = equipmentList.get(position);
        holder.tvName.setText(eq.getEquipmentName());
        holder.tvSerialType.setText(eq.getSerialNumber() + " • " + eq.getType());
        holder.tvAssignedUser.setText("👤 " + (eq.getBrand() != null ? eq.getBrand() : "Unassigned"));

        String status = eq.getStatus() != null ? eq.getStatus() : "Available";
        holder.tvStatus.setText(status.toUpperCase());

        // Status badge colors
        if ("Available".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#22c55e"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_available);
        } else if ("Maintenance".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#f59e0b"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else if ("Under Repair".equalsIgnoreCase(status) || "Broken".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#ef4444"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#475569"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_card);
        }

        // Icon based on type
        String type = eq.getType() != null ? eq.getType().toLowerCase() : "";
        if (type.contains("laptop") || type.contains("computer") || type.contains("pc")) {
            holder.ivIcon.setImageResource(R.drawable.ic_laptop);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_department);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEquipmentClick(eq);
            }
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvSerialType, tvAssignedUser, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivEquipmentIcon);
            tvName = itemView.findViewById(R.id.tvEquipmentName);
            tvSerialType = itemView.findViewById(R.id.tvEquipmentSerialType);
            tvAssignedUser = itemView.findViewById(R.id.tvEquipmentAssignedUser);
            tvStatus = itemView.findViewById(R.id.tvEquipmentStatus);
        }
    }
}
