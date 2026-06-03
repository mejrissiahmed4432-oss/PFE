package com.medina.app.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Equipment;
import com.medina.app.model.Ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveWorkbenchActivity extends AppCompatActivity {

    public static class WbAction {
        public String id;
        public String category = "Hardware";
        public String type = "Replace";
        public String priority = "Medium";
        public String target = "";
        public String description = "";
        public String status = "Pending"; // Pending or Done
        public List<Map<String, Object>> resources = new ArrayList<>();
    }

    private SharedPreferences prefs;
    private Ticket ticket;
    private Equipment equipment;
    private List<Equipment> availableInventory = new ArrayList<>();
    private List<WbAction> actionsList = new ArrayList<>();
    private int currentStep = 1;

    // Header Views
    private ImageButton btnWbBack;
    private TextView tvWbHeaderSub;
    private Button btnWbCancel;

    // Stepper Views
    private TextView tvStepCircle1, tvStepCircle2, tvStepCircle3, tvStepCircle4;
    private View stepLine1, stepLine2, stepLine3;

    // Step Containers
    private View scrollStepDiagnosis, scrollStepPlan, layoutStepResources, scrollStepExecution;
    private Button btnWbPrev, btnWbNext;

    // Step 1: Diagnosis Views
    private TextView tvWbProblemDesc;
    private Button btnWbAiTabDiagnosis, btnWbAiTabPredictive, btnWbGenerateAi, btnWbUseAiFindings;
    private ProgressBar progressWbAi;
    private View layoutWbAiDiagnosisResult, layoutWbAiPredictiveResult;
    private EditText etWbManualObs, etWbDiagResult;
    private boolean activeAiTabDiagnosis = true;
    private boolean aiResultGenerated = false;

    // Premium Diagnosis Views
    private TextView tvWbCauseName1, tvWbCauseName2, tvWbCauseName3;
    private TextView tvWbCauseConf1, tvWbCauseConf2, tvWbCauseConf3;
    private CheckBox cbWbCause1, cbWbCause2, cbWbCause3;

    private TextView tvWbPredName1, tvWbPredName2, tvWbPredName3;
    private TextView tvWbPredVal1, tvWbPredVal2, tvWbPredVal3;
    private ProgressBar pbWbPred1, pbWbPred2, pbWbPred3;

    private View layoutWbSimilarCasesBlock;
    private TextView tvWbSimilarId1, tvWbSimilarId2;
    private TextView tvWbSimilarMatch1, tvWbSimilarMatch2;
    private TextView tvWbSimilarDesc1, tvWbSimilarDesc2;

    // Step 2: Plan Views
    private Button btnWbGeneratePlan, btnWbAddAction;
    private LinearLayout layoutWbActionsContainer;
    private int actionIndexCounter = 1;

    // Step 3: Resources Views
    private Spinner spWbResourceActions;
    private EditText etWbResourceSearch;
    private LinearLayout layoutWbPartsInventory, layoutWbAssignedResources;
    private TextView tvWbAssignedEmpty;
    private String currentResourceActionId = "";
    private String resourceSearchQuery = "";

    // Step 4: Execution Views
    private LinearLayout layoutWbExecutionChecklist;
    private EditText etWbExecutionNotes;
    private CheckBox cbWbIsBroken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_workbench);
        prefs = getSharedPreferences("medina_prefs", MODE_PRIVATE);

        // Fetch Ticket details
        String ticketId = getIntent().getStringExtra("ticket_id");
        if (ticketId == null) {
            Toast.makeText(this, "No ticket specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        loadTicketAndEquipment(ticketId);
        setupStepNavigation();
        setupDiagnosisStep();
        setupPlanStep();
        setupResourcesStep();
        setupExecutionStep();
    }

    private void bindViews() {
        btnWbBack = findViewById(R.id.btnWbBack);
        tvWbHeaderSub = findViewById(R.id.tvWbHeaderSub);
        btnWbCancel = findViewById(R.id.btnWbCancel);

        tvStepCircle1 = findViewById(R.id.tvStepCircle1);
        tvStepCircle2 = findViewById(R.id.tvStepCircle2);
        tvStepCircle3 = findViewById(R.id.tvStepCircle3);
        tvStepCircle4 = findViewById(R.id.tvStepCircle4);
        stepLine1 = findViewById(R.id.stepLine1);
        stepLine2 = findViewById(R.id.stepLine2);
        stepLine3 = findViewById(R.id.stepLine3);

        scrollStepDiagnosis = findViewById(R.id.scrollStepDiagnosis);
        scrollStepPlan = findViewById(R.id.scrollStepPlan);
        layoutStepResources = findViewById(R.id.layoutStepResources);
        scrollStepExecution = findViewById(R.id.scrollStepExecution);
        btnWbPrev = findViewById(R.id.btnWbPrev);
        btnWbNext = findViewById(R.id.btnWbNext);

        tvWbProblemDesc = findViewById(R.id.tvWbProblemDesc);
        btnWbAiTabDiagnosis = findViewById(R.id.btnWbAiTabDiagnosis);
        btnWbAiTabPredictive = findViewById(R.id.btnWbAiTabPredictive);
        btnWbGenerateAi = findViewById(R.id.btnWbGenerateAi);
        btnWbUseAiFindings = findViewById(R.id.btnWbUseAiFindings);
        progressWbAi = findViewById(R.id.progressWbAi);
        layoutWbAiDiagnosisResult = findViewById(R.id.layoutWbAiDiagnosisResult);
        layoutWbAiPredictiveResult = findViewById(R.id.layoutWbAiPredictiveResult);
        tvWbCauseName1 = findViewById(R.id.tvWbCauseName1);
        tvWbCauseName2 = findViewById(R.id.tvWbCauseName2);
        tvWbCauseName3 = findViewById(R.id.tvWbCauseName3);
        tvWbCauseConf1 = findViewById(R.id.tvWbCauseConf1);
        tvWbCauseConf2 = findViewById(R.id.tvWbCauseConf2);
        tvWbCauseConf3 = findViewById(R.id.tvWbCauseConf3);
        cbWbCause1 = findViewById(R.id.cbWbCause1);
        cbWbCause2 = findViewById(R.id.cbWbCause2);
        cbWbCause3 = findViewById(R.id.cbWbCause3);

        tvWbPredName1 = findViewById(R.id.tvWbPredName1);
        tvWbPredName2 = findViewById(R.id.tvWbPredName2);
        tvWbPredName3 = findViewById(R.id.tvWbPredName3);
        tvWbPredVal1 = findViewById(R.id.tvWbPredVal1);
        tvWbPredVal2 = findViewById(R.id.tvWbPredVal2);
        tvWbPredVal3 = findViewById(R.id.tvWbPredVal3);
        pbWbPred1 = findViewById(R.id.pbWbPred1);
        pbWbPred2 = findViewById(R.id.pbWbPred2);
        pbWbPred3 = findViewById(R.id.pbWbPred3);

        layoutWbSimilarCasesBlock = findViewById(R.id.layoutWbSimilarCasesBlock);
        tvWbSimilarId1 = findViewById(R.id.tvWbSimilarId1);
        tvWbSimilarId2 = findViewById(R.id.tvWbSimilarId2);
        tvWbSimilarMatch1 = findViewById(R.id.tvWbSimilarMatch1);
        tvWbSimilarMatch2 = findViewById(R.id.tvWbSimilarMatch2);
        tvWbSimilarDesc1 = findViewById(R.id.tvWbSimilarDesc1);
        tvWbSimilarDesc2 = findViewById(R.id.tvWbSimilarDesc2);

        etWbManualObs = findViewById(R.id.etWbManualObs);
        etWbDiagResult = findViewById(R.id.etWbDiagResult);

        btnWbGeneratePlan = findViewById(R.id.btnWbGeneratePlan);
        btnWbAddAction = findViewById(R.id.btnWbAddAction);
        layoutWbActionsContainer = findViewById(R.id.layoutWbActionsContainer);

        spWbResourceActions = findViewById(R.id.spWbResourceActions);
        etWbResourceSearch = findViewById(R.id.etWbResourceSearch);
        layoutWbPartsInventory = findViewById(R.id.layoutWbPartsInventory);
        layoutWbAssignedResources = findViewById(R.id.layoutWbAssignedResources);
        tvWbAssignedEmpty = findViewById(R.id.tvWbAssignedEmpty);

        layoutWbExecutionChecklist = findViewById(R.id.layoutWbExecutionChecklist);
        etWbExecutionNotes = findViewById(R.id.etWbExecutionNotes);
        cbWbIsBroken = findViewById(R.id.cbWbIsBroken);

        btnWbBack.setOnClickListener(v -> finish());
        btnWbCancel.setOnClickListener(v -> showCancelConfirmDialog());
    }

    private void loadTicketAndEquipment(String ticketId) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().getTicketById(ticketId).enqueue(new Callback<Ticket>() {
            @Override
            public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ticket = response.body();
                    tvWbHeaderSub.setText("Ticket #" + ticket.getTicketNumber() + " • " + (ticket.getEquipmentName() != null ? ticket.getEquipmentName() : "Unknown Equipment"));
                    tvWbProblemDesc.setText(ticket.getDescription() != null ? ticket.getDescription() : "No description provided.");
                    loadEquipmentAndInventory();
                } else {
                    Toast.makeText(LiveWorkbenchActivity.this, "Failed to load ticket details", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Ticket> call, Throwable t) {
                Toast.makeText(LiveWorkbenchActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadEquipmentAndInventory() {
        if (ticket.getEquipmentId() != null) {
            ApiClient.getApiService().getEquipmentById(ticket.getEquipmentId()).enqueue(new Callback<Equipment>() {
                @Override
                public void onResponse(Call<Equipment> call, Response<Equipment> response) {
                    if (response.isSuccessful()) {
                        equipment = response.body();
                    }
                }
                @Override
                public void onFailure(Call<Equipment> call, Throwable t) {}
            });
        }

        // Fetch central inventory items for Step 3 resources
        ApiClient.getApiService().getAllEquipment().enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    availableInventory.clear();
                    // Keep only items that are "Available" or categorized as parts
                    for (Equipment e : response.body()) {
                        String st = e.getStatus() != null ? e.getStatus() : "";
                        if (st.equalsIgnoreCase("Available") || e.getQte() != null && e.getQte() > 0) {
                            availableInventory.add(e);
                        }
                    }
                    populateResourcesInventory();
                }
            }
            @Override
            public void onFailure(Call<List<Equipment>> call, Throwable t) {}
        });
    }

    // ─────────────────────────────────────────────
    //  STEP NAVIGATION & TRANSITIONS
    // ─────────────────────────────────────────────
    private void setupStepNavigation() {
        btnWbPrev.setOnClickListener(v -> navigateToStep(currentStep - 1));
        btnWbNext.setOnClickListener(v -> {
            if (validateStep(currentStep)) {
                if (currentStep < 4) {
                    navigateToStep(currentStep + 1);
                } else {
                    finalizeWorkbenchTicket();
                }
            }
        });
    }

    private void navigateToStep(int step) {
        currentStep = step;

        // Toggle Step Views
        scrollStepDiagnosis.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        scrollStepPlan.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStepResources.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        scrollStepExecution.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        // Toggle Bottom Buttons
        btnWbPrev.setVisibility(step > 1 ? View.VISIBLE : View.GONE);
        btnWbNext.setText(step == 4 ? "Complete Ticket" : "Next Step");

        updateStepperUI();

        // Specific actions on step enter
        if (step == 3) {
            setupStep3ActionSpinner();
        } else if (step == 4) {
            populateExecutionChecklist();
        }
    }

    private void updateStepperUI() {
        int activeColor = getResources().getColor(R.color.colorPrimary);
        int inactiveColor = getResources().getColor(R.color.divider);

        // Reset circles backgrounds
        tvStepCircle1.setBackgroundResource(currentStep >= 1 ? R.drawable.bg_tab_active : R.drawable.bg_card);
        tvStepCircle1.setTextColor(currentStep >= 1 ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        tvStepCircle2.setBackgroundResource(currentStep >= 2 ? R.drawable.bg_tab_active : R.drawable.bg_card);
        tvStepCircle2.setTextColor(currentStep >= 2 ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        tvStepCircle3.setBackgroundResource(currentStep >= 3 ? R.drawable.bg_tab_active : R.drawable.bg_card);
        tvStepCircle3.setTextColor(currentStep >= 3 ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        tvStepCircle4.setBackgroundResource(currentStep >= 4 ? R.drawable.bg_tab_active : R.drawable.bg_card);
        tvStepCircle4.setTextColor(currentStep >= 4 ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        // Connectors lines
        stepLine1.setBackgroundColor(currentStep >= 2 ? activeColor : inactiveColor);
        stepLine2.setBackgroundColor(currentStep >= 3 ? activeColor : inactiveColor);
        stepLine3.setBackgroundColor(currentStep >= 4 ? activeColor : inactiveColor);
    }

    private boolean validateStep(int step) {
        if (step == 1) {
            String result = etWbDiagResult.getText().toString().trim();
            if (result.length() < 3) {
                etWbDiagResult.setError("Please enter a diagnosis conclusion of at least 3 characters");
                Toast.makeText(this, "Diagnosis conclusion is required.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (step == 2) {
            if (actionsList.isEmpty()) {
                Toast.makeText(this, "Please plan at least one technical action before proceeding.", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Sync values from custom action view elements into model
            syncActionsFromUI();
            for (WbAction a : actionsList) {
                if (a.target.trim().isEmpty()) {
                    Toast.makeText(this, "Target elements cannot be empty.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } else if (step == 4) {
            for (WbAction a : actionsList) {
                if (!a.status.equalsIgnoreCase("Done")) {
                    Toast.makeText(this, "All planned actions must be completed (Done) to proceed.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            String notes = etWbExecutionNotes.getText().toString().trim();
            if (notes.isEmpty()) {
                etWbExecutionNotes.setError("Work notes are required to complete the ticket");
                Toast.makeText(this, "Please enter work notes.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    // ─────────────────────────────────────────────
    //  STEP 1: DIAGNOSIS (AI ASSISTANT)
    // ─────────────────────────────────────────────
    private void setupDiagnosisStep() {
        btnWbAiTabDiagnosis.setOnClickListener(v -> toggleAiTabs(true));
        btnWbAiTabPredictive.setOnClickListener(v -> toggleAiTabs(false));

        btnWbGenerateAi.setOnClickListener(v -> {
            btnWbGenerateAi.setVisibility(View.GONE);
            progressWbAi.setVisibility(View.VISIBLE);
            layoutWbAiDiagnosisResult.setVisibility(View.GONE);
            layoutWbAiPredictiveResult.setVisibility(View.GONE);
            layoutWbSimilarCasesBlock.setVisibility(View.GONE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                progressWbAi.setVisibility(View.GONE);
                btnWbGenerateAi.setVisibility(View.VISIBLE);
                aiResultGenerated = true;
                showAiDiagnosisResults();
            }, 1500);
        });

        btnWbUseAiFindings.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder("AI Diagnostic Findings Applied:\n");
            if (cbWbCause1.isChecked()) {
                sb.append("- ").append(tvWbCauseName1.getText().toString()).append("\n");
            }
            if (cbWbCause2.isChecked()) {
                sb.append("- ").append(tvWbCauseName2.getText().toString()).append("\n");
            }
            if (cbWbCause3.isChecked()) {
                sb.append("- ").append(tvWbCauseName3.getText().toString()).append("\n");
            }
            etWbDiagResult.setText(sb.toString().trim());
            Toast.makeText(this, "Checked findings applied to conclusion!", Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleAiTabs(boolean diagnosisTab) {
        activeAiTabDiagnosis = diagnosisTab;

        btnWbAiTabDiagnosis.setBackgroundTintList(android.content.res.ColorStateList.valueOf(diagnosisTab ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.inputBackground)));
        btnWbAiTabDiagnosis.setTextColor(diagnosisTab ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        btnWbAiTabPredictive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(!diagnosisTab ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.inputBackground)));
        btnWbAiTabPredictive.setTextColor(!diagnosisTab ? Color.WHITE : getResources().getColor(R.color.textSecondary));

        showAiDiagnosisResults();
    }

    private void showAiDiagnosisResults() {
        if (!aiResultGenerated) return;

        String cat = ticket.getCategory() != null ? ticket.getCategory() : "Hardware";

        if (activeAiTabDiagnosis) {
            layoutWbAiDiagnosisResult.setVisibility(View.VISIBLE);
            layoutWbAiPredictiveResult.setVisibility(View.GONE);
            layoutWbSimilarCasesBlock.setVisibility(View.VISIBLE);

            if (cat.equalsIgnoreCase("Software")) {
                tvWbCauseName1.setText("Registry configuration corrupt");
                tvWbCauseConf1.setText("92%");
                tvWbCauseConf1.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause1.setChecked(true);

                tvWbCauseName2.setText("Chipset driver incompatibility");
                tvWbCauseConf2.setText("75%");
                tvWbCauseConf2.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause2.setChecked(true);

                tvWbCauseName3.setText("BIOS firmware version outdated");
                tvWbCauseConf3.setText("45%");
                tvWbCauseConf3.setBackgroundResource(R.drawable.bg_badge_maintenance);
                cbWbCause3.setChecked(false);

                tvWbSimilarId1.setText("TKT-0824");
                tvWbSimilarMatch1.setText("90% Match");
                tvWbSimilarDesc1.setText("Defective OS boot sector configuration. Resolution: Rebuilt partition schema & registry entries.");

                tvWbSimilarId2.setText("TKT-0751");
                tvWbSimilarMatch2.setText("70% Match");
                tvWbSimilarDesc2.setText("System driver conflicts causing BSOD. Resolution: Clean-installed chipset firmware.");
            } else if (cat.equalsIgnoreCase("Network")) {
                tvWbCauseName1.setText("Subnet VLAN / IP conflict");
                tvWbCauseConf1.setText("90%");
                tvWbCauseConf1.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause1.setChecked(true);

                tvWbCauseName2.setText("Inoperable physical RJ45 connection");
                tvWbCauseConf2.setText("80%");
                tvWbCauseConf2.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause2.setChecked(true);

                tvWbCauseName3.setText("High router network latency peaks");
                tvWbCauseConf3.setText("55%");
                tvWbCauseConf3.setBackgroundResource(R.drawable.bg_badge_maintenance);
                cbWbCause3.setChecked(false);

                tvWbSimilarId1.setText("TKT-0931");
                tvWbSimilarMatch1.setText("95% Match");
                tvWbSimilarDesc1.setText("Network packet loops in office switch. Resolution: Configured VLAN partitioning.");

                tvWbSimilarId2.setText("TKT-0872");
                tvWbSimilarMatch2.setText("80% Match");
                tvWbSimilarDesc2.setText("Faulty wall Ethernet jack. Resolution: Recrimped connections.");
            } else {
                tvWbCauseName1.setText("Cooling fan mechanical failure");
                tvWbCauseConf1.setText("95%");
                tvWbCauseConf1.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause1.setChecked(true);

                tvWbCauseName2.setText("Thermal paste dry degradation");
                tvWbCauseConf2.setText("80%");
                tvWbCauseConf2.setBackgroundResource(R.drawable.bg_badge_red);
                cbWbCause2.setChecked(true);

                tvWbCauseName3.setText("Vents dust clog obstruction");
                tvWbCauseConf3.setText("50%");
                tvWbCauseConf3.setBackgroundResource(R.drawable.bg_badge_maintenance);
                cbWbCause3.setChecked(false);

                tvWbSimilarId1.setText("TKT-1002");
                tvWbSimilarMatch1.setText("95% Match");
                tvWbSimilarDesc1.setText("CPU Overheating shutoff. Resolution: Replaced thermal paste & CPU cooler fan.");

                tvWbSimilarId2.setText("TKT-0985");
                tvWbSimilarMatch2.setText("80% Match");
                tvWbSimilarDesc2.setText("System shutdown during backup. Vents were clogged with dust. Resolution: Cleaned computer interior with compressed air.");
            }
        } else {
            layoutWbAiDiagnosisResult.setVisibility(View.GONE);
            layoutWbAiPredictiveResult.setVisibility(View.VISIBLE);
            layoutWbSimilarCasesBlock.setVisibility(View.GONE);

            if (cat.equalsIgnoreCase("Software")) {
                tvWbPredName1.setText("Registry Access Loop");
                tvWbPredVal1.setText("90%");
                pbWbPred1.setProgress(90);
                pbWbPred1.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName2.setText("Kernel Panic Shutdown");
                tvWbPredVal2.setText("40%");
                pbWbPred2.setProgress(40);
                pbWbPred2.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName3.setText("System Files Corruption");
                tvWbPredVal3.setText("15%");
                pbWbPred3.setProgress(15);
                pbWbPred3.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.textSecondary)));
            } else if (cat.equalsIgnoreCase("Network")) {
                tvWbPredName1.setText("Network Packet Drops");
                tvWbPredVal1.setText("85%");
                pbWbPred1.setProgress(85);
                pbWbPred1.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName2.setText("Switch port lockouts");
                tvWbPredVal2.setText("30%");
                pbWbPred2.setProgress(30);
                pbWbPred2.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName3.setText("Router Buffer Overflows");
                tvWbPredVal3.setText("20%");
                pbWbPred3.setProgress(20);
                pbWbPred3.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.textSecondary)));
            } else {
                tvWbPredName1.setText("Processor fan lock");
                tvWbPredVal1.setText("95%");
                pbWbPred1.setProgress(95);
                pbWbPred1.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName2.setText("Core overheat cutoff");
                tvWbPredVal2.setText("80%");
                pbWbPred2.setProgress(80);
                pbWbPred2.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                tvWbPredName3.setText("Mainboard circuit short");
                tvWbPredVal3.setText("10%");
                pbWbPred3.setProgress(10);
                pbWbPred3.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.textSecondary)));
            }
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 2: PLAN ACTIONS
    // ─────────────────────────────────────────────
    private void setupPlanStep() {
        btnWbGeneratePlan.setOnClickListener(v -> {
            // Generate standard steps depending on category
            actionsList.clear();
            layoutWbActionsContainer.removeAllViews();
            actionIndexCounter = 1;

            String cat = ticket.getCategory() != null ? ticket.getCategory() : "Hardware";
            if (cat.equalsIgnoreCase("Software")) {
                addPreplannedAction("Software", "Update", "High", "Operating System", "Patch security files and refresh OS system files");
                addPreplannedAction("Software", "Install", "Medium", "System Drivers", "Install official manufacturer chipset drivers");
            } else if (cat.equalsIgnoreCase("Network")) {
                addPreplannedAction("Network", "Configure", "High", "Router VLAN", "Rebuild subnet allocations and router IP paths");
                addPreplannedAction("Network", "Test", "Medium", "Ethernet Cable", "Check line latency and physical connector integrity");
            } else {
                addPreplannedAction("Hardware", "Replace", "High", "Cooling Fan", "Replace defective system cooling fan component");
                addPreplannedAction("Maintenance", "Clean", "Medium", "Cooling System", "Clean dust and apply premium thermal paste compound");
            }
            Toast.makeText(this, "AI Technical Plan generated successfully!", Toast.LENGTH_SHORT).show();
        });

        btnWbAddAction.setOnClickListener(v -> addBlankActionCard());
    }

    private void addPreplannedAction(String cat, String type, String priority, String target, String desc) {
        WbAction action = new WbAction();
        action.id = "act_" + System.currentTimeMillis() + "_" + actionIndexCounter;
        action.category = cat;
        action.type = type;
        action.priority = priority;
        action.target = target;
        action.description = desc;

        actionsList.add(action);
        inflateActionCard(action);
        actionIndexCounter++;
    }

    private void addBlankActionCard() {
        WbAction action = new WbAction();
        action.id = "act_" + System.currentTimeMillis() + "_" + actionIndexCounter;
        actionsList.add(action);
        inflateActionCard(action);
        actionIndexCounter++;
    }

    private void inflateActionCard(WbAction action) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_wb_action, layoutWbActionsContainer, false);
        card.setTag(action.id);

        TextView tvIndex = card.findViewById(R.id.tvWbActionIndex);
        tvIndex.setText("Action #" + actionIndexCounter);

        ImageButton btnDelete = card.findViewById(R.id.btnWbActionDelete);
        btnDelete.setOnClickListener(v -> {
            layoutWbActionsContainer.removeView(card);
            actionsList.remove(action);
            reorderActionIndexLabels();
        });

        // Set Spinners Content
        Spinner spCategory = card.findViewById(R.id.spWbActionCategory);
        String[] categories = {"Hardware", "Software", "Network", "Maintenance", "Other"};
        spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories));
        ((ArrayAdapter<?>) spCategory.getAdapter()).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spType = card.findViewById(R.id.spWbActionType);
        String[] types = {"Replace", "Install", "Remove", "Repair", "Configure", "Update", "Clean", "Test", "Inspect"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types));
        ((ArrayAdapter<?>) spType.getAdapter()).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spPriority = card.findViewById(R.id.spWbActionPriority);
        String[] priorities = {"Low", "Medium", "High", "Critical"};
        spPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities));
        ((ArrayAdapter<?>) spPriority.getAdapter()).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Prepopulate fields if action contains preplanned values
        EditText etTarget = card.findViewById(R.id.etWbActionTarget);
        EditText etDesc = card.findViewById(R.id.etWbActionDesc);

        etTarget.setText(action.target);
        etDesc.setText(action.description);

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(action.category)) {
                spCategory.setSelection(i);
                break;
            }
        }

        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(action.type)) {
                spType.setSelection(i);
                break;
            }
        }

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equalsIgnoreCase(action.priority)) {
                spPriority.setSelection(i);
                break;
            }
        }

        layoutWbActionsContainer.addView(card);
    }

    private void reorderActionIndexLabels() {
        int childCount = layoutWbActionsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View card = layoutWbActionsContainer.getChildAt(i);
            TextView tvIndex = card.findViewById(R.id.tvWbActionIndex);
            if (tvIndex != null) {
                tvIndex.setText("Action #" + (i + 1));
            }
        }
    }

    private void syncActionsFromUI() {
        int childCount = layoutWbActionsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View card = layoutWbActionsContainer.getChildAt(i);
            String id = (String) card.getTag();

            WbAction act = null;
            for (WbAction a : actionsList) {
                if (a.id.equals(id)) {
                    act = a;
                    break;
                }
            }

            if (act != null) {
                Spinner spCategory = card.findViewById(R.id.spWbActionCategory);
                Spinner spType = card.findViewById(R.id.spWbActionType);
                Spinner spPriority = card.findViewById(R.id.spWbActionPriority);
                EditText etTarget = card.findViewById(R.id.etWbActionTarget);
                EditText etDesc = card.findViewById(R.id.etWbActionDesc);

                act.category = spCategory.getSelectedItem().toString();
                act.type = spType.getSelectedItem().toString();
                act.priority = spPriority.getSelectedItem().toString();
                act.target = etTarget.getText().toString().trim();
                act.description = etDesc.getText().toString().trim();
            }
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 3: RESOURCES ALLOCATION
    // ─────────────────────────────────────────────
    private void setupResourcesStep() {
        etWbResourceSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                resourceSearchQuery = s.toString().trim();
                populateResourcesInventory();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        spWbResourceActions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < actionsList.size()) {
                    currentResourceActionId = actionsList.get(position).id;
                    refreshAssignedResourcesList();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStep3ActionSpinner() {
        syncActionsFromUI();
        List<String> options = new ArrayList<>();
        for (WbAction a : actionsList) {
            options.add(a.type + " " + a.target + " (" + a.category + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWbResourceActions.setAdapter(adapter);

        if (!actionsList.isEmpty()) {
            spWbResourceActions.setSelection(0);
            currentResourceActionId = actionsList.get(0).id;
            refreshAssignedResourcesList();
        }
    }

    private void populateResourcesInventory() {
        layoutWbPartsInventory.removeAllViews();
        String q = resourceSearchQuery.toLowerCase();

        for (Equipment eq : availableInventory) {
            String name = eq.getEquipmentName() != null ? eq.getEquipmentName() : "";
            String brand = eq.getBrand() != null ? eq.getBrand() : "";
            if (!q.isEmpty() && !name.toLowerCase().contains(q) && !brand.toLowerCase().contains(q)) {
                continue;
            }

            View row = LayoutInflater.from(this).inflate(R.layout.item_wb_resource, layoutWbPartsInventory, false);
            TextView tvName = row.findViewById(R.id.tvWbResPartName);
            TextView tvMeta = row.findViewById(R.id.tvWbResPartMeta);
            TextView tvStock = row.findViewById(R.id.tvWbResPartStock);
            Button btnAdd = row.findViewById(R.id.btnWbResAdd);

            tvName.setText(eq.getEquipmentName());
            tvMeta.setText("Brand: " + (eq.getBrand() != null ? eq.getBrand() : "—") + " • S/N: " + (eq.getSerialNumber() != null ? eq.getSerialNumber() : "—"));
            tvStock.setText("Stock: " + (eq.getQte() != null ? eq.getQte() : 1));

            btnAdd.setOnClickListener(v -> allocatePartToAction(eq));

            layoutWbPartsInventory.addView(row);
        }
    }

    private void allocatePartToAction(Equipment eq) {
        if (currentResourceActionId.isEmpty()) {
            Toast.makeText(this, "Please select an action first.", Toast.LENGTH_SHORT).show();
            return;
        }

        WbAction selectedAction = null;
        for (WbAction a : actionsList) {
            if (a.id.equals(currentResourceActionId)) {
                selectedAction = a;
                break;
            }
        }

        if (selectedAction == null) return;

        // Check if already assigned
        boolean alreadyAssigned = false;
        for (Map<String, Object> res : selectedAction.resources) {
            if (eq.getId().equals(res.get("equipmentId"))) {
                alreadyAssigned = true;
                break;
            }
        }

        if (alreadyAssigned) {
            Toast.makeText(this, "This part is already assigned to this action.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add resource allocation
        Map<String, Object> allocation = new HashMap<>();
        allocation.put("equipmentId", eq.getId());
        allocation.put("name", eq.getEquipmentName());
        allocation.put("resourceType", "Hardware");
        allocation.put("qty", 1);
        allocation.put("specification", eq.getSpecifications() != null ? eq.getSpecifications().toString() : "");

        selectedAction.resources.add(allocation);
        refreshAssignedResourcesList();
        Toast.makeText(this, eq.getEquipmentName() + " assigned to action.", Toast.LENGTH_SHORT).show();
    }

    private void refreshAssignedResourcesList() {
        layoutWbAssignedResources.removeAllViews();
        tvWbAssignedEmpty.setVisibility(View.GONE);

        WbAction selectedAction = null;
        for (WbAction a : actionsList) {
            if (a.id.equals(currentResourceActionId)) {
                selectedAction = a;
                break;
            }
        }

        if (selectedAction == null || selectedAction.resources.isEmpty()) {
            tvWbAssignedEmpty.setVisibility(View.VISIBLE);
            layoutWbAssignedResources.addView(tvWbAssignedEmpty);
            return;
        }

        final WbAction finalAction = selectedAction;

        for (int i = 0; i < finalAction.resources.size(); i++) {
            final int index = i;
            Map<String, Object> res = finalAction.resources.get(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_wb_assigned, layoutWbAssignedResources, false);

            TextView tvName = row.findViewById(R.id.tvWbAssignedName);
            TextView tvType = row.findViewById(R.id.tvWbAssignedType);
            ImageButton btnRemove = row.findViewById(R.id.btnWbAssignedRemove);

            tvName.setText((String) res.get("name"));
            tvType.setText("Hardware x" + res.get("qty"));

            btnRemove.setOnClickListener(v -> {
                finalAction.resources.remove(index);
                refreshAssignedResourcesList();
            });

            layoutWbAssignedResources.addView(row);
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 4: EXECUTION
    // ─────────────────────────────────────────────
    private void setupExecutionStep() {}

    private void populateExecutionChecklist() {
        layoutWbExecutionChecklist.removeAllViews();

        for (WbAction action : actionsList) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_wb_execution_action, layoutWbExecutionChecklist, false);
            CheckBox cb = item.findViewById(R.id.cbWbExecAction);
            cb.setText(action.type + " " + action.target + " (" + action.category + ")");
            cb.setChecked(action.status.equalsIgnoreCase("Done"));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                action.status = isChecked ? "Done" : "Pending";
            });

            layoutWbExecutionChecklist.addView(item);
        }
    }

    // ─────────────────────────────────────────────
    //  FINALIZE AND SUBMIT
    // ─────────────────────────────────────────────
    private void finalizeWorkbenchTicket() {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Completing technical task...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Prepare ticket update payload
        ticket.setStatus(cbWbIsBroken.isChecked() ? "Unrepairable" : "Completed");
        ticket.setWorkNote(etWbExecutionNotes.getText().toString().trim());

        // Map actions to task logs
        List<Map<String, Object>> tasksMapped = new ArrayList<>();
        for (WbAction act : actionsList) {
            Map<String, Object> tm = new HashMap<>();
            tm.put("category", act.category);
            tm.put("type", act.type);
            tm.put("priority", act.priority);
            tm.put("target", act.target);
            tm.put("description", act.description);
            tm.put("status", act.status);
            tasksMapped.add(tm);
        }
        // Save execution list in dynamic map fields (technician schema supports custom arrays)
        ticket.setRepairTasks(tasksMapped);

        // Gather all parts used
        List<Map<String, Object>> consumedParts = new ArrayList<>();
        for (WbAction act : actionsList) {
            for (Map<String, Object> res : act.resources) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", res.get("name"));
                item.put("qty", res.get("qty"));
                item.put("specification", res.get("specification"));
                item.put("assignedToEquipmentName", ticket.getEquipmentName());
                item.put("assignedToEquipmentId", ticket.getEquipmentId());
                consumedParts.add(item);
            }
        }
        ticket.setPartsUsed(consumedParts);

        ApiClient.authToken = prefs.getString("auth_token", null);
        String currentTechId = prefs.getString("user_id", "");

        // 1. Update Ticket on Backend
        ApiClient.getApiService().updateTicket(ticket.getId(), ticket).enqueue(new Callback<Ticket>() {
            @Override
            public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                if (response.isSuccessful()) {
                    // 2. Consume allocated parts
                    if (!consumedParts.isEmpty()) {
                        ApiClient.getApiService().consumeParts(currentTechId, consumedParts).enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
                    }

                    // 3. Update Equipment status
                    if (equipment != null) {
                        equipment.setStatus(cbWbIsBroken.isChecked() ? "Broken" : "Available");
                        ApiClient.getApiService().updateEquipment(equipment.getId(), equipment).enqueue(new Callback<Equipment>() {
                            @Override
                            public void onResponse(Call<Equipment> call, Response<Equipment> response) {
                                progressDialog.dismiss();
                                Toast.makeText(LiveWorkbenchActivity.this, "Ticket completed successfully!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }

                            @Override
                            public void onFailure(Call<Equipment> call, Throwable t) {
                                progressDialog.dismiss();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(LiveWorkbenchActivity.this, "Ticket completed successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(LiveWorkbenchActivity.this, "Failed to update ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Ticket> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(LiveWorkbenchActivity.this, "Network error updating ticket", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCancelConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Live Workbench?")
                .setMessage("Are you sure you want to exit? Your current step modifications will not be saved.")
                .setPositiveButton("Confirm Exit", (dialog, which) -> finish())
                .setNegativeButton("Resume Work", null)
                .show();
    }

    private void showCancelConfirmDialogForDeletion() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ticket?")
                .setMessage("Are you sure you want to cancel and delete this ticket entirely?")
                .setPositiveButton("Confirm Cancellation", (dialog, which) -> {
                    // Call delete API
                    ApiClient.getApiService().deleteTicket(ticket.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            Toast.makeText(LiveWorkbenchActivity.this, "Ticket cancelled and deleted", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            finish();
                        }
                    });
                })
                .setNegativeButton("Continue Work", null)
                .show();
    }
}
