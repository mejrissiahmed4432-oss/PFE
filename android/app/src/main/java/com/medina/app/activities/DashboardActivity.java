package com.medina.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.medina.app.R;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private View btnOpenDrawer;
    private TextView tvPageTitle, tvUserAvatar;

    // Sidebar items and layouts
    private LinearLayout navDashboard, navSchedule, navReports, navParts, navRequests, navTickets, navSettings, navLogout;
    private ImageView iconDashboard, iconSchedule, iconReports, iconParts, iconRequests, iconTickets;
    private TextView labelDashboard, labelSchedule, labelReports, labelParts, labelRequests, labelTickets;
    private TextView badgeTickets;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = getSharedPreferences("medina_prefs", MODE_PRIVATE);

        // Bind layout views
        drawerLayout = findViewById(R.id.drawerLayout);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        tvUserAvatar = findViewById(R.id.tvUserAvatar);

        // Sidebar Items
        navDashboard = findViewById(R.id.navDashboard);
        navSchedule = findViewById(R.id.navSchedule);
        navReports = findViewById(R.id.navReports);
        navParts = findViewById(R.id.navParts);
        navRequests = findViewById(R.id.navRequests);
        navTickets = findViewById(R.id.navTickets);
        navSettings = findViewById(R.id.navSettings);
        navLogout = findViewById(R.id.navLogout);

        // Sidebar Icons
        iconDashboard = findViewById(R.id.iconDashboard);
        iconSchedule = findViewById(R.id.iconSchedule);
        iconReports = findViewById(R.id.iconReports);
        iconParts = findViewById(R.id.iconParts);
        iconRequests = findViewById(R.id.iconRequests);
        iconTickets = findViewById(R.id.iconTickets);

        // Sidebar Labels
        labelDashboard = findViewById(R.id.labelDashboard);
        labelSchedule = findViewById(R.id.labelSchedule);
        labelReports = findViewById(R.id.labelReports);
        labelParts = findViewById(R.id.labelParts);
        labelRequests = findViewById(R.id.labelRequests);
        labelTickets = findViewById(R.id.labelTickets);
        badgeTickets = findViewById(R.id.badgeTickets);

        // Display user info in topbar
        String userName = prefs.getString("user_name", "Morad Mejri");
        String initials = "";
        String[] parts = userName.split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            initials += parts[0].substring(0, 1).toUpperCase();
        }
        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials += parts[1].substring(0, 1).toUpperCase();
        }
        if (initials.isEmpty()) {
            initials = "MM";
        }
        tvUserAvatar.setText(initials);

        // Set Click listeners for Sidebar Navigation
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navDashboard.setOnClickListener(v -> selectMenuItem(0));
        navSchedule.setOnClickListener(v -> selectMenuItem(1));
        navReports.setOnClickListener(v -> selectMenuItem(2));
        navParts.setOnClickListener(v -> selectMenuItem(3));
        navRequests.setOnClickListener(v -> selectMenuItem(4));
        navTickets.setOnClickListener(v -> selectMenuItem(5));

        navSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Account Settings is clicked", Toast.LENGTH_SHORT).show();
        });

        navLogout.setOnClickListener(v -> {
            // Clear preferences and logout
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Load Default view: Support Tickets tab (index 5)
        selectMenuItem(5);
    }

    private void selectMenuItem(int position) {
        Fragment fragment = null;
        String title = "";

        // Reset backgrounds and colors
        resetMenuItems();

        switch (position) {
            case 0:
                fragment = new DashboardFragment();
                title = getString(R.string.service_desk);
                navDashboard.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconDashboard.setColorFilter(Color.WHITE);
                labelDashboard.setTextColor(Color.WHITE);
                break;
            case 1:
                fragment = new ScheduleFragment();
                title = getString(R.string.nav_schedule);
                navSchedule.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconSchedule.setColorFilter(Color.WHITE);
                labelSchedule.setTextColor(Color.WHITE);
                break;
            case 2:
                fragment = new ReportsFragment();
                title = getString(R.string.nav_reports);
                navReports.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconReports.setColorFilter(Color.WHITE);
                labelReports.setTextColor(Color.WHITE);
                break;
            case 3:
                fragment = new PartsFragment();
                title = getString(R.string.nav_parts);
                navParts.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconParts.setColorFilter(Color.WHITE);
                labelParts.setTextColor(Color.WHITE);
                break;
            case 4:
                fragment = new RequestsFragment();
                title = getString(R.string.nav_requests);
                navRequests.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconRequests.setColorFilter(Color.WHITE);
                labelRequests.setTextColor(Color.WHITE);
                break;
            case 5:
                fragment = new TicketsFragment();
                title = getString(R.string.nav_tickets);
                navTickets.setBackgroundResource(R.drawable.bg_nav_item_active);
                iconTickets.setColorFilter(Color.WHITE);
                labelTickets.setTextColor(Color.WHITE);
                break;
        }

        if (fragment != null) {
            tvPageTitle.setText(title);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void resetMenuItems() {
        int defaultColor = getResources().getColor(R.color.sidebarText);

        navDashboard.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconDashboard.setColorFilter(defaultColor);
        labelDashboard.setTextColor(defaultColor);

        navSchedule.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconSchedule.setColorFilter(defaultColor);
        labelSchedule.setTextColor(defaultColor);

        navReports.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconReports.setColorFilter(defaultColor);
        labelReports.setTextColor(defaultColor);

        navParts.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconParts.setColorFilter(defaultColor);
        labelParts.setTextColor(defaultColor);

        navRequests.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconRequests.setColorFilter(defaultColor);
        labelRequests.setTextColor(defaultColor);

        navTickets.setBackgroundResource(R.drawable.bg_nav_item_default);
        iconTickets.setColorFilter(defaultColor);
        labelTickets.setTextColor(defaultColor);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
