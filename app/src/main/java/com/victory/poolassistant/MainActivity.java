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
import com.victory.poolassistant.overlay.FloatingOverlayService; // ADDED: Real overlay service import
import com.victory.poolassistant.ui.fragments.HomeFragment;
import com.victory.poolassistant.ui.fragments.SettingsFragment;
import com.victory.poolassistant.ui.fragments.AboutFragment;
import com.victory.poolassistant.ui.fragments.StatsFragment;
import com.victory.poolassistant.utils.PermissionUtils;
import com.victory.poolassistant.utils.ThemeManager;

/**
 * MainActivity - Main entry point with professional UI
 * Features modern Material Design with navigation drawer
 * ENHANCED: Real FloatingOverlayService integration
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "MainActivity";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    
    // UI Components
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private Handler uiHandler;
    
    // App managers
    private ThemeManager themeManager;
    private PoolAssistantApplication app;
    
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
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Logger.d(TAG, "Components initialized");
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
                toggleOverlayService();
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
        
        // Check overlay permission
        boolean hasOverlayPerm = PermissionUtils.hasOverlayPermission(this);
        
        // Update state
        permissionsGranted = hasOverlayPerm;
        
        // ENHANCED: Check if service is already running
        checkOverlayServiceStatus();
        
        // Update UI
        updateUIState();
        
        if (!hasOverlayPerm) {
            showPermissionDialog();
        }
        
        Logger.d(TAG, "Permissions check completed - Granted: " + permissionsGranted);
    }
    
    /**
     * ADDED: Check if overlay service is currently running
     */
    private void checkOverlayServiceStatus() {
        FloatingOverlayService service = FloatingOverlayService.getInstance();
        isOverlayServiceRunning = (service != null && service.isOverlayVisible());
        Logger.d(TAG, "Overlay service status: " + isOverlayServiceRunning);
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
    
    /**
     * Toggle overlay service on/off
     */
    private void toggleOverlayService() {
        if (!permissionsGranted) {
            requestOverlayPermission();
            return;
        }
        
        if (isOverlayServiceRunning) {
            stopOverlayService();
        } else {
            startOverlayService();
        }
    }
    
    /**
     * Start overlay service - REPLACED: Real implementation instead of simulation
     */
    private void startOverlayService() {
        Logger.i(TAG, "Starting overlay service...");
        
        // Start loading animation
        animateFab(true);
        
        try {
            // Create intent for overlay service
            Intent serviceIntent = new Intent(this, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_START_OVERLAY);
            
            // Start foreground service
            startForegroundService(serviceIntent);
            
            // Check status after delay to allow service to start
            uiHandler.postDelayed(() -> {
                checkOverlayServiceStatus();
                animateFab(false);
                
                if (isOverlayServiceRunning) {
                    // Update state on success
                    AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, true);
                    updateUIState();
                    showSnackbar("✅ Overlay service started successfully!", false);
                    Logger.i(TAG, "Overlay service started successfully");
                } else {
                    // Handle failure
                    showSnackbar("❌ Failed to start overlay service", true);
                    Logger.e(TAG, "Failed to start overlay service");
                }
            }, 1500);
            
        } catch (Exception e) {
            Logger.e(TAG, "Error starting overlay service", e);
            animateFab(false);
            showSnackbar("❌ Error: " + e.getMessage(), true);
        }
    }
    
    /**
     * Stop overlay service - REPLACED: Real implementation instead of simulation
     */
    private void stopOverlayService() {
        Logger.i(TAG, "Stopping overlay service...");
        
        // Start loading animation
        animateFab(true);
        
        try {
            // Stop service via intent
            Intent serviceIntent = new Intent(this, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_STOP_OVERLAY);
            startService(serviceIntent);
            
            // Also try direct service shutdown
            FloatingOverlayService service = FloatingOverlayService.getInstance();
            if (service != null) {
                service.stopSelf();
            }
            
            // Check status after delay
            uiHandler.postDelayed(() -> {
                checkOverlayServiceStatus();
                animateFab(false);
                
                // Update state
                isOverlayServiceRunning = false;
                AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, false);
                updateUIState();
                showSnackbar("🛑 Overlay service stopped", false);
                Logger.i(TAG, "Overlay service stopped successfully");
            }, 1000);
            
        } catch (Exception e) {
            Logger.e(TAG, "Error stopping overlay service", e);
            animateFab(false);
            showSnackbar("❌ Error: " + e.getMessage(), true);
        }
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
                    if (loading) { // Continue animation while loading
                        binding.fab.setRotation(0); // Reset rotation for smooth loop
                        animateFab(true);
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
        
        // ENHANCED: Update toolbar title with status
        updateToolbarTitle();
        
        // Update current fragment if it implements state update
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).updateStatus(isOverlayServiceRunning, permissionsGranted);
        }
    }
    
    /**
     * ADDED: Update toolbar title with status indicator
     */
    private void updateToolbarTitle() {
        String baseTitle = getString(R.string.app_name);
        if (isOverlayServiceRunning) {
            setTitle(baseTitle + " • Active");
        } else {
            setTitle(baseTitle);
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
        
        // ENHANCED: Add action buttons for overlay-related messages
        if (message.contains("overlay") || message.contains("Overlay")) {
            if (isError && !permissionsGranted) {
                snackbar.setAction("Grant Permission", v -> requestOverlayPermission());
            } else if (isOverlayServiceRunning) {
                snackbar.setAction("Hide", v -> {
                    FloatingOverlayService service = FloatingOverlayService.getInstance();
                    if (service != null) {
                        service.hideOverlay();
                    }
                });
            }
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
            if (PermissionUtils.hasOverlayPermission(this)) {
                permissionsGranted = true;
                updateUIState();
                showSnackbar("✅ Overlay permission granted!", false);
            } else {
                showSnackbar("❌ Overlay permission denied. Some features may not work.", true);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // ENHANCED: Show exit confirmation if overlay is running
            if (isOverlayServiceRunning) {
                showExitConfirmation();
            } else {
                super.onBackPressed();
            }
        }
    }
    
    /**
     * ADDED: Show exit confirmation when overlay is active
     */
    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Pool Assistant Active")
            .setMessage("Pool Assistant overlay is currently running. What would you like to do?")
            .setPositiveButton("Keep Running & Exit", (dialog, which) -> {
                moveTaskToBack(true); // Exit but keep service running
            })
            .setNegativeButton("Stop & Exit", (dialog, which) -> {
                stopOverlayService();
                finish();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if permissions changed
        boolean currentOverlayPerm = PermissionUtils.hasOverlayPermission(this);
        if (currentOverlayPerm != permissionsGranted) {
            permissionsGranted = currentOverlayPerm;
            updateUIState();
        }
        
        // ENHANCED: Refresh overlay service status
        checkOverlayServiceStatus();
        updateUIState();
        
        Logger.d(TAG, "MainActivity resumed");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        
        Logger.d(TAG, "MainActivity destroyed");
    }
}