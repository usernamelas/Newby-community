package com.victory.poolassistant.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.victory.poolassistant.MainActivity;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * Foreground service untuk floating overlay Pool Assistant
 * Handles window management dan lifecycle overlay
 * FIXED: Complete drag functionality + position persistence
 */
public class FloatingOverlayService extends Service {
    
    private static final String TAG = "FloatingOverlayService";
    private static final String CHANNEL_ID = "pool_assistant_overlay";
    private static final int NOTIFICATION_ID = 1001;
    
    // Actions
    public static final String ACTION_START_OVERLAY = "com.victory.poolassistant.START_OVERLAY";
    public static final String ACTION_STOP_OVERLAY = "com.victory.poolassistant.STOP_OVERLAY";
    public static final String ACTION_TOGGLE_OVERLAY = "com.victory.poolassistant.TOGGLE_OVERLAY";
    public static final String ACTION_SHOW_FULL = "com.victory.poolassistant.SHOW_FULL";
    public static final String ACTION_SHOW_SETTINGS = "com.victory.poolassistant.SHOW_SETTINGS";
    
    // Preferences
    private static final String PREFS_NAME = "overlay_prefs";
    private static final String PREF_OVERLAY_X = "overlay_x";
    private static final String PREF_OVERLAY_Y = "overlay_y";
    private static final String PREF_FIRST_RUN = "first_run";
    
    // Window management
    private WindowManager windowManager;
    private OverlayView overlayView;
    private WindowManager.LayoutParams layoutParams;
    
    // Screen info
    private int screenWidth;
    private int screenHeight;
    
    // State
    private boolean isOverlayVisible = false;
    private static FloatingOverlayService instance;
    private SharedPreferences preferences;
    
    // Error handling
    private int restartAttempts = 0;
    private static final int MAX_RESTART_ATTEMPTS = 3;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "FloatingOverlayService created");
        
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Get screen dimensions
        getScreenDimensions();
        
        // Create notification channel
        createNotificationChannel();
        
        // Initialize overlay view
        initializeOverlayView();
        
        Logger.i(TAG, "FloatingOverlayService initialization completed");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Logger.d(TAG, "onStartCommand: " + action);
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        if (intent != null && action != null) {
            switch (action) {
                case ACTION_START_OVERLAY:
                    showOverlay();
                    break;
                case ACTION_STOP_OVERLAY:
                    hideOverlay();
                    break;
                case ACTION_TOGGLE_OVERLAY:
                    toggleOverlay();
                    break;
                case ACTION_SHOW_FULL:
                    showOverlay();
                    if (overlayView != null) {
                        overlayView.forceState(OverlayView.OverlayState.FULL);
                    }
                    break;
                case ACTION_SHOW_SETTINGS:
                    showOverlay();
                    if (overlayView != null) {
                        overlayView.forceState(OverlayView.OverlayState.SETTINGS);
                    }
                    break;
                default:
                    // Default behavior - show overlay
                    showOverlay();
                    break;
            }
        } else {
            // Default: show overlay
            showOverlay();
        }
        
        // Service akan restart jika di-kill system
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Logger.d(TAG, "FloatingOverlayService destroyed");
        
        // Save current position
        saveOverlayPosition();
        
        // Hide overlay
        hideOverlay();
        
        // Cleanup overlay view
        if (overlayView != null) {
            overlayView.cleanup();
            overlayView = null;
        }
        
        instance = null;
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service tidak di-bind
    }
    
    /**
     * Get screen dimensions
     */
    private void getScreenDimensions() {
        try {
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            
            Logger.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to get screen dimensions", e);
            // Default fallback
            screenWidth = 1080;
            screenHeight = 1920;
        }
    }
    
    /**
     * Initialize overlay view dan layout parameters
     */
    private void initializeOverlayView() {
        try {
            // Create overlay view
            overlayView = new OverlayView(this);
            
            // Setup window layout parameters
            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }
            
            layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            
            // Position overlay - load saved position or use default
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            loadOverlayPosition();
            
            Logger.d(TAG, "Overlay view initialized successfully at position: " + 
                     layoutParams.x + ", " + layoutParams.y);
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize overlay view", e);
            
            // Retry initialization
            if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                restartAttempts++;
                Logger.w(TAG, "Retrying overlay initialization (attempt " + restartAttempts + ")");
                
                // Retry after delay
                new android.os.Handler().postDelayed(() -> {
                    initializeOverlayView();
                }, 1000);
            } else {
                Logger.e(TAG, "Max restart attempts reached - overlay initialization failed");
            }
        }
    }
    
    /**
     * Load overlay position from preferences
     */
    private void loadOverlayPosition() {
        boolean isFirstRun = preferences.getBoolean(PREF_FIRST_RUN, true);
        
        if (isFirstRun) {
            // First run - position di tengah kiri layar
            layoutParams.x = 50;
            layoutParams.y = screenHeight / 2 - 100;
            
            // Mark as not first run
            preferences.edit().putBoolean(PREF_FIRST_RUN, false).apply();
            Logger.d(TAG, "First run - using default position: " + layoutParams.x + ", " + layoutParams.y);
        } else {
            // Load saved position
            layoutParams.x = preferences.getInt(PREF_OVERLAY_X, 100);
            layoutParams.y = preferences.getInt(PREF_OVERLAY_Y, 300);
            Logger.d(TAG, "Loaded saved position: " + layoutParams.x + ", " + layoutParams.y);
        }
        
        // Ensure position is within screen bounds
        validatePosition();
    }
    
    /**
     * Save overlay position to preferences
     */
    private void saveOverlayPosition() {
        if (layoutParams != null) {
            preferences.edit()
                .putInt(PREF_OVERLAY_X, layoutParams.x)
                .putInt(PREF_OVERLAY_Y, layoutParams.y)
                .apply();
            
            Logger.d(TAG, "Overlay position saved: " + layoutParams.x + ", " + layoutParams.y);
        }
    }
    
    /**
     * Validate and fix position within screen bounds
     */
    private void validatePosition() {
        if (layoutParams == null) return;
        
        // Ensure X is within bounds
        if (layoutParams.x < 0) {
            layoutParams.x = 0;
        } else if (layoutParams.x > screenWidth - 100) {
            layoutParams.x = screenWidth - 100;
        }
        
        // Ensure Y is within bounds
        if (layoutParams.y < 0) {
            layoutParams.y = 0;
        } else if (layoutParams.y > screenHeight - 100) {
            layoutParams.y = screenHeight - 100;
        }
        
        Logger.d(TAG, "Position validated: " + layoutParams.x + ", " + layoutParams.y);
    }
    
    /**
     * Show floating overlay
     */
    public void showOverlay() {
        if (isOverlayVisible || overlayView == null) {
            Logger.w(TAG, "Cannot show overlay - already visible or view is null");
            return;
        }
        
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Logger.e(TAG, "No overlay permission - cannot show overlay");
            updateNotification("Permission required - tap to grant");
            return;
        }
        
        try {
            // Validate position before showing
            validatePosition();
            
            // Add view to window manager
            windowManager.addView(overlayView, layoutParams);
            isOverlayVisible = true;
            restartAttempts = 0; // Reset restart attempts on success
            
            // Set initial position in OverlayView
            overlayView.updateInitialPosition(layoutParams.x, layoutParams.y);
            
            Logger.i(TAG, "Overlay shown successfully at position: " + layoutParams.x + ", " + layoutParams.y);
            
            // Update notification
            updateNotification("Pool Assistant overlay active");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to show overlay", e);
            isOverlayVisible = false;
            
            // Retry if possible
            if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                restartAttempts++;
                Logger.w(TAG, "Retrying show overlay (attempt " + restartAttempts + ")");
                
                new android.os.Handler().postDelayed(() -> {
                    showOverlay();
                }, 2000);
            }
        }
    }
    
    /**
     * Hide floating overlay
     */
    public void hideOverlay() {
        if (!isOverlayVisible || overlayView == null) {
            Logger.w(TAG, "Cannot hide overlay - not visible or view is null");
            return;
        }
        
        try {
            // Save position before hiding
            saveOverlayPosition();
            
            // Remove view from window manager
            windowManager.removeView(overlayView);
            isOverlayVisible = false;
            
            Logger.i(TAG, "Overlay hidden successfully");
            
            // Update notification
            updateNotification("Pool Assistant overlay hidden");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to hide overlay", e);
            
            // Force reset state
            isOverlayVisible = false;
        }
    }
    
    /**
     * Toggle overlay visibility
     */
    public void toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay();
        } else {
            showOverlay();
        }
    }
    
    /**
     * Update overlay position (called by OverlayView)
     */
    public void updateOverlayPosition(int x, int y) {
        if (layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
            
            // Validate position
            validatePosition();
            
            if (isOverlayVisible && overlayView != null) {
                try {
                    windowManager.updateViewLayout(overlayView, layoutParams);
                    
                    // Update OverlayView dengan posisi terbaru
                    overlayView.updateInitialPosition(layoutParams.x, layoutParams.y);
                    
                    Logger.d(TAG, "Overlay position updated to: " + layoutParams.x + ", " + layoutParams.y);
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to update overlay position", e);
                }
            }
        }
    }
    
    /**
     * Get current overlay X position
     */
    public int getCurrentX() {
        return layoutParams != null ? layoutParams.x : 0;
    }
    
    /**
     * Get current overlay Y position
     */
    public int getCurrentY() {
        return layoutParams != null ? layoutParams.y : 0;
    }
    
    /**
     * Create notification channel untuk Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pool Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Floating overlay service for Pool Assistant");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Logger.d(TAG, "Notification channel created");
            }
        }
    }
    
    /**
     * Create foreground service notification
     */
    private Notification createNotification() {
        return createNotificationWithText("Pool Assistant is running");
    }
    
    /**
     * Create notification dengan custom text
     */
    private Notification createNotificationWithText(String text) {
        // Main app intent
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        // Toggle action
        Intent toggleIntent = new Intent(this, FloatingOverlayService.class);
        toggleIntent.setAction(ACTION_TOGGLE_OVERLAY);
        PendingIntent togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        // Settings action
        Intent settingsIntent = new Intent(this, FloatingOverlayService.class);
        settingsIntent.setAction(ACTION_SHOW_SETTINGS);
        PendingIntent settingsPendingIntent = PendingIntent.getService(
            this, 2, settingsIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pool Assistant")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification) // Create this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false);
        
        // Add actions
        builder.addAction(
            R.drawable.ic_visibility, // Create this icon
            isOverlayVisible ? "Hide Overlay" : "Show Overlay",
            togglePendingIntent
        );
        
        builder.addAction(
            R.drawable.ic_settings, // Create this icon
            "Settings",
            settingsPendingIntent
        );
        
        return builder.build();
    }
    
    /**
     * Update notification text
     */
    public void updateNotification(String text) {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotificationWithText(text));
                Logger.d(TAG, "Notification updated: " + text);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to update notification", e);
        }
    }
    
    /**
     * Get service instance (for external control)
     */
    public static FloatingOverlayService getInstance() {
        return instance;
    }
    
    /**
     * Check if overlay is currently visible
     */
    public boolean isOverlayVisible() {
        return isOverlayVisible;
    }
    
    /**
     * Get overlay view instance
     */
    public OverlayView getOverlayView() {
        return overlayView;
    }
    
    /**
     * Check if service is properly initialized
     */
    public boolean isInitialized() {
        return overlayView != null && overlayView.isInitialized();
    }
    
    /**
     * Reset overlay position to center
     */
    public void resetPosition() {
        int centerX = screenWidth / 4;
        int centerY = screenHeight / 2;
        updateOverlayPosition(centerX, centerY);
        
        // Save new position
        saveOverlayPosition();
        
        Logger.i(TAG, "Overlay position reset to center");
    }
    
    /**
     * Debug method - log service state
     */
    public void logServiceState() {
        Logger.d(TAG, "=== FloatingOverlayService State Debug ===");
        Logger.d(TAG, "Is Overlay Visible: " + isOverlayVisible);
        Logger.d(TAG, "Screen Size: " + screenWidth + "x" + screenHeight);
        Logger.d(TAG, "Current Position: " + getCurrentX() + ", " + getCurrentY());
        Logger.d(TAG, "Restart Attempts: " + restartAttempts);
        Logger.d(TAG, "Is Initialized: " + isInitialized());
        
        if (overlayView != null) {
            overlayView.logCurrentState();
        } else {
            Logger.d(TAG, "OverlayView: null");
        }
        
        Logger.d(TAG, "==========================================");
    }
}