package com.victory.poolassistant;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.databinding.ActivityMainBinding;
import com.victory.poolassistant.ui.fragments.HomeFragment;
import com.victory.poolassistant.ui.fragments.SettingsFragment;
import com.victory.poolassistant.ui.fragments.AboutFragment;
import com.victory.poolassistant.ui.fragments.StatsFragment;
import com.victory.poolassistant.utils.PermissionUtils;
import com.victory.poolassistant.utils.ThemeManager;
// ADD THESE IMPORTS FOR OVERLAY INTEGRATION
import com.victory.poolassistant.overlay.OverlayManager;
import com.victory.poolassistant.utils.PermissionHelper;

/**
 * MainActivity - Main entry point with professional UI
 * Features modern Material Design with navigation drawer
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OverlayManager.OnOverlayStateChangeListener {
    
    private static final String TAG = "MainActivity";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    
    // UI Components
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private Handler uiHandler;
    
    // App managers
    private ThemeManager themeManager;
    private PoolAssistantApplication app;
    // ADD OVERLAY MANAGER
    private OverlayManager overlayManager;
    
    // State
    private String currentFragmentTag = "home";
    private boolean isOverlayServiceRunning = false;
    private boolean permissionsGranted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize
        initializeComponents();
        
        // Setup UI
        setupUI();
        
        // Check permissions
        checkPermissions();
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "home", "Home");
        }
        
        Logger.i(TAG, "MainActivity created successfully");
    }
    
    /**
     * Initialize components
     */
    private void initializeComponents() {
        // Get app instance
        app = PoolAssistantApplication.getInstance();
        
        // Initialize UI handler
        uiHandler = new Handler();
        
        // Get theme manager
        themeManager = app.getThemeManager();
        
        // INITIALIZE OVERLAY MANAGER
        initializeOverlayManager();
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Logger.d(TAG, "Components initialized");
    }
    
    /**
     * Initialize overlay manager
     */
    private void initializeOverlayManager() {
        overlayManager = OverlayManager.getInstance(this);
        overlayManager.setOnOverlayStateChangeListener(this);
        
        Logger.d(TAG, "OverlayManager initialized");
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        
        // Setup navigation drawer
        setupNavigationDrawer();
        
        // Setup floating action button
        setupFloatingActionButton();
        
        // Update UI state
        updateUIState();
        
        Logger.d(TAG, "UI setup completed");
    }
    
    /**
     * Setup navigation drawer
     */
    private void setupNavigationDrawer() {
        // Setup drawer toggle
        toggle = new ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Setup navigation view
        binding.navView.setNavigationItemSelectedListener(this);
        
        // Update header info
        updateNavigationHeader();
    }
    
    /**
     * Setup floating action button
     */
    private void setupFloatingActionButton() {
        binding.fab.setOnClickListener(view -> {
            if (permissionsGranted) {
                toggleFloatingOverlay(); // CHANGED TO USE OVERLAY SYSTEM
            } else {
                requestOverlayPermission();
            }
        });
        
        // Initial FAB state
        updateFabState();
    }
    
    /**
     * Update navigation header with app info
     */
    private void updateNavigationHeader() {
        View headerView = binding.navView.getHeaderView(0);
        
        // Update version info, theme, etc.
        // Will be implemented when we create the header layout
    }
    
    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        Logger.d(TAG, "Checking permissions...");
        
        // CHANGED TO USE NEW PERMISSION HELPER
        boolean hasOverlayPerm = PermissionHelper.hasOverlayPermission(this);
        
        // Update state
        permissionsGranted = hasOverlayPerm;
        
        // Update UI
        updateUIState();
        
        if (!hasOverlayPerm) {
            showOverlayPermissionDialog(); // CHANGED METHOD NAME
        }
        
        Logger.d(TAG, "Permissions check completed - Granted: " + permissionsGranted);
    }
    
    /**
     * Show overlay permission dialog
     */
    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("Pool Assistant needs overlay permission to show floating controls over other apps.")
            .setPositiveButton("Grant Permission", (dialog, which) -> {
                PermissionHelper.requestOverlayPermission(this);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Show permission explanation dialog
     */
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Pool Assistant needs overlay permission to display trajectory lines over games.")
            .setPositiveButton("Grant Permission", (dialog, which) -> requestOverlayPermission())
            .setNegativeButton("Later", (dialog, which) -> {
                Toast.makeText(this, "Some features may not work without permissions", 
                    Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (!PermissionUtils.hasOverlayPermission(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
    
    // ===== NEW OVERLAY METHODS =====
    
    /**
     * Start floating overlay (NEW)
     */
    public void startFloatingOverlay() {
        Logger.d(TAG, "Starting floating overlay from MainActivity");
        
        if (overlayManager != null) {
            overlayManager.startOverlay();
        }
    }
    
    /**
     * Stop floating overlay (NEW)
     */
    public void stopFloatingOverlay() {
        Logger.d(TAG, "Stopping floating overlay from MainActivity");
        
        if (overlayManager != null) {
            overlayManager.stopOverlay();
        }
    }
    
    /**
     * Toggle floating overlay (NEW)
     */
    public void toggleFloatingOverlay() {
        Logger.d(TAG, "Toggling floating overlay from MainActivity");
        
        if (overlayManager != null) {
            overlayManager.toggleOverlay();
        }
    }
    
    // ===== OVERLAY STATE LISTENER IMPLEMENTATION =====
    
    @Override
    public void onOverlayStarted() {
        Logger.i(TAG, "Overlay started successfully");
        runOnUiThread(() -> {
            isOverlayServiceRunning = true;
            updateUIState();
            Toast.makeText(this, "Floating overlay started", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onOverlayStopped() {
        Logger.i(TAG, "Overlay stopped");
        runOnUiThread(() -> {
            isOverlayServiceRunning = false;
            updateUIState();
            Toast.makeText(this, "Floating overlay stopped", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onOverlayPermissionRequired() {
        Logger.w(TAG, "Overlay permission required");
        runOnUiThread(() -> {
            showOverlayPermissionDialog();
        });
    }
    
    @Override
    public void onOverlayError(String error) {
        Logger.e(TAG, "Overlay error: " + error);
        runOnUiThread(() -> {
            Toast.makeText(this, "Overlay error: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    // ===== EXISTING METHODS (MODIFIED) =====
    
    /**
     * Toggle overlay service on/off
     */
    private void toggleOverlayService() {
        if (!permissionsGranted) {
            requestOverlayPermission();
            return;
        }
        
        if (isOverlayServiceRunning) {
            stopFloatingOverlay(); // CHANGED TO USE NEW METHOD
        } else {
            startFloatingOverlay(); // CHANGED TO USE NEW METHOD
        }
    }
    
    /**
     * Start overlay service
     */
    private void startOverlayService() {
        Logger.i(TAG, "Starting overlay service...");
        
        // REPLACED WITH NEW OVERLAY SYSTEM
        startFloatingOverlay();
    }
    
    /**
     * Stop overlay service
     */
    private void stopOverlayService() {
        Logger.i(TAG, "Stopping overlay service...");
        
        // REPLACED WITH NEW OVERLAY SYSTEM
        stopFloatingOverlay();
    }
    
    /**
     * Simulate service start (temporary)
     */
    private void simulateServiceStart() {
        // Add loading animation to FAB
        animateFab(true);
        
        // Simulate delay
        uiHandler.postDelayed(() -> {
            animateFab(false);
            updateFabState();
        }, 1500);
    }
    
    /**
     * Simulate service stop (temporary)
     */
    private void simulateServiceStop() {
        // Add loading animation to FAB
        animateFab(true);
        
        // Simulate delay
        uiHandler.postDelayed(() -> {
            animateFab(false);
            updateFabState();
        }, 1000);
    }
    
    /**
     * Animate FAB
     */
    private void animateFab(boolean loading) {
        if (loading) {
            // Start rotation animation
            binding.fab.animate()
                .rotation(360)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (isOverlayServiceRunning) {
                        animateFab(true); // Continue if still loading
                    }
                });
        } else {
            // Stop animation
            binding.fab.animate().cancel();
            binding.fab.setRotation(0);
        }
    }
    
    /**
     * Update FAB state
     */
    private void updateFabState() {
        if (isOverlayServiceRunning) {
            binding.fab.setImageResource(R.drawable.ic_stop);
            binding.fab.setBackgroundTintList(getColorStateList(R.color.color_error));
        } else {
            binding.fab.setImageResource(R.drawable.ic_play_arrow);
            binding.fab.setBackgroundTintList(getColorStateList(R.color.color_primary));
        }
    }
    
    /**
     * Update overall UI state
     */
    private void updateUIState() {
        updateFabState();
        updateNavigationHeader();
        
        // Update current fragment if it implements state update
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).updateStatus(isOverlayServiceRunning, permissionsGranted);
        }
    }
    
    /**
     * Load fragment
     */
    private void loadFragment(Fragment fragment, String tag, String title) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit();
        
        currentFragmentTag = tag;
        setTitle(title);
        
        Logger.d(TAG, "Fragment loaded: " + tag);
    }
    
    /**
     * Show snackbar message
     */
    private void showSnackbar(String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG);
        
        if (isError) {
            snackbar.setBackgroundTint(getColor(R.color.color_error));
        }
        
        snackbar.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_toggle_theme) {
            themeManager.toggleTheme();
            recreate(); // Restart activity to apply theme
            return true;
        } else if (id == R.id.action_settings) {
            loadFragment(new SettingsFragment(), "settings", "Settings");
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            loadFragment(new HomeFragment(), "home", "Pool Assistant");
        } else if (id == R.id.nav_settings) {
            loadFragment(new SettingsFragment(), "settings", "Settings");
        } else if (id == R.id.nav_statistics) {
            loadFragment(new StatsFragment(), "stats", "Statistics");
        } else if (id == R.id.nav_about) {
            loadFragment(new AboutFragment(), "about", "About");
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            // UPDATED TO USE NEW PERMISSION HELPER
            if (PermissionHelper.handleOverlayPermissionResult(this)) {
                permissionsGranted = true;
                updateUIState();
                showSnackbar("Overlay permission granted!", false);
                
                // Auto-start overlay after permission granted
                new Handler().postDelayed(() -> {
                    startFloatingOverlay();
                }, 1000);
            } else {
                showSnackbar("Overlay permission denied. Some features may not work.", true);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if permissions changed - UPDATED TO USE NEW HELPER
        boolean currentOverlayPerm = PermissionHelper.hasOverlayPermission(this);
        if (currentOverlayPerm != permissionsGranted) {
            permissionsGranted = currentOverlayPerm;
            updateUIState();
        }
        
        Logger.d(TAG, "MainActivity resumed");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        
        // CLEANUP OVERLAY MANAGER
        if (overlayManager != null) {
            overlayManager.cleanup();
        }
        
        Logger.d(TAG, "MainActivity destroyed");
    }
}