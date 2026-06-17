package com.medina.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
import com.medina.app.model.AiChatMessage;
import com.medina.app.model.AiConversation;
import com.medina.app.model.AiRequest;
import com.medina.app.model.AiResponse;
import com.medina.app.model.ConversationTurn;
import com.medina.app.model.User;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiAssistantFragment extends Fragment implements
        AiConversationsAdapter.OnConversationClickListener,
        AiMessagesAdapter.OnAiMessageListener {

    private static final int PICK_IMAGE_REQUEST = 101;

    // Panels
    private LinearLayout panelConversations, panelChat;
    private RecyclerView rvConversations, rvAiMessages;

    // Compose Bar
    private EditText etAiCompose;
    private CardView btnSendAiMessage;
    private ImageButton btnAttachImage;

    // Welcome Screen
    private ScrollView scrollWelcome;
    private TextView tvWelcomeMessage;
    private LinearLayout layoutWelcomeSuggestions;

    // Attachment Preview
    private FrameLayout layoutAttachmentPreview;
    private ImageView ivAttachedImage;
    private ImageButton btnRemoveAttachment;

    // Confirmation Modal Overlay
    private RelativeLayout layoutConfirmAction;
    private TextView tvConfirmActionBadge, tvConfirmActionMsg;
    private Button btnConfirmCancel, btnConfirmProceed;

    // Header Actions
    private LinearLayout btnNewChat;
    private ImageButton btnChatBack, btnDeleteConversation;
    private TextView tvChatTitle;
    private LinearLayout layoutConversationsEmpty;

    // Data structures
    private List<AiConversation> conversations = new ArrayList<>();
    private AiConversation activeConversation;
    private AiConversationsAdapter conversationsAdapter;
    private AiMessagesAdapter messagesAdapter;

    private String selectedImageBase64 = null;
    private AiChatMessage pendingConfirmMessage = null;

    private SharedPreferences prefs;
    private String currentUserId;
    private String currentUserRole;
    private String currentUserName;

    private boolean isSending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "unknown");
        currentUserRole = prefs.getString("user_role", "technician");
        currentUserName = prefs.getString("user_name", "Technician");

        // Bind Views
        panelConversations = view.findViewById(R.id.panelConversations);
        panelChat = view.findViewById(R.id.panelChat);
        rvConversations = view.findViewById(R.id.rvConversations);
        rvAiMessages = view.findViewById(R.id.rvAiMessages);
        etAiCompose = view.findViewById(R.id.etAiCompose);
        btnSendAiMessage = view.findViewById(R.id.btnSendAiMessage);
        btnAttachImage = view.findViewById(R.id.btnAttachImage);

        scrollWelcome = view.findViewById(R.id.scrollWelcome);
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        layoutWelcomeSuggestions = view.findViewById(R.id.layoutWelcomeSuggestions);

        layoutAttachmentPreview = view.findViewById(R.id.layoutAttachmentPreview);
        ivAttachedImage = view.findViewById(R.id.ivAttachedImage);
        btnRemoveAttachment = view.findViewById(R.id.btnRemoveAttachment);

        layoutConfirmAction = view.findViewById(R.id.layoutConfirmAction);
        tvConfirmActionBadge = view.findViewById(R.id.tvConfirmActionBadge);
        tvConfirmActionMsg = view.findViewById(R.id.tvConfirmActionMsg);
        btnConfirmCancel = view.findViewById(R.id.btnConfirmCancel);
        btnConfirmProceed = view.findViewById(R.id.btnConfirmProceed);

        btnNewChat = view.findViewById(R.id.btnNewChat);
        btnChatBack = view.findViewById(R.id.btnChatBack);
        btnDeleteConversation = view.findViewById(R.id.btnDeleteConversation);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);
        layoutConversationsEmpty = view.findViewById(R.id.layoutConversationsEmpty);

        // RecyclerView Layout managers
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAiMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set Click Listeners
        btnNewChat.setOnClickListener(v -> startNewConversation());
        btnChatBack.setOnClickListener(v -> showConversationsPanel());
        btnDeleteConversation.setOnClickListener(v -> {
            if (activeConversation != null) {
                deleteConversation(activeConversation);
            }
        });

        btnAttachImage.setOnClickListener(v -> selectImage());
        btnRemoveAttachment.setOnClickListener(v -> clearAttachment());

        btnSendAiMessage.setOnClickListener(v -> {
            String text = etAiCompose.getText().toString().trim();
            if (!text.isEmpty() || selectedImageBase64 != null) {
                sendMessage(text);
            }
        });

        btnConfirmCancel.setOnClickListener(v -> hideConfirmModal());
        btnConfirmProceed.setOnClickListener(v -> confirmAction());

        // Load Welcome specific layout
        setupWelcomeScreen();

        // Load all conversations
        loadConversations();
    }

    private void showConversationsPanel() {
        panelChat.setVisibility(View.GONE);
        panelConversations.setVisibility(View.VISIBLE);
        loadConversations();
    }

    private void setupWelcomeScreen() {
        String role = currentUserRole.toUpperCase();
        String welcomeText;
        List<String> suggestions = new ArrayList<>();

        if ("STOCK_MANAGER".equals(role) || "IT_MANAGER".equals(role)) {
            welcomeText = "Hello! I'm your AI Assistant. I can help you manage inventory, suppliers, equipment, and handle part requests.";
            suggestions.add("Show me low stock items");
            suggestions.add("Add a new laptop Dell XPS");
            suggestions.add("How many suppliers do I have?");
            suggestions.add("Approve pending part requests");
        } else if ("TECHNICIAN".equals(role)) {
            welcomeText = "Hello! I'm your AI Assistant. I can help you with your tickets, spare parts, and maintenance guidance.";
            suggestions.add("Show my assigned tickets");
            suggestions.add("Request a 16GB RAM spare part");
            suggestions.add("How do I replace a laptop battery?");
            suggestions.add("What parts are available for me?");
        } else {
            welcomeText = "Hello! I'm your AI Assistant. I can help you manage your work efficiently.";
            suggestions.add("How many suppliers do I have?");
            suggestions.add("Which is better: i5 or i7?");
            suggestions.add("Show me low stock items");
            suggestions.add("What is my current stock status?");
        }

        tvWelcomeMessage.setText(welcomeText);

        layoutWelcomeSuggestions.removeAllViews();
        for (String suggestion : suggestions) {
            TextView tvSuggestion = new TextView(getContext());
            tvSuggestion.setText(suggestion);
            tvSuggestion.setTextSize(12);
            tvSuggestion.setTextColor(getResources().getColor(R.color.colorPrimary));
            tvSuggestion.setBackgroundResource(R.drawable.bg_badge_installed);
            tvSuggestion.setPadding(24, 12, 24, 12);
            tvSuggestion.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 8, 0, 8);
            tvSuggestion.setLayoutParams(lp);

            tvSuggestion.setOnClickListener(v -> sendMessage(suggestion));

            layoutWelcomeSuggestions.addView(tvSuggestion);
        }
    }

    private void loadConversations() {
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().getAllAiConversations().enqueue(new Callback<List<AiConversation>>() {
            @Override
            public void onResponse(Call<List<AiConversation>> call, Response<List<AiConversation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversations.clear();
                    conversations.addAll(response.body());

                    conversationsAdapter = new AiConversationsAdapter(conversations, AiAssistantFragment.this);
                    rvConversations.setAdapter(conversationsAdapter);

                    if (conversations.isEmpty()) {
                        layoutConversationsEmpty.setVisibility(View.VISIBLE);
                        rvConversations.setVisibility(View.GONE);
                    } else {
                        layoutConversationsEmpty.setVisibility(View.GONE);
                        rvConversations.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AiConversation>> call, Throwable t) {
                Toast.makeText(getContext(), "Could not retrieve history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNewConversation() {
        AiConversation newConv = new AiConversation();
        newConv.setId("conv_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 900 + 100));
        newConv.setTitle("New Conversation");
        newConv.setUserId(currentUserId);
        newConv.setCreatedAt(new Date());
        newConv.setUpdatedAt(new Date());
        newConv.setMessages(new ArrayList<>());

        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().saveAiConversation(newConv).enqueue(new Callback<AiConversation>() {
            @Override
            public void onResponse(Call<AiConversation> call, Response<AiConversation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    loadConversation(response.body());
                } else {
                    // Failover locally
                    loadConversation(newConv);
                }
            }

            @Override
            public void onFailure(Call<AiConversation> call, Throwable t) {
                // Failover locally
                loadConversation(newConv);
            }
        });
    }

    private void loadConversation(AiConversation conversation) {
        activeConversation = conversation;
        tvChatTitle.setText(conversation.getTitle() != null ? conversation.getTitle() : "AI Assistant");

        messagesAdapter = new AiMessagesAdapter(activeConversation.getMessages(), this);
        rvAiMessages.setAdapter(messagesAdapter);

        if (activeConversation.getMessages().isEmpty()) {
            scrollWelcome.setVisibility(View.VISIBLE);
            rvAiMessages.setVisibility(View.GONE);
        } else {
            scrollWelcome.setVisibility(View.GONE);
            rvAiMessages.setVisibility(View.VISIBLE);
            scrollToBottom();
        }

        panelConversations.setVisibility(View.GONE);
        panelChat.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConversationClick(AiConversation conversation) {
        loadConversation(conversation);
    }

    @Override
    public void onConversationDelete(AiConversation conversation) {
        deleteConversation(conversation);
    }

    private void deleteConversation(AiConversation conversation) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().deleteAiConversation(conversation.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                conversations.remove(conversation);
                if (activeConversation != null && activeConversation.getId().equals(conversation.getId())) {
                    showConversationsPanel();
                } else {
                    loadConversations();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Could not delete conversation", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String text) {
        if (isSending || activeConversation == null) return;
        isSending = true;

        String userText = text.trim();
        String imageAttachment = selectedImageBase64;

        // Reset composer
        etAiCompose.setText("");
        clearAttachment();

        // Create UI message
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setId(System.currentTimeMillis());
        userMsg.setSender("user");
        userMsg.setText(userText);
        userMsg.setImageUrl(imageAttachment);
        userMsg.setTimestamp(new Date());

        activeConversation.getMessages().add(userMsg);
        messagesAdapter.notifyItemInserted(activeConversation.getMessages().size() - 1);
        scrollWelcome.setVisibility(View.GONE);
        rvAiMessages.setVisibility(View.VISIBLE);
        scrollToBottom();

        // Auto title
        if ("New Conversation".equalsIgnoreCase(activeConversation.getTitle())) {
            String title = userText.length() > 30 ? userText.substring(0, 30) + "…" : userText;
            if (title.isEmpty()) title = "Image Attachment";
            activeConversation.setTitle(title);
            tvChatTitle.setText(title);
        }

        // Save conversation status
        activeConversation.setUpdatedAt(new Date());
        saveConversationLocal(activeConversation);

        // Build history for payload (last 10 turns)
        List<ConversationTurn> history = new ArrayList<>();
        int msgCount = activeConversation.getMessages().size();
        int startIndex = Math.max(0, msgCount - 11);
        for (int i = startIndex; i < msgCount - 1; i++) {
            AiChatMessage m = activeConversation.getMessages().get(i);
            history.add(new ConversationTurn(m.getSender(), m.getText()));
        }

        // Setup AI typing indicators
        AiChatMessage typingMsg = new AiChatMessage();
        typingMsg.setSender("assistant");
        typingMsg.setText("● ● ●");
        typingMsg.setId(System.currentTimeMillis() + 1);
        activeConversation.getMessages().add(typingMsg);
        int typingIdx = activeConversation.getMessages().size() - 1;
        messagesAdapter.notifyItemInserted(typingIdx);
        scrollToBottom();

        // Perform request
        AiRequest req = new AiRequest();
        req.setUserId(currentUserId);
        req.setRole(currentUserRole);
        req.setMessage(userText.isEmpty() ? "Analyze this image" : userText);
        req.setConversationHistory(history);
        if (imageAttachment != null) {
            req.setImageBase64(imageAttachment.contains(",") ? imageAttachment.split(",")[1] : imageAttachment);
        }

        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().queryAi(req).enqueue(new Callback<AiResponse>() {
            @Override
            public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {
                // Remove typing bubble
                activeConversation.getMessages().remove(typingMsg);
                messagesAdapter.notifyItemRemoved(typingIdx);

                isSending = false;

                if (response.isSuccessful() && response.body() != null) {
                    AiResponse res = response.body();

                    AiChatMessage aiMsg = new AiChatMessage();
                    aiMsg.setId(System.currentTimeMillis());
                    aiMsg.setSender("assistant");
                    aiMsg.setText(res.isSuccess() ? res.getAnswer() : res.getErrorMessage());
                    aiMsg.setError(!res.isSuccess());
                    aiMsg.setTimestamp(new Date());
                    aiMsg.setSuggestions(res.getSuggestions());
                    aiMsg.setData(res.getData());
                    aiMsg.setActionPending(res.isActionPending());
                    aiMsg.setActionType(res.getActionType());
                    aiMsg.setActionPayload(res.getActionPayload());

                    activeConversation.getMessages().add(aiMsg);
                    messagesAdapter.notifyItemInserted(activeConversation.getMessages().size() - 1);
                    saveConversationLocal(activeConversation);
                } else {
                    addErrorMsg("Sorry, I encountered an error communicating with the AI service.");
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<AiResponse> call, Throwable t) {
                activeConversation.getMessages().remove(typingMsg);
                messagesAdapter.notifyItemRemoved(typingIdx);
                isSending = false;

                addErrorMsg("Failed to connect to the AI Server. Please check your connection.");
                scrollToBottom();
            }
        });
    }

    private void addErrorMsg(String text) {
        AiChatMessage errorMsg = new AiChatMessage();
        errorMsg.setId(System.currentTimeMillis());
        errorMsg.setSender("assistant");
        errorMsg.setText(text);
        errorMsg.setError(true);
        errorMsg.setTimestamp(new Date());

        activeConversation.getMessages().add(errorMsg);
        messagesAdapter.notifyItemInserted(activeConversation.getMessages().size() - 1);
        saveConversationLocal(activeConversation);
    }

    private void saveConversationLocal(AiConversation conv) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().saveAiConversation(conv).enqueue(new Callback<AiConversation>() {
            @Override
            public void onResponse(Call<AiConversation> call, Response<AiConversation> response) {}
            @Override
            public void onFailure(Call<AiConversation> call, Throwable t) {}
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream is = requireActivity().getContentResolver().openInputStream(imageUri);
                    byte[] bytes = is != null ? is.readAllBytes() : new byte[0];
                    if (is != null) is.close();

                    ivAttachedImage.setImageURI(imageUri);
                    layoutAttachmentPreview.setVisibility(View.VISIBLE);

                    selectedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void clearAttachment() {
        selectedImageBase64 = null;
        layoutAttachmentPreview.setVisibility(View.GONE);
        ivAttachedImage.setImageDrawable(null);
    }

    private void scrollToBottom() {
        if (messagesAdapter != null && messagesAdapter.getItemCount() > 0) {
            rvAiMessages.smoothScrollToPosition(messagesAdapter.getItemCount() - 1);
        }
    }

    // ── Dialog Action Confirmation ──

    @Override
    public void onSuggestionClick(String suggestion) {
        sendMessage(suggestion);
    }

    @Override
    public void onProceedAction(AiChatMessage message) {
        pendingConfirmMessage = message;
        tvConfirmActionBadge.setText(message.getActionType());
        tvConfirmActionMsg.setText("Are you sure you want to execute " + getActionLabel(message.getActionType()) + "? This cannot be undone.");
        layoutConfirmAction.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCancelAction(AiChatMessage message) {
        if (activeConversation == null) return;
        message.setActionPending(false);

        AiChatMessage cancelMsg = new AiChatMessage();
        cancelMsg.setId(System.currentTimeMillis());
        cancelMsg.setSender("assistant");
        cancelMsg.setText("Action cancelled. Let me know if you need anything else.");
        cancelMsg.setTimestamp(new Date());

        activeConversation.getMessages().add(cancelMsg);
        messagesAdapter.notifyDataSetChanged();
        saveConversationLocal(activeConversation);
        scrollToBottom();
    }

    private void hideConfirmModal() {
        layoutConfirmAction.setVisibility(View.GONE);
        pendingConfirmMessage = null;
    }

    private void confirmAction() {
        if (pendingConfirmMessage == null || activeConversation == null) return;

        layoutConfirmAction.setVisibility(View.GONE);

        // Pre-build API execution body
        Map<String, Object> body = new HashMap<>();
        body.put("userId", currentUserId);
        body.put("role", currentUserRole);
        body.put("actionType", pendingConfirmMessage.getActionType());
        body.put("payload", pendingConfirmMessage.getActionPayload());

        // Remove pending action flag from bubble locally
        pendingConfirmMessage.setActionPending(false);
        messagesAdapter.notifyDataSetChanged();

        // Setup AI typing indicators
        AiChatMessage typingMsg = new AiChatMessage();
        typingMsg.setSender("assistant");
        typingMsg.setText("● ● ●");
        typingMsg.setId(System.currentTimeMillis() + 1);
        activeConversation.getMessages().add(typingMsg);
        int typingIdx = activeConversation.getMessages().size() - 1;
        messagesAdapter.notifyItemInserted(typingIdx);
        scrollToBottom();

        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().executeAiAction(body).enqueue(new Callback<AiResponse>() {
            @Override
            public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {
                // Remove typing bubble
                activeConversation.getMessages().remove(typingMsg);
                messagesAdapter.notifyItemRemoved(typingIdx);

                if (response.isSuccessful() && response.body() != null) {
                    AiResponse res = response.body();

                    AiChatMessage resMsg = new AiChatMessage();
                    resMsg.setId(System.currentTimeMillis());
                    resMsg.setSender("assistant");
                    resMsg.setText(res.getAnswer());
                    resMsg.setError(!res.isSuccess());
                    resMsg.setTimestamp(new Date());

                    activeConversation.getMessages().add(resMsg);
                    messagesAdapter.notifyItemInserted(activeConversation.getMessages().size() - 1);
                    saveConversationLocal(activeConversation);

                    Toast.makeText(getContext(), "Action executed: " + getActionLabel(pendingConfirmMessage.getActionType()), Toast.LENGTH_SHORT).show();
                } else {
                    addErrorMsg("Action execution failed.");
                }
                scrollToBottom();
                pendingConfirmMessage = null;
            }

            @Override
            public void onFailure(Call<AiResponse> call, Throwable t) {
                activeConversation.getMessages().remove(typingMsg);
                messagesAdapter.notifyItemRemoved(typingIdx);

                addErrorMsg("Connection error executing action.");
                scrollToBottom();
                pendingConfirmMessage = null;
            }
        });
    }

    private String getActionLabel(String actionType) {
        if (actionType == null) return "";
        switch (actionType) {
            case "ADD_EQUIPMENT": return "Add Equipment";
            case "UPDATE_EQUIPMENT": return "Update Equipment";
            case "DELETE_EQUIPMENT": return "Delete Equipment";
            case "APPROVE_REQUEST": return "Approve Part Request";
            case "REJECT_REQUEST": return "Reject Part Request";
            case "SUBMIT_PART_REQUEST": return "Submit Part Request";
            case "CREATE_TASK": return "Create Task";
            case "UPDATE_TICKET": return "Update Ticket";
            default: return actionType;
        }
    }
}
