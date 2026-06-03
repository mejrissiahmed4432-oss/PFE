package com.medina.app.activities;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Task;
import com.medina.app.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiAssistantBottomSheet extends BottomSheetDialogFragment {

    private LinearLayout layoutAiMessages;
    private ScrollView scrollAiChat;
    private EditText etAiInput;
    private CardView btnAiSend;
    private ImageButton btnCloseAi;
    private CardView cardWelcome;

    private SharedPreferences prefs;
    private boolean isTyping = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                sheet.requestLayout();
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences("medina_prefs", android.content.Context.MODE_PRIVATE);

        layoutAiMessages = view.findViewById(R.id.layoutAiMessages);
        scrollAiChat     = view.findViewById(R.id.scrollAiChat);
        etAiInput        = view.findViewById(R.id.etAiInput);
        btnAiSend        = view.findViewById(R.id.btnAiSend);
        btnCloseAi       = view.findViewById(R.id.btnCloseAi);
        cardWelcome      = view.findViewById(R.id.cardWelcome);

        // Suggestion chips
        view.findViewById(R.id.chipTasks).setOnClickListener(v ->
                processPrompt("Check my pending tasks"));
        view.findViewById(R.id.chipStock).setOnClickListener(v ->
                processPrompt("Check current stock status"));
        view.findViewById(R.id.chipTickets).setOnClickListener(v ->
                processPrompt("Show my open support tickets"));
        view.findViewById(R.id.chipReport).setOnClickListener(v ->
                processPrompt("Generate a daily summary report"));

        btnAiSend.setOnClickListener(v -> {
            String text = etAiInput.getText().toString().trim();
            if (!text.isEmpty()) {
                processPrompt(text);
                etAiInput.setText("");
            }
        });

        etAiInput.setOnEditorActionListener((v, actionId, event) -> {
            String text = etAiInput.getText().toString().trim();
            if (!text.isEmpty()) {
                processPrompt(text);
                etAiInput.setText("");
            }
            return true;
        });

        btnCloseAi.setOnClickListener(v -> dismiss());
    }

    private void processPrompt(String prompt) {
        if (isTyping) return;
        isTyping = true;

        // Hide welcome card after first interaction
        if (cardWelcome.getVisibility() == View.VISIBLE) {
            cardWelcome.setVisibility(View.GONE);
        }

        // Show user bubble
        addBubble(prompt, true);
        scrollToBottom();

        // Show typing indicator
        View typingBubble = addTypingIndicator();

        // Determine response after simulated delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            layoutAiMessages.removeView(typingBubble);
            handleResponse(prompt);
            isTyping = false;
        }, 1200);
    }

    private void handleResponse(String prompt) {
        String lower = prompt.toLowerCase();

        if (lower.contains("task") || lower.contains("pending")) {
            fetchTasksResponse();
        } else if (lower.contains("stock") || lower.contains("inventory") || lower.contains("part")) {
            addBubble("📦 Checking your inventory status...\n\n" +
                    "• Total shelves: 6 active\n" +
                    "• Low stock items: 3 parts below threshold\n" +
                    "• Last restocked: 2 days ago\n\n" +
                    "Tip: Navigate to Parts → Inventory to view full details.", false);
        } else if (lower.contains("ticket") || lower.contains("support")) {
            addBubble("🎫 Support Ticket Overview:\n\n" +
                    "• Open tickets: 4\n" +
                    "• In Progress: 2\n" +
                    "• Awaiting parts: 1\n" +
                    "• Resolved today: 1\n\n" +
                    "Navigate to Support Tickets for full management.", false);
        } else if (lower.contains("report") || lower.contains("summary")) {
            String today = new SimpleDateFormat("EEEE, MMM d yyyy", Locale.getDefault()).format(new Date());
            addBubble("📊 Daily Summary — " + today + "\n\n" +
                    "• Tasks completed: 3 / 7\n" +
                    "• New tickets: 2\n" +
                    "• Alerts: 1 active\n" +
                    "• Messages: check your Messages tab\n\n" +
                    "Overall status: 🟡 Moderate activity. Focus on pending tasks.", false);
        } else if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            String name = prefs.getString("user_name", "Technician");
            addBubble("👋 Hello " + name + "! I'm here to help you navigate MedinaFlux efficiently.\n\n" +
                    "You can ask me about your tasks, stock status, tickets, or request a daily summary!", false);
        } else if (lower.contains("schedule") || lower.contains("calendar")) {
            addBubble("📅 Your schedule overview:\n\n" +
                    "• Tasks due today: Check your Schedule tab\n" +
                    "• Upcoming deadlines: 2 tasks due this week\n\n" +
                    "Navigate to Schedule for the full calendar view.", false);
        } else {
            addBubble("🤖 I understand you're asking about: \"" + prompt + "\"\n\n" +
                    "Currently I can help with:\n" +
                    "• Pending tasks overview\n" +
                    "• Stock & inventory status\n" +
                    "• Support tickets summary\n" +
                    "• Daily reports\n\n" +
                    "Try one of the quick suggestion chips above!", false);
        }
        scrollToBottom();
    }

    private void fetchTasksResponse() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userId = prefs.getString("user_id", null);

        if (userId == null || userId.isEmpty()) {
            ApiClient.getApiService().getCurrentUser().enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String newUserId = response.body().getId();
                        prefs.edit().putString("user_id", newUserId).apply();
                        fetchTasksResponse();
                    } else {
                        addBubble("📋 Could not retrieve tasks — please log in again.", false);
                        scrollToBottom();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    addBubble("📋 Unable to connect to the server. Please check your network.", false);
                    scrollToBottom();
                }
            });
            return;
        }

        ApiClient.getApiService().getTasksByUser(userId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Task> tasks = response.body();
                    int total = tasks.size();
                    int pending = 0, done = 0, inProgress = 0;
                    for (Task t : tasks) {
                        String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                        if (s.contains("DONE") || s.contains("COMPLETED")) done++;
                        else if (s.contains("PROGRESS") || s.contains("ONGOING")) inProgress++;
                        else pending++;
                    }
                    addBubble("📋 Your Task Summary:\n\n" +
                            "• Total tasks: " + total + "\n" +
                            "• Pending: " + pending + "\n" +
                            "• In Progress: " + inProgress + "\n" +
                            "• Completed: " + done + "\n\n" +
                            "Navigate to Schedule to manage your tasks.", false);
                } else {
                    addBubble("📋 Your tasks: Could not load from server. " +
                            "Navigate to Schedule tab to view your tasks directly.", false);
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                addBubble("📋 Unable to connect to the server. " +
                        "Please check your network and try again.", false);
                scrollToBottom();
            }
        });
    }

    private void addBubble(String text, boolean isUser) {
        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin  = dpToPx(6);
        params.bottomMargin = dpToPx(2);

        if (isUser) {
            params.gravity = android.view.Gravity.END;
            params.leftMargin = dpToPx(40);
        } else {
            params.gravity = android.view.Gravity.START;
            params.rightMargin = dpToPx(40);
        }
        bubble.setLayoutParams(params);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setLineSpacing(0, 1.3f);
        tv.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        if (isUser) {
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundResource(R.drawable.bg_button_gradient);
        } else {
            tv.setTextColor(getResources().getColor(R.color.textPrimary));
            tv.setBackgroundResource(R.drawable.bg_card);
        }

        bubble.addView(tv);
        layoutAiMessages.addView(bubble);
    }

    private View addTypingIndicator() {
        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin  = dpToPx(6);
        params.gravity    = android.view.Gravity.START;
        bubble.setLayoutParams(params);

        TextView tv = new TextView(requireContext());
        tv.setText("● ● ●");
        tv.setTextSize(14f);
        tv.setTextColor(getResources().getColor(R.color.textHint));
        tv.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        tv.setBackgroundResource(R.drawable.bg_card);
        bubble.addView(tv);

        layoutAiMessages.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private void scrollToBottom() {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> scrollAiChat.fullScroll(View.FOCUS_DOWN), 150);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
