package com.victory.poolassistant.overlay;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * Custom view untuk floating overlay Pool Assistant
 * Handles UI, touch events, dan basic controls
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // Touch handling
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private static final int CLICK_THRESHOLD = 10; // pixels
    
    // UI components
    private ViewGroup mainContainer;
    private ViewGroup minimizedContainer;
    private ImageButton btnClose;
    private ImageButton btnMinimize;
    private ImageButton btnExpand;
    private ImageButton btnSettings;
    private TextView tvStatus;
    
    // State
    private boolean isMinimized = false;
    private boolean isInitialized = false;
    
    // Service reference
    private FloatingOverlayService service;
    
    public OverlayView(Context context) {
        super(context);
        this.service = (FloatingOverlayService) context;
        initView();
    }
    
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    
    /**
     * Initialize overlay view
     */
    private void initView() {
        try {
            // Inflate layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.overlay_floating_window, this, true);
            
            // Find views
            findViews();
            
            // Setup click listeners
            setupClickListeners();
            
            // Setup touch handling
            setupTouchHandling();
            
            // Initial state
            setMinimized(false);
            
            isInitialized = true;
            Logger.d(TAG, "OverlayView initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize OverlayView", e);
        }
    }
    
    /**
     * Find all UI components
     */
    private void findViews() {
        mainContainer = findViewById(R.id.main_container);
        minimizedContainer = findViewById(R.id.minimized_container);
        
        btnClose = findViewById(R.id.btn_close);
        btnMinimize = findViewById(R.id.btn_minimize);
        btnExpand = findViewById(R.id.btn_expand);
        btnSettings = findViewById(R.id.btn_settings);
        
        tvStatus = findViewById(R.id.tv_status);
    }
    
    /**
     * Setup click listeners untuk semua buttons
     */
    private void setupClickListeners() {
        // Close button
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                Logger.d(TAG, "Close button clicked");
                if (service != null) {
                    service.hideOverlay();
                }
            });
        }
        
        // Minimize button
        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> {
                Logger.d(TAG, "Minimize button clicked");
                setMinimized(true);
            });
        }
        
        // Expand button (pada minimized state)
        if (btnExpand != null) {
            btnExpand.setOnClickListener(v -> {
                Logger.d(TAG, "Expand button clicked");
                setMinimized(false);
            });
        }
        
        // Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Logger.d(TAG, "Settings button clicked");
                // TODO: Open settings atau show settings popup
                updateStatus("Settings clicked!");
            });
        }
        
        // Double tap pada minimized container untuk expand
        if (minimizedContainer != null) {
            minimizedContainer.setOnClickListener(v -> {
                setMinimized(false);
            });
        }
    }
    
    /**
     * Setup touch handling untuk dragging
     */
    private void setupTouchHandling() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouch(event);
            }
        });
    }
    
    /**
     * Handle touch events untuk dragging
     */
    private boolean handleTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = event.getRawX();
                initialY = event.getRawY();
                initialTouchX = event.getX();
                initialTouchY = event.getY();
                isDragging = false;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - initialX;
                float deltaY = event.getRawY() - initialY;
                
                // Check if this is a drag gesture
                if (!isDragging && (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD)) {
                    isDragging = true;
                }
                
                if (isDragging && service != null) {
                    // Update overlay position
                    int newX = (int) (event.getRawX() - initialTouchX);
                    int newY = (int) (event.getRawY() - initialTouchY);
                    
                    service.updateOverlayPosition(newX, newY);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    // This was a click, not a drag
                    performClick();
                }
                isDragging = false;
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Set minimized state
     */
    public void setMinimized(boolean minimized) {
        if (!isInitialized) return;
        
        isMinimized = minimized;
        
        if (mainContainer != null && minimizedContainer != null) {
            if (minimized) {
                // Show minimized view
                mainContainer.setVisibility(GONE);
                minimizedContainer.setVisibility(VISIBLE);
                
                // Animate scale down
                ObjectAnimator scaleDown = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 0.8f);
                scaleDown.setDuration(200);
                scaleDown.setInterpolator(new DecelerateInterpolator());
                scaleDown.start();
                
                Logger.d(TAG, "Overlay minimized");
            } else {
                // Show full view
                mainContainer.setVisibility(VISIBLE);
                minimizedContainer.setVisibility(GONE);
                
                // Animate scale up
                ObjectAnimator scaleUp = ObjectAnimator.ofFloat(this, "scaleX", 0.8f, 1.0f);
                scaleUp.setDuration(200);
                scaleUp.setInterpolator(new DecelerateInterpolator());
                scaleUp.start();
                
                Logger.d(TAG, "Overlay expanded");
            }
        }
    }
    
    /**
     * Update status text
     */
    public void updateStatus(String status) {
        if (tvStatus != null) {
            post(() -> {
                tvStatus.setText(status);
                Logger.d(TAG, "Status updated: " + status);
            });
        }
    }
    
    /**
     * Get current minimized state
     */
    public boolean isMinimized() {
        return isMinimized;
    }
    
    /**
     * Custom drawing untuk trajectory lines (future)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // TODO: Draw trajectory lines, aiming guides, etc.
        // For now, just draw a subtle border
        drawBorder(canvas);
    }
    
    /**
     * Draw border untuk overlay window
     */
    private void drawBorder(Canvas canvas) {
        if (isMinimized) return;
        
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#4CAF50")); // Pool Assistant green
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setAntiAlias(true);
        
        RectF rect = new RectF(2, 2, getWidth() - 2, getHeight() - 2);
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint);
    }
    
    /**
     * Handle window focus changes
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        
        if (hasWindowFocus) {
            updateStatus("Pool Assistant Ready");
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayView resources");
        
        // Remove listeners
        setOnTouchListener(null);
        
        // Clear references
        service = null;
    }
}