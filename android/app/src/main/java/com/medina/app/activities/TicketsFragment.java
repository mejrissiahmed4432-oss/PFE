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
import android.widget.ImageButton;
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

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Equipment;
import com.medina.app.model.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private Spinner spTicketStatus;
    private Spinner spEqCategory, spEqStatus;
    private ImageButton btnScanQR;
    private RecyclerView rvTickets, rvEquipment;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty;
    private ProgressBar progressTickets;
    private Button btnCreateTicket;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Ticket> allTickets = new ArrayList<>();
    private final List<Ticket> filteredTickets = new ArrayList<>();
    private final List<Equipment> allEquipment = new ArrayList<>();
    private final List<Equipment> filteredEquipment = new ArrayList<>();

    // ── Adapters ──────────────────────────────────────────────────────────────
    private TicketAdapter ticketAdapter;
    private EquipmentAdapter equipmentAdapter;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isTicketsTabActive = true;
    private boolean filtersVisible = false;
    private String currentTicketStatusFilter = "all";
    private String currentEqCategoryFilter = "all";
    private String currentEqStatusFilter = "all";
    private SharedPreferences prefs;

    // ── QR Scanner ────────────────────────────────────────────────────────────
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;

    // Spinner value arrays for Tickets (categories from web: Maintenance | Inspection | Incident | Upgrade)
    private final String[] STATUS_VALUES  = {"all", "open", "in_progress", "resolved", "closed"};
    private final String[] STATUS_LABELS  = {"All Statuses", "Open", "In Progress", "Resolved", "Closed"};
    private final String[] TICKET_CATEGORY_VALUES = {"all", "Maintenance", "Inspection", "Incident", "Upgrade"};
    private final String[] TICKET_CATEGORY_LABELS = {"All Categories", "Maintenance", "Inspection", "Incident", "Upgrade"};
    // Equipment status filter options
    private final String[] EQ_STATUS_VALUES = {"all", "available", "in_use", "under_maintenance", "decommissioned"};
    private final String[] EQ_STATUS_LABELS = {"All Statuses", "Available", "In Use", "Under Maintenance", "Decommissioned"};

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
        qrScanLauncher = registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
            if (result.getContents() != null) {
                String rawContent = result.getContents().trim();
                String serialNumber = null;
                String equipmentId = null;
                
                // Try parsing as JSON (web generates: {id, name, serial})
                try {
                    org.json.JSONObject json = new org.json.JSONObject(rawContent);
                    // Web encodes key "serial" (not "serialNumber")
                    if (json.has("serial")) {
                        serialNumber = json.getString("serial");
                    } else if (json.has("serialNumber")) {
                        serialNumber = json.getString("serialNumber");
                    }
                    if (json.has("id")) {
                        equipmentId = json.getString("id");
                    }
                } catch (Exception e) {
                    // Not JSON - treat raw string as serial number
                    serialNumber = rawContent;
                }
                
                // Search equipment by serial number first, then by id as fallback
                Equipment foundEq = null;
                if (serialNumber != null) {
                    final String sn = serialNumber;
                    for (Equipment eq : allEquipment) {
                        if (sn.equalsIgnoreCase(eq.getSerialNumber())) {
                            foundEq = eq;
                            break;
                        }
                    }
                }
                // Fallback: match by equipment ID
                if (foundEq == null && equipmentId != null) {
                    final String eqId = equipmentId;
                    for (Equipment eq : allEquipment) {
                        if (eqId.equalsIgnoreCase(eq.getId())) {
                            foundEq = eq;
                            break;
                        }
                    }
                }
                
                if (foundEq != null) {
                    Toast.makeText(requireContext(), "Equipment found: " + foundEq.getEquipmentName(), Toast.LENGTH_SHORT).show();
                    showEquipmentDetailDialog(foundEq);
                } else {
                    String displaySerial = serialNumber != null ? serialNumber : rawContent;
                    Toast.makeText(requireContext(), "No equipment found with S/N: " + displaySerial, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireActivity().getSharedPreferences("medina_prefs", 0);

        bindViews(view);
        setupRoleBasedUI();
        setupTabListeners();
        setupSpinners();
        setupAdapters();
        setupSearchListener();
        setupFilterToggle();
        setupCreateTicketButton();
        loadTickets();
        loadEquipment(); // Load equipment for all users to support QR scanning and ticket details
    }

    private boolean isTechnicianOrAdmin() {
        String role = prefs.getString("user_role", "");
        return "TECHNICIAN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private void setupRoleBasedUI() {
        // Hide equipment tab for non-tech users
        boolean isTech = isTechnicianOrAdmin();
        if (tabEquipment != null)
            tabEquipment.setVisibility(isTech ? View.VISIBLE : View.GONE);
        // QR Scanner is visible for everyone
        if (btnScanQR != null)
            btnScanQR.setVisibility(View.VISIBLE);
        if (rvEquipment != null)
            rvEquipment.setVisibility(View.GONE);
        // Search hint based on role
        if (etSearch != null)
            etSearch.setHint(isTech ? "Search tickets or equipment..." : "Search my tickets...");
            
        // Hide Create Ticket floating button for non-technicians
        if (btnCreateTicket != null)
            btnCreateTicket.setVisibility(isTech ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BIND VIEWS
    // ─────────────────────────────────────────────────────────────────────────
    private void bindViews(View v) {
        tvStatOpen           = v.findViewById(R.id.tvStatOpen);
        tvStatInProgress     = v.findViewById(R.id.tvStatInProgress);
        tvStatResolved       = v.findViewById(R.id.tvStatResolved);
        etSearch             = v.findViewById(R.id.etSearch);
        btnToggleFilters     = v.findViewById(R.id.btnToggleFilters);
        tvToggleFiltersLabel = v.findViewById(R.id.tvToggleFiltersLabel);
        layoutFilterPanel    = v.findViewById(R.id.layoutFilterPanel);
        layoutTicketFilters  = v.findViewById(R.id.layoutTicketFilters);
        layoutEquipmentFilters= v.findViewById(R.id.layoutEquipmentFilters);
        spTicketStatus       = v.findViewById(R.id.spTicketStatus);
        spEqCategory         = v.findViewById(R.id.spEqCategory);
        spEqStatus           = v.findViewById(R.id.spEqStatus);
        btnScanQR            = v.findViewById(R.id.btnScanQR);
        rvTickets            = v.findViewById(R.id.rvTickets);
        rvEquipment          = v.findViewById(R.id.rvEquipment);
        tabTickets           = v.findViewById(R.id.tabTickets);
        tabEquipment         = v.findViewById(R.id.tabEquipment);
        layoutEmpty          = v.findViewById(R.id.layoutEmpty);
        tvEmpty              = v.findViewById(R.id.tvEmpty);
        progressTickets      = v.findViewById(R.id.progressTickets);
        btnCreateTicket      = v.findViewById(R.id.btnCreateTicket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABS
    // ─────────────────────────────────────────────────────────────────────────
    private void setupTabListeners() {
        tabTickets.setOnClickListener(v -> switchTab(true));
        tabEquipment.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean isTickets) {
        isTicketsTabActive = isTickets;
        
        // Update tab styling
        tabTickets.setTextColor(isTickets ? ContextCompat.getColor(requireContext(), R.color.colorPrimary) : Color.parseColor("#9CA3AF"));
        tabEquipment.setTextColor(!isTickets ? ContextCompat.getColor(requireContext(), R.color.colorPrimary) : Color.parseColor("#9CA3AF"));
        
        // Update list visibility
        rvTickets.setVisibility(isTickets ? View.VISIBLE : View.GONE);
        rvEquipment.setVisibility(!isTickets ? View.VISIBLE : View.GONE);
        
        // Update filter visibility
        if (filtersVisible) {
            layoutTicketFilters.setVisibility(isTickets ? View.VISIBLE : View.GONE);
            layoutEquipmentFilters.setVisibility(!isTickets ? View.VISIBLE : View.GONE);
        } else {
            layoutTicketFilters.setVisibility(View.GONE);
            layoutEquipmentFilters.setVisibility(View.GONE);
        }
        
        // Button only visible for Tickets tab
        if (btnCreateTicket != null) {
            btnCreateTicket.setVisibility(isTickets ? View.VISIBLE : View.GONE);
        }
        
        // Reset search field
        etSearch.setHint(isTickets ? "Search tickets..." : "Search equipment...");
        applyFilters();
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

        // Equipment Category Spinner - populated dynamically after equipment loads
        // Start with just "All Categories" placeholder
        ArrayAdapter<String> eqCatAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[]{"All Categories"});
        eqCatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEqCategory.setAdapter(eqCatAdapter);
        spEqCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String[] items = new String[p.getCount()];
                for (int i = 0; i < p.getCount(); i++) items[i] = (String) p.getItemAtPosition(i);
                currentEqCategoryFilter = (pos == 0) ? "all" : items[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Equipment Status Spinner
        ArrayAdapter<String> eqStatusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, EQ_STATUS_LABELS);
        eqStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEqStatus.setAdapter(eqStatusAdapter);
        spEqStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentEqStatusFilter = EQ_STATUS_VALUES[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
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
    //  FILTER TOGGLE
    // ─────────────────────────────────────────────────────────────────────────
    private void setupFilterToggle() {
        btnToggleFilters.setOnClickListener(v -> {
            filtersVisible = !filtersVisible;
            if (filtersVisible) {
                layoutFilterPanel.setVisibility(View.VISIBLE);
                tvToggleFiltersLabel.setText("Filters ▴");
                layoutTicketFilters.setVisibility(isTicketsTabActive ? View.VISIBLE : View.GONE);
                layoutEquipmentFilters.setVisibility(!isTicketsTabActive ? View.VISIBLE : View.GONE);
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
        if (isTicketsTabActive) {
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
        filteredEquipment.clear();
        for (Equipment eq : allEquipment) {
            // Category filter
            boolean matchesCat = currentEqCategoryFilter.equals("all") ||
                    (eq.getCategory() != null && eq.getCategory().equalsIgnoreCase(currentEqCategoryFilter));
            // Status filter
            boolean matchesStatus = currentEqStatusFilter.equals("all") ||
                    (eq.getStatus() != null && eq.getStatus().equalsIgnoreCase(currentEqStatusFilter));
            // Search filter
            boolean matchesSearch = query.isEmpty() ||
                    (eq.getEquipmentName() != null && eq.getEquipmentName().toLowerCase().contains(query)) ||
                    (eq.getSerialNumber() != null && eq.getSerialNumber().toLowerCase().contains(query)) ||
                    (eq.getBrand() != null && eq.getBrand().toLowerCase().contains(query));

            if (matchesCat && matchesStatus && matchesSearch) {
                filteredEquipment.add(eq);
            }
        }
        equipmentAdapter.notifyDataSetChanged();
        showEmptyState(filteredEquipment.isEmpty(), "No equipment found");
    }



    // ─────────────────────────────────────────────────────────────────────────
    //  Create Ticket Button → New Ticket
    // ─────────────────────────────────────────────────────────────────────────
    private void setupCreateTicketButton() {
        if (btnCreateTicket != null) {
            btnCreateTicket.setOnClickListener(v -> showNewTicketDialog());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD DATA
    // ─────────────────────────────────────────────────────────────────────────
    private void loadTickets() {
        showProgress(true);
        String userId   = prefs.getString("user_id", "");
        String userRole = prefs.getString("user_role", "");
        ApiClient.authToken = prefs.getString("auth_token", null);

        Call<List<Ticket>> call = ApiClient.getApiService().getTicketsByUser(userId);

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
                    populateEquipmentCategorySpinner();
                    applyFilters();
                }
            }
            @Override
            public void onFailure(Call<List<Equipment>> c, Throwable t) {}
        });
    }

    private void populateEquipmentCategorySpinner() {
        if (spEqCategory == null || !isAdded()) return;
        // Collect unique non-null categories from loaded equipment
        java.util.LinkedHashSet<String> cats = new java.util.LinkedHashSet<>();
        for (Equipment eq : allEquipment) {
            if (eq.getCategory() != null && !eq.getCategory().isEmpty()) {
                // Capitalize first letter for display
                String cat = eq.getCategory();
                cats.add(cat);
            }
        }
        List<String> labelList = new ArrayList<>();
        labelList.add("All Categories");
        labelList.addAll(cats);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labelList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEqCategory.setAdapter(adapter);
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

        // Equipment Details Card
        View layoutEqInfo = v.findViewById(R.id.layoutDetailEquipmentInfo);
        if (layoutEqInfo != null) {
            layoutEqInfo.setVisibility(View.VISIBLE);
            TextView tvEqName   = v.findViewById(R.id.tvDetailEqName);
            TextView tvEqSerial = v.findViewById(R.id.tvDetailEqSerial);
            TextView tvEqBrand  = v.findViewById(R.id.tvDetailEqBrand);
            TextView tvEqStatus = v.findViewById(R.id.tvDetailEqStatus);

            Equipment relatedEq = null;
            if (ticket.getEquipmentId() != null) {
                for (Equipment eq : allEquipment) {
                    if (ticket.getEquipmentId().equals(eq.getId())) {
                        relatedEq = eq;
                        break;
                    }
                }
            }
            if (relatedEq != null) {
                if (tvEqName   != null) tvEqName.setText(relatedEq.getEquipmentName() != null ? relatedEq.getEquipmentName() : "—");
                if (tvEqSerial != null) tvEqSerial.setText(relatedEq.getSerialNumber() != null ? relatedEq.getSerialNumber() : "—");
                if (tvEqBrand  != null) tvEqBrand.setText(relatedEq.getBrand() != null ? relatedEq.getBrand() : "—");
                if (tvEqStatus != null) tvEqStatus.setText(relatedEq.getStatus() != null ? relatedEq.getStatus().replace("_", " ") : "—");
            } else {
                if (tvEqName   != null) tvEqName.setText(ticket.getEquipmentName() != null ? ticket.getEquipmentName() : "—");
                if (tvEqSerial != null) tvEqSerial.setText("—");
                if (tvEqBrand  != null) tvEqBrand.setText("—");
                if (tvEqStatus != null) tvEqStatus.setText("—");
            }
        }

        // Edit/Delete buttons visible for tech, admin, or ticket creator
        String currentUserId = prefs.getString("user_id", "");
        boolean canEdit = isTechnicianOrAdmin() || (ticket.getUserId() != null && ticket.getUserId().equals(currentUserId));
        v.findViewById(R.id.btnTicketDetailEdit).setVisibility(canEdit ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnTicketDetailDelete).setVisibility(canEdit ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Close
        v.findViewById(R.id.btnTicketDetailCancel).setOnClickListener(x -> dialog.dismiss());

        // "Update Ticket"
        Button btnAction = v.findViewById(R.id.btnTicketDetailAction);
        if (canEdit) {
            btnAction.setText("Update Ticket");
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
    //  EDIT TICKET DIALOG  (Title, Desc, Category, Priority, Deadline)
    // ─────────────────────────────────────────────────────────────────────────
    private void showUpdateStatusDialog(Ticket ticket) {
        if (!isAdded()) return;
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_ticket, null);

        // ── Pre-fill Title ────────────────────────────────────────────────
        EditText etTitle = v.findViewById(R.id.etTicketTitle);
        if (etTitle != null && ticket.getTitle() != null) etTitle.setText(ticket.getTitle());

        // ── Pre-fill Description ──────────────────────────────────────────
        EditText etDesc = v.findViewById(R.id.etTicketDescription);
        if (etDesc != null && ticket.getDescription() != null) etDesc.setText(ticket.getDescription());

        // ── Pre-fill Deadline ─────────────────────────────────────────────
        LinearLayout layoutDeadline = v.findViewById(R.id.layoutDeadline);
        TextView tvDeadline = v.findViewById(R.id.tvTicketDeadline);
        final String[] selectedDeadline = {ticket.getDeadline() != null ? ticket.getDeadline() : null};
        if (tvDeadline != null && selectedDeadline[0] != null) {
            tvDeadline.setText(selectedDeadline[0]);
            tvDeadline.setTextColor(0xFF1E293B);
        }
        if (layoutDeadline != null) {
            layoutDeadline.setOnClickListener(x -> {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                new android.app.DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                    selectedDeadline[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    tvDeadline.setText(selectedDeadline[0]);
                    tvDeadline.setTextColor(0xFF1E293B);
                }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
            });
        }

        // ── Category spinner ──────────────────────────────────────────────
        final String[] categories = {"Maintenance", "Inspection", "Incident"};
        Spinner spCategory = v.findViewById(R.id.spTicketCategory);
        if (spCategory != null) {
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, categories);
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(catAdapter);
            if (ticket.getCategory() != null) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(ticket.getCategory())) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            }
        }

        // ── Priority spinner ──────────────────────────────────────────────
        final String[] priorities     = {"low", "medium", "high", "critical"};
        final String[] priorityLabels = {"Low", "Medium", "High", "Critical"};
        Spinner spPriority = v.findViewById(R.id.spTicketPriority);
        if (spPriority != null) {
            ArrayAdapter<String> priAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, priorityLabels);
            priAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spPriority.setAdapter(priAdapter);
            if (ticket.getPriority() != null) {
                for (int i = 0; i < priorities.length; i++) {
                    if (priorities[i].equalsIgnoreCase(ticket.getPriority())) {
                        spPriority.setSelection(i);
                        break;
                    }
                }
            }
        }

        // ── Hide equipment selector (not editable in update mode) ─────────
        View layoutEqCard   = v.findViewById(R.id.layoutSelectedEqCard);
        View layoutEqSearch = v.findViewById(R.id.layoutEqSearchSelector);
        if (layoutEqCard   != null) layoutEqCard.setVisibility(View.GONE);
        if (layoutEqSearch != null) layoutEqSearch.setVisibility(View.GONE);

        // ── Build dialog ──────────────────────────────────────────────────
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // ── Cancel button ─────────────────────────────────────────────────
        Button btnCancel = v.findViewById(R.id.btnCancelTicket);
        if (btnCancel != null) btnCancel.setOnClickListener(x -> dialog.dismiss());

        // ── Save button ───────────────────────────────────────────────────
        Button btnSubmit = v.findViewById(R.id.btnSubmitTicket);
        if (btnSubmit != null) {
            btnSubmit.setText("Save Changes");
            btnSubmit.setOnClickListener(x -> {
                String newTitle = etTitle != null ? etTitle.getText().toString().trim() : "";
                if (newTitle.isEmpty()) {
                    Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                ticket.setTitle(newTitle);
                if (etDesc != null) ticket.setDescription(etDesc.getText().toString().trim());
                if (spCategory != null) ticket.setCategory(categories[spCategory.getSelectedItemPosition()]);
                if (spPriority != null) ticket.setPriority(priorities[spPriority.getSelectedItemPosition()]);
                if (selectedDeadline[0] != null) ticket.setDeadline(selectedDeadline[0]);

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

        // Timeline empty message and History
        TextView timelineEmpty = v.findViewById(R.id.tvEqTimelineEmpty);
        
        String lastRepairDate = "Never";
        StringBuilder historyBuilder = new StringBuilder();
        int historyCount = 0;
        for (Ticket t : allTickets) {
            if (eq.getId() != null && eq.getId().equals(t.getEquipmentId())) {
                historyCount++;
                String dateStr = t.getCreatedAt() != null && t.getCreatedAt().length() >= 10 
                                 ? t.getCreatedAt().substring(0, 10) : "Unknown Date";
                String titleStr = t.getTitle() != null ? t.getTitle() : "Untitled";
                String statusStr = t.getStatus() != null ? t.getStatus().toUpperCase() : "OPEN";
                historyBuilder.append("• ").append(dateStr).append(" - ").append(titleStr)
                              .append(" [").append(statusStr).append("]\n");
                              
                if (statusStr.equals("RESOLVED") || statusStr.equals("CLOSED") || statusStr.equals("COMPLETED")) {
                    if (lastRepairDate.equals("Never") || dateStr.compareTo(lastRepairDate) > 0) {
                        lastRepairDate = dateStr;
                    }
                }
            }
        }
        setTextSafe(v, R.id.tvEqDetailLastRepair, lastRepairDate);
        
        if (timelineEmpty != null) {
            timelineEmpty.setVisibility(View.VISIBLE);
            if (historyCount > 0) {
                timelineEmpty.setText(historyBuilder.toString().trim());
                timelineEmpty.setGravity(android.view.Gravity.START);
                timelineEmpty.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            } else {
                timelineEmpty.setText("No previous repair records found.");
                timelineEmpty.setGravity(android.view.Gravity.CENTER);
            }
        }

        // Specifications section - dynamically add key/value spec rows
        LinearLayout layoutSpecsContainer = v.findViewById(R.id.layoutEqSpecsContainer);
        TextView tvEqSpecsTitle = v.findViewById(R.id.tvEqSpecsTitle);
        java.util.Map<String, String> specs = eq.getSpecifications();
        if (specs != null && !specs.isEmpty() && layoutSpecsContainer != null) {
            layoutSpecsContainer.setVisibility(View.VISIBLE);
            if (tvEqSpecsTitle != null) tvEqSpecsTitle.setVisibility(View.VISIBLE);

            for (java.util.Map.Entry<String, String> entry : specs.entrySet()) {
                String val = entry.getValue();
                if (val == null || val.trim().isEmpty() || val.equalsIgnoreCase("null") || val.equalsIgnoreCase("N/A") || val.equalsIgnoreCase("-")) {
                    continue; // Skip empty specs dynamically
                }
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 3, 0, 3);

                TextView tvKey = new TextView(requireContext());
                tvKey.setText(entry.getKey() + ":");
                tvKey.setTextSize(12f);
                tvKey.setTextColor(ContextCompat.getColor(requireContext(), R.color.textHint));
                LinearLayout.LayoutParams halfParams = new LinearLayout.LayoutParams(
                        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tvKey.setLayoutParams(halfParams);

                TextView tvVal = new TextView(requireContext());
                tvVal.setText(entry.getValue() != null ? entry.getValue() : "—");
                tvVal.setTextSize(12f);
                tvVal.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
                tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
                tvVal.setLayoutParams(new LinearLayout.LayoutParams(
                        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                row.addView(tvKey);
                row.addView(tvVal);
                layoutSpecsContainer.addView(row);
            }
        }

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
                btnAction.setVisibility(View.GONE); // Hide button to prevent creating another ticket
            } else {
                btnAction.setVisibility(View.VISIBLE);
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
        
        // ── Deadline DatePicker ──────────────────────────────────────────────
        LinearLayout layoutDeadline   = v.findViewById(R.id.layoutDeadline);
        TextView     tvTicketDeadline = v.findViewById(R.id.tvTicketDeadline);
        final String[] selectedDeadline = {null};
        layoutDeadline.setOnClickListener(x -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDeadline[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvTicketDeadline.setText(selectedDeadline[0]);
                tvTicketDeadline.setTextColor(0xFF1E293B); // Dark text color
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

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
            if (selectedDeadline[0] != null) {
                newTicket.setDeadline(selectedDeadline[0]);
            }
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
