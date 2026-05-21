package com.medina.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
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
import androidx.cardview.widget.CardView;

import com.medina.app.R;
import com.medina.app.api.ApiClient;
import com.medina.app.model.Alert;
import com.medina.app.model.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private View btnOpenDrawer;
    private TextView tvPageTitle, tvUserAvatar;

    // Topbar Actions & Dropdowns
    private View layoutUserAvatar;
    private CardView profileDropdownCard;
    private TextView tvDropdownAvatar, tvDropdownName, tvDropdownEmail;
    private View btnAI, btnLanguage, btnModeToggle, btnAlert, btnNotification;
    private View menuProfile, menuSettings, menuLogout;

    // Badge Views
    private TextView tvAlertBadge, tvNotificationBadge;
    private CardView notificationDropdownCard;
    private LinearLayout layoutNotificationsList;
    private View btnMarkAllRead, btnClearAllNotifications;

    // Sidebar items and layouts
    private LinearLayout navDashboard, navSchedule, navReports, navParts, navRequests, navTickets, navSettings, navLogout;
    private ImageView iconDashboard, iconSchedule, iconReports, iconParts, iconRequests, iconTickets;
    private TextView labelDashboard, labelSchedule, labelReports, labelParts, labelRequests, labelTickets;
    private TextView badgeTickets;

    private SharedPreferences prefs;
    private Handler syncHandler;
    private Runnable syncRunnable;
    private static final int SYNC_INTERVAL_MS = 15000; // Poll notifications/alerts every 15s

    private List<Notification> notificationsList = new ArrayList<>();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences tempPrefs = getSharedPreferences("medina_prefs", MODE_PRIVATE);
        boolean isDark = tempPrefs.getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                   : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = getSharedPreferences("medina_prefs", MODE_PRIVATE);

        // Bind layout views
        drawerLayout = findViewById(R.id.drawerLayout);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        tvUserAvatar = findViewById(R.id.tvUserAvatar);

        // Topbar Actions
        layoutUserAvatar = findViewById(R.id.layoutUserAvatar);
        profileDropdownCard = findViewById(R.id.profileDropdownCard);
        tvDropdownAvatar = findViewById(R.id.tvDropdownAvatar);
        tvDropdownName = findViewById(R.id.tvDropdownName);
        tvDropdownEmail = findViewById(R.id.tvDropdownEmail);

        btnAI = findViewById(R.id.btnAI);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnModeToggle = findViewById(R.id.btnModeToggle);
        btnAlert = findViewById(R.id.btnAlert);
        btnNotification = findViewById(R.id.btnNotification);

        // Badges & Dropdown
        tvAlertBadge = findViewById(R.id.tvAlertBadge);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        notificationDropdownCard = findViewById(R.id.notificationDropdownCard);
        layoutNotificationsList = findViewById(R.id.layoutNotificationsList);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAllNotifications = findViewById(R.id.btnClearAllNotifications);

        // Dropdown Items
        menuProfile = findViewById(R.id.menuProfile);
        menuSettings = findViewById(R.id.menuSettings);
        menuLogout = findViewById(R.id.menuLogout);

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

        // Display user info in topbar and dropdown
        String userName = prefs.getString("user_name", "Morad Mejri");
        updateUserAvatarInitials(userName);

        // Set Click listeners for Sidebar Navigation
        btnOpenDrawer.setOnClickListener(v -> {
            closeDropdowns();
            drawerLayout.openDrawer(GravityCompat.START);
        });

        navDashboard.setOnClickListener(v -> selectMenuItem(0));
        navSchedule.setOnClickListener(v -> selectMenuItem(1));
        navReports.setOnClickListener(v -> selectMenuItem(2));
        navParts.setOnClickListener(v -> selectMenuItem(3));
        navRequests.setOnClickListener(v -> selectMenuItem(4));
        navTickets.setOnClickListener(v -> selectMenuItem(5));

        navSettings.setOnClickListener(v -> {
            closeDropdowns();
            selectMenuItem(6);
        });

        navLogout.setOnClickListener(v -> {
            performLogout();
        });

        // Profile Dropdown Toggle
        layoutUserAvatar.setOnClickListener(v -> {
            notificationDropdownCard.setVisibility(View.GONE);
            if (profileDropdownCard.getVisibility() == View.VISIBLE) {
                profileDropdownCard.setVisibility(View.GONE);
            } else {
                profileDropdownCard.setVisibility(View.VISIBLE);
            }
        });

        // Notifications Dropdown Toggle
        btnNotification.setOnClickListener(v -> {
            profileDropdownCard.setVisibility(View.GONE);
            if (notificationDropdownCard.getVisibility() == View.VISIBLE) {
                notificationDropdownCard.setVisibility(View.GONE);
            } else {
                notificationDropdownCard.setVisibility(View.VISIBLE);
                loadNotifications(); // Reload fresh dropdown content on open
            }
        });

        // Alert topbar click: navigate directly to Alerts log fragment, matching the web behavior
        btnAlert.setOnClickListener(v -> {
            closeDropdowns();
            selectMenuItem(8);
        });

        // Dropdown Items Clicks
        menuProfile.setOnClickListener(v -> {
            profileDropdownCard.setVisibility(View.GONE);
            selectMenuItem(7); // Navigate to My Profile page
        });

        menuSettings.setOnClickListener(v -> {
            profileDropdownCard.setVisibility(View.GONE);
            selectMenuItem(6); // Navigate to Settings page
        });

        menuLogout.setOnClickListener(v -> {
            profileDropdownCard.setVisibility(View.GONE);
            performLogout();
        });

        // Bind Actions Clicks
        btnAI.setOnClickListener(v -> Toast.makeText(this, "AI Ecosystem analysis is active.", Toast.LENGTH_SHORT).show());
        btnLanguage.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, btnLanguage);
            popup.getMenu().add(0, 1, 0, "English");
            popup.getMenu().add(0, 2, 0, "Français");
            popup.setOnMenuItemClickListener(item -> {
                String newLang = item.getItemId() == 1 ? "en" : "fr";
                LocaleHelper.setLocale(this, newLang);
                recreate();
                return true;
            });
            popup.show();
        });
        btnModeToggle.setOnClickListener(v -> {
            boolean isDarkTheme = prefs.getBoolean("dark_mode", false);
            boolean newDarkTheme = !isDarkTheme;
            prefs.edit().putBoolean("dark_mode", newDarkTheme).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                newDarkTheme ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                             : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );
            Toast.makeText(this, newDarkTheme ? "Dark mode activated" : "Light mode activated", Toast.LENGTH_SHORT).show();
            recreate();
        });

        // Notifications action buttons
        btnMarkAllRead.setOnClickListener(v -> handleMarkAllNotificationsRead());
        btnClearAllNotifications.setOnClickListener(v -> handleClearAllNotifications());

        // Default view: Dashboard
        selectMenuItem(0);

        // Setup background status syncing
        setupPeriodicSync();
    }

    private void closeDropdowns() {
        profileDropdownCard.setVisibility(View.GONE);
        notificationDropdownCard.setVisibility(View.GONE);
    }

    private void performLogout() {
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void updateUserAvatarInitials(String name) {
        String initials = "";
        String[] parts = name.split(" ");
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

        if (tvDropdownAvatar != null) {
            tvDropdownAvatar.setText(initials);
        }
        if (tvDropdownName != null) {
            tvDropdownName.setText(name);
        }
        String email = prefs.getString("user_email", "morad.mejri@medina.com");
        if (tvDropdownEmail != null) {
            tvDropdownEmail.setText(email);
        }
    }

    private void setupPeriodicSync() {
        syncHandler = new Handler(Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                syncNotificationsAndAlerts();
                syncHandler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        };
        syncHandler.post(syncRunnable);
    }

    private void syncNotificationsAndAlerts() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userRole = prefs.getString("user_role", null);

        // Fetch alerts counts
        ApiClient.getApiService().getAlerts(null, userRole).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int activeAlerts = 0;
                    for (Alert a : response.body()) {
                        if ("ACTIVE".equalsIgnoreCase(a.getStatus())) {
                            activeAlerts++;
                        }
                    }
                    updateAlertBadgeCount(activeAlerts);
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {}
        });

        // Fetch notifications count
        ApiClient.getApiService().getNotifications(null, userRole).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    notificationsList = response.body();
                    int unread = 0;
                    for (Notification n : notificationsList) {
                        if (!n.isRead()) {
                            unread++;
                        }
                    }
                    updateNotificationBadgeCount(unread);
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {}
        });
    }

    public void updateAlertBadgeCount(int count) {
        if (count > 0) {
            tvAlertBadge.setText(String.valueOf(count));
            tvAlertBadge.setVisibility(View.VISIBLE);
        } else {
            tvAlertBadge.setVisibility(View.GONE);
        }
    }

    public void updateNotificationBadgeCount(int count) {
        if (count > 0) {
            tvNotificationBadge.setText(String.valueOf(count));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void loadNotifications() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userRole = prefs.getString("user_role", null);

        ApiClient.getApiService().getNotifications(null, userRole).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    notificationsList = response.body();
                } else {
                    loadFallbackNotifications();
                }
                populateNotificationsDropdown();
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                loadFallbackNotifications();
                populateNotificationsDropdown();
            }
        });
    }

    private void loadFallbackNotifications() {
        if (notificationsList.isEmpty()) {
            notificationsList.clear();
            notificationsList.add(new Notification("1", "Main Generator Shutdown", "Alert registered on main backup generator.", "2 mins ago", false));
            notificationsList.add(new Notification("2", "Password Changed", "Your MedinaFlux profile security was updated.", "1 hour ago", true));
            notificationsList.add(new Notification("3", "Server Ticket #1080", "Equipment database sync failure reported.", "Yesterday", false));
        }
    }

    private void populateNotificationsDropdown() {
        layoutNotificationsList.removeAllViews();

        if (notificationsList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No new notifications");
            tvEmpty.setTextColor(getResources().getColor(R.color.textHint));
            tvEmpty.setTextSize(12);
            tvEmpty.setPadding(32, 32, 32, 32);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            layoutNotificationsList.addView(tvEmpty);
            updateNotificationBadgeCount(0);
            return;
        }

        int unreadCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Notification notification : notificationsList) {
            if (!notification.isRead()) {
                unreadCount++;
            }

            View row = inflater.inflate(R.layout.item_dropdown_notification, layoutNotificationsList, false);

            View viewUnreadDot = row.findViewById(R.id.viewUnreadDot);
            TextView tvDropdownNotifTitle = row.findViewById(R.id.tvDropdownNotifTitle);
            TextView tvDropdownNotifMsg = row.findViewById(R.id.tvDropdownNotifMsg);
            TextView tvDropdownNotifTime = row.findViewById(R.id.tvDropdownNotifTime);

            tvDropdownNotifTitle.setText(notification.getTitle());
            tvDropdownNotifMsg.setText(notification.getMessage());
            tvDropdownNotifTime.setText(notification.getCreatedAt());

            viewUnreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Row click: mark notification as read
            row.setOnClickListener(v -> {
                if (!notification.isRead()) {
                    handleMarkNotificationSingleRead(notification, viewUnreadDot);
                }
            });

            layoutNotificationsList.addView(row);
        }

        updateNotificationBadgeCount(unreadCount);
    }

    private void handleMarkNotificationSingleRead(Notification notif, View dot) {
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().markNotificationAsRead(notif.getId()).enqueue(new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                if (response.isSuccessful()) {
                    notif.setRead(true);
                    dot.setVisibility(View.GONE);
                    syncNotificationsAndAlerts();
                } else {
                    // Failover locally
                    notif.setRead(true);
                    dot.setVisibility(View.GONE);
                    syncNotificationsAndAlerts();
                }
            }

            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                // Failover locally
                notif.setRead(true);
                dot.setVisibility(View.GONE);
                syncNotificationsAndAlerts();
            }
        });
    }

    private void handleMarkAllNotificationsRead() {
        ApiClient.authToken = prefs.getString("auth_token", null);
        String userRole = prefs.getString("user_role", null);

        ApiClient.getApiService().markAllNotificationsAsRead(null, userRole).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                markAllReadLocal();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                markAllReadLocal();
            }
        });
    }

    private void markAllReadLocal() {
        for (Notification n : notificationsList) {
            n.setRead(true);
        }
        populateNotificationsDropdown();
        updateNotificationBadgeCount(0);
        Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show();
    }

    private void handleClearAllNotifications() {
        ApiClient.authToken = prefs.getString("auth_token", null);

        ApiClient.getApiService().deleteAllNotifications().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                clearNotificationsLocal();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                clearNotificationsLocal();
            }
        });
    }

    private void clearNotificationsLocal() {
        notificationsList.clear();
        populateNotificationsDropdown();
        updateNotificationBadgeCount(0);
        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show();
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
            case 6:
                fragment = new SettingsFragment();
                title = getString(R.string.nav_settings);
                navSettings.setBackgroundResource(R.drawable.bg_nav_item_active);
                break;
            case 7:
                fragment = new ProfileFragment();
                title = getString(R.string.nav_profile);
                break;
            case 8:
                fragment = new AlertsFragment();
                title = getString(R.string.nav_alerts);
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

        navSettings.setBackgroundResource(android.R.color.transparent);
    }

    @Override
    public void onBackPressed() {
        if (profileDropdownCard.getVisibility() == View.VISIBLE || notificationDropdownCard.getVisibility() == View.VISIBLE) {
            closeDropdowns();
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }
}
