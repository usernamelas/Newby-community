package com.victory.poolassistant.overlay;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.PermissionHelper;

/**
 * Manager class untuk koordinasi floating overlay system
 * Handles service lifecycle, permissions, dan state management
 */
public class OverlayManager {
    
    private static final String TAG = "OverlayManager";
    
    private final Context context;
    private static OverlayManager instance;
    
    // State tracking
    private boolean isServiceRunning = false;
    private boolean hasOverlayPermission = false;
    
    // Listeners
    private OnOverlayStateChangeListener stateChangeListener;
    
    /**
     * Interface untuk overlay state changes
     */
    public interface OnOverlayStateChangeListener {
        void onOverlayStarted();
        void onOverlayStopped();
        void onOverlayPermissionRequired();
        void onOverlayError(String error);
    }
    
    private OverlayManager(Context context) {
        this.context = context.getApplicationContext();
        checkPermissions();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized OverlayManager getInstance(Context context) {
        if (instance == null) {
            instance = new OverlayManager(context);
        }
        return instance;
    }
    
    /**
     * Set state change listener
     */
    public void setOnOverlayStateChangeListener(OnOverlayStateChangeListener listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Start floating overlay service
     */
    public void startOverlay() {
        Logger.d(TAG, "Starting overlay service...");
        
        // Check permissions first
        if (!checkPermissions()) {
            Logger.w(TAG, "Cannot start overlay - missing permissions");
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayPermissionRequired();
            }
            return;
        }
        
        try {
            Intent serviceIntent = new Intent(context, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_START_OVERLAY);
            
            context.startForegroundService(serviceIntent);
            isServiceRunning = true;
            
            Logger.i(TAG, "Overlay service started successfully");
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayStarted();
            }
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to start overlay service", e);
            isServiceRunning = false;
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayError("Failed to start overlay: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop floating overlay service
     */
    public void stopOverlay() {
        Logger.d(TAG, "Stopping overlay service...");
        
        try {
            Intent serviceIntent = new Intent(context, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_STOP_OVERLAY);
            
            context.stopService(serviceIntent);
            isServiceRunning = false;
            
            Logger.i(TAG, "Overlay service stopped successfully");
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayStopped();
            }
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to stop overlay service", e);
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayError("Failed to stop overlay: " + e.getMessage());
            }
        }
    }
    
    /**
     * Toggle overlay visibility
     */
    public void toggleOverlay() {
        Logger.d(TAG, "Toggling overlay...");
        
        if (!isServiceRunning) {
            startOverlay();
        } else {
            Intent serviceIntent = new Intent(context, FloatingOverlayService.class);
            serviceIntent.setAction(FloatingOverlayService.ACTION_TOGGLE_OVERLAY);
            context.startService(serviceIntent);
        }
    }
    
    /**
     * Check if overlay is currently visible
     */
    public boolean isOverlayVisible() {
        FloatingOverlayService service = FloatingOverlayService.getInstance();
        return service != null && service.isOverlayVisible();
    }
    
    /**
     * Check if service is running
     */
    public boolean isServiceRunning() {
        return isServiceRunning;
    }
    
    /**
     * Check overlay permissions
     */
    public boolean checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(context);
        
        Logger.d(TAG, "Overlay permission status: " + hasOverlayPermission);
        return hasOverlayPermission;
    }
    
    /**
     * Request overlay permission
     */
    public void requestOverlayPermission() {
        Logger.d(TAG, "Requesting overlay permission...");
        
        try {
            PermissionHelper.requestOverlayPermission(context);
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to request overlay permission", e);
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayError("Failed to request permission: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if has overlay permission
     */
    public boolean hasOverlayPermission() {
        return hasOverlayPermission;
    }
    
    /**
     * Get overlay service instance
     */
    public FloatingOverlayService getService() {
        return FloatingOverlayService.getInstance();
    }
    
    /**
     * Update overlay status
     */
    public void updateOverlayStatus(String status) {
        FloatingOverlayService service = FloatingOverlayService.getInstance();
        if (service != null && service.isOverlayVisible()) {
            // Get overlay view and update status
            // TODO: Implement status update mechanism
            Logger.d(TAG, "Status update: " + status);
        }
    }
    
    /**
     * Show overlay for specific duration
     */
    public void showOverlayTemporary(int durationMs) {
        Logger.d(TAG, "Showing overlay temporarily for " + durationMs + "ms");
        
        startOverlay();
        
        // Auto-hide after duration
        new android.os.Handler(context.getMainLooper()).postDelayed(() -> {
            stopOverlay();
        }, durationMs);
    }
    
    /**
     * Quick start untuk testing
     */
    public void quickStart() {
        Logger.d(TAG, "Quick start overlay for testing...");
        
        if (checkPermissions()) {
            startOverlay();
        } else {
            Logger.w(TAG, "Quick start failed - no overlay permission");
            requestOverlayPermission();
        }
    }
    
    /**
     * Emergency stop (force stop)
     */
    public void emergencyStop() {
        Logger.w(TAG, "Emergency stop triggered");
        
        try {
            context.stopService(new Intent(context, FloatingOverlayService.class));
            isServiceRunning = false;
            
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayStopped();
            }
            
        } catch (Exception e) {
            Logger.e(TAG, "Emergency stop failed", e);
        }
    }
    
    /**
     * Get overlay state info
     */
    public String getStateInfo() {
        return String.format(
            "Service Running: %s, Overlay Visible: %s, Has Permission: %s",
            isServiceRunning,
            isOverlayVisible(),
            hasOverlayPermission
        );
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayManager");
        
        stopOverlay();
        stateChangeListener = null;
        instance = null;
    }
}