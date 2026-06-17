package com.medina.app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.AiChatMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnAiMessageListener {
        void onSuggestionClick(String suggestion);
        void onProceedAction(AiChatMessage message);
        void onCancelAction(AiChatMessage message);
    }

    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;

    private List<AiChatMessage> messages;
    private OnAiMessageListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AiMessagesAdapter(List<AiChatMessage> messages, OnAiMessageListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        AiChatMessage msg = messages.get(position);
        if ("user".equalsIgnoreCase(msg.getSender())) {
            return TYPE_USER;
        } else {
            return TYPE_ASSISTANT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ai_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ai_message_assistant, parent, false);
            return new AssistantViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AiChatMessage msg = messages.get(position);
        String timeStr = msg.getTimestamp() != null ? timeFormat.format(msg.getTimestamp()) : "";

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvUserMsg.setText(msg.getText());
            userHolder.tvUserMsgTime.setText(timeStr);

            // Handle attachment image
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                try {
                    String base64Image = msg.getImageUrl();
                    // Strip data:image/...;base64, prefix if present
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        userHolder.ivUserImage.setImageBitmap(decodedByte);
                        userHolder.cardUserImage.setVisibility(View.VISIBLE);
                    } else {
                        userHolder.cardUserImage.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    userHolder.cardUserImage.setVisibility(View.GONE);
                }
            } else {
                userHolder.cardUserImage.setVisibility(View.GONE);
            }

        } else if (holder instanceof AssistantViewHolder) {
            AssistantViewHolder assistantHolder = (AssistantViewHolder) holder;
            assistantHolder.tvAssistantMsg.setText(msg.getText());
            assistantHolder.tvAiMsgTime.setText(timeStr);

            // ── Dynamic Data Table ──
            if (msg.getData() != null && !msg.getData().isEmpty()) {
                assistantHolder.tableData.removeAllViews();
                assistantHolder.scrollDataTable.setVisibility(View.VISIBLE);

                List<Map<String, Object>> dataList = msg.getData();
                Map<String, Object> firstRow = dataList.get(0);

                // Add header row
                TableRow headerRow = new TableRow(holder.itemView.getContext());
                headerRow.setBackgroundColor(Color.parseColor("#E0E0E0"));
                headerRow.setPadding(4, 4, 4, 4);

                for (String key : firstRow.keySet()) {
                    TextView tvHeader = new TextView(holder.itemView.getContext());
                    tvHeader.setText(key.toUpperCase());
                    tvHeader.setTextSize(11);
                    tvHeader.setTypeface(null, Typeface.BOLD);
                    tvHeader.setTextColor(Color.BLACK);
                    tvHeader.setPadding(12, 8, 12, 8);
                    tvHeader.setGravity(Gravity.CENTER);
                    headerRow.addView(tvHeader);
                }
                assistantHolder.tableData.addView(headerRow);

                // Add data rows
                for (Map<String, Object> rowMap : dataList) {
                    TableRow dataRow = new TableRow(holder.itemView.getContext());
                    dataRow.setPadding(4, 4, 4, 4);

                    for (Object val : rowMap.values()) {
                        TextView tvVal = new TextView(holder.itemView.getContext());
                        tvVal.setText(val != null ? val.toString() : "N/A");
                        tvVal.setTextSize(11);
                        tvVal.setTextColor(Color.DKGRAY);
                        tvVal.setPadding(12, 8, 12, 8);
                        tvVal.setGravity(Gravity.CENTER);
                        dataRow.addView(tvVal);
                    }
                    assistantHolder.tableData.addView(dataRow);
                }
            } else {
                assistantHolder.scrollDataTable.setVisibility(View.GONE);
            }

            // ── Action Pending ──
            if (msg.isActionPending()) {
                assistantHolder.tvActionPendingType.setText(msg.getActionType());
                assistantHolder.layoutActionPending.setVisibility(View.VISIBLE);

                assistantHolder.btnActionConfirm.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onProceedAction(msg);
                    }
                });

                assistantHolder.btnActionCancel.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCancelAction(msg);
                    }
                });
            } else {
                assistantHolder.layoutActionPending.setVisibility(View.GONE);
            }

            // ── Dynamic Suggestions ──
            if (msg.getSuggestions() != null && !msg.getSuggestions().isEmpty()) {
                assistantHolder.layoutAiSuggestions.removeAllViews();
                assistantHolder.layoutAiSuggestions.setVisibility(View.VISIBLE);

                for (String suggestion : msg.getSuggestions()) {
                    TextView tvSuggestion = new TextView(holder.itemView.getContext());
                    tvSuggestion.setText(suggestion);
                    tvSuggestion.setTextSize(12);
                    tvSuggestion.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorPrimary));
                    tvSuggestion.setBackgroundResource(R.drawable.bg_badge_installed);
                    tvSuggestion.setPadding(24, 12, 24, 12);
                    tvSuggestion.setGravity(Gravity.CENTER);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lp.setMargins(0, 4, 0, 4);
                    tvSuggestion.setLayoutParams(lp);

                    tvSuggestion.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onSuggestionClick(suggestion);
                        }
                    });

                    assistantHolder.layoutAiSuggestions.addView(tvSuggestion);
                }
            } else {
                assistantHolder.layoutAiSuggestions.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMsg, tvUserMsgTime;
        CardView cardUserImage;
        ImageView ivUserImage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMsg = itemView.findViewById(R.id.tvUserMsg);
            tvUserMsgTime = itemView.findViewById(R.id.tvUserMsgTime);
            cardUserImage = itemView.findViewById(R.id.cardUserImage);
            ivUserImage = itemView.findViewById(R.id.ivUserImage);
        }
    }

    public static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView tvAssistantMsg, tvAiMsgTime, tvActionPendingType;
        LinearLayout layoutActionPending, layoutAiSuggestions, layoutAiBubble;
        HorizontalScrollView scrollDataTable;
        TableLayout tableData;
        Button btnActionCancel, btnActionConfirm;

        public AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAssistantMsg = itemView.findViewById(R.id.tvAssistantMsg);
            tvAiMsgTime = itemView.findViewById(R.id.tvAiMsgTime);
            tvActionPendingType = itemView.findViewById(R.id.tvActionPendingType);
            layoutActionPending = itemView.findViewById(R.id.layoutActionPending);
            layoutAiSuggestions = itemView.findViewById(R.id.layoutAiSuggestions);
            layoutAiBubble = itemView.findViewById(R.id.layoutAiBubble);
            scrollDataTable = itemView.findViewById(R.id.scrollDataTable);
            tableData = itemView.findViewById(R.id.tableData);
            btnActionCancel = itemView.findViewById(R.id.btnActionCancel);
            btnActionConfirm = itemView.findViewById(R.id.btnActionConfirm);
        }
    }
}
