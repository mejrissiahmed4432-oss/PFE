package com.medina.app.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.ConversationSummary;
import com.medina.app.model.Message;
import com.medina.app.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private LinearLayout panelContacts, panelChat;
    private RecyclerView rvContacts, rvMessages;
    private EditText etContactSearch, etMessageCompose;
    private CardView btnSendMessage;
    private ImageButton btnChatBack;
    private TextView tvChatHeaderName, tvChatHeaderStatus, tvChatHeaderAvatar;
    private View viewChatOnlineDot;
    private LinearLayout layoutChatEmpty;

    private ContactsAdapter contactsAdapter;
    private MessagesAdapter messagesAdapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();

    private String currentUserId;
    private String selectedContactId;
    private String selectedContactName;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private static final int POLL_INTERVAL_MS = 4000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        panelContacts = view.findViewById(R.id.panelContacts);
        panelChat = view.findViewById(R.id.panelChat);
        rvContacts = view.findViewById(R.id.rvContacts);
        rvMessages = view.findViewById(R.id.rvMessages);
        etContactSearch = view.findViewById(R.id.etContactSearch);
        etMessageCompose = view.findViewById(R.id.etMessageCompose);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        btnChatBack = view.findViewById(R.id.btnChatBack);
        tvChatHeaderName = view.findViewById(R.id.tvChatHeaderName);
        tvChatHeaderStatus = view.findViewById(R.id.tvChatHeaderStatus);
        tvChatHeaderAvatar = view.findViewById(R.id.tvChatHeaderAvatar);
        viewChatOnlineDot = view.findViewById(R.id.viewChatOnlineDot);
        layoutChatEmpty = view.findViewById(R.id.layoutChatEmpty);

        // Retrieve current user id
        android.content.SharedPreferences prefs = requireActivity()
                .getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        // Setup RecyclerViews
        contactsAdapter = new ContactsAdapter(filteredUsers, this::openChat);
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(contactsAdapter);

        messagesAdapter = new MessagesAdapter(messages, currentUserId);
        LinearLayoutManager msgLayout = new LinearLayoutManager(getContext());
        msgLayout.setStackFromEnd(true);
        rvMessages.setLayoutManager(msgLayout);
        rvMessages.setAdapter(messagesAdapter);

        // Search filter
        etContactSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnChatBack.setOnClickListener(v -> showContactsPanel());

        btnSendMessage.setOnClickListener(v -> {
            String text = etMessageCompose.getText().toString().trim();
            if (!text.isEmpty() && selectedContactId != null) {
                sendMessage(text);
            }
        });

        loadUsers();
    }

    private void loadUsers() {
        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    for (User u : response.body()) {
                        if (!u.getId().equals(currentUserId)) {
                            allUsers.add(u);
                        }
                    }
                    filteredUsers.clear();
                    filteredUsers.addAll(allUsers);
                    contactsAdapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    loadFallbackUsers();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                loadFallbackUsers();
            }
        });
    }

    private void loadFallbackUsers() {
        allUsers.clear();
        User u1 = new User();
        u1.setId("fallback_1");
        u1.setName("Ahmed Mansouri");
        u1.setRole("Technician");
        allUsers.add(u1);
        User u2 = new User();
        u2.setId("fallback_2");
        u2.setName("Sara Ben Ali");
        u2.setRole("Manager");
        allUsers.add(u2);
        filteredUsers.clear();
        filteredUsers.addAll(allUsers);
        contactsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void filterContacts(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String lower = query.toLowerCase();
            for (User u : allUsers) {
                if (u.getName() != null && u.getName().toLowerCase().contains(lower)) {
                    filteredUsers.add(u);
                }
            }
        }
        contactsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredUsers.isEmpty()) {
            layoutChatEmpty.setVisibility(View.VISIBLE);
            rvContacts.setVisibility(View.GONE);
        } else {
            layoutChatEmpty.setVisibility(View.GONE);
            rvContacts.setVisibility(View.VISIBLE);
        }
    }

    private void openChat(User contact) {
        selectedContactId = contact.getId();
        selectedContactName = contact.getName() != null ? contact.getName() : "Unknown";

        // Set header
        tvChatHeaderName.setText(selectedContactName);
        tvChatHeaderStatus.setText(contact.isOnline() ? "● Online" : "Offline");
        tvChatHeaderStatus.setTextColor(contact.isOnline()
                ? getResources().getColor(R.color.successText)
                : getResources().getColor(R.color.textHint));
        viewChatOnlineDot.setVisibility(contact.isOnline() ? View.VISIBLE : View.GONE);

        // Avatar initials
        String initials = getInitials(selectedContactName);
        tvChatHeaderAvatar.setText(initials);

        // Show chat panel
        panelContacts.setVisibility(View.GONE);
        panelChat.setVisibility(View.VISIBLE);

        // Load messages
        loadMessages();
        startPolling();

        // Mark as read
        markAsRead();
    }

    private void loadMessages() {
        if (selectedContactId == null) return;

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().getChatHistory(selectedContactId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    messagesAdapter.notifyDataSetChanged();
                    scrollToBottom();
                } else {
                    loadFallbackMessages();
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                loadFallbackMessages();
            }
        });
    }

    private void loadFallbackMessages() {
        messages.clear();
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Message m1 = new Message();
        m1.setSenderId(selectedContactId);
        m1.setReceiverId(currentUserId);
        m1.setContent("Hello! How can I assist you today?");
        m1.setTimestamp(now);
        m1.setStatus("READ");
        messages.add(m1);
        messagesAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void sendMessage(String text) {
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setReceiverId(selectedContactId);
        msg.setContent(text);
        msg.setTimestamp(now);
        msg.setStatus("SENT");

        // Append immediately (optimistic UI)
        messages.add(msg);
        messagesAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        etMessageCompose.setText("");

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().sendMessage(msg).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int last = messages.size() - 1;
                    messages.set(last, response.body());
                    messagesAdapter.notifyItemChanged(last);
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                // Keep optimistic message in list
            }
        });
    }

    private void markAsRead() {
        if (selectedContactId == null) return;
        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE)
                .getString("auth_token", null);
        ApiClient.getApiService().markAsRead(selectedContactId).enqueue(new Callback<java.util.Map<String, String>>() {
            @Override public void onResponse(Call<java.util.Map<String, String>> call, Response<java.util.Map<String, String>> r) {}
            @Override public void onFailure(Call<java.util.Map<String, String>> call, Throwable t) {}
        });
    }

    private void startPolling() {
        stopPolling();
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (selectedContactId != null && panelChat.getVisibility() == View.VISIBLE) {
                    loadMessages();
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                }
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void showContactsPanel() {
        stopPolling();
        selectedContactId = null;
        panelChat.setVisibility(View.GONE);
        panelContacts.setVisibility(View.VISIBLE);
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            rvMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) sb.append(parts[i].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    // ─────────────────────────────────────────────
    //  Inner Adapter: Contacts
    // ─────────────────────────────────────────────
    interface OnContactClick { void onClick(User user); }

    static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.VH> {
        private final List<User> users;
        private final OnContactClick listener;

        ContactsAdapter(List<User> users, OnContactClick listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_contact, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            User u = users.get(pos);
            String name = u.getName() != null ? u.getName() : "Unknown";
            h.tvName.setText(name);
            h.tvRole.setText(u.getRole() != null ? u.getRole() : "");
            h.tvLastMsg.setText("Tap to start chatting");
            h.tvTime.setText("");
            h.tvUnread.setVisibility(View.GONE);

            // Initials avatar
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
            }
            h.tvAvatar.setText(initials.toString().toUpperCase());

            // Online dot
            h.viewDot.setVisibility(u.isOnline() ? View.VISIBLE : View.GONE);

            h.itemView.setOnClickListener(v -> listener.onClick(u));
        }

        @Override
        public int getItemCount() { return users.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvRole, tvLastMsg, tvTime, tvUnread;
            View viewDot;
            VH(View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvContactAvatar);
                tvName = v.findViewById(R.id.tvContactName);
                tvRole = v.findViewById(R.id.tvContactRole);
                tvLastMsg = v.findViewById(R.id.tvContactLastMessage);
                tvTime = v.findViewById(R.id.tvContactTime);
                tvUnread = v.findViewById(R.id.tvContactUnread);
                viewDot = v.findViewById(R.id.viewOnlineDot);
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Inner Adapter: Messages
    // ─────────────────────────────────────────────
    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {
        private final List<Message> messages;
        private final String currentUserId;

        MessagesAdapter(List<Message> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Message msg = messages.get(pos);
            boolean isSent = currentUserId != null && currentUserId.equals(msg.getSenderId());

            if (isSent) {
                h.bubbleSent.setVisibility(View.VISIBLE);
                h.bubbleReceived.setVisibility(View.GONE);
                String content = msg.isDeletedForEveryone() ? "🚫 This message was deleted"
                        : msg.getContent();
                h.tvSentContent.setText(content);
                h.tvReadStatus.setText("READ".equals(msg.getStatus()) ? "✓✓" : "✓");
                h.tvReadStatus.setTextColor(
                        "READ".equals(msg.getStatus())
                                ? 0xFF3b82f6 : 0xFF94a3b8);
            } else {
                h.bubbleReceived.setVisibility(View.VISIBLE);
                h.bubbleSent.setVisibility(View.GONE);
                String content = msg.isDeletedForEveryone() ? "🚫 This message was deleted"
                        : msg.getContent();
                h.tvReceivedContent.setText(content);
            }

            // Show timestamp
            if (msg.getTimestamp() != null && !msg.getTimestamp().isEmpty()) {
                h.tvTime.setVisibility(View.VISIBLE);
                h.tvTime.setText(msg.getTimestamp());
            } else {
                h.tvTime.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout bubbleSent, bubbleReceived;
            TextView tvSentContent, tvReceivedContent, tvReadStatus, tvTime;
            VH(View v) {
                super(v);
                bubbleSent = v.findViewById(R.id.bubbleSent);
                bubbleReceived = v.findViewById(R.id.bubbleReceived);
                tvSentContent = v.findViewById(R.id.tvSentContent);
                tvReceivedContent = v.findViewById(R.id.tvReceivedContent);
                tvReadStatus = v.findViewById(R.id.tvReadStatus);
                tvTime = v.findViewById(R.id.tvMessageTime);
            }
        }
    }
}
