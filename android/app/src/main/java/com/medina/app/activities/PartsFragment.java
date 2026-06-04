package com.medina.app.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.PartRequest;
import com.medina.app.model.PartRequestItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PartsFragment extends Fragment implements PartsAdapter.OnPartActionListener {

    private SharedPreferences prefs;
    private String userId;
    private String userName;

    // UI elements
    private EditText etSearchParts;
    private ImageButton btnToggleFilters;
    private LinearLayout layoutFilterOptions;
    private Spinner spCategoryFilter, spTypeFilter;
    private RecyclerView rvParts;
    private View layoutEmptyParts;
    private View fabRequestPart;
    
    private ImageButton btnViewCards, btnViewTable;

    private List<PartRequest> allRequests = new ArrayList<>();
    private List<PartsAdapter.FlatPartItem> flatList = new ArrayList<>();
    private PartsAdapter adapter;

    // Search & Filter state
    private String searchQuery = "";
    private String categoryFilter = "All";
    private String typeFilter = "All";
    private boolean isTableView = false;
    private final Map<String, Boolean> expandedGroups = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parts, container, false);

        prefs = requireActivity().getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        userName = prefs.getString("user_name", "Technician");

        // Bind views
        etSearchParts = view.findViewById(R.id.etSearchParts);
        btnToggleFilters = view.findViewById(R.id.btnToggleFilters);
        layoutFilterOptions = view.findViewById(R.id.layoutFilterOptions);
        spCategoryFilter = view.findViewById(R.id.spCategoryFilter);
        spTypeFilter = view.findViewById(R.id.spTypeFilter);
        rvParts = view.findViewById(R.id.rvParts);
        layoutEmptyParts = view.findViewById(R.id.layoutEmptyParts);
        fabRequestPart = view.findViewById(R.id.fabRequestPart);
        btnViewCards = view.findViewById(R.id.btnViewCards);
        btnViewTable = view.findViewById(R.id.btnViewTable);

        // RecyclerView setup
        rvParts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PartsAdapter(flatList, this);
        rvParts.setAdapter(adapter);

        // Spinners setup
        setupFiltersSpinners();

        // Search text watcher
        etSearchParts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFiltersAndGrouping();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter Toggle listener
        btnToggleFilters.setOnClickListener(v -> {
            if (layoutFilterOptions.getVisibility() == View.VISIBLE) {
                layoutFilterOptions.setVisibility(View.GONE);
            } else {
                layoutFilterOptions.setVisibility(View.VISIBLE);
            }
        });

        // View switchers click listeners
        btnViewCards.setOnClickListener(v -> {
            if (isTableView) {
                isTableView = false;
                updateViewModeUI();
                applyFiltersAndGrouping();
            }
        });

        btnViewTable.setOnClickListener(v -> {
            if (!isTableView) {
                isTableView = true;
                updateViewModeUI();
                applyFiltersAndGrouping();
            }
        });

        updateViewModeUI();

        // FAB Click listener
        fabRequestPart.setOnClickListener(v -> showRequestWizardDialog());

        // Fetch parts
        loadAllocatedParts();

        return view;
    }

    private void setupFiltersSpinners() {
        String[] categories = {"All", "NETWORK", "STORAGE", "COMPONENT", "PERIPHERAL", "SERVER & DEVICE"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoryFilter.setAdapter(catAdapter);

        String[] types = {"All", "Router", "Switch", "Access Point", "SSD", "HDD", "NVMe", "USB Flash", "RAM", "CPU", "GPU", "Motherboard", "NIC", "Keyboard", "Mouse", "Printer", "Scanner", "Headset", "Webcam", "HDMI Cable", "USB Cable", "Charger", "Adapter", "USB Hub", "Laptop", "System Unit", "Desktop", "Server", "Monitor"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTypeFilter.setAdapter(typeAdapter);

        // Select actions
        spCategoryFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                categoryFilter = categories[position];
                applyFiltersAndGrouping();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spTypeFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                typeFilter = types[position];
                applyFiltersAndGrouping();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateViewModeUI() {
        if (adapter != null) {
            adapter.setTableView(isTableView);
        }
        if (getContext() != null) {
            if (isTableView) {
                btnViewTable.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorPrimary));
                btnViewCards.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.textSecondary));
            } else {
                btnViewCards.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorPrimary));
                btnViewTable.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.textSecondary));
            }
        }
    }

    private void loadAllocatedParts() {
        if (userId == null || userId.isEmpty()) {
            userId = prefs.getString("user_id", "");
        }
        if (userId == null || userId.isEmpty()) {
            ApiClient.getApiService().getCurrentUser().enqueue(new Callback<com.medina.app.model.User>() {
                @Override
                public void onResponse(Call<com.medina.app.model.User> call, Response<com.medina.app.model.User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        userId = response.body().getId();
                        prefs.edit().putString("user_id", userId).apply();
                        loadAllocatedParts();
                    }
                }
                @Override
                public void onFailure(Call<com.medina.app.model.User> call, Throwable t) {}
            });
            return;
        }

        ApiClient.getApiService().getMyPartRequests(userId).enqueue(new Callback<List<PartRequest>>() {
            @Override
            public void onResponse(Call<List<PartRequest>> call, Response<List<PartRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRequests = response.body();
                    applyFiltersAndGrouping();
                }
            }

            @Override
            public void onFailure(Call<List<PartRequest>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to fetch parts allocation", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFiltersAndGrouping() {
        flatList.clear();

        // 1. Filter items
        List<PartsAdapter.FlatPartItem> filteredItems = new ArrayList<>();
        for (PartRequest req : allRequests) {
            String status = req.getStatus() != null ? req.getStatus() : "PENDING";
            if (!"APPROVED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(status)) {
                continue;
            }

            List<PartRequestItem> items = req.getItems();
            if (items == null) continue;

            for (PartRequestItem item : items) {
                Boolean processed = item.getProcessed() != null ? item.getProcessed() : false;
                if (!processed) {
                    continue;
                }

                // Apply text search
                String name = item.getPartName() != null ? item.getPartName().toLowerCase() : "";
                String brand = item.getBrand() != null ? item.getBrand().toLowerCase() : "";
                if (!searchQuery.isEmpty() && !name.contains(searchQuery.toLowerCase()) && !brand.contains(searchQuery.toLowerCase())) {
                    continue;
                }

                // Apply Category spinner filter
                String category = item.getCategory() != null ? item.getCategory() : "Others";
                if (!"All".equalsIgnoreCase(categoryFilter) && !categoryFilter.equalsIgnoreCase(category)) {
                    continue;
                }

                // Apply Type spinner filter
                String type = item.getType() != null ? item.getType() : "Hardware";
                if (!"All".equalsIgnoreCase(typeFilter) && !typeFilter.equalsIgnoreCase(type)) {
                    continue;
                }

                PartsAdapter.FlatPartItem flatItem = new PartsAdapter.FlatPartItem();
                flatItem.isHeader = false;
                flatItem.parentRequest = req;
                flatItem.item = item;
                filteredItems.add(flatItem);
            }
        }

        if (isTableView) {
            // Group by name + brand + category
            Map<String, List<PartsAdapter.FlatPartItem>> groups = new HashMap<>();
            for (PartsAdapter.FlatPartItem flatItem : filteredItems) {
                PartRequestItem item = flatItem.item;
                String partName = item.getPartName() != null ? item.getPartName() : "Unknown";
                String brand = item.getBrand() != null ? item.getBrand() : "Unknown";
                String category = item.getCategory() != null ? item.getCategory() : "Others";
                String key = partName + "||" + brand + "||" + category;

                if (!groups.containsKey(key)) {
                    groups.put(key, new ArrayList<>());
                }
                groups.get(key).add(flatItem);
            }

            // Build flat list with headers for Table View
            for (Map.Entry<String, List<PartsAdapter.FlatPartItem>> entry : groups.entrySet()) {
                String key = entry.getKey();
                List<PartsAdapter.FlatPartItem> items = entry.getValue();

                String[] parts = key.split("\\|\\|");
                String title = parts[0] + " (" + parts[1] + ")";

                PartsAdapter.FlatPartItem header = new PartsAdapter.FlatPartItem();
                header.isHeader = true;
                header.headerTitle = title;
                header.headerCount = items.size();
                header.groupKey = key;
                
                boolean isExpanded = items.size() == 1 || Boolean.TRUE.equals(expandedGroups.get(key));
                header.isExpanded = isExpanded;

                flatList.add(header);
                if (isExpanded) {
                    flatList.addAll(items);
                }
            }
        } else {
            // Group by category (Card View)
            Map<String, List<PartsAdapter.FlatPartItem>> groups = new HashMap<>();
            for (PartsAdapter.FlatPartItem flatItem : filteredItems) {
                String category = flatItem.item.getCategory() != null ? flatItem.item.getCategory() : "Others";
                if (!groups.containsKey(category)) {
                    groups.put(category, new ArrayList<>());
                }
                groups.get(category).add(flatItem);
            }

            // Build flat list with headers for Card View
            for (Map.Entry<String, List<PartsAdapter.FlatPartItem>> entry : groups.entrySet()) {
                String category = entry.getKey();
                List<PartsAdapter.FlatPartItem> items = entry.getValue();

                PartsAdapter.FlatPartItem header = new PartsAdapter.FlatPartItem();
                header.isHeader = true;
                header.headerTitle = category;
                header.headerCount = items.size();
                header.isExpanded = true;

                flatList.add(header);
                flatList.addAll(items);
            }
        }

        adapter.updateList(flatList);

        if (flatList.isEmpty()) {
            layoutEmptyParts.setVisibility(View.VISIBLE);
            rvParts.setVisibility(View.GONE);
        } else {
            layoutEmptyParts.setVisibility(View.GONE);
            rvParts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onGroupHeaderClick(PartsAdapter.FlatPartItem headerItem) {
        if (headerItem.groupKey != null) {
            boolean isExpanded = Boolean.TRUE.equals(expandedGroups.get(headerItem.groupKey));
            expandedGroups.put(headerItem.groupKey, !isExpanded);
            applyFiltersAndGrouping();
        }
    }

    @Override
    public void onPartItemClick(PartRequest parent, PartRequestItem item) {
        showPartDetailDialog(parent, item);
    }

    private void showPartDetailDialog(PartRequest parent, PartRequestItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View detailView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_part_detail, null);
        builder.setView(detailView);
        AlertDialog detailDialog = builder.create();

        if (detailDialog.getWindow() != null) {
            detailDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDlgName = detailView.findViewById(R.id.tvPartDlgName);
        TextView tvDlgCategoryBrand = detailView.findViewById(R.id.tvPartDlgCategoryBrand);
        TextView tvDlgSN = detailView.findViewById(R.id.tvPartDlgSN);
        TextView tvDlgSpecs = detailView.findViewById(R.id.tvPartDlgSpecs);
        TextView tvDlgLocation = detailView.findViewById(R.id.tvPartDlgLocation);
        TextView tvDlgQty = detailView.findViewById(R.id.tvPartDlgQty);
        TextView tvDlgStatus = detailView.findViewById(R.id.tvPartDlgStatus);
        TextView tvDlgReqId = detailView.findViewById(R.id.tvPartDlgReqId);
        TextView tvDlgReqDesc = detailView.findViewById(R.id.tvPartDlgReqDesc);
        ImageButton btnClose = detailView.findViewById(R.id.btnPartDlgClose);
        Button btnReturn = detailView.findViewById(R.id.btnPartDlgReturn);

        tvDlgName.setText(item.getPartName());
        tvDlgCategoryBrand.setText("Category: " + (item.getCategory() != null ? item.getCategory() : "Others") + 
                " | Brand: " + (item.getBrand() != null ? item.getBrand() : "Unknown"));
        
        String sn = item.getMatchedSerialNumber() != null ? item.getMatchedSerialNumber() : "S/N: Not Allocated Yet";
        tvDlgSN.setText(sn);
        
        tvDlgSpecs.setText(item.getSpecification() != null ? item.getSpecification() : "No specifications provided");
        tvDlgLocation.setText("📍 Allocated Mode");
        tvDlgQty.setText(String.valueOf(item.getQuantity() != null ? item.getQuantity() : 1));

        if (item.getReturned() != null && item.getReturned()) {
            tvDlgStatus.setText("Returned to Stock");
            tvDlgStatus.setTextColor(Color.parseColor("#94a3b8"));
            btnReturn.setVisibility(View.GONE);
        } else {
            tvDlgStatus.setText("Allocated / In Use");
            tvDlgStatus.setTextColor(Color.parseColor("#10b981"));
            btnReturn.setVisibility(View.VISIBLE);
        }

        tvDlgReqId.setText("#" + (parent.getId() != null ? parent.getId() : "N/A"));
        tvDlgReqDesc.setText(parent.getDescription() != null ? parent.getDescription() : "No description provided");

        btnClose.setOnClickListener(v -> detailDialog.dismiss());
        
        btnReturn.setOnClickListener(v -> {
            detailDialog.dismiss();
            onReturnToStock(parent, item);
        });

        detailDialog.show();
    }

    @Override
    public void onReturnToStock(PartRequest parent, PartRequestItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View confirmView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirm_return, null);
        builder.setView(confirmView);
        AlertDialog confirmDialog = builder.create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = confirmView.findViewById(R.id.tvConfirmTitle);
        TextView tvMessage = confirmView.findViewById(R.id.tvConfirmMessage);
        Button btnCancel = confirmView.findViewById(R.id.btnConfirmCancel);
        Button btnProceed = confirmView.findViewById(R.id.btnConfirmProceed);

        tvTitle.setText("Return to Stock");
        tvMessage.setText("Are you sure you want to return " + item.getPartName() + " to the main inventory stock?");

        btnCancel.setOnClickListener(v -> confirmDialog.dismiss());

        btnProceed.setOnClickListener(v -> {
            confirmDialog.dismiss();
            
            String eqId = item.getEquipmentId();
            if (eqId != null && !eqId.isEmpty()) {
                ApiClient.getApiService().returnPart(eqId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        item.setReturned(true);
                        ApiClient.getApiService().updatePartRequest(parent.getId(), parent).enqueue(new Callback<PartRequest>() {
                            @Override
                            public void onResponse(Call<PartRequest> call, Response<PartRequest> response) {
                                Toast.makeText(getContext(), "Item successfully returned to stock", Toast.LENGTH_SHORT).show();
                                loadAllocatedParts();
                            }
                            @Override
                            public void onFailure(Call<PartRequest> call, Throwable t) {
                                loadAllocatedParts();
                            }
                        });
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Failed to return part to stock", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                item.setReturned(true);
                ApiClient.getApiService().updatePartRequest(parent.getId(), parent).enqueue(new Callback<PartRequest>() {
                    @Override
                    public void onResponse(Call<PartRequest> call, Response<PartRequest> response) {
                        Toast.makeText(getContext(), "Item successfully returned to stock", Toast.LENGTH_SHORT).show();
                        loadAllocatedParts();
                    }
                    @Override
                    public void onFailure(Call<PartRequest> call, Throwable t) {
                        loadAllocatedParts();
                    }
                });
            }
        });

        confirmDialog.show();
    }

    private void showRequestWizardDialog() {
        if (getContext() == null) return;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_request_part_wizard, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(v)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // ── Step indicator UI ─────────────────────────────────────────────────
        TextView tvSubtitle  = v.findViewById(R.id.tvWizardSubtitle);
        View     dotStep1    = v.findViewById(R.id.dotStep1);
        View     dotStep2    = v.findViewById(R.id.dotStep2);

        // ── Step containers ───────────────────────────────────────────────────
        View scrollStep1 = v.findViewById(R.id.scrollStep1);
        View scrollStep2 = v.findViewById(R.id.scrollStep2);

        // ── Step 1 views ──────────────────────────────────────────────────────
        EditText     etDesc           = v.findViewById(R.id.etWizardDescription);
        Spinner      spPriority       = v.findViewById(R.id.spWizardPriority);
        LinearLayout layoutEqCard     = v.findViewById(R.id.layoutWizardEqCard);
        TextView     tvEqName         = v.findViewById(R.id.tvWizardEqName);
        TextView     tvEqSerial       = v.findViewById(R.id.tvWizardEqSerial);
        Button       btnChangeEq      = v.findViewById(R.id.btnWizardChangeEq);
        LinearLayout layoutEqSearch   = v.findViewById(R.id.layoutWizardEqSearch);
        EditText     etEqSearch       = v.findViewById(R.id.etWizardEqSearch);
        LinearLayout layoutEqList     = v.findViewById(R.id.layoutWizardEqList);

        // ── Step 2 views ──────────────────────────────────────────────────────
        EditText     etPartName       = v.findViewById(R.id.etWizardPartName);
        EditText     etBrand          = v.findViewById(R.id.etWizardBrand);
        EditText     etQty            = v.findViewById(R.id.etWizardQty);
        Spinner      spItemCategory   = v.findViewById(R.id.spWizardItemCategory);
        EditText     etSpecs          = v.findViewById(R.id.etWizardSpecs);
        Button       btnAddItem       = v.findViewById(R.id.btnWizardAddItem);
        LinearLayout layoutCart       = v.findViewById(R.id.layoutWizardCart);

        // ── Nav buttons ───────────────────────────────────────────────────────
        Button btnCancel = v.findViewById(R.id.btnWizardCancel);
        Button btnBack   = v.findViewById(R.id.btnWizardBack);
        Button btnNext   = v.findViewById(R.id.btnWizardNext);

        // ── Priority spinner ─────────────────────────────────────────────────
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(prioAdapter);
        spPriority.setSelection(1);

        // ── Item category spinner ─────────────────────────────────────────────
        String[] itemCategories = {"NETWORK", "STORAGE", "COMPONENT", "PERIPHERAL", "SERVER & DEVICE", "Others"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, itemCategories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spItemCategory.setAdapter(catAdapter);

        // ── Equipment data ────────────────────────────────────────────────────
        final List<com.medina.app.model.Equipment> loadedEquipment = new ArrayList<>();
        final com.medina.app.model.Equipment[] selectedEquipment = {null};

        // Helper: refresh equipment card/search visibility
        Runnable refreshEqUI = () -> {
            if (selectedEquipment[0] != null) {
                com.medina.app.model.Equipment eq = selectedEquipment[0];
                tvEqName.setText(eq.getEquipmentName() != null ? eq.getEquipmentName() : "Unknown");
                String sn = eq.getSerialNumber() != null ? eq.getSerialNumber() : "—";
                tvEqSerial.setText("S/N: " + sn);
                layoutEqCard.setVisibility(View.VISIBLE);
                layoutEqSearch.setVisibility(View.GONE);
            } else {
                layoutEqCard.setVisibility(View.GONE);
                layoutEqSearch.setVisibility(View.VISIBLE);
            }
        };

        // Helper: rebuild the equipment search list
        Runnable rebuildEqList = () -> {
            layoutEqList.removeAllViews();
            String q = etEqSearch.getText().toString().trim().toLowerCase();
            boolean any = false;
            for (com.medina.app.model.Equipment eq : loadedEquipment) {
                String name   = eq.getEquipmentName() != null ? eq.getEquipmentName().toLowerCase() : "";
                String serial = eq.getSerialNumber()  != null ? eq.getSerialNumber().toLowerCase()  : "";
                String type   = eq.getType()           != null ? eq.getType().toLowerCase()           : "";
                if (!q.isEmpty() && !name.contains(q) && !serial.contains(q) && !type.contains(q)) continue;
                View row = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, layoutEqList, false);
                ((TextView) row.findViewById(android.R.id.text1))
                        .setText(eq.getEquipmentName() != null ? eq.getEquipmentName() : "Unknown");
                String sub = (eq.getSerialNumber() != null ? eq.getSerialNumber() : "—")
                        + (eq.getType() != null ? " • " + eq.getType() : "");
                ((TextView) row.findViewById(android.R.id.text2)).setText(sub);
                row.setPadding(8, 10, 8, 10);
                row.setOnClickListener(x -> {
                    selectedEquipment[0] = eq;
                    refreshEqUI.run();
                });
                layoutEqList.addView(row);
                any = true;
            }
            if (!any) {
                TextView tv = new TextView(getContext());
                tv.setText(q.isEmpty() ? "Loading equipment…" : "No match for \"" + q + "\"");
                tv.setPadding(12, 14, 12, 14);
                tv.setTextColor(0xFF94a3b8);
                layoutEqList.addView(tv);
            }
        };

        refreshEqUI.run();
        rebuildEqList.run();

        // Load equipment from API
        ApiClient.getApiService().getAllEquipment().enqueue(new Callback<List<com.medina.app.model.Equipment>>() {
            @Override
            public void onResponse(Call<List<com.medina.app.model.Equipment>> call,
                                   Response<List<com.medina.app.model.Equipment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    loadedEquipment.clear();
                    loadedEquipment.addAll(response.body());
                    rebuildEqList.run();
                }
            }
            @Override public void onFailure(Call<List<com.medina.app.model.Equipment>> call, Throwable t) {}
        });

        etEqSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { rebuildEqList.run(); }
        });

        btnChangeEq.setOnClickListener(x -> {
            selectedEquipment[0] = null;
            etEqSearch.setText("");
            refreshEqUI.run();
            rebuildEqList.run();
        });

        // ── Cart (step 2) ─────────────────────────────────────────────────────
        List<PartRequestItem> cartItems = new ArrayList<>();

        final Runnable[] refreshCart = new Runnable[1];
        refreshCart[0] = () -> {
            layoutCart.removeAllViews();
            if (cartItems.isEmpty()) {
                TextView tv = new TextView(getContext());
                tv.setText("No parts added yet");
                tv.setPadding(8, 12, 8, 12);
                tv.setTextColor(0xFF94a3b8);
                layoutCart.addView(tv);
            } else {
                for (int i = 0; i < cartItems.size(); i++) {
                    PartRequestItem item = cartItems.get(i);
                    final int idx = i;
                    View row = LayoutInflater.from(getContext())
                            .inflate(android.R.layout.simple_list_item_2, layoutCart, false);
                    ((TextView) row.findViewById(android.R.id.text1)).setText(
                            item.getQuantity() + "x " + item.getPartName()
                                    + (item.getBrand() != null && !item.getBrand().isEmpty()
                                    ? " (" + item.getBrand() + ")" : ""));
                    String sub2 = item.getCategory() != null ? item.getCategory() : "";
                    if (item.getSpecification() != null && !item.getSpecification().isEmpty())
                        sub2 += (sub2.isEmpty() ? "" : " · ") + item.getSpecification();
                    ((TextView) row.findViewById(android.R.id.text2)).setText(sub2);
                    row.setPadding(8, 8, 8, 8);
                    // Long-click to remove
                    row.setOnLongClickListener(x -> {
                        cartItems.remove(idx);
                        refreshCart[0].run();
                        return true;
                    });
                    layoutCart.addView(row);
                }
            }
        };
        refreshCart[0].run();

        btnAddItem.setOnClickListener(x -> {
            String name = etPartName.getText().toString().trim();
            if (name.isEmpty()) {
                etPartName.setError("Part name required");
                return;
            }
            String brand = etBrand.getText().toString().trim();
            String specs = etSpecs.getText().toString().trim();
            String category = itemCategories[spItemCategory.getSelectedItemPosition()];
            int qty = 1;
            try { qty = Integer.parseInt(etQty.getText().toString().trim()); } catch (NumberFormatException ignored) {}

            PartRequestItem item = new PartRequestItem();
            item.setPartName(name);
            item.setBrand(brand.isEmpty() ? null : brand);
            item.setCategory(category);
            item.setSpecification(specs.isEmpty() ? null : specs);
            item.setQuantity(qty);
            item.setProcessed(false);
            item.setReturned(false);

            // Link equipment if selected
            if (selectedEquipment[0] != null) {
                item.setEquipmentId(selectedEquipment[0].getId());
                item.setMatchedEquipmentName(selectedEquipment[0].getEquipmentName());
                item.setMatchedSerialNumber(selectedEquipment[0].getSerialNumber());
            }

            cartItems.add(item);
            refreshCart[0].run();

            // Clear item fields
            etPartName.setText("");
            etBrand.setText("");
            etQty.setText("");
            etSpecs.setText("");
            spItemCategory.setSelection(0);

            Toast.makeText(getContext(), "\"" + name + "\" added to cart", Toast.LENGTH_SHORT).show();
        });

        // ── Step navigation ───────────────────────────────────────────────────
        final int[] currentStep = {1};

        Runnable applyStep = () -> {
            boolean onStep1 = currentStep[0] == 1;
            scrollStep1.setVisibility(onStep1 ? View.VISIBLE : View.GONE);
            scrollStep2.setVisibility(onStep1 ? View.GONE : View.VISIBLE);
            btnBack.setVisibility(onStep1 ? View.GONE : View.VISIBLE);
            btnNext.setText(onStep1 ? "Next →" : "Submit");
            tvSubtitle.setText(onStep1
                    ? "Step 1 of 2 · Request Details"
                    : "Step 2 of 2 · Add Parts");
            int activeColor = 0xFF2563EB;   // colorPrimary
            int inactiveColor = 0xFF94a3b8; // textHint
            dotStep1.setBackgroundColor(onStep1 ? activeColor : inactiveColor);
            dotStep2.setBackgroundColor(onStep1 ? inactiveColor : activeColor);
        };

        btnBack.setOnClickListener(x -> {
            currentStep[0] = 1;
            applyStep.run();
        });

        btnNext.setOnClickListener(x -> {
            if (currentStep[0] == 1) {
                // Validate step 1
                currentStep[0] = 2;
                applyStep.run();
            } else {
                // Submit
                if (cartItems.isEmpty()) {
                    Toast.makeText(getContext(), "Add at least one part to the cart", Toast.LENGTH_SHORT).show();
                    return;
                }
                PartRequest req = new PartRequest();
                req.setDescription(etDesc.getText().toString().trim());
                req.setPriority(priorities[spPriority.getSelectedItemPosition()]);
                req.setItems(cartItems);
                req.setStatus("PENDING");
                req.setRequesterId(userId);
                req.setRequesterName(userName);

                ApiClient.getApiService().createPartRequest(req).enqueue(new Callback<PartRequest>() {
                    @Override
                    public void onResponse(Call<PartRequest> call, Response<PartRequest> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    "Part request submitted for approval", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadAllocatedParts();
                        } else {
                            Toast.makeText(getContext(), "Failed to submit request", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<PartRequest> call, Throwable t) {
                        Toast.makeText(getContext(), "Network error, please retry", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnCancel.setOnClickListener(x -> dialog.dismiss());

        dialog.show();
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}
