package com.victory.poolassistant.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.victory.poolassistant.MainActivity;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * FloatingOverlayService - FIXED untuk allow background touch
 * FIXED: Window flags yang tidak block background apps
 */
public class FloatingOverlayService extends Service {
    
    private static final String TAG = "FloatingOverlayService";
    private static final String CHANNEL_ID = "pool_assistant_overlay";
    private static final int NOTIFICATION_ID = 1001;
    
    // Actions
    public static final String ACTION_START_OVERLAY = "com.victory.poolassistant.START_OVERLAY";
    public static final String ACTION_STOP_OVERLAY = "com.victory.poolassistant.STOP_OVERLAY";
    public static final String ACTION_TOGGLE_OVERLAY = "com.victory.poolassistant.TOGGLE_OVERLAY";
    
    // Window management
    private WindowManager windowManager;
    private OverlayView overlayView;
    private WindowManager.LayoutParams layoutParams;
    
    // State
    private boolean isOverlayVisible = false;
    private static FloatingOverlayService instance;
    
    // Enhanced state tracking
    private int currentX = 100;
    private int currentY = 100;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "FloatingOverlayService created - Touch Fixed Version");
        
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        
        // Initialize overlay view
        initializeOverlayView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        if (intent != null) {
            String action = intent.getAction();
            
            switch (action != null ? action : "") {
                case ACTION_START_OVERLAY:
                    showOverlay();
                    break;
                case ACTION_STOP_OVERLAY:
                    hideOverlay();
                    break;
                case ACTION_TOGGLE_OVERLAY:
                    toggleOverlay();
                    break;
                default:
                    // Default behavior - show overlay
                    showOverlay();
                    break;
            }
        }
        
        // Service akan restart jika di-kill system
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Logger.d(TAG, "FloatingOverlayService destroyed");
        
        hideOverlay();
        instance = null;
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service tidak di-bind
    }
    
    /**
     * FIXED: Initialize overlay dengan optimal flags untuk background touch
     */
    private void initializeOverlayView() {
        try {
            // Create overlay view
            overlayView = new OverlayView(this);
            
            // FIXED: Setup window parameters dengan proper flags
            int layoutFlag = getOptimalWindowType();
            
            layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                getBackgroundTouchFriendlyFlags(), // FIXED: New flags
                PixelFormat.TRANSLUCENT
            );
            
            // Optimal positioning
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = currentX;
            layoutParams.y = currentY;
            
            // FIXED: Additional properties untuk better behavior
            layoutParams.windowAnimations = 0; // No animation interference
            layoutParams.alpha = 1.0f;
            layoutParams.dimAmount = 0f; // No background dimming
            
            Logger.d(TAG, "Overlay view initialized with background-touch-friendly flags");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize overlay view", e);
        }
    }
    
    /**
     * Get optimal window type based on Android version
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
     * FIXED: Window flags yang allow background apps untuk di-touch
     */
    private int getBackgroundTouchFriendlyFlags() {
        int flags = 0;
        
        // CRITICAL: Allow background touch - apps lain bisa disentuh
        flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;     // Don't steal focus
        flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;   // Allow background touch
        
        // LAYOUT FLAGS untuk positioning
        flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        // PERFORMANCE FLAGS
        flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        
        // REMOVED: Flags yang block background interaction
        // REMOVED: FLAG_SHOW_WHEN_LOCKED (block background)
        // REMOVED: FLAG_DISMISS_KEYGUARD (block background) 
        // REMOVED: FLAG_TURN_SCREEN_ON (too aggressive)
        // REMOVED: FLAG_KEEP_SCREEN_ON (unnecessary)
        
        Logger.d(TAG, "Background-touch-friendly flags: " + Integer.toHexString(flags));
        return flags;
    }
    
    /**
     * Show overlay dengan better error handling
     */
    public void showOverlay() {
        if (isOverlayVisible || overlayView == null) {
            Logger.w(TAG, "Cannot show overlay - already visible or view is null");
            return;
        }
        
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Logger.e(TAG, "No overlay permission - cannot show overlay");
            return;
        }
        
        try {
            // Add overlay dengan background-friendly parameters
            windowManager.addView(overlayView, layoutParams);
            isOverlayVisible = true;
            
            Logger.i(TAG, "Overlay shown successfully at (" + currentX + "," + currentY + ") - Background touch enabled");
            
            // Update notification
            updateNotification("Pool Assistant overlay active");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to show overlay", e);
            isOverlayVisible = false;
        }
    }
    
    /**
     * Hide overlay dengan proper cleanup
     */
    public void hideOverlay() {
        if (!isOverlayVisible || overlayView == null) {
            Logger.w(TAG, "Cannot hide overlay - not visible or view is null");
            return;
        }
        
        try {
            // Save current position before hiding
            currentX = layoutParams.x;
            currentY = layoutParams.y;
            
            windowManager.removeView(overlayView);
            isOverlayVisible = false;
            
            Logger.i(TAG, "Overlay hidden successfully, position saved: (" + currentX + "," + currentY + ")");
            
            // Update notification
            updateNotification("Pool Assistant overlay hidden");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to hide overlay", e);
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
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
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
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE 
                : 0
        );
        
        // Toggle action
        Intent toggleIntent = new Intent(this, FloatingOverlayService.class);
        toggleIntent.setAction(ACTION_TOGGLE_OVERLAY);
        PendingIntent togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE 
                : 0
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pool Assistant")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_visibility,
                isOverlayVisible ? "Hide Overlay" : "Show Overlay",
                togglePendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    /**
     * Update notification text
     */
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotificationWithText(text));
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
     * Update overlay position dengan bounds checking
     */
    public void updateOverlayPosition(int x, int y) {
        if (layoutParams != null && isOverlayVisible) {
            // Update internal tracking
            currentX = x;
            currentY = y;
            
            // Apply to layout params
            layoutParams.x = currentX;
            layoutParams.y = currentY;
            
            try {
                windowManager.updateViewLayout(overlayView, layoutParams);
                Logger.v(TAG, "Overlay position updated to (" + currentX + "," + currentY + ")");
            } catch (Exception e) {
                Logger.e(TAG, "Failed to update overlay position", e);
            }
        }
    }
    
    /**
     * Get current overlay X position
     */
    public int getCurrentX() {
        return layoutParams != null ? layoutParams.x : currentX;
    }
    
    /**
     * Get current overlay Y position  
     */
    public int getCurrentY() {
        return layoutParams != null ? layoutParams.y : currentY;
    }
    
    /**
     * Get overlay view instance (for advanced control)
     */
    public OverlayView getOverlayView() {
        return overlayView;
    }
    
    /**
     * Check if service is running
     */
    public static boolean isServiceRunning() {
        return instance != null;
    }
    
    /**
     * Get window layout parameters (for debugging)
     */
    public WindowManager.LayoutParams getLayoutParams() {
        return layoutParams;
    }
    
    /**
     * Graceful shutdown
     */
    public void shutdownService() {
        Logger.i(TAG, "Shutting down FloatingOverlayService gracefully...");
        
        // Hide overlay first
        hideOverlay();
        
        // Clean up overlay view
        if (overlayView != null) {
            overlayView.cleanup();
            overlayView = null;
        }
        
        // Stop foreground service
        stopForeground(true);
        
        // Stop service
        stopSelf();
    }
    
    /**
     * Get service info untuk debugging
     */
    public String getServiceInfo() {
        return String.format(
            "Service - Visible: %s, Position: (%d,%d), Background Touch: ENABLED", 
            isOverlayVisible, getCurrentX(), getCurrentY()
        );
    }
}