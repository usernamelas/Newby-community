package com.victory.poolassistant.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.card.MaterialCardView;
import com.victory.poolassistant.BuildConfig;
import com.victory.poolassistant.MainActivity;
import com.victory.poolassistant.PoolAssistantApplication;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.RootManager;

/**
 * HomeFragment - Main dashboard showing app status and quick actions
 */
public class HomeFragment extends Fragment {
    
    private static final String TAG = "HomeFragment";
    
    // UI Components (No ViewBinding)
    private MaterialCardView cardSystemStatus;
    private MaterialCardView cardGameDetection;
    private MaterialCardView cardRootStatus;
    private MaterialCardView cardSettings;
    private MaterialCardView cardStatistics;
    private MaterialCardView cardAbout;
    private MaterialCardView cardHelp;
    
    private TextView tvSystemStatus;
    private TextView tvDetectionStatus;
    private TextView tvRootStatus;
    private ImageView ivSystemStatusIcon;
    private ImageView ivDetectionStatusIcon;
    private ImageView ivRootStatusIcon;
    private View indicatorSystemStatus;
    private View indicatorDetectionStatus;
    private View indicatorRootStatus;
    
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
        // No ViewBinding - use regular inflate
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupUI();
        checkSystemStatus();
        startStatusUpdates();
        
        Logger.d(TAG, "HomeFragment view created and initialized");
    }
    
    /**
     * Initialize views manually (No ViewBinding)
     */
    private void initializeViews(View view) {
        // Status cards
        cardSystemStatus = view.findViewById(R.id.card_system_status);
        cardGameDetection = view.findViewById(R.id.card_game_detection);
        cardRootStatus = view.findViewById(R.id.card_root_status);
        
        // Navigation cards
        cardSettings = view.findViewById(R.id.card_settings);
        cardStatistics = view.findViewById(R.id.card_statistics);
        cardAbout = view.findViewById(R.id.card_about);
        cardHelp = view.findViewById(R.id.card_help);
        
        // Status text views
        tvSystemStatus = view.findViewById(R.id.tv_system_status);
        tvDetectionStatus = view.findViewById(R.id.tv_detection_status);
        tvRootStatus = view.findViewById(R.id.tv_root_status);
        
        // Status icons
        ivSystemStatusIcon = view.findViewById(R.id.iv_system_status_icon);
        ivDetectionStatusIcon = view.findViewById(R.id.iv_detection_status_icon);
        ivRootStatusIcon = view.findViewById(R.id.iv_root_status_icon);
        
        // Status indicators
        indicatorSystemStatus = view.findViewById(R.id.indicator_system_status);
        indicatorDetectionStatus = view.findViewById(R.id.indicator_detection_status);
        indicatorRootStatus = view.findViewById(R.id.indicator_root_status);
    }
    
    /**
     * Setup UI components and click listeners
     */
    private void setupUI() {
        // Setup click listeners for status cards (if exist)
        if (cardSystemStatus != null) {
            cardSystemStatus.setOnClickListener(v -> showSystemStatusDialog());
        }
        if (cardGameDetection != null) {
            cardGameDetection.setOnClickListener(v -> refreshGameDetection());
        }
        
        // Setup root status card (only show for pro version)
        if (BuildConfig.ROOT_FEATURES && cardRootStatus != null) {
            cardRootStatus.setVisibility(View.VISIBLE);
            cardRootStatus.setOnClickListener(v -> showRootStatusDialog());
        } else if (cardRootStatus != null) {
            cardRootStatus.setVisibility(View.GONE);
        }
        
        // Setup quick action cards with REAL navigation
        if (cardSettings != null) {
            cardSettings.setOnClickListener(v -> openSettings());
        }
        if (cardStatistics != null) {
            cardStatistics.setOnClickListener(v -> openStatistics());
        }
        if (cardAbout != null) {
            cardAbout.setOnClickListener(v -> openAbout());
        }
        if (cardHelp != null) {
            cardHelp.setOnClickListener(v -> openHelp());
        }
        
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
        uiHandler.postDelayed(statusUpdateRunnable, 2000);
    }
    
    /**
     * Stop status updates
     */
    private void stopStatusUpdates() {
        uiHandler.removeCallbacks(statusUpdateRunnable);
    }
    
    /**
     * Status update runnable
     */
    private final Runnable statusUpdateRunnable = new Runnable() {
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
        if (tvSystemStatus == null) return;
        
        if (overlayServiceRunning) {
            tvSystemStatus.setText("Active");
            if (indicatorSystemStatus != null) {
                indicatorSystemStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_active));
            }
            if (ivSystemStatusIcon != null) {
                ivSystemStatusIcon.setImageResource(R.drawable.ic_check_circle);
            }
        } else {
            tvSystemStatus.setText("Ready");
            if (indicatorSystemStatus != null) {
                indicatorSystemStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_ready));
            }
            if (ivSystemStatusIcon != null) {
                ivSystemStatusIcon.setImageResource(R.drawable.ic_system);
            }
        }
    }
    
    /**
     * Update game detection card
     */
    private void updateGameDetectionCard() {
        if (tvDetectionStatus == null) return;
        
        if (gameDetected) {
            tvDetectionStatus.setText("Game Detected");
            if (indicatorDetectionStatus != null) {
                indicatorDetectionStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_active));
            }
            if (ivDetectionStatusIcon != null) {
                ivDetectionStatusIcon.setImageResource(R.drawable.ic_check_circle);
            }
        } else {
            tvDetectionStatus.setText("Scanning");
            if (indicatorDetectionStatus != null) {
                indicatorDetectionStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_scanning));
            }
            if (ivDetectionStatusIcon != null) {
                ivDetectionStatusIcon.setImageResource(R.drawable.ic_detection);
            }
        }
    }
    
    /**
     * Update root status card (pro version only)
     */
    private void updateRootStatusCard() {
        if (tvRootStatus == null) return;
        
        if (rootAvailable) {
            tvRootStatus.setText("Root Available");
            if (indicatorRootStatus != null) {
                indicatorRootStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_active));
            }
            if (ivRootStatusIcon != null) {
                ivRootStatusIcon.setImageResource(R.drawable.ic_check_circle);
            }
        } else {
            tvRootStatus.setText("Root Unavailable");
            if (indicatorRootStatus != null) {
                indicatorRootStatus.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.status_inactive));
            }
            if (ivRootStatusIcon != null) {
                ivRootStatusIcon.setImageResource(R.drawable.ic_root);
            }
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
                        "Version: " + AppConfig.VERSION_NAME;
        
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
     * Open settings - REAL NAVIGATION
     */
    private void openSettings() {
        navigateToFragment(new SettingsFragment(), "Settings");
    }
    
    /**
     * Open statistics - REAL NAVIGATION
     */
    private void openStatistics() {
        navigateToFragment(new StatsFragment(), "Statistics");
    }
    
    /**
     * Open about - REAL NAVIGATION
     */
    private void openAbout() {
        navigateToFragment(new AboutFragment(), "About");
    }
    
    /**
     * Open help
     */
    private void openHelp() {
        Toast.makeText(requireContext(), "Help - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Navigate to fragment helper
     */
    private void navigateToFragment(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            
            // Update title
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.getSupportActionBar() != null) {
                mainActivity.getSupportActionBar().setTitle(title);
            }
            
            Logger.d(TAG, "Navigated to " + title);
        }
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
        Logger.d(TAG, "HomeFragment view destroyed");
    }
}