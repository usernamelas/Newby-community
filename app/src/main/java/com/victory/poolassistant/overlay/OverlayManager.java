package com.victory.poolassistant.overlay;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.WindowManager;

import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.PermissionHelper;

/**
 * Enhanced OverlayManager untuk coordinate 3-state overlay system
 * Manages overlay lifecycle, permissions, dan state transitions
 * FIXED VERSION - Compatible dengan programmatic OverlayView
 */
public class OverlayManager {
    
    private static final String TAG = "OverlayManager";
    
    private Context context;
    private OverlayWindowManager windowManager;
    private OverlayView overlayView;
    private FloatingOverlayService overlayService;
    
    // State tracking
    private boolean isOverlayShowing = false;
    private OverlayView.OverlayState lastKnownState = OverlayView.OverlayState.FULL;
    
    // Position tracking
    private int lastX = 100;
    private int lastY = 100;
    private int defaultX = 100;
    private int defaultY = 100;
    
    public OverlayManager(Context context) {
        this.context = context;
        this.windowManager = new OverlayWindowManager(context);
        
        Logger.d(TAG, "OverlayManager initialized");
    }
    
    /**
     * Set overlay service reference
     */
    public void setOverlayService(FloatingOverlayService service) {
        this.overlayService = service;
        if (overlayView != null) {
            // Service reference is already set in OverlayView constructor
            Logger.d(TAG, "Overlay service reference updated");
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    public boolean hasOverlayPermission() {
        return PermissionHelper.hasOverlayPermission(context);
    }
    
    /**
     * Request overlay permission
     */
    public void requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            Logger.d(TAG, "Requesting overlay permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    /**
     * Show overlay dengan state tertentu
     */
    public boolean showOverlay(OverlayView.OverlayState initialState) {
        if (!hasOverlayPermission()) {
            Logger.w(TAG, "Cannot show overlay - permission not granted");
            requestOverlayPermission();
            return false;
        }
        
        if (isOverlayShowing) {
            Logger.d(TAG, "Overlay already showing, updating state to: " + initialState);
            updateOverlayState(initialState);
            return true;
        }
        
        try {
            // Create overlay view - pass service as context
            overlayView = new OverlayView(overlayService != null ? overlayService : context);
            
            // Set initial state using forceState method
            overlayView.forceState(initialState);
            lastKnownState = initialState;
            
            // Add to window manager
            WindowManager.LayoutParams params = createLayoutParams(initialState);
            windowManager.addOverlayView(overlayView, params);
            
            isOverlayShowing = true;
            Logger.d(TAG, "Overlay shown successfully with state: " + initialState);
            
            // Log debug info
            if (overlayView.isInitialized()) {
                overlayView.logCurrentState();
            }
            
            return true;
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to show overlay", e);
            return false;
        }
    }
    
    /**
     * Hide overlay completely
     */
    public void hideOverlay() {
        if (!isOverlayShowing || overlayView == null) {
            Logger.d(TAG, "Overlay not showing, nothing to hide");
            return;
        }
        
        try {
            // Save current position
            saveCurrentPosition();
            
            // Remove from window manager
            windowManager.removeOverlayView(overlayView);
            
            // Cleanup
            overlayView.cleanup();
            overlayView = null;
            
            isOverlayShowing = false;
            Logger.d(TAG, "Overlay hidden successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to hide overlay", e);
        }
    }
    
    /**
     * Update overlay state
     */
    public void updateOverlayState(OverlayView.OverlayState newState) {
        if (!isOverlayShowing || overlayView == null) {
            Logger.w(TAG, "Cannot update state - overlay not showing");
            return;
        }
        
        try {
            // Update view state using forceState
            overlayView.forceState(newState);
            lastKnownState = newState;
            
            // Update window parameters for new state
            WindowManager.LayoutParams params = createLayoutParams(newState);
            updateOverlayPosition(lastX, lastY, params);
            
            Logger.d(TAG, "Overlay state updated to: " + newState);
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to update overlay state", e);
        }
    }
    
    /**
     * Update overlay position
     */
    public void updateOverlayPosition(int x, int y) {
        updateOverlayPosition(x, y, null);
    }
    
    /**
     * Update overlay position dengan custom params
     */
    public void updateOverlayPosition(int x, int y, WindowManager.LayoutParams customParams) {
        if (!isOverlayShowing || overlayView == null) return;
        
        try {
            // Constrain position to screen bounds
            int[] constrainedPos = constrainToScreenBounds(x, y);
            lastX = constrainedPos[0];
            lastY = constrainedPos[1];
            
            // Update OverlayView's initial position
            overlayView.updateInitialPosition(lastX, lastY);
            
            // Use custom params or create new ones
            WindowManager.LayoutParams params = customParams != null ? 
                customParams : createLayoutParams(lastKnownState);
            
            params.x = lastX;
            params.y = lastY;
            
            windowManager.updateOverlayView(overlayView, params);
            
            Logger.d(TAG, "Overlay position updated to: " + lastX + ", " + lastY);
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to update overlay position", e);
        }
    }
    
    /**
     * Reset overlay position to default
     */
    public void resetOverlayPosition() {
        Logger.d(TAG, "Resetting overlay position to default");
        updateOverlayPosition(defaultX, defaultY);
        
        // Also trigger OverlayView's reset method if available
        if (overlayView != null) {
            // overlayView has its own resetOverlayPosition method
            // Let it handle through its own button click
        }
    }
    
    /**
     * Create layout parameters for different states
     */
    private WindowManager.LayoutParams createLayoutParams(OverlayView.OverlayState state) {
        WindowManager.LayoutParams params = windowManager.createOverlayLayoutParams();
        
        // Adjust size based on state - match OverlayView's programmatic sizes
        switch (state) {
            case FULL:
                params.width = dpToPx(320); // Match createFullState width
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                break;
                
            case ICON:
                params.width = dpToPx(72); // Match createIconState width (72dp)
                params.height = dpToPx(72);
                break;
                
            case SETTINGS:
                params.width = dpToPx(280); // Match createSettingsState width
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                break;
        }
        
        // Position
        params.x = lastX;
        params.y = lastY;
        
        return params;
    }
    
    /**
     * Constrain position to screen bounds
     */
    private int[] constrainToScreenBounds(int x, int y) {
        // Get screen dimensions
        int[] screenSize = windowManager.getScreenSize();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        // Get overlay dimensions based on state
        int overlayWidth = getOverlayWidth();
        int overlayHeight = getOverlayHeight();
        
        // Constrain X
        int constrainedX = Math.max(0, Math.min(x, screenWidth - overlayWidth));
        
        // Constrain Y (account for status bar)
        int statusBarHeight = windowManager.getStatusBarHeight();
        int constrainedY = Math.max(statusBarHeight, Math.min(y, screenHeight - overlayHeight));
        
        return new int[]{constrainedX, constrainedY};
    }
    
    /**
     * Get overlay width based on current state
     */
    private int getOverlayWidth() {
        switch (lastKnownState) {
            case FULL:
                return dpToPx(320);
            case ICON:
                return dpToPx(72); // Match OverlayView's icon size
            case SETTINGS:
                return dpToPx(280);
            default:
                return dpToPx(320);
        }
    }
    
    /**
     * Get overlay height (estimated)
     */
    private int getOverlayHeight() {
        switch (lastKnownState) {
            case FULL:
                return dpToPx(400); // Estimated based on UI elements
            case ICON:
                return dpToPx(72);
            case SETTINGS:
                return dpToPx(300); // Estimated
            default:
                return dpToPx(400);
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
    
    /**
     * Save current overlay position
     */
    private void saveCurrentPosition() {
        // TODO: Save to SharedPreferences for persistence
        Logger.d(TAG, "Saving overlay position: " + lastX + ", " + lastY);
    }
    
    /**
     * Load saved overlay position
     */
    private void loadSavedPosition() {
        // TODO: Load from SharedPreferences
        // For now, use defaults
        lastX = defaultX;
        lastY = defaultY;
    }
    
    /**
     * Get overlay status
     */
    public boolean isOverlayShowing() {
        return isOverlayShowing;
    }
    
    /**
     * Get current overlay state
     */
    public OverlayView.OverlayState getCurrentState() {
        if (overlayView != null) {
            return overlayView.getCurrentState();
        }
        return lastKnownState;
    }
    
    /**
     * Get overlay view instance
     */
    public OverlayView getOverlayView() {
        return overlayView;
    }
    
    /**
     * Handle overlay service destruction
     */
    public void onServiceDestroy() {
        Logger.d(TAG, "Service destroying, cleaning up overlay");
        hideOverlay();
        overlayService = null;
    }
    
    /**
     * Handle configuration changes (screen rotation, etc.)
     */
    public void onConfigurationChanged() {
        if (isOverlayShowing && overlayView != null) {
            Logger.d(TAG, "Configuration changed, updating overlay");
            
            // Reposition overlay to ensure it's still on screen
            int[] constrainedPos = constrainToScreenBounds(lastX, lastY);
            updateOverlayPosition(constrainedPos[0], constrainedPos[1]);
        }
    }
    
    /**
     * Get feature states dari overlay - FIXED method names to match OverlayView
     */
    public boolean isBasicAimEnabled() {
        return overlayView != null ? overlayView.isFiturAimEnabled() : false;
    }
    
    public boolean isRootAimEnabled() {
        return overlayView != null ? overlayView.isAimRootModeEnabled() : false;
    }
    
    public boolean isPredictionEnabled() {
        return overlayView != null ? overlayView.isPrediksiEnabled() : false;
    }
    
    public int getOpacityValue() {
        return overlayView != null ? overlayView.getOpacityValue() : 80;
    }
    
    public int getLineThicknessValue() {
        return overlayView != null ? overlayView.getThicknessValue() : 60;
    }
    
    /**
     * Additional helper methods to match OverlayView capabilities
     */
    public boolean isLightThemeEnabled() {
        return overlayView != null ? overlayView.isLightThemeEnabled() : false;
    }
    
    public boolean isOverlayDragging() {
        return overlayView != null ? overlayView.isDragging() : false;
    }
    
    /**
     * Set feature states (for programmatic control)
     */
    public void setBasicAim(boolean enabled) {
        if (overlayView != null) {
            overlayView.setFiturAim(enabled);
        }
    }
    
    public void setRootAim(boolean enabled) {
        if (overlayView != null) {
            overlayView.setAimRootMode(enabled);
        }
    }
    
    public void setPrediction(boolean enabled) {
        if (overlayView != null) {
            overlayView.setPrediksi(enabled);
        }
    }
    
    public void setOpacity(int value) {
        if (overlayView != null) {
            overlayView.setOpacityValue(value);
        }
    }
    
    public void setLineThickness(int value) {
        if (overlayView != null) {
            overlayView.setThicknessValue(value);
        }
    }
    
    public void setLightTheme(boolean enabled) {
        if (overlayView != null) {
            overlayView.setLightTheme(enabled);
        }
    }
    
    /**
     * Force overlay to specific state (external control)
     */
    public void forceOverlayState(OverlayView.OverlayState state) {
        if (overlayView != null) {
            overlayView.forceState(state);
            lastKnownState = state;
            
            // Update layout params for new state
            WindowManager.LayoutParams params = createLayoutParams(state);
            updateOverlayPosition(lastX, lastY, params);
        }
    }
    
    /**
     * Get detailed overlay status for debugging
     */
    public String getOverlayStatus() {
        if (!isOverlayShowing || overlayView == null) {
            return "Overlay not showing";
        }
        
        return String.format("State: %s | Position: (%d,%d) | Dragging: %s | Features: Aim=%s, Root=%s, Prediction=%s", 
            getCurrentState(),
            lastX, lastY,
            isOverlayDragging(),
            isBasicAimEnabled(),
            isRootAimEnabled(), 
            isPredictionEnabled()
        );
    }
    
    /**
     * Log complete overlay state for debugging
     */
    public void logOverlayState() {
        Logger.d(TAG, "=== OverlayManager State Debug ===");
        Logger.d(TAG, "Showing: " + isOverlayShowing);
        Logger.d(TAG, "State: " + getCurrentState());
        Logger.d(TAG, "Position: " + lastX + ", " + lastY);
        Logger.d(TAG, "Service: " + (overlayService != null ? "Connected" : "Null"));
        Logger.d(TAG, "WindowManager: " + (windowManager != null ? "Ready" : "Null"));
        Logger.d(TAG, "OverlayView: " + (overlayView != null ? "Created" : "Null"));
        
        if (overlayView != null && overlayView.isInitialized()) {
            Logger.d(TAG, "--- OverlayView Details ---");
            overlayView.logCurrentState();
        }
        
        Logger.d(TAG, "Status: " + getOverlayStatus());
        Logger.d(TAG, "==================================");
    }
    
    /**
     * Cleanup all resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayManager");
        
        hideOverlay();
        
        if (windowManager != null) {
            windowManager.cleanup();
            windowManager = null;
        }
        
        overlayService = null;
        context = null;
        
        Logger.d(TAG, "OverlayManager cleanup completed");
    }
}