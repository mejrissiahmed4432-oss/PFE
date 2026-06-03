package com.medina.app.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.PartRequest;
import com.medina.app.model.PartRequestItem;

import java.util.List;

public class PartsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private boolean isTableView = false;

    public static class FlatPartItem {
        public boolean isHeader;
        public String headerTitle;
        public int headerCount;
        public String groupKey;
        public boolean isExpanded;

        // Details
        public PartRequest parentRequest;
        public PartRequestItem item;
    }

    public interface OnPartActionListener {
        void onReturnToStock(PartRequest parent, PartRequestItem item);
        void onGroupHeaderClick(FlatPartItem headerItem);
        void onPartItemClick(PartRequest parent, PartRequestItem item);
    }

    private List<FlatPartItem> flatList;
    private OnPartActionListener actionListener;

    public PartsAdapter(List<FlatPartItem> flatList, OnPartActionListener actionListener) {
        this.flatList = flatList;
        this.actionListener = actionListener;
    }

    public void updateList(List<FlatPartItem> newList) {
        this.flatList = newList;
        notifyDataSetChanged();
    }

    public void setTableView(boolean tableView) {
        this.isTableView = tableView;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return flatList.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_part_group_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_part_detail, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FlatPartItem flatItem = flatList.get(position);

        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderViewHolder hHolder = (HeaderViewHolder) holder;
            String title = flatItem.headerTitle;
            if (isTableView && flatItem.headerCount > 1) {
                title = (flatItem.isExpanded ? "▼ " : "► ") + title;
            }
            hHolder.tvGroupTitle.setText(title);
            hHolder.tvGroupCount.setText(flatItem.headerCount + (flatItem.headerCount == 1 ? " Item" : " Items"));
            
            if (isTableView && flatItem.headerCount > 1) {
                hHolder.itemView.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onGroupHeaderClick(flatItem);
                    }
                });
            } else {
                hHolder.itemView.setOnClickListener(null);
            }
        } else {
            ItemViewHolder iHolder = (ItemViewHolder) holder;
            PartRequestItem part = flatItem.item;

            iHolder.tvPartName.setText(part.getPartName());
            
            String brand = part.getBrand() != null ? part.getBrand() : "Unknown Brand";
            String spec = part.getSpecification() != null ? part.getSpecification() : "No specs";
            iHolder.tvPartBrandSpec.setText("Brand: " + brand + " | Spec: " + spec);

            String sn = part.getMatchedSerialNumber() != null ? part.getMatchedSerialNumber() : "S/N: Not Allocated Yet";
            iHolder.tvPartSN.setText("S/N: " + sn);

            iHolder.tvPartLocation.setText("📍 Allocated Mode");

            // Indentation and name visibility based on view mode
            if (isTableView) {
                iHolder.tvPartName.setVisibility(View.GONE);
                iHolder.itemView.setPadding(40, 16, 16, 16);
            } else {
                iHolder.tvPartName.setVisibility(View.VISIBLE);
                iHolder.itemView.setPadding(16, 16, 16, 16);
            }

            // If it is already marked returned, adjust status badge
            if (part.getReturned() != null && part.getReturned()) {
                iHolder.tvPartStatus.setText("RETURNED");
                iHolder.tvPartStatus.setBackgroundResource(R.drawable.bg_badge_maintenance);
                iHolder.btnReturnToStock.setVisibility(View.GONE);
            } else {
                iHolder.tvPartStatus.setText("ALLOCATED");
                iHolder.tvPartStatus.setBackgroundResource(R.drawable.bg_badge_installed);
                iHolder.btnReturnToStock.setVisibility(View.VISIBLE);
            }

            iHolder.btnReturnToStock.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onReturnToStock(flatItem.parentRequest, part);
                }
            });

            iHolder.itemView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onPartItemClick(flatItem.parentRequest, part);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return flatList != null ? flatList.size() : 0;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupTitle, tvGroupCount;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            tvGroupCount = itemView.findViewById(R.id.tvGroupCount);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartName, tvPartStatus, tvPartBrandSpec, tvPartSN, tvPartLocation;
        Button btnReturnToStock;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPartName = itemView.findViewById(R.id.tvPartName);
            tvPartStatus = itemView.findViewById(R.id.tvPartStatus);
            tvPartBrandSpec = itemView.findViewById(R.id.tvPartBrandSpec);
            tvPartSN = itemView.findViewById(R.id.tvPartSN);
            tvPartLocation = itemView.findViewById(R.id.tvPartLocation);
            btnReturnToStock = itemView.findViewById(R.id.btnReturnToStock);
        }
    }
}
