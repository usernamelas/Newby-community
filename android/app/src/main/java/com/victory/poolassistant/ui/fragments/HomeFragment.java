package com.victory.poolassistant.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.victory.poolassistant.BuildConfig;
import com.victory.poolassistant.PoolAssistantApplication;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.databinding.FragmentHomeBinding;
import com.victory.poolassistant.utils.RootManager;

/**
 * HomeFragment - Main dashboard showing app status and quick actions
 */
public class HomeFragment extends Fragment {
    
    private static final String TAG = "HomeFragment";
    
    private FragmentHomeBinding binding;
    private Handler uiHandler;
    private PoolAssistantApplication app;
    
    // Status tracking
    private boolean overlayServiceRunning = false;
    private boolean gameDetected = false;
    private boolean rootAvailable = false;
    private boolean permissionsGranted = false;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        app = PoolAssistantApplication.getInstance();
        uiHandler = new Handler();
        
        Logger.d(TAG, "HomeFragment created");
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupUI();
        checkSystemStatus();
        startStatusUpdates();
        
        Logger.d(TAG, "HomeFragment view created and initialized");
    }
    
    /**
     * Setup UI components and click listeners
     */
    private void setupUI() {
        // Setup click listeners for status cards
        binding.cardSystemStatus.setOnClickListener(v -> showSystemStatusDialog());
        binding.cardGameDetection.setOnClickListener(v -> refreshGameDetection());
        
        // Setup root status card (only show for pro version)
        if (BuildConfig.ROOT_FEATURES) {
            binding.cardRootStatus.setVisibility(View.VISIBLE);
            binding.cardRootStatus.setOnClickListener(v -> showRootStatusDialog());
        } else {
            binding.cardRootStatus.setVisibility(View.GONE);
        }
        
        // Setup quick action cards
        binding.cardSettings.setOnClickListener(v -> openSettings());
        binding.cardStatistics.setOnClickListener(v -> openStatistics());
        binding.cardAbout.setOnClickListener(v -> openAbout());
        binding.cardHelp.setOnClickListener(v -> openHelp());
        
        Logger.d(TAG, "UI components setup completed");
    }
    
    /**
     * Check initial system status
     */
    private void checkSystemStatus() {
        // Check overlay service status
        overlayServiceRunning = AppConfig.isOverlayEnabled();
        
        // Check root availability (pro version only)
        if (BuildConfig.ROOT_FEATURES && app.getRootManager() != null) {
            rootAvailable = app.getRootManager().isRootAvailable();
        }
        
        // Update UI
        updateStatusUI();
        
        Logger.d(TAG, "System status checked - Overlay: " + overlayServiceRunning + ", Root: " + rootAvailable);
    }
    
    /**
     * Start periodic status updates
     */
    private void startStatusUpdates() {
        // Update status every 2 seconds
        uiHandler.postDelayed(statusUpdaterunnable, 2000);
    }
    
    /**
     * Stop status updates
     */
    private void stopStatusUpdates() {
        uiHandler.removeCallbacks(statusUpdaterunnable);
    }
    
    /**
     * Status update runnable
     */
    private final Runnable statusUpdaterunnable = new Runnable() {
        @Override
        public void run() {
            // Simulate game detection (replace with actual detection)
            gameDetected = Math.random() > 0.7; // 30% chance of game detected
            
            updateStatusUI();
            
            // Schedule next update
            uiHandler.postDelayed(this, 2000);
        }
    };
    
    /**
     * Update status UI elements
     */
    private void updateStatusUI() {
        if (binding == null) return;
        
        // Update system status
        updateSystemStatusCard();
        
        // Update game detection status
        updateGameDetectionCard();
        
        // Update root status (pro version only)
        if (BuildConfig.ROOT_FEATURES) {
            updateRootStatusCard();
        }
    }
    
    /**
     * Update system status card
     */
    private void updateSystemStatusCard() {
        if (overlayServiceRunning) {
            binding.tvSystemStatus.setText(R.string.status_active);
            binding.indicatorSystemStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_active));
            binding.ivSystemStatusIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            binding.tvSystemStatus.setText(R.string.status_ready);
            binding.indicatorSystemStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_ready));
            binding.ivSystemStatusIcon.setImageResource(R.drawable.ic_system);
        }
    }
    
    /**
     * Update game detection card
     */
    private void updateGameDetectionCard() {
        if (gameDetected) {
            binding.tvDetectionStatus.setText(R.string.status_game_detected);
            binding.indicatorDetectionStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_active));
            binding.ivDetectionStatusIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            binding.tvDetectionStatus.setText(R.string.status_scanning);
            binding.indicatorDetectionStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_scanning));
            binding.ivDetectionStatusIcon.setImageResource(R.drawable.ic_detection);
        }
    }
    
    /**
     * Update root status card (pro version only)
     */
    private void updateRootStatusCard() {
        if (rootAvailable) {
            binding.tvRootStatus.setText(R.string.status_root_available);
            binding.indicatorRootStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_active));
            binding.ivRootStatusIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            binding.tvRootStatus.setText(R.string.status_root_unavailable);
            binding.indicatorRootStatus.setBackgroundTintList(
                requireContext().getColorStateList(R.color.status_inactive));
            binding.ivRootStatusIcon.setImageResource(R.drawable.ic_root);
        }
    }
    
    /**
     * Public method to update status from MainActivity
     */
    public void updateStatus(boolean overlayRunning, boolean permissions) {
        this.overlayServiceRunning = overlayRunning;
        this.permissionsGranted = permissions;
        updateStatusUI();
    }
    
    /**
     * Show system status dialog
     */
    private void showSystemStatusDialog() {
        String message = "Overlay Service: " + (overlayServiceRunning ? "Running" : "Stopped") + "\n" +
                        "Permissions: " + (permissionsGranted ? "Granted" : "Required") + "\n" +
                        "Version: " + BuildConfig.VERSION_NAME;
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("System Status")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    /**
     * Refresh game detection
     */
    private void refreshGameDetection() {
        Toast.makeText(requireContext(), "Refreshing game detection...", Toast.LENGTH_SHORT).show();
        
        // Simulate detection refresh
        uiHandler.postDelayed(() -> {
            gameDetected = Math.random() > 0.5;
            updateGameDetectionCard();
            
            String message = gameDetected ? "Pool game detected!" : "No game detected";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }, 1000);
    }
    
    /**
     * Show root status dialog (pro version only)
     */
    private void showRootStatusDialog() {
        if (!BuildConfig.ROOT_FEATURES) return;
        
        RootManager rootManager = app.getRootManager();
        String message;
        
        if (rootManager != null) {
            message = rootManager.getRootInfo();
        } else {
            message = "Root manager not available";
        }
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Root Status")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    /**
     * Open settings
     */
    private void openSettings() {
        // Will be implemented when SettingsFragment is created
        Toast.makeText(requireContext(), "Settings - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Open statistics
     */
    private void openStatistics() {
        // Will be implemented when StatsFragment is created
        Toast.makeText(requireContext(), "Statistics - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Open about
     */
    private void openAbout() {
        // Will be implemented when AboutFragment is created
        Toast.makeText(requireContext(), "About - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Open help
     */
    private void openHelp() {
        Toast.makeText(requireContext(), "Help - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        checkSystemStatus();
        startStatusUpdates();
        Logger.d(TAG, "HomeFragment resumed");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopStatusUpdates();
        Logger.d(TAG, "HomeFragment paused");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopStatusUpdates();
        binding = null;
        Logger.d(TAG, "HomeFragment view destroyed");
    }
}