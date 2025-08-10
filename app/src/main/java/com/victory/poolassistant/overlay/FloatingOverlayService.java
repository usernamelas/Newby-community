package com.victory.poolassistant.overlay;

import android.animation.ValueAnimator;
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
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.victory.poolassistant.MainActivity;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * Foreground service untuk floating overlay Pool Assistant
 * Handles window management dan lifecycle overlay
 * FIXED: Anti-hide saat scroll + Auto snap to edge
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
    
    // Animation
    private ValueAnimator positionAnimator;
    
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
        
        // Cancel animation
        if (positionAnimator != null && positionAnimator.isRunning()) {
            positionAnimator.cancel();
        }
        
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
            
            // FIXED: Window flags untuk tidak hilang saat scroll
            layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            
            // Position overlay - start di kiri layar
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = 0; // Start at left edge
            layoutParams.y = 300; // Middle-ish of screen
            
            Logger.d(TAG, "Overlay view initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize overlay view", e);
        }
    }
    
    /**
     * Get current X position
     */
    public int getCurrentX() {
        return layoutParams != null ? layoutParams.x : 0;
    }

    /**
     * Get current Y position  
     */
    public int getCurrentY() {
        return layoutParams != null ? layoutParams.y : 0;
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
            
            Logger.i(TAG, "Overlay shown successfully at position: " + layoutParams.x + ", " + layoutParams.y);
            
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
     * Update overlay position dengan snap to edge (called by OverlayView)
     */
    public void updateOverlayPosition(int x, int y) {
        if (layoutParams != null && isOverlayVisible) {
            // Get screen dimensions
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            // Get overlay dimensions (estimate)
            int overlayWidth = 72; // Icon size in dp
            int overlayHeight = 72;
            
            // Convert dp to pixels
            float density = getResources().getDisplayMetrics().density;
            overlayWidth = (int) (overlayWidth * density);
            overlayHeight = (int) (overlayHeight * density);
            
            // Boundary constraints - keep within screen
            x = Math.max(0, Math.min(x, screenWidth - overlayWidth));
            y = Math.max(0, Math.min(y, screenHeight - overlayHeight));
            
            // Update position immediately (for smooth dragging)
            layoutParams.x = x;
            layoutParams.y = y;
            
            try {
                windowManager.updateViewLayout(overlayView, layoutParams);
                
                // Update OverlayView's position tracking
                if (overlayView != null) {
                    overlayView.updateInitialPosition(x, y);
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Failed to update overlay position", e);
            }
        }
    }
    
    /**
     * Snap overlay to nearest edge with animation
     */
    public void snapToEdge() {
        if (layoutParams == null || !isOverlayVisible) return;
        
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int currentX = layoutParams.x;
        int currentY = layoutParams.y;
        
        // Get overlay width
        float density = getResources().getDisplayMetrics().density;
        int overlayWidth = (int) (72 * density); // Icon size
        
        // Calculate distance to left and right edges
        int distanceToLeft = currentX;
        int distanceToRight = screenWidth - currentX - overlayWidth;
        
        // Snap to nearest horizontal edge
        int targetX = (distanceToLeft < distanceToRight) ? 0 : screenWidth - overlayWidth;
        
        // Keep current Y position (don't snap vertically)
        int targetY = currentY;
        
        Logger.d(TAG, "Snapping to edge - From: " + currentX + "," + currentY + " To: " + targetX + "," + targetY);
        
        // Animate to target position
        animateToPosition(targetX, targetY);
    }
    
    /**
     * Animate overlay to target position smoothly
     */
    private void animateToPosition(int targetX, int targetY) {
        if (layoutParams == null || !isOverlayVisible) return;
        
        int startX = layoutParams.x;
        int startY = layoutParams.y;
        
        // Don't animate if already at target
        if (startX == targetX && startY == targetY) {
            Logger.d(TAG, "Already at target position, skipping animation");
            return;
        }
        
        // Cancel any existing animation
        if (positionAnimator != null && positionAnimator.isRunning()) {
            positionAnimator.cancel();
        }
        
        // Create smooth animation
        positionAnimator = ValueAnimator.ofFloat(0f, 1f);
        positionAnimator.setDuration(300); // 300ms animation
        positionAnimator.setInterpolator(new DecelerateInterpolator());
        
        positionAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            
            // Interpolate position
            int currentX = startX + (int) ((targetX - startX) * progress);
            int currentY = startY + (int) ((targetY - startY) * progress);
            
            // Update layout params
            layoutParams.x = currentX;
            layoutParams.y = currentY;
            
            try {
                windowManager.updateViewLayout(overlayView, layoutParams);
            } catch (Exception e) {
                Logger.e(TAG, "Animation update failed", e);
                positionAnimator.cancel(); // Stop animation on error
            }
        });
        
        positionAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Update OverlayView's position tracking after animation
                if (overlayView != null) {
                    overlayView.updateInitialPosition(targetX, targetY);
                }
                Logger.d(TAG, "Snap animation completed - Final position: " + targetX + ", " + targetY);
            }
            
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                Logger.d(TAG, "Snap animation cancelled");
            }
        });
        
        positionAnimator.start();
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
            .setSmallIcon(R.drawable.ic_notification) // TODO: Create this icon
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_visibility, // TODO: Create this icon
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
     * Force stop animation (untuk emergency cleanup)
     */
    public void stopAnimation() {
        if (positionAnimator != null && positionAnimator.isRunning()) {
            positionAnimator.cancel();
            Logger.d(TAG, "Position animation force stopped");
        }
    }
    
    /**
     * Reset overlay position to default (center left)
     */
    public void resetPosition() {
        if (layoutParams != null) {
            animateToPosition(0, 300); // Left edge, middle of screen
            Logger.d(TAG, "Overlay position reset to default");
        }
    }
    
    /**
     * Get screen dimensions info (untuk debugging)
     */
    public String getScreenInfo() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        
        return "Screen: " + width + "x" + height + ", Density: " + density + 
               ", Current pos: " + getCurrentX() + "," + getCurrentY();
    }
}