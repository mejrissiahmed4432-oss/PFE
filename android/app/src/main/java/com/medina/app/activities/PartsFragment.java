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

    private List<PartRequest> allRequests = new ArrayList<>();
    private List<PartsAdapter.FlatPartItem> flatList = new ArrayList<>();
    private PartsAdapter adapter;

    // Search & Filter state
    private String searchQuery = "";
    private String categoryFilter = "All";

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

        // FAB Click listener
        fabRequestPart.setOnClickListener(v -> showRequestWizardDialog());

        // Fetch parts
        loadAllocatedParts();

        return view;
    }

    private void setupFiltersSpinners() {
        String[] categories = {"All", "Processors", "Memory", "Storage", "Graphics", "Motherboard", "Networking", "Others"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoryFilter.setAdapter(catAdapter);

        String[] types = {"All", "Hardware", "Peripheral", "Cable", "Tool"};
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
    }

    private void loadAllocatedParts() {
        if (userId == null || userId.isEmpty()) return;

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

        // 1. Filter items and group by category
        Map<String, List<PartsAdapter.FlatPartItem>> groups = new HashMap<>();

        for (PartRequest req : allRequests) {
            // Check status: only approved allocations
            String status = req.getStatus() != null ? req.getStatus() : "PENDING";
            if (!"APPROVED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(status)) {
                continue;
            }

            List<PartRequestItem> items = req.getItems();
            if (items == null) continue;

            for (PartRequestItem item : items) {
                // Filter: processed must be true, should not be returned yet
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

                // Create wrapper item
                PartsAdapter.FlatPartItem flatItem = new PartsAdapter.FlatPartItem();
                flatItem.isHeader = false;
                flatItem.parentRequest = req;
                flatItem.item = item;

                if (!groups.containsKey(category)) {
                    groups.put(category, new ArrayList<>());
                }
                groups.get(category).add(flatItem);
            }
        }

        // 2. Build flat list with headers
        for (Map.Entry<String, List<PartsAdapter.FlatPartItem>> entry : groups.entrySet()) {
            String category = entry.getKey();
            List<PartsAdapter.FlatPartItem> items = entry.getValue();

            PartsAdapter.FlatPartItem header = new PartsAdapter.FlatPartItem();
            header.isHeader = true;
            header.headerTitle = category;
            header.headerCount = items.size();

            flatList.add(header);
            flatList.addAll(items);
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
    public void onReturnToStock(PartRequest parent, PartRequestItem item) {
        new AlertDialog.Builder(getContext())
                .setTitle("Return to Stock")
                .setMessage("Are you sure you want to return " + item.getPartName() + " to the main inventory stock?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Call API to return equipment to stock
                    String eqId = item.getEquipmentId();
                    if (eqId != null && !eqId.isEmpty()) {
                        ApiClient.getApiService().returnPart(eqId).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                // Mark item as returned and update the request container
                                item.setReturned(true);
                                ApiClient.getApiService().updatePartRequest(parent.getId(), parent).enqueue(new Callback<PartRequest>() {
                                    @Override
                                    public void onResponse(Call<PartRequest> call, Response<PartRequest> response) {
                                        Toast.makeText(getContext(), "Item successfully returned to stock", Toast.LENGTH_SHORT).show();
                                        loadAllocatedParts();
                                    }

                                    @Override
                                    public void onFailure(Call<PartRequest> call, Throwable t) {
                                        loadAllocatedParts(); // refresh anyway
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(getContext(), "Failed to return part to stock", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Directly update request database state if no physical equipment identifier is bound
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
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRequestWizardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_request_part, null);
        builder.setView(v);

        AlertDialog dialog = builder.show();

        EditText etReqDescription = v.findViewById(R.id.etReqDescription);
        Spinner spReqPriority = v.findViewById(R.id.spReqPriority);
        EditText etItemPartName = v.findViewById(R.id.etItemPartName);
        EditText etItemBrand = v.findViewById(R.id.etItemBrand);
        Spinner spItemCategory = v.findViewById(R.id.spItemCategory);
        EditText etItemSpecs = v.findViewById(R.id.etItemSpecs);
        EditText etItemQty = v.findViewById(R.id.etItemQty);
        Button btnAddItem = v.findViewById(R.id.btnAddItemToList);
        LinearLayout layoutAddedItemsContainer = v.findViewById(R.id.layoutAddedItemsContainer);
        Button btnCancel = v.findViewById(R.id.btnCancelReq);
        Button btnSubmit = v.findViewById(R.id.btnSubmitReq);

        // Spinners binding
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, priorities);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReqPriority.setAdapter(prioAdapter);

        String[] categories = {"Processors", "Memory", "Storage", "Graphics", "Motherboard", "Networking", "Others"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spItemCategory.setAdapter(catAdapter);

        List<PartRequestItem> requestedItems = new ArrayList<>();

        // Add item helper action
        btnAddItem.setOnClickListener(view -> {
            String partName = etItemPartName.getText().toString().trim();
            String brand = etItemBrand.getText().toString().trim();
            String category = spItemCategory.getSelectedItem().toString();
            String specs = etItemSpecs.getText().toString().trim();
            String qtyStr = etItemQty.getText().toString().trim();

            if (partName.isEmpty()) {
                Toast.makeText(getContext(), "Part Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty = 1;
            if (!qtyStr.isEmpty()) {
                try {
                    qty = Integer.parseInt(qtyStr);
                } catch (NumberFormatException ignored) {}
            }

            PartRequestItem item = new PartRequestItem();
            item.setPartName(partName);
            item.setBrand(brand);
            item.setCategory(category);
            item.setSpecification(specs);
            item.setQuantity(qty);
            item.setProcessed(false);
            item.setReturned(false);

            requestedItems.add(item);

            // Add dynamic indicator label row to the dialog container
            TextView itemRow = new TextView(getContext());
            itemRow.setText("• " + qty + "x " + partName + " (" + category + ") - " + brand);
            itemRow.setTextColor(Color.parseColor(isNightMode() ? "#f8fafc" : "#0f172a"));
            itemRow.setPadding(0, 4, 0, 4);
            layoutAddedItemsContainer.addView(itemRow);

            // Clear item inputs for next input
            etItemPartName.setText("");
            etItemBrand.setText("");
            etItemSpecs.setText("");
            etItemQty.setText("");
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnSubmit.setOnClickListener(view -> {
            String desc = etReqDescription.getText().toString().trim();
            String priority = spReqPriority.getSelectedItem().toString();

            if (requestedItems.isEmpty()) {
                Toast.makeText(getContext(), "Please add at least one part item to the request", Toast.LENGTH_SHORT).show();
                return;
            }

            PartRequest req = new PartRequest();
            req.setDescription(desc);
            req.setPriority(priority);
            req.setItems(requestedItems);
            req.setStatus("PENDING");
            req.setRequesterId(userId);
            req.setRequesterName(userName);

            ApiClient.getApiService().createPartRequest(req).enqueue(new Callback<PartRequest>() {
                @Override
                public void onResponse(Call<PartRequest> call, Response<PartRequest> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Part request submitted successfully for approval", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadAllocatedParts();
                    }
                }

                @Override
                public void onFailure(Call<PartRequest> call, Throwable t) {
                    Toast.makeText(getContext(), "Failed to submit request", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}
