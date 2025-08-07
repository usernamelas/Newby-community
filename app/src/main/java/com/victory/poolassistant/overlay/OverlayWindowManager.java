package com.victory.poolassistant.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.victory.poolassistant.core.Logger;

/**
 * Advanced window manager untuk floating overlay
 * Handles positioning, screen boundaries, multi-display support
 */
public class OverlayWindowManager {
    
    private static final String TAG = "OverlayWindowManager";
    
    private final Context context;
    private final WindowManager windowManager;
    
    // Screen info
    private int screenWidth;
    private int screenHeight;
    private int statusBarHeight;
    
    // Window positioning
    private static final int EDGE_MARGIN = 16;
    private static final int SNAP_THRESHOLD = 50;
    
    public OverlayWindowManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        updateScreenInfo();
    }
    
    /**
     * Create default overlay layout parameters (MISSING METHOD)
     */
    public WindowManager.LayoutParams createOverlayLayoutParams() {
        return createLayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );
    }
    
    /**
     * Add overlay view to window manager (MISSING METHOD)
     */
    public void addOverlayView(OverlayView overlayView, WindowManager.LayoutParams params) {
        try {
            windowManager.addView(overlayView, params);
            Logger.d(TAG, "Overlay view added successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to add overlay view", e);
            throw e;
        }
    }
    
    /**
     * Remove overlay view from window manager (MISSING METHOD)
     */
    public void removeOverlayView(OverlayView overlayView) {
        try {
            windowManager.removeView(overlayView);
            Logger.d(TAG, "Overlay view removed successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to remove overlay view", e);
            throw e;
        }
    }
    
    /**
     * Update overlay view layout (MISSING METHOD)
     */
    public void updateOverlayView(OverlayView overlayView, WindowManager.LayoutParams params) {
        try {
            windowManager.updateViewLayout(overlayView, params);
            Logger.d(TAG, "Overlay view updated successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to update overlay view", e);
            throw e;
        }
    }
    
    /**
     * Get screen size as array (MISSING METHOD)
     */
    public int[] getScreenSize() {
        return new int[]{screenWidth, screenHeight};
    }
    
    /**
     * Get status bar height (Make PUBLIC instead of PRIVATE)
     */
    public int getStatusBarHeight() {
        return statusBarHeight;
    }
    
    /**
     * Cleanup resources (MISSING METHOD)
     */
    public void cleanup() {
        Logger.d(TAG, "OverlayWindowManager cleanup completed");
        // Currently no specific cleanup needed
        // Can be extended for future resource management
    }
    
    /**
     * Create optimal layout parameters untuk overlay window
     */
    public WindowManager.LayoutParams createLayoutParams(int width, int height) {
        int layoutFlag = getOptimalWindowType();
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            getOptimalWindowFlags(),
            PixelFormat.TRANSLUCENT
        );
        
        // Set initial position
        Point initialPos = getOptimalInitialPosition(width, height);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = initialPos.x;
        params.y = initialPos.y;
        
        Logger.d(TAG, "Created layout params: " + params.width + "x" + params.height + 
                 " at (" + params.x + "," + params.y + ")");
        
        return params;
    }
    
    /**
     * Get optimal window type berdasarkan Android version
     */
    private int getOptimalWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            return WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
    }
    
    /**
     * Get optimal window flags
     */
    private int getOptimalWindowFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
               WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
               WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
               WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    }
    
    /**
     * Get optimal initial position
     */
    private Point getOptimalInitialPosition(int windowWidth, int windowHeight) {
        // Default ke top-right corner dengan margin
        int x = screenWidth - windowWidth - EDGE_MARGIN;
        int y = statusBarHeight + EDGE_MARGIN;
        
        // Pastikan tidak keluar dari screen bounds
        x = Math.max(EDGE_MARGIN, Math.min(x, screenWidth - windowWidth - EDGE_MARGIN));
        y = Math.max(statusBarHeight, Math.min(y, screenHeight - windowHeight - EDGE_MARGIN));
        
        return new Point(x, y);
    }
    
    /**
     * Update screen information
     */
    public void updateScreenInfo() {
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            
            screenWidth = size.x;
            screenHeight = size.y;
            statusBarHeight = calculateStatusBarHeight();
            
            Logger.d(TAG, "Screen info updated: " + screenWidth + "x" + screenHeight + 
                     ", status bar: " + statusBarHeight);
        }
    }
    
    /**
     * Calculate status bar height
     */
    private int calculateStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier(
            "status_bar_height", "dimen", "android");
        
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    
    /**
     * Constrain position to screen boundaries
     */
    public Point constrainToScreen(int x, int y, int windowWidth, int windowHeight) {
        // Apply boundaries dengan margin
        int constrainedX = Math.max(EDGE_MARGIN, 
            Math.min(x, screenWidth - windowWidth - EDGE_MARGIN));
        int constrainedY = Math.max(statusBarHeight, 
            Math.min(y, screenHeight - windowHeight - EDGE_MARGIN));
        
        return new Point(constrainedX, constrainedY);
    }
    
    /**
     * Snap to screen edges if close enough
     */
    public Point snapToEdges(int x, int y, int windowWidth, int windowHeight) {
        int snappedX = x;
        int snappedY = y;
        
        // Snap to left edge
        if (x < SNAP_THRESHOLD) {
            snappedX = EDGE_MARGIN;
        }
        // Snap to right edge
        else if (x > screenWidth - windowWidth - SNAP_THRESHOLD) {
            snappedX = screenWidth - windowWidth - EDGE_MARGIN;
        }
        
        // Snap to top edge
        if (y < statusBarHeight + SNAP_THRESHOLD) {
            snappedY = statusBarHeight + EDGE_MARGIN;
        }
        // Snap to bottom edge
        else if (y > screenHeight - windowHeight - SNAP_THRESHOLD) {
            snappedY = screenHeight - windowHeight - EDGE_MARGIN;
        }
        
        return new Point(snappedX, snappedY);
    }
    
    /**
     * Get safe position yang tidak menghalangi UI penting
     */
    public Point getSafePosition(int windowWidth, int windowHeight) {
        // Avoid common UI areas (navigation bar, notch area, etc.)
        int safeX = screenWidth - windowWidth - EDGE_MARGIN * 2;
        int safeY = statusBarHeight + EDGE_MARGIN * 3;
        
        return constrainToScreen(safeX, safeY, windowWidth, windowHeight);
    }
    
    /**
     * Animate window position change
     */
    public void animateToPosition(View view, WindowManager.LayoutParams params, 
                                  int targetX, int targetY) {
        // Simple animation using post() method
        final int startX = params.x;
        final int startY = params.y;
        final int deltaX = targetX - startX;
        final int deltaY = targetY - startY;
        
        final int steps = 10;
        final long duration = 200; // ms
        
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            view.postDelayed(() -> {
                params.x = startX + (deltaX * step / steps);
                params.y = startY + (deltaY * step / steps);
                
                try {
                    windowManager.updateViewLayout(view, params);
                } catch (Exception e) {
                    Logger.e(TAG, "Animation update failed", e);
                }
            }, duration * i / steps);
        }
    }
    
    /**
     * Get screen center position
     */
    public Point getCenterPosition(int windowWidth, int windowHeight) {
        int centerX = (screenWidth - windowWidth) / 2;
        int centerY = (screenHeight - windowHeight) / 2;
        
        return constrainToScreen(centerX, centerY, windowWidth, windowHeight);
    }
    
    /**
     * Check if position is valid
     */
    public boolean isValidPosition(int x, int y, int windowWidth, int windowHeight) {
        return x >= 0 && y >= statusBarHeight && 
               x + windowWidth <= screenWidth && 
               y + windowHeight <= screenHeight;
    }
    
    /**
     * Get screen info summary
     */
    public String getScreenInfo() {
        return String.format(
            "Screen: %dx%d, Status Bar: %d, Safe Area: %dx%d",
            screenWidth, screenHeight, statusBarHeight,
            screenWidth - (EDGE_MARGIN * 2),
            screenHeight - statusBarHeight - (EDGE_MARGIN * 2)
        );
    }
    
    /**
     * Handle screen rotation
     */
    public void handleScreenRotation() {
        Logger.d(TAG, "Handling screen rotation...");
        updateScreenInfo();
        
        // TODO: Notify overlay service about screen change
        // so it can reposition window if needed
    }
}