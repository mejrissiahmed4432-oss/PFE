package com.medina.app.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.model.AiChatMessage;
import com.medina.app.model.AiConversation;

import java.util.List;

public class AiConversationsAdapter extends RecyclerView.Adapter<AiConversationsAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(AiConversation conversation);
        void onConversationDelete(AiConversation conversation);
    }

    private List<AiConversation> conversations;
    private OnConversationClickListener listener;

    public AiConversationsAdapter(List<AiConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiConversation conv = conversations.get(position);
        holder.tvConvTitle.setText(conv.getTitle() != null ? conv.getTitle() : "New Conversation");

        // Find last user message for preview
        String preview = "No messages yet";
        if (conv.getMessages() != null && !conv.getMessages().isEmpty()) {
            for (int i = conv.getMessages().size() - 1; i >= 0; i--) {
                AiChatMessage msg = conv.getMessages().get(i);
                if ("user".equalsIgnoreCase(msg.getSender())) {
                    preview = msg.getText();
                    if (preview != null && preview.length() > 35) {
                        preview = preview.substring(0, 35) + "…";
                    }
                    break;
                }
            }
        }
        holder.tvConvPreview.setText(preview);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conv);
            }
        });

        holder.btnDeleteConv.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationDelete(conv);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConvTitle, tvConvPreview;
        ImageButton btnDeleteConv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConvTitle = itemView.findViewById(R.id.tvConvTitle);
            tvConvPreview = itemView.findViewById(R.id.tvConvPreview);
            btnDeleteConv = itemView.findViewById(R.id.btnDeleteConv);
        }
    }
}
