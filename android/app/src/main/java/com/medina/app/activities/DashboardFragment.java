package com.medina.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medina.app.R;

public class DashboardFragment extends Fragment {

    private TextView statOpen, statInProgress, statWaiting, statTesting;
    private View equipItem1, equipItem2, equipItem3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        statOpen = view.findViewById(R.id.statOpen);
        statInProgress = view.findViewById(R.id.statInProgress);
        statWaiting = view.findViewById(R.id.statWaiting);
        statTesting = view.findViewById(R.id.statTesting);

        equipItem1 = view.findViewById(R.id.equipItem1);
        equipItem2 = view.findViewById(R.id.equipItem2);
        equipItem3 = view.findViewById(R.id.equipItem3);

        // Simple item click handlers to showcase interactivity
        equipItem1.setOnClickListener(v -> Toast.makeText(getContext(), "Selected: 2PYEEUM75Y (Laptop)", Toast.LENGTH_SHORT).show());
        equipItem2.setOnClickListener(v -> Toast.makeText(getContext(), "Selected: HYVREJZTHO (Laptop)", Toast.LENGTH_SHORT).show());
        equipItem3.setOnClickListener(v -> Toast.makeText(getContext(), "Selected: DELL44001 (Laptop)", Toast.LENGTH_SHORT).show());

        return view;
    }
}
