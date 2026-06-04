package com.medina.app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Equipment;
import com.medina.app.model.Ticket;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketsFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView tabTickets, tabEquipment, tvToggleFiltersLabel;
    private TextView tvStatOpen, tvStatInProgress, tvStatResolved;
    private EditText etSearch;
    private LinearLayout btnToggleFilters, layoutFilterPanel;
    private LinearLayout layoutTicketFilters, layoutEquipmentFilters;
    private Spinner spTicketStatus, spEqCategory, spEqType;
    private EditText etEqSerial;
    private Button btnScanQR;
    private RecyclerView rvTickets, rvEquipment;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty;
    private ProgressBar progressTickets;
    private FloatingActionButton fabNewTicket;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Ticket> allTickets = new ArrayList<>();
    private final List<Ticket> filteredTickets = new ArrayList<>();
    private final List<Equipment> allEquipment = new ArrayList<>();
    private final List<Equipment> filteredEquipment = new ArrayList<>();

    // ── Adapters ──────────────────────────────────────────────────────────────
    private TicketAdapter ticketAdapter;
    private EquipmentAdapter equipmentAdapter;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean showingTickets = true;
    private boolean filtersVisible = false;
    private String currentTicketStatusFilter = "all";
    private String currentEqCategoryFilter = "all";
    private String currentEqTypeFilter = "all";
    private SharedPreferences prefs;

    // ── QR Scanner ────────────────────────────────────────────────────────────
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;

    // Spinner value arrays
    private final String[] STATUS_VALUES  = {"all", "open", "in_progress", "resolved", "closed"};
    private final String[] STATUS_LABELS  = {"All Statuses", "Open", "In Progress", "Resolved", "Closed"};
    private final String[] CATEGORY_VALUES = {"all", "NETWORK", "STORAGE", "COMPONENT", "PERIPHERAL", "SERVER & DEVICE"};
    private final String[] CATEGORY_LABELS = {"All Categories", "Network", "Storage", "Component", "Peripheral", "Server & Device"};
    private final String[] TYPE_VALUES    = {"all", "Router", "Switch", "Access Point", "SSD", "HDD", "NVMe", "USB Flash", "RAM", "CPU", "GPU", "Motherboard", "NIC", "Keyboard", "Mouse", "Printer", "Scanner", "Headset", "Webcam", "HDMI Cable", "USB Cable", "Charger", "Adapter", "USB Hub", "Laptop", "System Unit", "Desktop", "Server", "Monitor"};
    private final String[] TYPE_LABELS    = {"All Types", "Router", "Switch", "Access Point", "SSD", "HDD", "NVMe", "USB Flash", "RAM", "CPU", "GPU", "Motherboard", "NIC", "Keyboard", "Mouse", "Printer", "Scanner", "Headset", "Webcam", "HDMI Cable", "USB Cable", "Charger", "Adapter", "USB Hub", "Laptop", "System Unit", "Desktop", "Server", "Monitor"};

    // ─────────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tickets, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register QR scanner result callback
        qrScanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String scanned = result.getContents().trim();
                // Switch to Equipment tab and set serial filter to scanned value
                switchTab(false);
                if (filtersVisible) {
                    etEqSerial.setText(scanned);
                } else {
                    // Open filter panel and set serial
                    filtersVisible = true;
                    if (layoutFilterPanel != null) layoutFilterPanel.setVisibility(View.VISIBLE);
                    if (tvToggleFiltersLabel != null) tvToggleFiltersLabel.setText("Filters ▴");
                    if (layoutEquipmentFilters != null) layoutEquipmentFilters.setVisibility(View.VISIBLE);
                    if (layoutTicketFilters != null) layoutTicketFilters.setVisibility(View.GONE);
                    etEqSerial.setText(scanned);
                }
                Toast.makeText(requireContext(),
                        "Scanning for: " + scanned, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireActivity().getSharedPreferences("medina_prefs", 0);

        bindViews(view);
        setupSpinners();
        setupAdapters();
        setupTabListeners();
        setupSearchListener();
        setupFilterToggle();
        setupFab();

        loadTickets();
        loadEquipment();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BIND VIEWS
    // ─────────────────────────────────────────────────────────────────────────
    private void bindViews(View v) {
        tabTickets           = v.findViewById(R.id.tabTickets);
        tabEquipment         = v.findViewById(R.id.tabEquipment);
        tvStatOpen           = v.findViewById(R.id.tvStatOpen);
        tvStatInProgress     = v.findViewById(R.id.tvStatInProgress);
        tvStatResolved       = v.findViewById(R.id.tvStatResolved);
        etSearch             = v.findViewById(R.id.etSearch);
        btnToggleFilters     = v.findViewById(R.id.btnToggleFilters);
        tvToggleFiltersLabel = v.findViewById(R.id.tvToggleFiltersLabel);
        layoutFilterPanel    = v.findViewById(R.id.layoutFilterPanel);
        layoutTicketFilters  = v.findViewById(R.id.layoutTicketFilters);
        layoutEquipmentFilters = v.findViewById(R.id.layoutEquipmentFilters);
        spTicketStatus       = v.findViewById(R.id.spTicketStatus);
        spEqCategory         = v.findViewById(R.id.spEqCategory);
        spEqType             = v.findViewById(R.id.spEqType);
        etEqSerial           = v.findViewById(R.id.etEqSerial);
        btnScanQR            = v.findViewById(R.id.btnScanQR);
        rvTickets            = v.findViewById(R.id.rvTickets);
        rvEquipment          = v.findViewById(R.id.rvEquipment);
        layoutEmpty          = v.findViewById(R.id.layoutEmpty);
        tvEmpty              = v.findViewById(R.id.tvEmpty);
        progressTickets      = v.findViewById(R.id.progressTickets);
        fabNewTicket         = v.findViewById(R.id.fabNewTicket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPINNERS SETUP
    // ─────────────────────────────────────────────────────────────────────────
    private void setupSpinners() {
        // Ticket Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, STATUS_LABELS);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTicketStatus.setAdapter(statusAdapter);
        spTicketStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentTicketStatusFilter = STATUS_VALUES[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Equipment Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, CATEGORY_LABELS);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEqCategory.setAdapter(catAdapter);
        spEqCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentEqCategoryFilter = CATEGORY_VALUES[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Equipment Type Spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, TYPE_LABELS);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEqType.setAdapter(typeAdapter);
        spEqType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentEqTypeFilter = TYPE_VALUES[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Serial Number text watcher
        etEqSerial.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // QR Scan button — request camera permission then launch ZXing scanner
        btnScanQR.setOnClickListener(v -> launchQrScanner());
    }

    private void launchQrScanner() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        ScanOptions options = new ScanOptions();
        options.setPrompt("Point camera at equipment QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        qrScanLauncher.launch(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner();
        } else {
            Toast.makeText(requireContext(),
                    "Camera permission required for QR scanning", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADAPTERS
    // ─────────────────────────────────────────────────────────────────────────
    private void setupAdapters() {
        ticketAdapter = new TicketAdapter(filteredTickets, ticket -> showTicketDetailDialog(ticket));
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(ticketAdapter);

        equipmentAdapter = new EquipmentAdapter(filteredEquipment, eq -> showEquipmentDetailDialog(eq));
        rvEquipment.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEquipment.setAdapter(equipmentAdapter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAB SWITCHING
    // ─────────────────────────────────────────────────────────────────────────
    private void setupTabListeners() {
        tabTickets.setOnClickListener(v -> switchTab(true));
        tabEquipment.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean tickets) {
        showingTickets = tickets;

        if (tickets) {
            tabTickets.setBackgroundResource(R.drawable.bg_tab_active);
            tabTickets.setTextColor(getResources().getColor(R.color.colorPrimary));
            tabEquipment.setBackgroundResource(R.drawable.bg_card);
            tabEquipment.setTextColor(getResources().getColor(R.color.textSecondary));
            rvTickets.setVisibility(View.VISIBLE);
            rvEquipment.setVisibility(View.GONE);
            etSearch.setHint("Search tickets...");
            fabNewTicket.setVisibility(View.VISIBLE);
        } else {
            tabEquipment.setBackgroundResource(R.drawable.bg_tab_active);
            tabEquipment.setTextColor(getResources().getColor(R.color.colorPrimary));
            tabTickets.setBackgroundResource(R.drawable.bg_card);
            tabTickets.setTextColor(getResources().getColor(R.color.textSecondary));
            rvTickets.setVisibility(View.GONE);
            rvEquipment.setVisibility(View.VISIBLE);
            etSearch.setHint("Search equipment...");
            fabNewTicket.setVisibility(View.GONE);
        }

        // Show correct filter sub-panel when filter panel is open
        if (filtersVisible) {
            layoutTicketFilters.setVisibility(tickets ? View.VISIBLE : View.GONE);
            layoutEquipmentFilters.setVisibility(tickets ? View.GONE : View.VISIBLE);
        }

        applyFilters();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTER TOGGLE
    // ─────────────────────────────────────────────────────────────────────────
    private void setupFilterToggle() {
        btnToggleFilters.setOnClickListener(v -> {
            filtersVisible = !filtersVisible;
            if (filtersVisible) {
                layoutFilterPanel.setVisibility(View.VISIBLE);
                tvToggleFiltersLabel.setText("Filters ▴");
                layoutTicketFilters.setVisibility(showingTickets ? View.VISIBLE : View.GONE);
                layoutEquipmentFilters.setVisibility(showingTickets ? View.GONE : View.VISIBLE);
            } else {
                layoutFilterPanel.setVisibility(View.GONE);
                tvToggleFiltersLabel.setText("Filters ▾");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ─────────────────────────────────────────────────────────────────────────
    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UNIFIED FILTER + SEARCH
    // ─────────────────────────────────────────────────────────────────────────
    private void applyFilters() {
        String q = etSearch.getText().toString().trim().toLowerCase();
        if (showingTickets) {
            applyTicketFilters(q);
        } else {
            applyEquipmentFilters(q);
        }
    }

    private void applyTicketFilters(String query) {
        filteredTickets.clear();
        for (Ticket t : allTickets) {
            // Status filter
            boolean matchesStatus = currentTicketStatusFilter.equals("all") ||
                    (t.getStatus() != null && t.getStatus().equalsIgnoreCase(currentTicketStatusFilter));
            // Search filter
            boolean matchesSearch = query.isEmpty() ||
                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(query)) ||
                    (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(query)) ||
                    (t.getUserName() != null && t.getUserName().toLowerCase().contains(query)) ||
                    (t.getEquipmentName() != null && t.getEquipmentName().toLowerCase().contains(query));
            if (matchesStatus && matchesSearch) filteredTickets.add(t);
        }
        ticketAdapter.notifyDataSetChanged();
        showEmptyState(filteredTickets.isEmpty(), "No tickets found");
    }

    private void applyEquipmentFilters(String query) {
        String serialQuery = etEqSerial != null ? etEqSerial.getText().toString().trim().toLowerCase() : "";
        filteredEquipment.clear();
        for (Equipment e : allEquipment) {
            // Category filter
            boolean matchesCat = currentEqCategoryFilter.equals("all") ||
                    (e.getCategory() != null && e.getCategory().equalsIgnoreCase(currentEqCategoryFilter));
            // Type filter
            boolean matchesType = currentEqTypeFilter.equals("all") ||
                    (e.getType() != null && e.getType().equalsIgnoreCase(currentEqTypeFilter));
            // Serial number filter
            boolean matchesSerial = serialQuery.isEmpty() ||
                    (e.getSerialNumber() != null && e.getSerialNumber().toLowerCase().contains(serialQuery));
            // Search filter
            boolean matchesSearch = query.isEmpty() ||
                    (e.getEquipmentName() != null && e.getEquipmentName().toLowerCase().contains(query)) ||
                    (e.getSerialNumber() != null && e.getSerialNumber().toLowerCase().contains(query)) ||
                    (e.getType() != null && e.getType().toLowerCase().contains(query)) ||
                    (e.getBrand() != null && e.getBrand().toLowerCase().contains(query));
            if (matchesCat && matchesType && matchesSerial && matchesSearch)
                filteredEquipment.add(e);
        }
        equipmentAdapter.notifyDataSetChanged();
        showEmptyState(filteredEquipment.isEmpty(), "No equipment found");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FAB → New Ticket
    // ─────────────────────────────────────────────────────────────────────────
    private void setupFab() {
        fabNewTicket.setOnClickListener(v -> showNewTicketDialog());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD DATA
    // ─────────────────────────────────────────────────────────────────────────
    private void loadTickets() {
        showProgress(true);
        String userId   = prefs.getString("user_id", "");
        String userRole = prefs.getString("user_role", "");
        ApiClient.authToken = prefs.getString("auth_token", null);

        Call<List<Ticket>> call;
        if ("admin".equalsIgnoreCase(userRole) || "manager".equalsIgnoreCase(userRole)) {
            call = ApiClient.getApiService().getAllTickets();
        } else if ("technician".equalsIgnoreCase(userRole)) {
            call = ApiClient.getApiService().getTicketsForTechnician(userId);
        } else {
            call = ApiClient.getApiService().getTicketsByUser(userId);
        }

        call.enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> c, Response<List<Ticket>> r) {
                showProgress(false);
                if (r.isSuccessful() && r.body() != null) {
                    allTickets.clear();
                    allTickets.addAll(r.body());
                    updateStats();
                    applyFilters();
                } else {
                    showEmptyState(true, "Could not load tickets");
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> c, Throwable t) {
                showProgress(false);
                showEmptyState(allTickets.isEmpty(), "Network error");
            }
        });
    }

    private void loadEquipment() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().getAllEquipment().enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(Call<List<Equipment>> c, Response<List<Equipment>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    allEquipment.clear();
                    allEquipment.addAll(r.body());
                    if (!showingTickets) applyFilters();
                }
            }
            @Override
            public void onFailure(Call<List<Equipment>> c, Throwable t) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATS
    // ─────────────────────────────────────────────────────────────────────────
    private void updateStats() {
        int open = 0, inProgress = 0, resolved = 0;
        for (Ticket t : allTickets) {
            String s = t.getStatus();
            if (s == null) continue;
            if (s.equalsIgnoreCase("open"))                                                          open++;
            else if (s.equalsIgnoreCase("in_progress") || s.equalsIgnoreCase("in progress")) inProgress++;
            else if (s.equalsIgnoreCase("resolved") || s.equalsIgnoreCase("closed"))          resolved++;
        }
        if (tvStatOpen != null)       tvStatOpen.setText(String.valueOf(open));
        if (tvStatInProgress != null) tvStatInProgress.setText(String.valueOf(inProgress));
        if (tvStatResolved != null)   tvStatResolved.setText(String.valueOf(resolved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TICKET DETAIL DIALOG (Premium)
    // ─────────────────────────────────────────────────────────────────────────
    private void showTicketDetailDialog(Ticket ticket) {
        if (!isAdded()) return;
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_detail_premium, null);

        // Category badge
        TextView tvCategory = v.findViewById(R.id.tvTicketDetailCategory);
        tvCategory.setText(ticket.getCategory() != null ? ticket.getCategory().toUpperCase() : "GENERAL");

        // Title
        ((TextView) v.findViewById(R.id.tvTicketDetailTitle))
                .setText(ticket.getTitle() != null ? ticket.getTitle() : "Untitled Ticket");

        // Priority
        TextView tvPriority = v.findViewById(R.id.tvTicketDetailPriority);
        String priority = ticket.getPriority() != null ? ticket.getPriority() : "medium";
        tvPriority.setText(priority.toUpperCase());
        applyPriorityColor(tvPriority, priority);

        // Status
        TextView tvStatus = v.findViewById(R.id.tvTicketDetailStatus);
        String status = ticket.getStatus() != null ? ticket.getStatus() : "open";
        tvStatus.setText(status.toUpperCase().replace("_", " "));
        applyStatusColor(tvStatus, status);

        // Technician / requester row
        TextView tvTechAvatar = v.findViewById(R.id.tvTicketDetailTechAvatar);
        TextView tvTechName   = v.findViewById(R.id.tvTicketDetailTechName);
        String requester = ticket.getUserName() != null ? ticket.getUserName() : "Unknown";
        tvTechName.setText(requester);
        // Avatar initials
        String[] parts = requester.split(" ");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : requester.length() >= 2 ? requester.substring(0, 2).toUpperCase() : "?";
        tvTechAvatar.setText(initials);

        // Description
        ((TextView) v.findViewById(R.id.tvTicketDetailDescription))
                .setText(ticket.getDescription() != null && !ticket.getDescription().isEmpty()
                        ? ticket.getDescription() : "No description provided.");

        // Edit/Delete buttons hidden for non-admin
        String role = prefs.getString("user_role", "");
        boolean isAdmin = "admin".equalsIgnoreCase(role) || "manager".equalsIgnoreCase(role);
        v.findViewById(R.id.btnTicketDetailEdit).setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnTicketDetailDelete).setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Close
        v.findViewById(R.id.btnTicketDetailCancel).setOnClickListener(x -> dialog.dismiss());

        // "Start Working" → launch LiveWorkbench
        Button btnAction = v.findViewById(R.id.btnTicketDetailAction);
        boolean isTech = "technician".equalsIgnoreCase(role);
        if (isTech && ("open".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status))) {
            btnAction.setText("Start Working");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(x -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), LiveWorkbenchActivity.class);
                intent.putExtra("ticket_id",     ticket.getId());
                intent.putExtra("ticket_title",  ticket.getTitle());
                intent.putExtra("ticket_number", ticket.getTicketNumber());
                intent.putExtra("eq_id",         ticket.getEquipmentId());
                intent.putExtra("eq_name",       ticket.getEquipmentName());
                startActivity(intent);
            });
        } else if (isAdmin) {
            btnAction.setText("Update Status");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(x -> {
                dialog.dismiss();
                showUpdateStatusDialog(ticket);
            });
        } else {
            btnAction.setVisibility(View.GONE);
        }

        // Edit
        v.findViewById(R.id.btnTicketDetailEdit).setOnClickListener(x -> {
            dialog.dismiss();
            showUpdateStatusDialog(ticket);
        });

        // Delete
        v.findViewById(R.id.btnTicketDetailDelete).setOnClickListener(x ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Ticket")
                        .setMessage("Are you sure you want to delete this ticket?")
                        .setPositiveButton("Delete", (d2, w) -> deleteTicket(ticket, dialog))
                        .setNegativeButton("Cancel", null)
                        .show());

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE STATUS DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showUpdateStatusDialog(Ticket ticket) {
        if (!isAdded()) return;
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_detail, null);

        // Populate basic fields
        setTextSafe(v, R.id.tvDetailTicketNumber,
                ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "—");
        setTextSafe(v, R.id.tvDetailTitle,
                ticket.getTitle() != null ? ticket.getTitle() : "Untitled");
        setTextSafe(v, R.id.tvDetailRequester,
                ticket.getUserName() != null ? ticket.getUserName() : "—");
        setTextSafe(v, R.id.tvDetailEquipment,
                ticket.getEquipmentName() != null ? ticket.getEquipmentName() : "—");
        setTextSafe(v, R.id.tvDetailCategory,
                ticket.getCategory() != null ? ticket.getCategory() : "—");

        String date = ticket.getCreatedAt();
        if (date != null && date.length() >= 10) date = date.substring(0, 10);
        setTextSafe(v, R.id.tvDetailDate, date != null ? date : "—");
        setTextSafe(v, R.id.tvDetailDescription,
                ticket.getDescription() != null ? ticket.getDescription() : "No description.");

        // Work note
        EditText etWorkNote = v.findViewById(R.id.etDetailWorkNote);
        if (etWorkNote != null)
            etWorkNote.setText(ticket.getWorkNote() != null ? ticket.getWorkNote() : "");

        // Status badge
        TextView tvStatus = v.findViewById(R.id.tvDetailStatus);
        String status = ticket.getStatus() != null ? ticket.getStatus() : "open";
        if (tvStatus != null) {
            tvStatus.setText(status.toUpperCase().replace("_", " "));
            applyStatusBadge(tvStatus, status);
        }

        // Priority badge
        TextView tvPriority = v.findViewById(R.id.tvDetailPriority);
        String priority = ticket.getPriority() != null ? ticket.getPriority() : "medium";
        if (tvPriority != null) {
            tvPriority.setText(priority.toUpperCase());
            applyPriorityColor(tvPriority, priority);
        }

        // Status spinner
        Spinner spStatus = v.findViewById(R.id.spDetailStatus);
        if (spStatus != null) {
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, STATUS_LABELS);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spStatus.setAdapter(statusAdapter);
            for (int i = 0; i < STATUS_VALUES.length; i++) {
                if (STATUS_VALUES[i].equalsIgnoreCase(status)) {
                    spStatus.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        View btnClose = v.findViewById(R.id.btnDetailClose);
        if (btnClose != null) btnClose.setOnClickListener(x -> dialog.dismiss());

        View btnUpdate = v.findViewById(R.id.btnDetailUpdate);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(x -> {
                int selectedIdx = spStatus != null ? spStatus.getSelectedItemPosition() : 0;
                String newStatus = STATUS_VALUES[Math.min(selectedIdx, STATUS_VALUES.length - 1)];
                String workNote  = etWorkNote != null ? etWorkNote.getText().toString().trim() : "";
                ticket.setStatus(newStatus);
                ticket.setWorkNote(workNote);
                updateTicket(ticket, dialog);
            });
        }

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EQUIPMENT DETAIL DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showEquipmentDetailDialog(Equipment eq) {
        if (!isAdded()) return;
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_equipment_detail, null);

        // Name & Serial
        setTextSafe(v, R.id.tvEqDetailName,
                eq.getEquipmentName() != null ? eq.getEquipmentName() : "Unknown Equipment");
        setTextSafe(v, R.id.tvEqDetailSerial,
                "S/N: " + (eq.getSerialNumber() != null ? eq.getSerialNumber() : "—"));

        // Type, Category, Assigned (brand used as assigned), Last Repair
        setTextSafe(v, R.id.tvEqDetailType,
                eq.getType() != null ? eq.getType() : "—");
        setTextSafe(v, R.id.tvEqDetailCategory,
                eq.getCategory() != null ? eq.getCategory() : "—");
        setTextSafe(v, R.id.tvEqDetailAssigned,
                eq.getBrand() != null ? eq.getBrand() : "Unassigned");
        setTextSafe(v, R.id.tvEqDetailLastRepair, "Never");

        // Status badge
        TextView tvStatusBadge = v.findViewById(R.id.tvEqDetailStatusBadge);
        if (tvStatusBadge != null) {
            String status = eq.getStatus() != null ? eq.getStatus() : "Available";
            tvStatusBadge.setText(status.toUpperCase());
            applyStatusBadge(tvStatusBadge, status);
        }

        // Warning banner if equipment has active ticket
        boolean hasActiveTicket = false;
        for (Ticket t : allTickets) {
            if (eq.getId() != null && eq.getId().equals(t.getEquipmentId())) {
                String s = t.getStatus();
                if ("open".equalsIgnoreCase(s) || "in_progress".equalsIgnoreCase(s)) {
                    hasActiveTicket = true;
                    break;
                }
            }
        }
        View warningBanner = v.findViewById(R.id.layoutEqWarningBanner);
        if (warningBanner != null)
            warningBanner.setVisibility(hasActiveTicket ? View.VISIBLE : View.GONE);

        // Timeline empty message
        View timelineEmpty = v.findViewById(R.id.tvEqTimelineEmpty);
        if (timelineEmpty != null) timelineEmpty.setVisibility(View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Close button
        View btnCancel = v.findViewById(R.id.btnEqDetailCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(x -> dialog.dismiss());

        // Action button: "Create Ticket" links to the new ticket dialog pre-filled with this equipment
        Button btnAction = v.findViewById(R.id.btnEqDetailAction);
        if (btnAction != null) {
            if (hasActiveTicket) {
                btnAction.setText("View Ticket");
                btnAction.setOnClickListener(x -> {
                    dialog.dismiss();
                    // Switch to Tickets tab and filter by this equipment name
                    switchTab(true);
                    etSearch.setText(eq.getEquipmentName() != null ? eq.getEquipmentName() : "");
                });
            } else {
                btnAction.setText("Create Ticket");
                btnAction.setOnClickListener(x -> {
                    dialog.dismiss();
                    showNewTicketDialogForEquipment(eq);
                });
            }
        }

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NEW TICKET DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showNewTicketDialog() {
        showNewTicketDialogForEquipment(null);
    }

    private void showNewTicketDialogForEquipment(@Nullable Equipment preselectedEq) {
        if (!isAdded()) return;
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_ticket, null);

        // ── Views ─────────────────────────────────────────────────────────────
        EditText etTitle      = v.findViewById(R.id.etTicketTitle);
        EditText etDesc       = v.findViewById(R.id.etTicketDescription);
        Spinner  spCategory   = v.findViewById(R.id.spTicketCategory);
        Spinner  spPriority   = v.findViewById(R.id.spTicketPriority);
        // Equipment selector (web-style)
        LinearLayout layoutEqCard     = v.findViewById(R.id.layoutSelectedEqCard);
        TextView     tvEqName         = v.findViewById(R.id.tvSelectedEqName);
        TextView     tvEqSerialType   = v.findViewById(R.id.tvSelectedEqSerialType);
        Button       btnChangeEq      = v.findViewById(R.id.btnChangeSelectedEq);
        LinearLayout layoutEqSearch   = v.findViewById(R.id.layoutEqSearchSelector);
        EditText     etDlgSearch      = v.findViewById(R.id.etDlgSearchEquipment);
        LinearLayout layoutDlgEqList  = v.findViewById(R.id.layoutDlgEquipmentList);

        // ── Category spinner ─────────────────────────────────────────────────
        final String[] categories  = {"Maintenance", "Inspection", "Incident"};
        spCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories));
        ((ArrayAdapter<?>) spCategory.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // ── Priority spinner ─────────────────────────────────────────────────
        final String[] priorities      = {"low", "medium", "high", "critical"};
        final String[] priorityLabels  = {"Low", "Medium", "High", "Critical"};
        spPriority.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, priorityLabels));
        ((ArrayAdapter<?>) spPriority.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setSelection(1);

        // ── Equipment state ──────────────────────────────────────────────────
        final Equipment[] selectedEquipment = {preselectedEq};

        // Helper: show the selected-equipment card
        Runnable showSelectedCard = () -> {
            Equipment eq = selectedEquipment[0];
            if (eq != null) {
                tvEqName.setText(eq.getEquipmentName() != null ? eq.getEquipmentName() : "Unknown");
                String sn   = eq.getSerialNumber() != null ? eq.getSerialNumber() : "—";
                String type = eq.getType() != null ? eq.getType() : "";
                tvEqSerialType.setText(sn + (type.isEmpty() ? "" : " • " + type));
                layoutEqCard.setVisibility(View.VISIBLE);
                layoutEqSearch.setVisibility(View.GONE);
            } else {
                layoutEqCard.setVisibility(View.GONE);
                layoutEqSearch.setVisibility(View.VISIBLE);
            }
        };

        showSelectedCard.run();

        // Helper: rebuild equipment list rows
        Runnable rebuildEqList = () -> {
            layoutDlgEqList.removeAllViews();
            String q = etDlgSearch.getText().toString().trim().toLowerCase();
            boolean anyShown = false;
            for (Equipment eq : allEquipment) {
                String name   = eq.getEquipmentName() != null ? eq.getEquipmentName().toLowerCase() : "";
                String serial = eq.getSerialNumber()  != null ? eq.getSerialNumber().toLowerCase()  : "";
                String type   = eq.getType()           != null ? eq.getType().toLowerCase()           : "";
                String brand  = eq.getBrand()          != null ? eq.getBrand().toLowerCase()          : "";
                if (!q.isEmpty() && !name.contains(q) && !serial.contains(q)
                        && !type.contains(q) && !brand.contains(q)) {
                    continue;
                }
                // Row card
                View row = LayoutInflater.from(requireContext())
                        .inflate(android.R.layout.simple_list_item_2, layoutDlgEqList, false);
                ((TextView) row.findViewById(android.R.id.text1))
                        .setText(eq.getEquipmentName() != null ? eq.getEquipmentName() : "Unknown");
                String subtitle = (eq.getSerialNumber() != null ? eq.getSerialNumber() : "—")
                        + (eq.getType() != null ? " • " + eq.getType() : "");
                ((TextView) row.findViewById(android.R.id.text2)).setText(subtitle);
                row.setPadding(8, 12, 8, 12);
                row.setOnClickListener(x -> {
                    selectedEquipment[0] = eq;
                    showSelectedCard.run();
                });
                layoutDlgEqList.addView(row);
                anyShown = true;
            }
            if (!anyShown) {
                TextView tv = new TextView(requireContext());
                tv.setText(q.isEmpty() ? "No equipment loaded yet" : "No equipment matches \"" + q + "\"");
                tv.setPadding(12, 16, 12, 16);
                tv.setTextColor(0xFF94a3b8);
                layoutDlgEqList.addView(tv);
            }
        };

        rebuildEqList.run();

        // Live search inside the dialog
        etDlgSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                rebuildEqList.run();
            }
        });

        // "Change" button resets selection
        btnChangeEq.setOnClickListener(x -> {
            selectedEquipment[0] = null;
            etDlgSearch.setText("");
            showSelectedCard.run();
            rebuildEqList.run();
        });

        // ── Dialog ──────────────────────────────────────────────────────────
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        v.findViewById(R.id.btnCancelTicket).setOnClickListener(x -> dialog.dismiss());

        v.findViewById(R.id.btnSubmitTicket).setOnClickListener(x -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }
            if (selectedEquipment[0] == null) {
                Toast.makeText(requireContext(),
                        "Please select equipment for this ticket", Toast.LENGTH_SHORT).show();
                return;
            }

            Ticket newTicket = new Ticket();
            newTicket.setTitle(title);
            newTicket.setDescription(etDesc.getText().toString().trim());
            newTicket.setCategory(categories[spCategory.getSelectedItemPosition()]);
            newTicket.setPriority(priorities[spPriority.getSelectedItemPosition()]);
            newTicket.setStatus("open");
            newTicket.setUserId(prefs.getString("user_id", ""));
            newTicket.setUserName(prefs.getString("user_name", ""));
            newTicket.setEquipmentId(selectedEquipment[0].getId());
            newTicket.setEquipmentName(selectedEquipment[0].getEquipmentName());

            submitNewTicket(newTicket, dialog);
        });

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  API CALLS
    // ─────────────────────────────────────────────────────────────────────────
    private void updateTicket(Ticket ticket, AlertDialog dialog) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().updateTicket(ticket.getId(), ticket)
                .enqueue(new Callback<Ticket>() {
            @Override
            public void onResponse(Call<Ticket> c, Response<Ticket> r) {
                if (!isAdded()) return;
                if (r.isSuccessful() && r.body() != null) {
                    for (int i = 0; i < allTickets.size(); i++) {
                        if (allTickets.get(i).getId().equals(ticket.getId())) {
                            allTickets.set(i, r.body());
                            break;
                        }
                    }
                    updateStats();
                    applyFilters();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Ticket updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update ticket", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Ticket> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error updating ticket", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTicket(Ticket ticket, AlertDialog dialog) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().deleteTicket(ticket.getId())
                .enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                if (!isAdded()) return;
                if (r.isSuccessful()) {
                    allTickets.remove(ticket);
                    updateStats();
                    applyFilters();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Ticket deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to delete ticket", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitNewTicket(Ticket ticket, AlertDialog dialog) {
        ApiClient.authToken = prefs.getString("auth_token", null);
        ApiClient.getApiService().createTicket(ticket).enqueue(new Callback<Ticket>() {
            @Override
            public void onResponse(Call<Ticket> c, Response<Ticket> r) {
                if (!isAdded()) return;
                if (r.isSuccessful() && r.body() != null) {
                    allTickets.add(0, r.body());
                    updateStats();
                    applyFilters();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Ticket submitted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to create ticket", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Ticket> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error creating ticket", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void showProgress(boolean show) {
        if (progressTickets != null)
            progressTickets.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show, String message) {
        if (layoutEmpty != null)
            layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (tvEmpty != null && message != null)
            tvEmpty.setText(message);
    }

    private void setTextSafe(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void applyStatusBadge(TextView tv, String status) {
        if (status.equalsIgnoreCase("open")) {
            tv.setTextColor(Color.parseColor("#ef4444"));
            tv.setBackgroundResource(R.drawable.bg_badge_rejected);
        } else if (status.equalsIgnoreCase("in_progress") || status.equalsIgnoreCase("in progress")) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
            tv.setBackgroundResource(R.drawable.bg_badge_maintenance);
        } else if (status.equalsIgnoreCase("resolved") || status.equalsIgnoreCase("closed")
                || status.equalsIgnoreCase("available")) {
            tv.setTextColor(Color.parseColor("#22c55e"));
            tv.setBackgroundResource(R.drawable.bg_badge_available);
        } else {
            tv.setTextColor(Color.parseColor("#64748b"));
            tv.setBackgroundResource(R.drawable.bg_card);
        }
    }

    private void applyStatusColor(TextView tv, String status) {
        if (status.equalsIgnoreCase("open")) {
            tv.setTextColor(Color.parseColor("#ef4444"));
        } else if (status.equalsIgnoreCase("in_progress") || status.equalsIgnoreCase("in progress")) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
        } else if (status.equalsIgnoreCase("resolved") || status.equalsIgnoreCase("closed")) {
            tv.setTextColor(Color.parseColor("#22c55e"));
        } else {
            tv.setTextColor(Color.parseColor("#64748b"));
        }
    }

    private void applyPriorityColor(TextView tv, String priority) {
        if (priority.equalsIgnoreCase("high") || priority.equalsIgnoreCase("critical")) {
            tv.setTextColor(Color.parseColor("#ef4444"));
        } else if (priority.equalsIgnoreCase("medium")) {
            tv.setTextColor(Color.parseColor("#f59e0b"));
        } else {
            tv.setTextColor(Color.parseColor("#22c55e"));
        }
    }
}
