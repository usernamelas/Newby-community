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
 */
public class OverlayManager {
    
    private static final String TAG = "OverlayManager";
    
    private Context context;
    private OverlayWindowManager windowManager;
    private OverlayView overlayView;
    private FloatingOverlayService overlayService;
    private OnOverlayStateChangeListener stateChangeListener;
    
    // State tracking
    private boolean isOverlayShowing = false;
    private OverlayView.OverlayState lastKnownState = OverlayView.OverlayState.FULL;
    
    // Position tracking
    private int lastX = 100;
    private int lastY = 100;
    private int defaultX = 100;
    private int defaultY = 100;
    
    /**
     * Interface yang hilang - diperlukan oleh MainActivity
     */
    public interface OnOverlayStateChangeListener {
        void onOverlayShown();
        void onOverlayHidden();
        void onOverlayStateChanged(boolean isVisible);
    }
    
    public OverlayManager(Context context) {
        this.context = context;
        this.windowManager = new OverlayWindowManager(context);
        
        Logger.d(TAG, "OverlayManager initialized");
    }
    
    /**
     * Set listener for overlay state changes
     */
    public void setOnOverlayStateChangeListener(OnOverlayStateChangeListener listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Set overlay service reference
     */
    public void setOverlayService(FloatingOverlayService service) {
        this.overlayService = service;
        if (overlayView != null) {
            // Update service reference in view
            // This will be handled in OverlayView constructor
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
            // Create overlay view
            overlayView = new OverlayView(overlayService != null ? overlayService : context);
            
            // Set initial state
            overlayView.setState(initialState);
            lastKnownState = initialState;
            
            // Add to window manager
            WindowManager.LayoutParams params = createLayoutParams(initialState);
            windowManager.addOverlayView(overlayView, params);
            
            isOverlayShowing = true;
            
            // Notify listener
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayShown();
                stateChangeListener.onOverlayStateChanged(true);
            }
            
            Logger.d(TAG, "Overlay shown successfully with state: " + initialState);
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
            
            // Notify listener
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayHidden();
                stateChangeListener.onOverlayStateChanged(false);
            }
            
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
            // Update view state
            overlayView.setState(newState);
            lastKnownState = newState;
            
            // Update window parameters for new state
            WindowManager.LayoutParams params = createLayoutParams(newState);
            updateOverlayPosition(lastX, lastY, params);
            
            // Notify listener
            if (stateChangeListener != null) {
                stateChangeListener.onOverlayStateChanged(isOverlayShowing);
            }
            
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
            
            // Use custom params or create new ones
            WindowManager.LayoutParams params = customParams != null ? 
                customParams : createLayoutParams(lastKnownState);
            
            params.x = lastX;
            params.y = lastY;
            
            windowManager.updateOverlayView(overlayView, params);
            
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
    }
    
    /**
     * Create layout parameters for different states
     */
    private WindowManager.LayoutParams createLayoutParams(OverlayView.OverlayState state) {
        WindowManager.LayoutParams params = windowManager.createOverlayLayoutParams();
        
        // Adjust size based on state
        switch (state) {
            case FULL:
                params.width = WindowManager.LayoutParams.WRAP_CONTENT; // 320dp from XML
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                break;
                
            case ICON:
                params.width = dpToPx(64); // 64dp circle
                params.height = dpToPx(64);
                break;
                
            case SETTINGS:
                params.width = WindowManager.LayoutParams.WRAP_CONTENT; // 280dp from XML
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
                return dpToPx(64);
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
                return dpToPx(400); // Estimated
            case ICON:
                return dpToPx(64);
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
     * Get feature states dari overlay
     */
    public boolean isBasicAimEnabled() {
        return overlayView != null ? overlayView.isBasicAimEnabled() : false;
    }
    
    public boolean isRootAimEnabled() {
        return overlayView != null ? overlayView.isRootAimEnabled() : false;
    }
    
    public boolean isPredictionEnabled() {
        return overlayView != null ? overlayView.isPredictionEnabled() : false;
    }
    
    public int getOpacityValue() {
        return overlayView != null ? overlayView.getOpacityValue() : 80;
    }
    
    public int getLineThicknessValue() {
        return overlayView != null ? overlayView.getLineThicknessValue() : 5;
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
        stateChangeListener = null;
    }
}