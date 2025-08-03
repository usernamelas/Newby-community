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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.ui.fragments.HomeFragment;
import com.victory.poolassistant.ui.fragments.SettingsFragment;
import com.victory.poolassistant.ui.fragments.AboutFragment;
import com.victory.poolassistant.ui.fragments.StatsFragment;
import com.victory.poolassistant.utils.PermissionUtils;
import com.victory.poolassistant.utils.ThemeManager;

/**
 * MainActivity - Main entry point with professional UI
 * Features modern Material Design with navigation drawer
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "MainActivity";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    
    // UI Components (Fixed - no ViewBinding)
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private androidx.appcompat.widget.Toolbar toolbar;
    private FloatingActionButton fab;
    private CoordinatorLayout coordinatorLayout;
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
        
        // Set content view (Fixed - no ViewBinding)
        setContentView(R.layout.activity_main);
        
        // Initialize views manually
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        
        Logger.d(TAG, "Components initialized");
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(toolbar);
        
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
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Setup navigation view
        navView.setNavigationItemSelectedListener(this);
        
        // Update header info
        updateNavigationHeader();
    }
    
    /**
     * Setup floating action button
     */
    private void setupFloatingActionButton() {
        fab.setOnClickListener(view -> {
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
        View headerView = navView.getHeaderView(0);
        
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
        
        // Update UI
        updateUIState();
        
        if (!hasOverlayPerm) {
            showPermissionDialog();
        }
        
        Logger.d(TAG, "Permissions check completed - Granted: " + permissionsGranted);
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
     * Start overlay service
     */
    private void startOverlayService() {
        Logger.i(TAG, "Starting overlay service...");
        
        // TODO: Implement overlay service startup
        // For now, simulate the action
        simulateServiceStart();
        
        // Update state
        isOverlayServiceRunning = true;
        AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, true);
        
        // Update UI
        updateUIState();
        
        // Show feedback
        showSnackbar("Overlay service started", false);
    }
    
    /**
     * Stop overlay service
     */
    private void stopOverlayService() {
        Logger.i(TAG, "Stopping overlay service...");
        
        // TODO: Implement overlay service shutdown
        // For now, simulate the action
        simulateServiceStop();
        
        // Update state
        isOverlayServiceRunning = false;
        AppConfig.setBoolean(AppConfig.PREF_OVERLAY_ENABLED, false);
        
        // Update UI
        updateUIState();
        
        // Show feedback
        showSnackbar("Overlay service stopped", false);
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
            fab.animate()
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
            fab.animate().cancel();
            fab.setRotation(0);
        }
    }
    
    /**
     * Update FAB state
     */
    private void updateFabState() {
        if (isOverlayServiceRunning) {
            fab.setImageResource(R.drawable.ic_stop);
            fab.setBackgroundTintList(getColorStateList(R.color.color_error));
        } else {
            fab.setImageResource(R.drawable.ic_play_arrow);
            fab.setBackgroundTintList(getColorStateList(R.color.color_primary));
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
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        
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
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (PermissionUtils.hasOverlayPermission(this)) {
                permissionsGranted = true;
                updateUIState();
                showSnackbar("Overlay permission granted!", false);
            } else {
                showSnackbar("Overlay permission denied. Some features may not work.", true);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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