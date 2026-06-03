package com.medina.app.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;
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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final int PICK_FILE_REQUEST = 42;

    private LinearLayout panelContacts, panelChat;
    private RecyclerView rvContacts, rvMessages;
    private EditText etContactSearch, etMessageCompose;
    private CardView btnSendMessage;
    private ImageButton btnChatBack, btnAttachFile;
    private TextView tvChatHeaderName, tvChatHeaderStatus, tvChatHeaderAvatar;
    private View viewChatOnlineDot;
    private LinearLayout layoutChatEmpty;

    private ContactsAdapter contactsAdapter;
    private MessagesAdapter messagesAdapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private List<ConversationSummary> conversations = new ArrayList<>();

    private String currentUserId;
    private String selectedContactId;
    private String selectedContactName;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private static final int POLL_INTERVAL_MS = 4000;

    // Edit mode state
    private Message editingMessage = null;
    private TextView tvEditingBanner;

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
        panelChat     = view.findViewById(R.id.panelChat);
        rvContacts    = view.findViewById(R.id.rvContacts);
        rvMessages    = view.findViewById(R.id.rvMessages);
        etContactSearch   = view.findViewById(R.id.etContactSearch);
        etMessageCompose  = view.findViewById(R.id.etMessageCompose);
        btnSendMessage    = view.findViewById(R.id.btnSendMessage);
        btnChatBack       = view.findViewById(R.id.btnChatBack);
        btnAttachFile     = view.findViewById(R.id.btnAttachFile);
        tvChatHeaderName  = view.findViewById(R.id.tvChatHeaderName);
        tvChatHeaderStatus = view.findViewById(R.id.tvChatHeaderStatus);
        tvChatHeaderAvatar = view.findViewById(R.id.tvChatHeaderAvatar);
        viewChatOnlineDot  = view.findViewById(R.id.viewChatOnlineDot);
        layoutChatEmpty    = view.findViewById(R.id.layoutChatEmpty);
        tvEditingBanner    = view.findViewById(R.id.tvEditingBanner);

        // Retrieve current user id
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        // Setup Contacts RecyclerView
        contactsAdapter = new ContactsAdapter(filteredUsers, this::openChat);
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(contactsAdapter);

        // Setup Messages RecyclerView
        messagesAdapter = new MessagesAdapter(messages, currentUserId, this::onMessageLongPress);
        LinearLayoutManager msgLayout = new LinearLayoutManager(getContext());
        msgLayout.setStackFromEnd(true);
        rvMessages.setLayoutManager(msgLayout);
        rvMessages.setAdapter(messagesAdapter);

        // Search filter
        etContactSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterContacts(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnChatBack.setOnClickListener(v -> showContactsPanel());

        // Send / confirm edit
        btnSendMessage.setOnClickListener(v -> {
            String text = etMessageCompose.getText().toString().trim();
            if (text.isEmpty()) return;
            if (editingMessage != null) {
                confirmEdit(editingMessage, text);
            } else if (selectedContactId != null) {
                sendMessage(text);
            }
        });

        // Attach file picker
        if (btnAttachFile != null) {
            btnAttachFile.setOnClickListener(v -> openFilePicker());
        }

        loadUsers();
        loadConversations();
    }

    // ──────────────────────────────────────────────
    //  FILE ATTACHMENT
    // ──────────────────────────────────────────────
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                uploadAndSendFile(fileUri);
            }
        }
    }

    private void uploadAndSendFile(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            byte[] bytes = inputStream != null ? inputStream.readAllBytes() : new byte[0];
            if (inputStream != null) inputStream.close();

            String mimeType = requireActivity().getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";

            RequestBody requestFile = RequestBody.create(bytes, MediaType.parse(mimeType));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestFile);

            ApiClient.authToken = requireActivity()
                    .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null);

            ApiClient.getApiService().uploadAttachment(filePart).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String fileUrl = response.body().get("url");
                        sendAttachmentMessage(fileName, fileUrl);
                    } else {
                        // Send with local name as fallback
                        sendAttachmentMessage(fileName, null);
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    sendAttachmentMessage(fileName, null);
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to attach file", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendAttachmentMessage(String fileName, String fileUrl) {
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setReceiverId(selectedContactId);
        msg.setContent("[Attachment] " + fileName);
        msg.setAttachmentName(fileName);
        msg.setAttachmentUrl(fileUrl);
        msg.setTimestamp(now);
        msg.setStatus("SENT");

        messages.add(msg);
        messagesAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        ApiClient.getApiService().sendMessage(msg).enqueue(new Callback<Message>() {
            @Override public void onResponse(Call<Message> c, Response<Message> r) {
                if (r.isSuccessful() && r.body() != null) {
                    int last = messages.size() - 1;
                    messages.set(last, r.body());
                    messagesAdapter.notifyItemChanged(last);
                }
            }
            @Override public void onFailure(Call<Message> c, Throwable t) {}
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "attachment";
    }

    // ──────────────────────────────────────────────
    //  LONG PRESS: Edit / Delete dialog
    // ──────────────────────────────────────────────
    private void onMessageLongPress(Message msg) {
        // Only allow editing/deleting own messages
        if (!currentUserId.equals(msg.getSenderId())) {
            showDeleteForMeDialog(msg);
            return;
        }

        String[] options = {"Edit Message", "Delete for Me", "Delete for Everyone"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Message Options")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0: startEditMode(msg); break;
                        case 1: deleteMessage(msg, false); break;
                        case 2: deleteMessage(msg, true);  break;
                    }
                })
                .show();
    }

    private void showDeleteForMeDialog(Message msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Message")
                .setMessage("Delete this message for you?")
                .setPositiveButton("Delete for Me", (d, w) -> deleteMessage(msg, false))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startEditMode(Message msg) {
        editingMessage = msg;
        etMessageCompose.setText(msg.getContent());
        etMessageCompose.requestFocus();
        etMessageCompose.setSelection(etMessageCompose.getText().length());
        if (tvEditingBanner != null) {
            tvEditingBanner.setVisibility(View.VISIBLE);
            tvEditingBanner.setText("Editing message…  Tap × to cancel");
            tvEditingBanner.setOnClickListener(v -> cancelEdit());
        }
    }

    private void cancelEdit() {
        editingMessage = null;
        etMessageCompose.setText("");
        if (tvEditingBanner != null) tvEditingBanner.setVisibility(View.GONE);
    }

    private void confirmEdit(Message msg, String newContent) {
        String id = msg.getId();
        if (id == null || id.isEmpty()) {
            // Optimistic only
            msg.setContent(newContent);
            msg.setEdited(true);
            messagesAdapter.notifyDataSetChanged();
            cancelEdit();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("content", newContent);

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().editMessage(id, body).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> r) {
                int idx = messages.indexOf(msg);
                if (r.isSuccessful() && r.body() != null && idx >= 0) {
                    messages.set(idx, r.body());
                } else if (idx >= 0) {
                    msg.setContent(newContent);
                    msg.setEdited(true);
                }
                messagesAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                // Optimistic
                int idx = messages.indexOf(msg);
                if (idx >= 0) {
                    msg.setContent(newContent);
                    msg.setEdited(true);
                    messagesAdapter.notifyItemChanged(idx);
                }
            }
        });
        cancelEdit();
    }

    private void deleteMessage(Message msg, boolean forEveryone) {
        String id = msg.getId();
        int idx = messages.indexOf(msg);

        if (id == null || id.isEmpty()) {
            // Optimistic
            if (forEveryone) {
                msg.setDeletedForEveryone(true);
                msg.setContent("🚫 This message was deleted");
                if (idx >= 0) messagesAdapter.notifyItemChanged(idx);
            } else {
                if (idx >= 0) {
                    messages.remove(idx);
                    messagesAdapter.notifyItemRemoved(idx);
                }
            }
            return;
        }

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().deleteMessage(id, forEveryone).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                int i = messages.indexOf(msg);
                if (forEveryone) {
                    if (i >= 0) {
                        msg.setDeletedForEveryone(true);
                        msg.setContent("🚫 This message was deleted");
                        messagesAdapter.notifyItemChanged(i);
                    }
                } else {
                    if (i >= 0) {
                        messages.remove(i);
                        messagesAdapter.notifyItemRemoved(i);
                    }
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Could not delete message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ──────────────────────────────────────────────
    //  CONVERSATIONS (to populate contacts list)
    // ──────────────────────────────────────────────
    private void loadConversations() {
        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().getConversations().enqueue(new Callback<List<ConversationSummary>>() {
            @Override
            public void onResponse(Call<List<ConversationSummary>> call, Response<List<ConversationSummary>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversations.clear();
                    conversations.addAll(response.body());

                    // Merge conversation metadata into users list
                    for (ConversationSummary conv : conversations) {
                        for (User u : allUsers) {
                            if (u.getId().equals(conv.getContactId())) {
                                u.setLastMessage(conv.getLastMessage());
                                u.setLastMessageTime(conv.getLastTime());
                                u.setUnreadCount((int) conv.getUnreadCount());
                                break;
                            }
                        }
                    }

                    // Sort: users with conversations first
                    allUsers.sort((a, b) -> {
                        boolean aHasConv = a.getLastMessage() != null;
                        boolean bHasConv = b.getLastMessage() != null;
                        if (aHasConv && !bHasConv) return -1;
                        if (!aHasConv && bHasConv) return 1;
                        return 0;
                    });

                    filteredUsers.clear();
                    filteredUsers.addAll(allUsers);
                    if (contactsAdapter != null) contactsAdapter.notifyDataSetChanged();
                    updateTopbarBadge();
                }
            }
            @Override
            public void onFailure(Call<List<ConversationSummary>> call, Throwable t) { /* silent */ }
        });
    }

    private void updateTopbarBadge() {
        int totalUnread = 0;
        for (User u : allUsers) totalUnread += u.getUnreadCount();

        // Update DashboardActivity topbar badge
        if (getActivity() instanceof DashboardActivity) {
            DashboardActivity dash = (DashboardActivity) getActivity();
            dash.updateChatBadge(totalUnread);
        }
    }

    // ──────────────────────────────────────────────
    //  USERS
    // ──────────────────────────────────────────────
    private void loadUsers() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = requireActivity()
                    .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                    .getString("user_id", "");
        }
        if (currentUserId == null || currentUserId.isEmpty()) {
            ApiClient.getApiService().getCurrentUser().enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentUserId = response.body().getId();
                        requireActivity()
                                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                                .edit().putString("user_id", currentUserId).apply();
                        messagesAdapter = new MessagesAdapter(messages, currentUserId, ChatFragment.this::onMessageLongPress);
                        rvMessages.setAdapter(messagesAdapter);
                        loadUsers();
                    }
                }
                @Override
                public void onFailure(Call<User> call, Throwable t) { loadFallbackUsers(); }
            });
            return;
        }

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    for (User u : response.body()) {
                        if (!u.getId().equals(currentUserId)) allUsers.add(u);
                    }
                    filteredUsers.clear();
                    filteredUsers.addAll(allUsers);
                    contactsAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    loadConversations();
                } else {
                    loadFallbackUsers();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) { loadFallbackUsers(); }
        });
    }

    private void loadFallbackUsers() {
        allUsers.clear();
        User u1 = new User(); u1.setId("fallback_1"); u1.setName("Ahmed Mansouri"); u1.setRole("Technician"); allUsers.add(u1);
        User u2 = new User(); u2.setId("fallback_2"); u2.setName("Sara Ben Ali");   u2.setRole("Manager");    allUsers.add(u2);
        filteredUsers.clear(); filteredUsers.addAll(allUsers);
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
                if (u.getName() != null && u.getName().toLowerCase().contains(lower)) filteredUsers.add(u);
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

    // ──────────────────────────────────────────────
    //  OPEN CHAT
    // ──────────────────────────────────────────────
    private void openChat(User contact) {
        selectedContactId   = contact.getId();
        selectedContactName = contact.getName() != null ? contact.getName() : "Unknown";

        tvChatHeaderName.setText(selectedContactName);
        tvChatHeaderStatus.setText(contact.isOnline() ? "● Online" : "Offline");
        tvChatHeaderStatus.setTextColor(contact.isOnline()
                ? getResources().getColor(R.color.successText)
                : getResources().getColor(R.color.textHint));
        if (viewChatOnlineDot != null)
            viewChatOnlineDot.setVisibility(contact.isOnline() ? View.VISIBLE : View.GONE);
        tvChatHeaderAvatar.setText(getInitials(selectedContactName));

        panelContacts.setVisibility(View.GONE);
        panelChat.setVisibility(View.VISIBLE);

        loadMessages();
        startPolling();
        markAsRead();
    }

    private void loadMessages() {
        if (selectedContactId == null) return;
        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
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
            public void onFailure(Call<List<Message>> call, Throwable t) { loadFallbackMessages(); }
        });
    }

    private void loadFallbackMessages() {
        messages.clear();
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Message m1 = new Message();
        m1.setSenderId(selectedContactId); m1.setReceiverId(currentUserId);
        m1.setContent("Hello! How can I assist you today?");
        m1.setTimestamp(now); m1.setStatus("READ");
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

        messages.add(msg);
        messagesAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        etMessageCompose.setText("");

        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);

        ApiClient.getApiService().sendMessage(msg).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> r) {
                if (r.isSuccessful() && r.body() != null) {
                    int last = messages.size() - 1;
                    messages.set(last, r.body());
                    messagesAdapter.notifyItemChanged(last);
                }
            }
            @Override public void onFailure(Call<Message> call, Throwable t) {}
        });
    }

    private void markAsRead() {
        if (selectedContactId == null) return;
        ApiClient.authToken = requireActivity()
                .getSharedPreferences("medina_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", null);
        ApiClient.getApiService().markAsRead(selectedContactId).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> r) {}
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {}
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
        cancelEdit();
        stopPolling();
        selectedContactId = null;
        panelChat.setVisibility(View.GONE);
        panelContacts.setVisibility(View.VISIBLE);
        loadConversations();
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
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

            // Last message from conversation summary
            String lastMsg = u.getLastMessage();
            h.tvLastMsg.setText(lastMsg != null ? lastMsg : "Tap to start chatting");
            h.tvTime.setText(u.getLastMessageTime() != null ? u.getLastMessageTime() : "");

            // Unread badge
            int unread = u.getUnreadCount();
            if (unread > 0) {
                h.tvUnread.setVisibility(View.VISIBLE);
                h.tvUnread.setText(unread > 9 ? "9+" : String.valueOf(unread));
            } else {
                h.tvUnread.setVisibility(View.GONE);
            }

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
                tvAvatar  = v.findViewById(R.id.tvContactAvatar);
                tvName    = v.findViewById(R.id.tvContactName);
                tvRole    = v.findViewById(R.id.tvContactRole);
                tvLastMsg = v.findViewById(R.id.tvContactLastMessage);
                tvTime    = v.findViewById(R.id.tvContactTime);
                tvUnread  = v.findViewById(R.id.tvContactUnread);
                viewDot   = v.findViewById(R.id.viewOnlineDot);
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Inner Adapter: Messages
    // ─────────────────────────────────────────────
    interface OnMessageLongPress { void onLongPress(Message msg); }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {
        private final List<Message> messages;
        private final String currentUserId;
        private final OnMessageLongPress longPressListener;

        // Day label formatter
        private final SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        private final SimpleDateFormat labelFmt = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

        MessagesAdapter(List<Message> messages, String currentUserId, OnMessageLongPress listener) {
            this.messages = messages;
            this.currentUserId = currentUserId;
            this.longPressListener = listener;
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

            // ── Day divider ──
            boolean showDivider = false;
            if (pos == 0) {
                showDivider = true;
            } else {
                String prevDate = getDateStr(messages.get(pos - 1).getTimestamp());
                String currDate = getDateStr(msg.getTimestamp());
                showDivider = prevDate == null || !prevDate.equals(currDate);
            }
            if (h.tvDayDivider != null) {
                if (showDivider && msg.getTimestamp() != null) {
                    h.tvDayDivider.setVisibility(View.VISIBLE);
                    h.tvDayDivider.setText(getDayLabel(msg.getTimestamp()));
                } else {
                    h.tvDayDivider.setVisibility(View.GONE);
                }
            }

            String content = msg.isDeletedForEveryone()
                    ? "🚫 This message was deleted"
                    : msg.getContent();
            boolean hasAttachment = msg.getAttachmentName() != null && !msg.getAttachmentName().isEmpty();

            if (isSent) {
                h.bubbleSent.setVisibility(View.VISIBLE);
                h.bubbleReceived.setVisibility(View.GONE);

                // Sent text bubble
                if (content != null && !content.isEmpty() && !hasAttachment) {
                    h.tvSentContent.setVisibility(View.VISIBLE);
                    h.tvSentContent.setText(content);
                } else {
                    h.tvSentContent.setVisibility(View.GONE);
                }

                // Sent attachment
                if (hasAttachment && h.layoutSentAttachment != null) {
                    h.layoutSentAttachment.setVisibility(View.VISIBLE);
                    if (h.tvSentFileName != null) h.tvSentFileName.setText(msg.getAttachmentName());
                } else if (h.layoutSentAttachment != null) {
                    h.layoutSentAttachment.setVisibility(View.GONE);
                }

                // Read status
                if (h.tvReadStatus != null) {
                    h.tvReadStatus.setText("READ".equals(msg.getStatus()) ? "✓✓" : "✓");
                    h.tvReadStatus.setTextColor("READ".equals(msg.getStatus()) ? 0xFF3b82f6 : 0xFF94a3b8);
                }

                // Edited label
                if (h.tvSentEdited != null) {
                    h.tvSentEdited.setVisibility(msg.isEdited() ? View.VISIBLE : View.GONE);
                }

                // Avatar initials (sender = you)
                if (h.tvSentAvatar != null) h.tvSentAvatar.setText("Me");

                // Timestamp
                if (h.tvSentTime != null) {
                    h.tvSentTime.setText(formatTime(msg.getTimestamp()));
                }

                h.bubbleSent.setOnLongClickListener(v -> {
                    if (longPressListener != null) longPressListener.onLongPress(msg);
                    return true;
                });

            } else {
                h.bubbleReceived.setVisibility(View.VISIBLE);
                h.bubbleSent.setVisibility(View.GONE);

                // Received text bubble
                if (content != null && !content.isEmpty() && !hasAttachment) {
                    h.tvReceivedContent.setVisibility(View.VISIBLE);
                    h.tvReceivedContent.setText(content);
                } else {
                    h.tvReceivedContent.setVisibility(View.GONE);
                }

                // Received attachment
                if (hasAttachment && h.layoutReceivedAttachment != null) {
                    h.layoutReceivedAttachment.setVisibility(View.VISIBLE);
                    if (h.tvReceivedFileName != null) h.tvReceivedFileName.setText(msg.getAttachmentName());
                } else if (h.layoutReceivedAttachment != null) {
                    h.layoutReceivedAttachment.setVisibility(View.GONE);
                }

                // Avatar initials from sender name
                if (h.tvReceivedAvatar != null) {
                    String senderName = msg.getSenderName() != null ? msg.getSenderName() : "?";
                    String[] parts = senderName.split(" ");
                    StringBuilder initials = new StringBuilder();
                    for (int i = 0; i < Math.min(2, parts.length); i++) {
                        if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
                    }
                    h.tvReceivedAvatar.setText(initials.length() > 0 ? initials.toString().toUpperCase() : "?");
                }

                // Timestamp
                if (h.tvReceivedTime != null) {
                    h.tvReceivedTime.setText(formatTime(msg.getTimestamp()));
                }

                h.bubbleReceived.setOnLongClickListener(v -> {
                    if (longPressListener != null) longPressListener.onLongPress(msg);
                    return true;
                });
            }
        }

        private String getDateStr(String timestamp) {
            if (timestamp == null) return null;
            // If timestamp already has a date portion (ISO format), extract date
            if (timestamp.contains("T")) return timestamp.substring(0, 10);
            // Otherwise return as-is (it's a time-only string)
            return timestamp;
        }

        private String getDayLabel(String timestamp) {
            if (timestamp == null) return "Unknown";
            try {
                if (timestamp.contains("T")) {
                    Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(timestamp);
                    return d != null ? labelFmt.format(d) : timestamp;
                }
            } catch (Exception e) { /* ignore */ }
            // Fallback: just show the raw string
            return timestamp;
        }

        private String formatTime(String timestamp) {
            if (timestamp == null) return "";
            try {
                if (timestamp.contains("T")) {
                    Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(timestamp);
                    return d != null ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d) : "";
                }
            } catch (Exception e) { /* ignore */ }
            return timestamp; // already HH:mm
        }

        @Override
        public int getItemCount() { return messages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout bubbleSent, bubbleReceived;
            LinearLayout layoutSentAttachment, layoutReceivedAttachment;
            TextView tvDayDivider;
            TextView tvSentContent, tvReceivedContent;
            TextView tvSentAvatar, tvReceivedAvatar;
            TextView tvReadStatus, tvSentTime, tvReceivedTime;
            TextView tvSentEdited;
            TextView tvSentFileName, tvReceivedFileName;

            VH(View v) {
                super(v);
                tvDayDivider           = v.findViewById(R.id.tvDayDivider);
                bubbleSent             = v.findViewById(R.id.bubbleSent);
                bubbleReceived         = v.findViewById(R.id.bubbleReceived);
                tvSentContent          = v.findViewById(R.id.tvSentContent);
                tvReceivedContent      = v.findViewById(R.id.tvReceivedContent);
                tvSentAvatar           = v.findViewById(R.id.tvSentAvatar);
                tvReceivedAvatar       = v.findViewById(R.id.tvReceivedAvatar);
                tvReadStatus           = v.findViewById(R.id.tvReadStatus);
                tvSentTime             = v.findViewById(R.id.tvSentTime);
                tvReceivedTime         = v.findViewById(R.id.tvReceivedTime);
                tvSentEdited           = v.findViewById(R.id.tvSentEdited);
                layoutSentAttachment   = v.findViewById(R.id.layoutSentAttachment);
                layoutReceivedAttachment = v.findViewById(R.id.layoutReceivedAttachment);
                tvSentFileName         = v.findViewById(R.id.tvSentFileName);
                tvReceivedFileName     = v.findViewById(R.id.tvReceivedFileName);
            }
        }
    }
}
