package com.victory.poolassistant.overlay;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * Enhanced OverlayView dengan 3-state system (ICON/FULL/SETTINGS)
 * Compatible dengan OverlayManager architecture
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    /**
     * 3-State Overlay System
     */
    public enum OverlayState {
        ICON,       // 64dp floating icon only
        FULL,       // 320dp main controls (toggles + sliders)  
        SETTINGS    // 280dp settings menu
    }
    
    // Touch handling
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private static final int CLICK_THRESHOLD = 10; // pixels
    
    // UI Containers untuk 3-state system
    private ViewGroup iconContainer;        // ICON state
    private ViewGroup fullContainer;        // FULL state  
    private ViewGroup settingsContainer;    // SETTINGS state
    
    // ICON state components
    private ImageButton iconButton;
    
    // FULL state components
    private ImageButton btnClose;
    private ImageButton btnMinimize;
    private ImageButton btnSettings;
    private TextView tvStatus;
    private Switch switchBasicAim;
    private Switch switchRootAim;
    private Switch switchPrediction;
    private SeekBar seekOpacity;
    private SeekBar seekThickness;
    private TextView tvOpacityValue;
    private TextView tvThicknessValue;
    
    // SETTINGS state components
    private ImageButton btnSettingsClose;
    private ImageButton btnThemeToggle;
    private ImageButton btnResetPosition;
    private ImageButton btnExitApp;
    private TextView tvSettingsTitle;
    
    // State management
    private OverlayState currentState = OverlayState.FULL;
    private boolean isInitialized = false;
    
    // Feature states
    private boolean basicAimEnabled = false;
    private boolean rootAimEnabled = false;
    private boolean predictionEnabled = false;
    private int opacityValue = 80;
    private int lineThicknessValue = 5;
    
    // Service reference
    private FloatingOverlayService service;
    
    public OverlayView(Context context) {
        super(context);
        if (context instanceof FloatingOverlayService) {
            this.service = (FloatingOverlayService) context;
        }
        initView();
    }
    
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    
    /**
     * Initialize 3-state overlay view
     */
    private void initView() {
        try {
            // Inflate layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.overlay_floating_window, this, true);
            
            // Find all containers and views
            findViews();
            
            // Setup all click listeners
            setupClickListeners();
            
            // Setup touch handling
            setupTouchHandling();
            
            // Setup seekbars
            setupSeekBars();
            
            // Set initial state
            setState(OverlayState.FULL);
            
            isInitialized = true;
            Logger.d(TAG, "Enhanced OverlayView initialized with 3-state system");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize OverlayView", e);
            // Create minimal fallback UI
            createFallbackUI();
        }
    }
    
    /**
     * Find all UI components for 3-state system
     */
    private void findViews() {
        // Containers
        iconContainer = findViewById(R.id.icon_container);
        fullContainer = findViewById(R.id.full_container); 
        settingsContainer = findViewById(R.id.settings_container);
        
        // Fallback ke alternative IDs jika tidak ditemukan
        if (iconContainer == null) iconContainer = findViewById(R.id.minimized_container);
        if (fullContainer == null) fullContainer = findViewById(R.id.main_container);
        
        // ICON state
        iconButton = findViewById(R.id.icon_button);
        if (iconButton == null) iconButton = findViewById(R.id.btn_expand);
        
        // FULL state
        btnClose = findViewById(R.id.btn_close);
        btnMinimize = findViewById(R.id.btn_minimize);
        btnSettings = findViewById(R.id.btn_settings);
        tvStatus = findViewById(R.id.tv_status);
        
        switchBasicAim = findViewById(R.id.switch_basic_aim);
        switchRootAim = findViewById(R.id.switch_root_aim);
        switchPrediction = findViewById(R.id.switch_prediction);
        
        seekOpacity = findViewById(R.id.seek_opacity);
        seekThickness = findViewById(R.id.seek_thickness);
        tvOpacityValue = findViewById(R.id.tv_opacity_value);
        tvThicknessValue = findViewById(R.id.tv_thickness_value);
        
        // SETTINGS state
        btnSettingsClose = findViewById(R.id.btn_settings_close);
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        btnResetPosition = findViewById(R.id.btn_reset_position);
        btnExitApp = findViewById(R.id.btn_exit_app);
        tvSettingsTitle = findViewById(R.id.tv_settings_title);
    }
    
    /**
     * Setup all click listeners untuk 3-state system
     */
    private void setupClickListeners() {
        // ICON state listeners
        if (iconButton != null) {
            iconButton.setOnClickListener(v -> {
                Logger.d(TAG, "Icon clicked - expanding to FULL");
                setState(OverlayState.FULL);
            });
        }
        
        // FULL state listeners
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                Logger.d(TAG, "Close button clicked");
                if (service != null) {
                    service.hideOverlay();
                }
            });
        }
        
        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> {
                Logger.d(TAG, "Minimize button clicked - switching to ICON");
                setState(OverlayState.ICON);
            });
        }
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Logger.d(TAG, "Settings button clicked - switching to SETTINGS");
                setState(OverlayState.SETTINGS);
            });
        }
        
        // Feature toggles
        if (switchBasicAim != null) {
            switchBasicAim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                basicAimEnabled = isChecked;
                updateStatus("Basic Aim: " + (isChecked ? "ON" : "OFF"));
                Logger.d(TAG, "Basic Aim toggled: " + isChecked);
            });
        }
        
        if (switchRootAim != null) {
            switchRootAim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                rootAimEnabled = isChecked;
                updateStatus("Root Aim: " + (isChecked ? "ON" : "OFF"));
                Logger.d(TAG, "Root Aim toggled: " + isChecked);
            });
        }
        
        if (switchPrediction != null) {
            switchPrediction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                predictionEnabled = isChecked;
                updateStatus("Prediction: " + (isChecked ? "ON" : "OFF"));
                Logger.d(TAG, "Prediction toggled: " + isChecked);
            });
        }
        
        // SETTINGS state listeners
        if (btnSettingsClose != null) {
            btnSettingsClose.setOnClickListener(v -> {
                Logger.d(TAG, "Settings close clicked - returning to FULL");
                setState(OverlayState.FULL);
            });
        }
        
        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> {
                Logger.d(TAG, "Theme toggle clicked");
                updateStatus("Theme toggled!");
                // TODO: Implement theme switching
            });
        }
        
        if (btnResetPosition != null) {
            btnResetPosition.setOnClickListener(v -> {
                Logger.d(TAG, "Reset position clicked");
                if (service != null) {
                    service.resetOverlayPosition();
                    updateStatus("Position reset!");
                }
            });
        }
        
        if (btnExitApp != null) {
            btnExitApp.setOnClickListener(v -> {
                Logger.d(TAG, "Exit app clicked");
                if (service != null) {
                    service.stopSelf();
                }
            });
        }
        
        // Container click handlers
        if (iconContainer != null) {
            iconContainer.setOnClickListener(v -> setState(OverlayState.FULL));
        }
    }
    
    /**
     * Setup seekbars untuk opacity dan thickness
     */
    private void setupSeekBars() {
        if (seekOpacity != null) {
            seekOpacity.setMax(100);
            seekOpacity.setProgress(opacityValue);
            seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        opacityValue = progress;
                        if (tvOpacityValue != null) {
                            tvOpacityValue.setText(progress + "%");
                        }
                        updateStatus("Opacity: " + progress + "%");
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            
            // Set initial value
            if (tvOpacityValue != null) {
                tvOpacityValue.setText(opacityValue + "%");
            }
        }
        
        if (seekThickness != null) {
            seekThickness.setMax(20);
            seekThickness.setProgress(lineThicknessValue);
            seekThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        lineThicknessValue = Math.max(1, progress); // Min 1px
                        if (tvThicknessValue != null) {
                            tvThicknessValue.setText(lineThicknessValue + "px");
                        }
                        updateStatus("Thickness: " + lineThicknessValue + "px");
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            
            // Set initial value
            if (tvThicknessValue != null) {
                tvThicknessValue.setText(lineThicknessValue + "px");
            }
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
     * Set overlay state (3-state system)
     */
    public void setState(OverlayState newState) {
        if (!isInitialized || newState == currentState) return;
        
        OverlayState oldState = currentState;
        currentState = newState;
        
        // Hide all containers first
        hideAllContainers();
        
        // Show appropriate container
        switch (newState) {
            case ICON:
                showIconState();
                break;
            case FULL:
                showFullState();
                break;
            case SETTINGS:
                showSettingsState();
                break;
        }
        
        // Animate transition
        animateStateTransition(oldState, newState);
        
        Logger.d(TAG, "State changed: " + oldState + " → " + newState);
    }
    
    /**
     * Hide all containers
     */
    private void hideAllContainers() {
        if (iconContainer != null) iconContainer.setVisibility(GONE);
        if (fullContainer != null) fullContainer.setVisibility(GONE);
        if (settingsContainer != null) settingsContainer.setVisibility(GONE);
    }
    
    /**
     * Show ICON state
     */
    private void showIconState() {
        if (iconContainer != null) {
            iconContainer.setVisibility(VISIBLE);
        } else if (fullContainer != null) {
            // Fallback: use full container but hide everything except minimize button
            fullContainer.setVisibility(VISIBLE);
            // Hide other elements programmatically
        }
        updateStatus("Pool Assistant");
    }
    
    /**
     * Show FULL state  
     */
    private void showFullState() {
        if (fullContainer != null) {
            fullContainer.setVisibility(VISIBLE);
            updateStatus("Pool Assistant Ready");
        }
    }
    
    /**
     * Show SETTINGS state
     */
    private void showSettingsState() {
        if (settingsContainer != null) {
            settingsContainer.setVisibility(VISIBLE);
        } else {
            // Fallback: use full container with settings layout
            if (fullContainer != null) {
                fullContainer.setVisibility(VISIBLE);
            }
        }
        updateStatus("Settings");
    }
    
    /**
     * Animate state transitions
     */
    private void animateStateTransition(OverlayState from, OverlayState to) {
        float fromScale = getScaleForState(from);
        float toScale = getScaleForState(to);
        
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "scaleX", fromScale, toScale);
        scaleAnimator.setDuration(250);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.start();
        
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", fromScale, toScale);
        scaleYAnimator.setDuration(250);
        scaleYAnimator.setInterpolator(new DecelerateInterpolator());
        scaleYAnimator.start();
    }
    
    /**
     * Get scale factor untuk state
     */
    private float getScaleForState(OverlayState state) {
        switch (state) {
            case ICON: return 0.8f;
            case FULL: return 1.0f;
            case SETTINGS: return 0.95f;
            default: return 1.0f;
        }
    }
    
    /**
     * Create fallback UI jika layout gagal di-load
     */
    private void createFallbackUI() {
        Logger.w(TAG, "Creating fallback UI");
        
        // Create simple text view
        TextView fallbackText = new TextView(getContext());
        fallbackText.setText("Pool Assistant");
        fallbackText.setTextColor(Color.WHITE);
        fallbackText.setBackgroundColor(Color.parseColor("#4CAF50"));
        fallbackText.setPadding(20, 10, 20, 10);
        
        addView(fallbackText);
        
        fallbackText.setOnClickListener(v -> {
            if (service != null) {
                service.hideOverlay();
            }
        });
        
        isInitialized = true;
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
    
    // ===== GETTER METHODS REQUIRED BY OVERLAYMANAGER =====
    
    public OverlayState getState() {
        return currentState;
    }
    
    public boolean isBasicAimEnabled() {
        return basicAimEnabled;
    }
    
    public boolean isRootAimEnabled() {
        return rootAimEnabled;
    }
    
    public boolean isPredictionEnabled() {
        return predictionEnabled;
    }
    
    public int getOpacityValue() {
        return opacityValue;
    }
    
    public int getLineThicknessValue() {
        return lineThicknessValue;
    }
    
    // ===== LEGACY COMPATIBILITY METHODS =====
    
    @Deprecated
    public void setMinimized(boolean minimized) {
        setState(minimized ? OverlayState.ICON : OverlayState.FULL);
    }
    
    @Deprecated
    public boolean isMinimized() {
        return currentState == OverlayState.ICON;
    }
    
    /**
     * Custom drawing untuk trajectory lines (future implementation)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw border based on state
        drawStateBorder(canvas);
        
        // TODO: Draw trajectory lines, aiming guides, etc.
    }
    
    /**
     * Draw border berdasarkan current state
     */
    private void drawStateBorder(Canvas canvas) {
        if (currentState == OverlayState.ICON) return;
        
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        
        // Color based on state
        switch (currentState) {
            case FULL:
                borderPaint.setColor(Color.parseColor("#4CAF50")); // Green
                break;
            case SETTINGS:
                borderPaint.setColor(Color.parseColor("#2196F3")); // Blue
                break;
            default:
                borderPaint.setColor(Color.parseColor("#757575")); // Gray
        }
        
        RectF rect = new RectF(2, 2, getWidth() - 2, getHeight() - 2);
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint);
    }
    
    /**
     * Handle window focus changes
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        
        if (hasWindowFocus && currentState == OverlayState.FULL) {
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
        
        // Clear UI references
        iconContainer = null;
        fullContainer = null;
        settingsContainer = null;
    }
}