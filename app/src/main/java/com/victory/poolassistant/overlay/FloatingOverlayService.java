package com.victory.poolassistant.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.victory.poolassistant.MainActivity;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.ThemeManager;

/**
 * Enhanced FloatingOverlayService dengan 3-state overlay support
 * Handles overlay lifecycle, theme changes, dan system events
 */
public class FloatingOverlayService extends Service {
    
    private static final String TAG = "FloatingOverlayService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "overlay_service_channel";
    
    // Service actions
    public static final String ACTION_SHOW_OVERLAY = "show_overlay";
    public static final String ACTION_HIDE_OVERLAY = "hide_overlay"; 
    public static final String ACTION_TOGGLE_OVERLAY = "toggle_overlay";
    public static final String ACTION_EXIT_APP = "exit_app";
    
    // State extras
    public static final String EXTRA_OVERLAY_STATE = "overlay_state";
    
    // Core components
    private OverlayManager overlayManager;
    private ThemeManager themeManager;
    private NotificationManager notificationManager;
    
    // State tracking
    private boolean isServiceRunning = false;
    private OverlayView.OverlayState currentOverlayState = OverlayView.OverlayState.FULL;
    
    // Broadcast receiver untuk theme changes
    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
                Logger.d(TAG, "Configuration changed, updating overlay theme");
                handleConfigurationChange();
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Logger.d(TAG, "FloatingOverlayService created");
        
        initializeComponents();
        createNotificationChannel();
        registerReceivers();
        
        isServiceRunning = true;
    }
    
    /**
     * Initialize service components
     */
    private void initializeComponents() {
        try {
            overlayManager = new OverlayManager(this);
            overlayManager.setOverlayService(this);
            
            themeManager = new ThemeManager(this);
            notificationManager = getSystemService(NotificationManager.class);
            
            Logger.d(TAG, "Service components initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize service components", e);
        }
    }
    
    /**
     * Create notification channel untuk Android 8+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pool Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Floating overlay service notification");
            channel.setShowBadge(false);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Register broadcast receivers
     */
    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(themeChangeReceiver, filter);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand called with action: " + 
            (intent != null ? intent.getAction() : "null"));
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Handle intent actions
        if (intent != null) {
            handleServiceAction(intent);
        } else {
            // Default action - show overlay
            showOverlay(OverlayView.OverlayState.FULL);
        }
        
        return START_STICKY; // Restart if killed by system
    }
    
    /**
     * Handle service actions dari intents
     */
    private void handleServiceAction(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        
        switch (action) {
            case ACTION_SHOW_OVERLAY:
                String stateExtra = intent.getStringExtra(EXTRA_OVERLAY_STATE);
                OverlayView.OverlayState state = parseOverlayState(stateExtra);
                showOverlay(state);
                break;
                
            case ACTION_HIDE_OVERLAY:
                hideOverlay();
                break;
                
            case ACTION_TOGGLE_OVERLAY:
                toggleOverlay();
                break;
                
            case ACTION_EXIT_APP:
                exitApplication();
                break;
                
            default:
                Logger.w(TAG, "Unknown service action: " + action);
                break;
        }
    }
    
    /**
     * Parse overlay state dari string
     */
    private OverlayView.OverlayState parseOverlayState(String stateString) {
        if (stateString == null) return OverlayView.OverlayState.FULL;
        
        try {
            return OverlayView.OverlayState.valueOf(stateString);
        } catch (IllegalArgumentException e) {
            Logger.w(TAG, "Invalid overlay state: " + stateString);
            return OverlayView.OverlayState.FULL;
        }
    }
    
    /**
     * Show overlay dengan state tertentu
     */
    public void showOverlay(OverlayView.OverlayState state) {
        if (!isServiceRunning) {
            Logger.w(TAG, "Service not running, cannot show overlay");
            return;
        }
        
        Logger.d(TAG, "Showing overlay with state: " + state);
        
        if (overlayManager.showOverlay(state)) {
            currentOverlayState = state;
            updateNotification();
        } else {
            Logger.e(TAG, "Failed to show overlay");
        }
    }
    
    /**
     * Hide overlay
     */
    public void hideOverlay() {
        Logger.d(TAG, "Hiding overlay");
        
        if (overlayManager != null) {
            overlayManager.hideOverlay();
            updateNotification();
        }
    }
    
    /**
     * Toggle overlay visibility
     */
    public void toggleOverlay() {
        if (overlayManager.isOverlayShowing()) {
            hideOverlay();
        } else {
            showOverlay(currentOverlayState);
        }
    }
    
    /**
     * Update overlay position
     */
    public void updateOverlayPosition(int x, int y) {
        if (overlayManager != null) {
            overlayManager.updateOverlayPosition(x, y);
        }
    }
    
    /**
     * Update overlay size untuk state changes
     */
    public void updateOverlaySize(OverlayView.OverlayState newState) {
        if (overlayManager != null) {
            currentOverlayState = newState;
            overlayManager.updateOverlayState(newState);
            updateNotification();
        }
    }
    
    /**
     * Reset overlay position ke default
     */
    public void resetOverlayPosition() {
        if (overlayManager != null) {
            overlayManager.resetOverlayPosition();
        }
    }
    
    /**
     * Exit entire application
     */
    public void exitApplication() {
        Logger.d(TAG, "Exiting application");
        
        // Hide overlay first
        hideOverlay();
        
        // Stop service
        stopSelf();
        
        // Exit main activity if running
        Intent exitIntent = new Intent(this, MainActivity.class);
        exitIntent.setAction("EXIT_APP");
        exitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(exitIntent);
        
        // Force exit
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    /**
     * Handle configuration changes (theme, rotation, etc.)
     */
    private void handleConfigurationChange() {
        if (overlayManager != null) {
            overlayManager.onConfigurationChanged();
        }
    }
    
    /**
     * Create foreground notification
     */
    private Notification createNotification() {
        Intent openMainIntent = new Intent(this, MainActivity.class);
        PendingIntent openMainPendingIntent = PendingIntent.getActivity(
            this, 0, openMainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent hideIntent = new Intent(this, FloatingOverlayService.class);
        hideIntent.setAction(ACTION_HIDE_OVERLAY);
        PendingIntent hidePendingIntent = PendingIntent.getService(
            this, 1, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent exitIntent = new Intent(this, FloatingOverlayService.class);
        exitIntent.setAction(ACTION_EXIT_APP);
        PendingIntent exitPendingIntent = PendingIntent.getService(
            this, 2, exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String contentText = getNotificationText();
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pool Assistant Overlay")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_pool_ball)
            .setContentIntent(openMainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_minimize, "Hide", hidePendingIntent)
            .addAction(R.drawable.ic_stop, "Exit", exitPendingIntent)
            .setAutoCancel(false)
            .build();
    }
    
    /**
     * Get notification text based on current state
     */
    private String getNotificationText() {
        if (!overlayManager.isOverlayShowing()) {
            return "Overlay hidden • Tap to open";
        }
        
        switch (currentOverlayState) {
            case FULL:
                return "Full overlay active • Ready for pool assistance";
            case ICON:
                return "Minimized mode • Tap icon to expand";
            case SETTINGS:
                return "Settings menu • Configure your overlay";
            default:
                return "Overlay service running";
        }
    }
    
    /**
     * Update existing notification
     */
    private void updateNotification() {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification());
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logger.d(TAG, "Configuration changed");
        handleConfigurationChange();
    }
    
    @Override
    public void onDestroy() {
        Logger.d(TAG, "FloatingOverlayService destroyed");
        
        isServiceRunning = false;
        
        // Cleanup overlay
        if (overlayManager != null) {
            overlayManager.onServiceDestroy();
            overlayManager.cleanup();
            overlayManager = null;
        }
        
        // Unregister receivers
        try {
            unregisterReceiver(themeChangeReceiver);
        } catch (Exception e) {
            Logger.w(TAG, "Error unregistering receiver", e);
        }
        
        // Clear references
        themeManager = null;
        notificationManager = null;
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }
    
    /**
     * Get current overlay state
     */
    public OverlayView.OverlayState getCurrentOverlayState() {
        return currentOverlayState;
    }
    
    /**
     * Check if overlay is currently showing
     */
    public boolean isOverlayShowing() {
        return overlayManager != null && overlayManager.isOverlayShowing();
    }
    
    /**
     * Get overlay manager instance
     */
    public OverlayManager getOverlayManager() {
        return overlayManager;
    }
    
    /**
     * Get theme manager instance
     */
    public ThemeManager getThemeManager() {
        return themeManager;
    }
    
    /**
     * Static helper methods untuk start service dengan specific actions
     */
    
    public static void startOverlayService(Context context, OverlayView.OverlayState state) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_SHOW_OVERLAY);
        intent.putExtra(EXTRA_OVERLAY_STATE, state.name());
        context.startForegroundService(intent);
    }
    
    public static void hideOverlay(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_HIDE_OVERLAY);
        context.startService(intent);
    }
    
    public static void toggleOverlay(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_TOGGLE_OVERLAY);
        context.startService(intent);
    }
    
    public static void stopOverlayService(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        context.stopService(intent);
    }
}