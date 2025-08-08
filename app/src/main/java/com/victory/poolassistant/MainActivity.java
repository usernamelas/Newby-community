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
import com.victory.poolassistant.overlay.FloatingOverlayService;
import com.victory.poolassistant.ui.fragments.HomeFragment;
import com.victory.poolassistant.ui.fragments.SettingsFragment;
import com.victory.poolassistant.ui.fragments.AboutFragment;
import com.victory.poolassistant.ui.fragments.StatsFragment;
import com.victory.poolassistant.utils.PermissionUtils;
import com.victory.poolassistant.utils.ThemeManager;

/**
 * MainActivity - Fixed overlay service integration
 * Properly handles 3-state overlay system
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
    private boolean isAnimatingFab = false;
    
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
            loadFragment(new HomeFragment(), "home", "Pool Assistant");
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
     * Setup floating action button dengan proper overlay service integration
     */
    private void setupFloatingActionButton() {
        binding.fab.setOnClickListener(view -> {
            if (isAnimatingFab) {
                Logger.d(TAG, "FAB click ignored - animation in progress");
                return;
            }
            
            if (permissionsGranted) {
                toggleOverlayService();
            } else {
                requestOverlayPermission();
            }
        });
        
        // Initial FAB state
        updateFabState();
        
        Logger.d(TAG, "FAB setup completed");
    }
    
    /**
     * Update navigation header with app info
     */
    private void updateNavigationHeader() {
        View headerView = binding.navView.getHeaderView(0);
        // TODO: Update header when layout is available
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
        
        // Check if service is already running
        checkOverlayServiceStatus();
        
        // Update UI
        updateUIState();
        
        if (!hasOverlayPerm) {
            showPermissionDialog();
        }
        
        Logger.d(TAG, "Permissions check completed - Granted: " + permissionsGranted + 
                ", Service running: " + isOverlayServiceRunning);
    }
    
    /**
     * Check if overlay service is currently running
     */
    private void checkOverlayServiceStatus() {
        FloatingOverlayService service = FloatingOverlayService.getInstance();
        isOverlayServiceRunning = (service != null && service.isOverlayVisible());
        
        Logger.d(TAG, "Overlay service status checked: " + isOverlayServiceRunning);
    }
    
    /**
     * Show permission explanation dialog
     */
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("Pool Assistant needs overlay permission to display the floating assistant over games.\n\n" +
                       "This allows you to see trajectory lines and aim assistance while playing.")
            .setPositiveButton("Grant Permission", (dialog, which) -> requestOverlayPermission())
            .setNegativeButton("Not Now", (dialog, which) -> {
                showSnackbar("Overlay features will be disabled without permission", true);
            })
            .setIcon(R.drawable.ic_pool_ball)
            .setCancelable(false)
            .show();
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (!PermissionUtils.hasOverlayPermission(this)) {
            Logger.i(TAG, "Requesting overlay permission...");
            
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            Logger.d(TAG, "Overlay permission already granted");
            permissionsGranted = true;
            updateUIState();
        }
    }
    
    /**
     * Toggle overlay service on/off dengan proper error handling
     */
    private void toggleOverlayService() {
        if (!permissionsGranted) {
            Logger.w(TAG, "Cannot toggle overlay - permissions not granted");
            requestOverlayPermission();
            return;
        }
        
        Logger.i(TAG, "Toggling overlay service - Current state: " + isOverlayServiceRunning);
        
        if (isOverlayServiceRunning) {
            stopOverlayService();
        } else {
            startOverlayService();
        }
    }
    
    /**
     * Start overlay service dengan proper implementation
     */
    private void startOverlayService() {
        Logger.i(TAG, "Starting overlay service...");
        
        // Start animation
        animateFab(true);
        
        try {
            // Create intent for overlay service
            Intent serviceIntent = new Intent(this, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_START_OVERLAY);
            
            // Start foreground service
            startForegroundService(serviceIntent);
            
            // Update state after small delay to allow service to start
            uiHandler.postDelayed(() -> {
                checkOverlayServiceStatus();
                animateFab(false);
                updateUIState();
                
                if (isOverlayServiceRunning) {
                    AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, true);
                    showSnackbar("✅ Pool Assistant overlay started!", false);
                    Logger.i(TAG, "Overlay service started successfully");
                } else {
                    showSnackbar("❌ Failed to start overlay service", true);
                    Logger.e(TAG, "Failed to start overlay service");
                }
            }, 1000);
            
        } catch (Exception e) {
            Logger.e(TAG, "Error starting overlay service", e);
            animateFab(false);
            showSnackbar("❌ Error starting overlay: " + e.getMessage(), true);
        }
    }
    
    /**
     * Stop overlay service dengan proper cleanup
     */
    private void stopOverlayService() {
        Logger.i(TAG, "Stopping overlay service...");
        
        // Start animation
        animateFab(true);
        
        try {
            // Stop service via intent
            Intent serviceIntent = new Intent(this, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_STOP_OVERLAY);
            startService(serviceIntent);
            
            // Alternative: Stop service directly
            FloatingOverlayService service = FloatingOverlayService.getInstance();
            if (service != null) {
                service.stopSelf();
            }
            
            // Update state after delay
            uiHandler.postDelayed(() -> {
                checkOverlayServiceStatus();
                animateFab(false);
                updateUIState();
                
                AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, false);
                showSnackbar("🛑 Pool Assistant overlay stopped", false);
                Logger.i(TAG, "Overlay service stopped successfully");
            }, 800);
            
        } catch (Exception e) {
            Logger.e(TAG, "Error stopping overlay service", e);
            animateFab(false);
            showSnackbar("❌ Error stopping overlay: " + e.getMessage(), true);
        }
    }
    
    /**
     * Animate FAB dengan improved animation
     */
    private void animateFab(boolean loading) {
        isAnimatingFab = loading;
        
        if (loading) {
            // Start smooth rotation animation
            ValueAnimator rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
            rotationAnimator.setDuration(1200);
            rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            rotationAnimator.addUpdateListener(animation -> {
                if (isAnimatingFab) {
                    binding.fab.setRotation((Float) animation.getAnimatedValue());
                }
            });
            rotationAnimator.start();
            
            // Scale animation for visual feedback
            binding.fab.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(600)
                .start();
                
        } else {
            // Stop animations
            binding.fab.animate().cancel();
            binding.fab.setRotation(0f);
            binding.fab.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .start();
        }
    }
    
    /**
     * Update FAB state dengan proper icons dan colors
     */
    private void updateFabState() {
        if (isOverlayServiceRunning) {
            // Service is running - show stop button
            binding.fab.setImageResource(R.drawable.ic_stop);
            binding.fab.setBackgroundTintList(getColorStateList(R.color.color_error));
            binding.fab.setContentDescription("Stop Pool Assistant Overlay");
        } else {
            // Service is stopped - show play button
            binding.fab.setImageResource(R.drawable.ic_play_arrow);
            binding.fab.setBackgroundTintList(getColorStateList(R.color.color_primary));
            binding.fab.setContentDescription("Start Pool Assistant Overlay");
        }
        
        // Disable FAB if no permissions
        binding.fab.setEnabled(permissionsGranted);
        binding.fab.setAlpha(permissionsGranted ? 1.0f : 0.5f);
        
        Logger.d(TAG, "FAB state updated - Running: " + isOverlayServiceRunning + 
                ", Permissions: " + permissionsGranted);
    }
    
    /**
     * Update overall UI state
     */
    private void updateUIState() {
        updateFabState();
        updateNavigationHeader();
        updateToolbarTitle();
        
        // Update current fragment if it implements state update
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).updateStatus(isOverlayServiceRunning, permissionsGranted);
        }
    }
    
    /**
     * Update toolbar title dengan status indicator
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
     * Load fragment dengan improved transitions
     */
    private void loadFragment(Fragment fragment, String tag, String title) {
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_slide_in_right,
                R.anim.fragment_slide_out_left,
                R.anim.fragment_slide_in_left,
                R.anim.fragment_slide_out_right
            )
            .replace(R.id.fragment_container, fragment, tag)
            .commit();
        
        currentFragmentTag = tag;
        setTitle(title);
        
        Logger.d(TAG, "Fragment loaded: " + tag);
    }
    
    /**
     * Show snackbar message dengan improved styling
     */
    private void showSnackbar(String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG);
        
        if (isError) {
            snackbar.setBackgroundTint(getColor(R.color.color_error));
            snackbar.setTextColor(getColor(R.color.white));
        } else {
            snackbar.setBackgroundTint(getColor(R.color.color_primary));
            snackbar.setTextColor(getColor(R.color.white));
        }
        
        // Add action button for overlay-related messages
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
        } else if (id == R.id.action_toggle_overlay) {
            // Quick overlay toggle from menu
            toggleOverlayService();
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
        } else if (id == R.id.nav_overlay_toggle) {
            // Direct overlay toggle from navigation
            toggleOverlayService();
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            boolean hasPermission = PermissionUtils.hasOverlayPermission(this);
            permissionsGranted = hasPermission;
            
            if (hasPermission) {
                updateUIState();
                showSnackbar("✅ Overlay permission granted! You can now use Pool Assistant overlay.", false);
                Logger.i(TAG, "Overlay permission granted by user");
            } else {
                showSnackbar("❌ Overlay permission is required for Pool Assistant to work properly.", true);
                Logger.w(TAG, "Overlay permission denied by user");
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Show exit confirmation if overlay is running
            if (isOverlayServiceRunning) {
                showExitConfirmation();
            } else {
                super.onBackPressed();
            }
        }
    }
    
    /**
     * Show exit confirmation when overlay is active
     */
    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Pool Assistant Active")
            .setMessage("Pool Assistant overlay is currently running. What would you like to do?")
            .setPositiveButton("Keep Running & Exit", (dialog, which) -> {
                // Exit app but keep service running
                moveTaskToBack(true);
            })
            .setNegativeButton("Stop & Exit", (dialog, which) -> {
                // Stop service and exit
                stopOverlayService();
                finish();
            })
            .setNeutralButton("Cancel", null)
            .setIcon(R.drawable.ic_pool_ball)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if permissions changed while app was in background
        boolean currentOverlayPerm = PermissionUtils.hasOverlayPermission(this);
        if (currentOverlayPerm != permissionsGranted) {
            permissionsGranted = currentOverlayPerm;
            Logger.d(TAG, "Overlay permission changed while app was paused: " + permissionsGranted);
        }
        
        // Check service status
        checkOverlayServiceStatus();
        
        // Update UI
        updateUIState();
        
        Logger.d(TAG, "MainActivity resumed - Permissions: " + permissionsGranted + 
                ", Service: " + isOverlayServiceRunning);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "MainActivity paused");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        
        // Stop any ongoing animations
        if (binding != null && binding.fab != null) {
            binding.fab.animate().cancel();
        }
        
        Logger.d(TAG, "MainActivity destroyed");
    }
    
    /**
     * Handle service lifecycle callbacks (if service notifies main activity)
     */
    public void onOverlayServiceStarted() {
        runOnUiThread(() -> {
            isOverlayServiceRunning = true;
            updateUIState();
            showSnackbar("✅ Pool Assistant overlay is now active!", false);
        });
    }
    
    public void onOverlayServiceStopped() {
        runOnUiThread(() -> {
            isOverlayServiceRunning = false;
            updateUIState();
            showSnackbar("🛑 Pool Assistant overlay stopped", false);
        });
    }
    
    public void onOverlayServiceError(String error) {
        runOnUiThread(() -> {
            isOverlayServiceRunning = false;
            updateUIState();
            showSnackbar("❌ Overlay Error: " + error, true);
        });
    }
}