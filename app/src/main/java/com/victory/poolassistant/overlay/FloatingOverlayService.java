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
 * Foreground service untuk floating overlay Pool Assistant
 * Handles window management dan lifecycle overlay
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
    
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "FloatingOverlayService created");
        
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
            
            // Position overlay
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = 100; // Initial X position
            layoutParams.y = 100; // Initial Y position
            
            Logger.d(TAG, "Overlay view initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize overlay view", e);
        }
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
            // TODO: Notify user about permission requirement
            return;
        }
        
        try {
            windowManager.addView(overlayView, layoutParams);
            isOverlayVisible = true;
            
            Logger.i(TAG, "Overlay shown successfully");
            
            // Update notification
            updateNotification("Pool Assistant overlay active");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to show overlay", e);
            isOverlayVisible = false;
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
            windowManager.removeView(overlayView);
            isOverlayVisible = false;
            
            Logger.i(TAG, "Overlay hidden successfully");
            
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
            .setSmallIcon(android.R.drawable.ic_dialog_info) // FIXED: Use system icon
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view, // FIXED: Use system icon
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
     * Update overlay position (called by OverlayView)
     */
    public void updateOverlayPosition(int x, int y) {
        if (layoutParams != null && isOverlayVisible) {
            layoutParams.x = x;
            layoutParams.y = y;
            
            try {
                windowManager.updateViewLayout(overlayView, layoutParams);
            } catch (Exception e) {
                Logger.e(TAG, "Failed to update overlay position", e);
            }
        }
    }
}