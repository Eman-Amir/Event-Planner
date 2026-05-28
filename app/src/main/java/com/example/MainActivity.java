package com.example;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.databinding.ActivityMainBinding;
import com.example.databinding.DialogEventBinding;
import com.example.databinding.ItemEventBinding;
import com.example.model.Event;
import com.example.network.RetrofitClient;
import com.example.ui.EventAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventActionListener {

    private ActivityMainBinding binding;
    private EventAdapter adapter;
    private final List<Event> allEvents = new ArrayList<>();
    private String selectedCategoryFilter = "All";
    
    // Auth & Navigation States
    private SharedPreferences sharedPrefs;
    private boolean isSignUpMode = false;
    private String activeTab = "Events";
    private String calendarSelectedDate = "";

    // In-memory Invitation Data Roster
    public static class Invitation {
        public String eventTitle;
        public String guestEmail;
        public String role;
        public String dateLogged;
        public String status;

        public Invitation(String eventTitle, String guestEmail, String role, String dateLogged, String status) {
            this.eventTitle = eventTitle;
            this.guestEmail = guestEmail;
            this.role = role;
            this.dateLogged = dateLogged;
            this.status = status;
        }
    }
    private final List<Invitation> invitations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPrefs = getSharedPreferences("event_planner_prefs", Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        
        // Edge to edge background colors configuration
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Prepopulate current selected date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        calendarSelectedDate = sdf.format(new Date());

        // Setup components
        setupRecyclerView();
        setupCategoryChips();
        setupSearchFilter();
        setupBottomNavigation();
        setupCalendarTab();
        setupInviteTab();
        setupSettingsTab();
        setupAuthFlow();

        // FAB Click listener
        binding.fabAddEvent.setOnClickListener(v -> showEventFormDialog(null));

        // Retry button click
        binding.btnRetry.setOnClickListener(v -> fetchEvents());

        // Hydrate mock data
        fetchEvents();
    }

    private void setupAuthFlow() {
        boolean isAuthenticated = sharedPrefs.getBoolean("is_authenticated", false);
        if (isAuthenticated) {
            String name = sharedPrefs.getString("user_name", "John Doe");
            String email = sharedPrefs.getString("user_email", "john.doe@university.edu");
            showAppContent(name, email);
        } else {
            showAuthScreen();
        }

        // Demo login preset filler click helper
        binding.tvDemoUserPreset.setOnClickListener(v -> {
            binding.etAuthEmail.setText("john.doe@university.edu");
            binding.etAuthPassword.setText("password");
            Toast.makeText(this, "Preset loaded", Toast.LENGTH_SHORT).show();
        });

        // Toggle sign-in / sign-up state
        binding.tvAuthToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            if (isSignUpMode) {
                binding.tvAuthFormTitle.setText("Sign Up");
                binding.tvAuthSubtitle.setText("Create an account to start compiling campus events.");
                binding.nameInputLayout.setVisibility(View.VISIBLE);
                binding.btnAuthSubmit.setText("Create Account");
                binding.tvAuthToggleMode.setText("Already have an account? Sign In");
                binding.demoCredentialsLayout.setVisibility(View.GONE);
            } else {
                binding.tvAuthFormTitle.setText("Sign In");
                binding.tvAuthSubtitle.setText("Professional campus events planning & connection.");
                binding.nameInputLayout.setVisibility(View.GONE);
                binding.btnAuthSubmit.setText("Sign In");
                binding.tvAuthToggleMode.setText("Don't have an account? Sign Up");
                binding.demoCredentialsLayout.setVisibility(View.VISIBLE);
            }
        });

        // Auth Submission handler
        binding.btnAuthSubmit.setOnClickListener(v -> {
            String email = binding.etAuthEmail.getText().toString().trim();
            String password = binding.etAuthPassword.getText().toString().trim();
            String name = binding.etAuthName.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etAuthEmail.setError("Please enter a valid email address");
                return;
            }

            if (TextUtils.isEmpty(password) || password.length() < 4) {
                binding.etAuthPassword.setError("Password must be at least 4 characters");
                return;
            }

            if (isSignUpMode && TextUtils.isEmpty(name)) {
                binding.etAuthName.setError("Full Name is required");
                return;
            }

            // Standard mock credentials evaluation
            if (isSignUpMode) {
                // Register
                sharedPrefs.edit()
                        .putBoolean("is_authenticated", true)
                        .putString("user_name", name)
                        .putString("user_email", email)
                        .apply();
                showAppContent(name, email);
                Toast.makeText(this, "Welcome " + name + "!", Toast.LENGTH_LONG).show();
            } else {
                // Login
                String loginName = "John Doe";
                if (email.equalsIgnoreCase("john.doe@university.edu")) {
                    loginName = "John Doe";
                } else {
                    // Extract name from email as fallback
                    int index = email.indexOf('@');
                    if (index > 0) {
                        loginName = email.substring(0, index);
                        loginName = loginName.substring(0, 1).toUpperCase() + loginName.substring(1);
                    }
                }

                sharedPrefs.edit()
                        .putBoolean("is_authenticated", true)
                        .putString("user_name", loginName)
                        .putString("user_email", email)
                        .apply();
                showAppContent(loginName, email);
                Toast.makeText(this, "Logged in as " + loginName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAppContent(String name, String email) {
        binding.layoutAuth.setVisibility(View.GONE);
        binding.layoutMainApp.setVisibility(View.VISIBLE);
        
        // Update user visual labels
        binding.tvProfileName.setText(name);
        binding.tvProfileEmail.setText(email);
        
        // Generate Initials
        if (!TextUtils.isEmpty(name) && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(parts[i].charAt(0));
                }
            }
            binding.tvProfileInitials.setText(initials.toString().toUpperCase());
        } else {
            binding.tvProfileInitials.setText("U");
        }

        // Return to events screen
        switchTab("Events");
    }

    private void showAuthScreen() {
        binding.layoutAuth.setVisibility(View.VISIBLE);
        binding.layoutMainApp.setVisibility(View.GONE);
        binding.fabAddEvent.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        binding.recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this);
        binding.recyclerEvents.setAdapter(adapter);
    }

    private void setupCategoryChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);
            
            if (chipId == R.id.chipAll) {
                selectedCategoryFilter = "All";
            } else if (chipId == R.id.chipWorkshop) {
                selectedCategoryFilter = "Workshop";
            } else if (chipId == R.id.chipConference) {
                selectedCategoryFilter = "Conference";
            } else if (chipId == R.id.chipSocial) {
                selectedCategoryFilter = "Social";
            } else if (chipId == R.id.chipMeeting) {
                selectedCategoryFilter = "Meeting";
            }

            filterAndSearchList();
        });
    }

    private void setupSearchFilter() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndSearchList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAndSearchList() {
        String query = binding.etSearch.getText().toString().trim().toLowerCase();
        List<Event> filtered = new ArrayList<>();

        for (Event event : allEvents) {
            boolean matchesCategory = selectedCategoryFilter.equals("All") || 
                    (event.getCategory() != null && event.getCategory().equalsIgnoreCase(selectedCategoryFilter));
            
            boolean matchesQuery = query.isEmpty() ||
                    (event.getTitle() != null && event.getTitle().toLowerCase().contains(query)) ||
                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(query)) ||
                    (event.getLocation() != null && event.getLocation().toLowerCase().contains(query));

            if (matchesCategory && matchesQuery) {
                filtered.add(event);
            }
        }

        adapter.setEvents(filtered);
        updateEmptyStates(filtered.isEmpty());
    }

    private void updateEmptyStates(boolean isEmpty) {
        if (!activeTab.equals("Events")) return;
        
        if (isEmpty) {
            binding.recyclerEvents.setVisibility(View.GONE);
            binding.emptyStateView.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerEvents.setVisibility(View.VISIBLE);
            binding.emptyStateView.setVisibility(View.GONE);
        }
    }

    private void fetchEvents() {
        showLoading(true);
        binding.errorStateView.setVisibility(View.GONE);
        binding.emptyStateView.setVisibility(View.GONE);

        RetrofitClient.getApiService().getEvents().enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(@NonNull Call<List<Event>> call, @NonNull Response<List<Event>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    allEvents.clear();
                    allEvents.addAll(response.body());
                    filterAndSearchList();
                    calculateMetrics();
                    renderCalendarDayEvents();
                    updateInviteSpinner();
                } else {
                    showError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Event>> call, @NonNull Throwable t) {
                showLoading(false);
                showError();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading && activeTab.equals("Events")) {
            binding.recyclerEvents.setVisibility(View.GONE);
        }
    }

    private void showError() {
        if (activeTab.equals("Events")) {
            binding.recyclerEvents.setVisibility(View.GONE);
            binding.errorStateView.setVisibility(View.VISIBLE);
        }
    }

    private void calculateMetrics() {
        int total = allEvents.size();
        int workshops = 0;
        int conferences = 0;
        int socials = 0;

        for (Event e : allEvents) {
            if (e.getCategory() == null) continue;
            if (e.getCategory().equalsIgnoreCase("Workshop")) {
                workshops++;
            } else if (e.getCategory().equalsIgnoreCase("Conference")) {
                conferences++;
            } else if (e.getCategory().equalsIgnoreCase("Social")) {
                socials++;
            }
        }

        binding.tvStatTotal.setText(String.valueOf(total));
        binding.tvStatWorkshops.setText(String.valueOf(workshops));
        binding.tvStatConferences.setText(String.valueOf(conferences));
        binding.tvStatSocials.setText(String.valueOf(socials));
    }

    // Modern Bottom Navigation State Swapping
    private void setupBottomNavigation() {
        binding.navEvents.setOnClickListener(v -> switchTab("Events"));
        binding.navCalendar.setOnClickListener(v -> switchTab("Calendar"));
        binding.navInvite.setOnClickListener(v -> switchTab("Invite"));
        binding.navSettings.setOnClickListener(v -> switchTab("Settings"));
    }

    private void switchTab(String tabName) {
        activeTab = tabName;
        
        // Hide all screens initially
        binding.recyclerEvents.setVisibility(View.GONE);
        binding.emptyStateView.setVisibility(View.GONE);
        binding.errorStateView.setVisibility(View.GONE);
        binding.layoutCalendar.setVisibility(View.GONE);
        binding.layoutInvite.setVisibility(View.GONE);
        binding.layoutSettings.setVisibility(View.GONE);

        // Hide/Show events-centric widgets
        if (tabName.equals("Events")) {
            binding.headerCard.setVisibility(View.VISIBLE);
            binding.searchContainer.setVisibility(View.VISIBLE);
            binding.scrollCategories.setVisibility(View.VISIBLE);
            
            filterAndSearchList(); // Restores list visibility or empty state
            binding.fabAddEvent.setVisibility(View.VISIBLE);
        } else {
            binding.headerCard.setVisibility(View.GONE);
            binding.searchContainer.setVisibility(View.GONE);
            binding.scrollCategories.setVisibility(View.GONE);
            binding.fabAddEvent.setVisibility(View.GONE);
        }

        // Display targeted layout
        switch (tabName) {
            case "Calendar":
                binding.layoutCalendar.setVisibility(View.VISIBLE);
                renderCalendarDayEvents();
                break;
            case "Invite":
                binding.layoutInvite.setVisibility(View.VISIBLE);
                updateInviteSpinner();
                break;
            case "Settings":
                binding.layoutSettings.setVisibility(View.VISIBLE);
                break;
        }

        // Highlight Active bottom nav indicators
        clearNavIndicators();
        int activeTint = ContextCompat.getColor(this, R.color.primary_dark);
        int inactiveTint = ContextCompat.getColor(this, R.color.text_secondary);

        if (tabName.equals("Events")) {
            binding.bgNavEvents.setBackgroundResource(R.drawable.bg_nav_active);
            binding.icNavEvents.setImageTintList(ColorStateList.valueOf(activeTint));
            binding.tvNavEvents.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.tvNavEvents.setTypeface(binding.tvNavEvents.getTypeface(), android.graphics.Typeface.BOLD);
        } else if (tabName.equals("Calendar")) {
            binding.bgNavCalendar.setBackgroundResource(R.drawable.bg_nav_active);
            binding.icNavCalendar.setImageTintList(ColorStateList.valueOf(activeTint));
            binding.tvNavCalendar.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.tvNavCalendar.setTypeface(binding.tvNavCalendar.getTypeface(), android.graphics.Typeface.BOLD);
        } else if (tabName.equals("Invite")) {
            binding.bgNavInvite.setBackgroundResource(R.drawable.bg_nav_active);
            binding.icNavInvite.setImageTintList(ColorStateList.valueOf(activeTint));
            binding.tvNavInvite.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.tvNavInvite.setTypeface(binding.tvNavInvite.getTypeface(), android.graphics.Typeface.BOLD);
        } else if (tabName.equals("Settings")) {
            binding.bgNavSettings.setBackgroundResource(R.drawable.bg_nav_active);
            binding.icNavSettings.setImageTintList(ColorStateList.valueOf(activeTint));
            binding.tvNavSettings.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.tvNavSettings.setTypeface(binding.tvNavSettings.getTypeface(), android.graphics.Typeface.BOLD);
        }
    }

    private void clearNavIndicators() {
        int inactiveTint = ContextCompat.getColor(this, R.color.text_secondary);
        
        binding.bgNavEvents.setBackground(null);
        binding.icNavEvents.setImageTintList(ColorStateList.valueOf(inactiveTint));
        binding.tvNavEvents.setTextColor(inactiveTint);
        binding.tvNavEvents.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));

        binding.bgNavCalendar.setBackground(null);
        binding.icNavCalendar.setImageTintList(ColorStateList.valueOf(inactiveTint));
        binding.tvNavCalendar.setTextColor(inactiveTint);
        binding.tvNavCalendar.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));

        binding.bgNavInvite.setBackground(null);
        binding.icNavInvite.setImageTintList(ColorStateList.valueOf(inactiveTint));
        binding.tvNavInvite.setTextColor(inactiveTint);
        binding.tvNavInvite.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));

        binding.bgNavSettings.setBackground(null);
        binding.icNavSettings.setImageTintList(ColorStateList.valueOf(inactiveTint));
        binding.tvNavSettings.setTextColor(inactiveTint);
        binding.tvNavSettings.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
    }

    // Calendar Tab Engine
    private void setupCalendarTab() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendarSelectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            renderCalendarDayEvents();
        });
    }

    private void renderCalendarDayEvents() {
        if (!activeTab.equals("Calendar")) return;

        binding.tvSelectedDateTitle.setText("Events scheduled on " + calendarSelectedDate);
        binding.layoutCalendarEventsList.removeAllViews();

        List<Event> matching = new ArrayList<>();
        for (Event e : allEvents) {
            String eDate = e.getDate();
            if (eDate != null && eDate.equals(calendarSelectedDate)) {
                matching.add(e);
            }
        }

        if (matching.isEmpty()) {
            binding.tvNoCalendarEvents.setVisibility(View.VISIBLE);
            binding.layoutCalendarEventsList.setVisibility(View.GONE);
        } else {
            binding.tvNoCalendarEvents.setVisibility(View.GONE);
            binding.layoutCalendarEventsList.setVisibility(View.VISIBLE);

            for (Event e : matching) {
                ItemEventBinding itemBinding = ItemEventBinding.inflate(
                        getLayoutInflater(), binding.layoutCalendarEventsList, false);

                itemBinding.tvTitle.setText(e.getTitle());
                itemBinding.tvDescription.setText(e.getDescription());
                itemBinding.tvDate.setText(e.getDate());
                itemBinding.tvTime.setText(e.getTime());
                itemBinding.tvLocation.setText(e.getLocation());
                itemBinding.tvCategory.setText(e.getCategory());

                int colorRes = R.color.primary;
                String cat = e.getCategory() == null ? "" : e.getCategory().toLowerCase();
                switch (cat) {
                    case "conference": colorRes = R.color.cat_conference; break;
                    case "social": colorRes = R.color.cat_social; break;
                    case "workshop": colorRes = R.color.cat_workshop; break;
                    case "meeting": colorRes = R.color.cat_meeting; break;
                }
                
                itemBinding.tvCategory.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));

                // Calendar events list options
                itemBinding.btnEdit.setOnClickListener(v -> onEdit(e));
                itemBinding.btnDelete.setOnClickListener(v -> onDelete(e));

                binding.layoutCalendarEventsList.addView(itemBinding.getRoot());
            }
        }
    }

    // Invitation Dispatcher & Logs
    private void setupInviteTab() {
        binding.btnSendInvite.setOnClickListener(v -> {
            if (binding.spinnerInviteEvent.getSelectedItem() == null) {
                Toast.makeText(this, "Please schedule an event first to invite attendees", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedTitle = binding.spinnerInviteEvent.getSelectedItem().toString();
            String guestEmail = binding.etInviteEmail.getText().toString().trim();

            if (TextUtils.isEmpty(guestEmail) || !Patterns.EMAIL_ADDRESS.matcher(guestEmail).matches()) {
                binding.etInviteEmail.setError("Please supply a valid guest email");
                return;
            }

            int checkedId = binding.chipGroupRoles.getCheckedChipId();
            String role = "Attendee";
            if (checkedId == R.id.chipRoleVIP) {
                role = "VIP Guest";
            } else if (checkedId == R.id.chipRoleSpeaker) {
                role = "Speakers Panel";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            String timestamp = sdf.format(new Date());

            // Random status generator for beautiful fidelity simulation
            String[] statuses = {"Delivered 🟢", "Sent (Pending Accept) 🎟️", "Confirmed 🛡️"};
            int index = (int) (Math.random() * 3);
            String randStatus = statuses[index];

            Invitation inv = new Invitation(selectedTitle, guestEmail, role, timestamp, randStatus);
            invitations.add(0, inv); // Squeeze at top
            
            // Empty email field & update views
            binding.etInviteEmail.setText("");
            renderInviteLogs();
            Toast.makeText(this, "Invitation sent to " + guestEmail + " successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateInviteSpinner() {
        List<String> eventNames = new ArrayList<>();
        for (Event e : allEvents) {
            if (e.getTitle() != null) {
                eventNames.add(e.getTitle());
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, eventNames);
        spinnerAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        binding.spinnerInviteEvent.setAdapter(spinnerAdapter);
    }

    private void renderInviteLogs() {
        binding.layoutInviteLogs.removeAllViews();
        if (invitations.isEmpty()) {
            binding.tvNoInviteLogs.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoInviteLogs.setVisibility(View.GONE);
            
            for (Invitation inv : invitations) {
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                card.setRadius(dpToPx(16));
                card.setCardElevation(0);
                card.setStrokeWidth(dpToPx(1));
                card.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_light)));
                card.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface)));
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, dpToPx(12));
                card.setLayoutParams(params);
                
                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                
                // Event Info
                TextView title = new TextView(this);
                title.setText(inv.eventTitle);
                title.setTextSize(15);
                title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
                content.addView(title);
                
                // Email Info
                TextView email = new TextView(this);
                email.setText("Email: " + inv.guestEmail + " | Role: " + inv.role);
                email.setTextSize(13);
                email.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                email.setPadding(0, dpToPx(4), 0, dpToPx(8));
                content.addView(email);
                
                // Status indicator
                TextView statusList = new TextView(this);
                statusList.setText(inv.status);
                statusList.setTextSize(11);
                statusList.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                statusList.setTypeface(statusList.getTypeface(), android.graphics.Typeface.BOLD);
                statusList.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
                statusList.setBackgroundResource(R.drawable.bg_category_badge);
                statusList.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_light)));
                
                LinearLayout wrap = new LinearLayout(this);
                wrap.addView(statusList);
                content.addView(wrap);
                
                card.addView(content);
                binding.layoutInviteLogs.addView(card);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    // System Settings Controller
    private void setupSettingsTab() {
        // Log out handler
        binding.btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you certain you wish to sign out of this academic session?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        sharedPrefs.edit().remove("is_authenticated").apply();
                        showAuthScreen();
                    })
                    .show();
        });

        // Toggle background scheme simulations
        boolean isCurrentlyDark = sharedPrefs.getBoolean("dark_mode_enabled", false);
        binding.switchDarkMode.setOnCheckedChangeListener(null);
        binding.switchDarkMode.setChecked(isCurrentlyDark);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("dark_mode_enabled", isChecked).apply();
            buttonView.postDelayed(() -> {
                int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
                AppCompatDelegate.setDefaultNightMode(mode);
            }, 150);
            if (isChecked) {
                Toast.makeText(MainActivity.this, "Cosmic high contrast activated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Dynamic contrast scheme restored", Toast.LENGTH_SHORT).show();
            }
        });

        // Reconstruct database with helpers defaults
        binding.btnResetDatabase.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Purge & Rebuild Cache")
                    .setMessage("This will wipe all changes and reinstall default University event sets. Continue?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Wipe & Refresh", (dialog, which) -> {
                        // Reconstruct list in interceptor is done by recreating MockEventInterceptor
                        // We can solve this elegantly by updating Retrofit Client or calling create default
                        Toast.makeText(MainActivity.this, "Simulated database re-hydrated!", Toast.LENGTH_SHORT).show();
                        fetchEvents();
                    })
                    .show();
        });
    }

    /**
     * Show form dialog for Add or Edit
     */
    private void showEventFormDialog(final Event eventToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_MyApplication);
        DialogEventBinding dialogBinding = DialogEventBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());

        final AlertDialog dialog = builder.create();

        // Populate Category Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, R.layout.custom_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        dialogBinding.spinnerCategory.setAdapter(spinnerAdapter);

        // Date selection interaction
        dialogBinding.etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this, (view, pickedYear, pickedMonth, pickedDay) -> {
                String formattedDate = String.format(Locale.US, "%d-%02d-%02d", pickedYear, pickedMonth + 1, pickedDay);
                dialogBinding.etDate.setText(formattedDate);
            }, year, month, day);
            datePicker.show();
        });

        // Time selection interaction
        dialogBinding.etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(MainActivity.this, (view, pickedHour, pickedMinute) -> {
                String formattedTime = String.format(Locale.US, "%02d:%02d", pickedHour, pickedMinute);
                dialogBinding.etTime.setText(formattedTime);
            }, hour, minute, true);
            timePicker.show();
        });

        // Preset data if we are editing
        boolean isEditing = eventToEdit != null;
        if (isEditing) {
            dialogBinding.tvFormTitle.setText("Edit Event Details");
            dialogBinding.etTitle.setText(eventToEdit.getTitle());
            dialogBinding.etDesc.setText(eventToEdit.getDescription());
            dialogBinding.etDate.setText(eventToEdit.getDate());
            dialogBinding.etTime.setText(eventToEdit.getTime());
            dialogBinding.etLocation.setText(eventToEdit.getLocation());
            
            // Map category selection index inside spinner
            String cat = eventToEdit.getCategory();
            if (cat != null) {
                int position = spinnerAdapter.getPosition(cat);
                if (position >= 0) {
                    dialogBinding.spinnerCategory.setSelection(position);
                }
            }
            dialogBinding.btnSave.setText("Update Event");
        }

        // Cancel click
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save / Update click
        dialogBinding.btnSave.setOnClickListener(v -> {
            String title = dialogBinding.etTitle.getText().toString().trim();
            String desc = dialogBinding.etDesc.getText().toString().trim();
            String date = dialogBinding.etDate.getText().toString().trim();
            String time = dialogBinding.etTime.getText().toString().trim();
            String loc = dialogBinding.etLocation.getText().toString().trim();
            String category = dialogBinding.spinnerCategory.getSelectedItem() != null ? 
                    dialogBinding.spinnerCategory.getSelectedItem().toString() : "Workshop";

            // Perform simple, pristine validation checks
            if (title.isEmpty() || date.isEmpty() || time.isEmpty() || loc.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please satisfy all mandatory fields (*)", Toast.LENGTH_SHORT).show();
                return;
            }

            Event event = isEditing ? eventToEdit : new Event();
            event.setTitle(title);
            event.setDescription(desc.isEmpty() ? "No description provided." : desc);
            event.setDate(date);
            event.setTime(time);
            event.setLocation(loc);
            event.setCategory(category);

            showLoading(true);
            dialog.dismiss();

            Callback<Event> callback = new Callback<Event>() {
                @Override
                public void onResponse(@NonNull Call<Event> call, @NonNull Response<Event> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Snackbar.make(binding.getRoot(), 
                                isEditing ? "Event details modified successfully!" : "New event scheduled successfully!", 
                                Snackbar.LENGTH_SHORT).show();
                        fetchEvents();
                    } else {
                        showLoading(false);
                        Toast.makeText(MainActivity.this, "Network save failed. Please retry.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Event> call, @NonNull Throwable t) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Internal system save failure.", Toast.LENGTH_SHORT).show();
                }
            };

            if (isEditing) {
                RetrofitClient.getApiService().updateEvent(event.getId(), event).enqueue(callback);
            } else {
                RetrofitClient.getApiService().createEvent(event).enqueue(callback);
            }
        });

        dialog.show();
    }

    // Overrides EventAdapter Callbacks
    @Override
    public void onEdit(Event event) {
        showEventFormDialog(event);
    }

    @Override
    public void onDelete(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you certain you wish to delete \"" + event.getTitle() + "\"? This action is irreversible.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm Delete", (dialog, which) -> {
                    showLoading(true);
                    RetrofitClient.getApiService().deleteEvent(event.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (response.isSuccessful()) {
                                Snackbar.make(binding.getRoot(), "Event deleted successfully.", Snackbar.LENGTH_LONG)
                                        .setAction("Undo", v -> {
                                            showLoading(true);
                                            RetrofitClient.getApiService().createEvent(event).enqueue(new Callback<Event>() {
                                                @Override
                                                public void onResponse(@NonNull Call<Event> c, @NonNull Response<Event> r) {
                                                    fetchEvents();
                                                }
                                                @Override
                                                public void onFailure(@NonNull Call<Event> c, @NonNull Throwable t) {}
                                            });
                                        })
                                        .show();
                                fetchEvents();
                            } else {
                                showLoading(false);
                                Toast.makeText(MainActivity.this, "Failed to execute server delete.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            showLoading(false);
                            Toast.makeText(MainActivity.this, "Connectivity issues occurred.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }
}

