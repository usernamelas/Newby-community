package com.victory.poolassistant.overlay;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.Button;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * OverlayView Versi Canggih - 3-State System
 * States: ICON (72dp) → FULL (320dp) → SETTINGS (280dp)
 * Compatible dengan OverlayManager + fixes semua compile errors
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // 3-State System (seperti Easy Victory)
    public enum OverlayState {
        ICON,       // Floating icon 72dp - clean tanpa warna biru/hijau
        FULL,       // Full overlay 320dp dengan toggles + sliders
        SETTINGS    // Settings menu 280dp dengan options
    }
    
    // Touch handling
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private static final int CLICK_THRESHOLD = 10;
    
    // State containers
    private ViewGroup iconContainer;
    private ViewGroup fullContainer;
    private ViewGroup settingsContainer;
    
    // Icon state components
    private ImageButton iconButton;
    
    // Full state components
    private ImageButton btnSettings;
    private ImageButton btnClose;
    private TextView tvPoolAssistant;
    private Switch switchFiturAim;
    private Switch switchAimRootMode;
    private Switch switchPrediksi;
    private SeekBar seekBarOpacity;
    private SeekBar seekBarKetebalan;
    private TextView tvStatus;
    
    // Settings state components
    private ImageButton btnPlus;
    private Switch switchTheme;
    private Button btnReset;
    private Button btnExit;
    
    // Current state
    private OverlayState currentState = OverlayState.ICON;
    private boolean isInitialized = false;
    
    // COMPATIBILITY: Old system properties untuk backward compatibility
    private boolean isMinimized = false;
    private ViewGroup mainContainer;
    private ViewGroup minimizedContainer;
    private ImageButton btnMinimize;
    private ImageButton btnExpand;
    
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
     * Initialize 3-state overlay system
     */
    private void initView() {
        try {
            // Create all 3 state containers
            iconContainer = createIconState();
            fullContainer = createFullState();
            settingsContainer = createSettingsState();
            
            // Add containers to layout
            addView(iconContainer);
            addView(fullContainer);
            addView(settingsContainer);
            
            // Setup interactions
            setupClickListeners();
            setupTouchHandling();
            
            // Set initial state
            setState(OverlayState.ICON);
            
            // COMPATIBILITY: Setup backward compatibility references
            setupBackwardCompatibility();
            
            isInitialized = true;
            Logger.d(TAG, "3-State OverlayView initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize OverlayView", e);
        }
    }
    
    /**
     * FIXED: Create Icon State (72dp - clean seperti app icon)
     */
    private ViewGroup createIconState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (72 * getResources().getDisplayMetrics().density), // FIXED: 72dp
            (int) (72 * getResources().getDisplayMetrics().density)
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        
        // FIXED: No blue background - clean like app icon
        container.setBackgroundResource(android.R.drawable.btn_default); // Fallback system background
        container.setElevation(8f);
        
        // App icon button
        iconButton = new ImageButton(getContext());
        LayoutParams iconParams = new LayoutParams(
            (int) (64 * getResources().getDisplayMetrics().density), // FIXED: 64dp proportional
            (int) (64 * getResources().getDisplayMetrics().density)
        );
        iconButton.setLayoutParams(iconParams);
        
        // FIXED: Clean transparent background + app icon
        iconButton.setBackground(null);
        
        // Use app icon - fallback to system icon if custom not found
        try {
            iconButton.setImageResource(R.drawable.ic_pool_assistant_icon);
        } catch (Exception e) {
            // Fallback to system icon
            iconButton.setImageResource(android.R.drawable.ic_dialog_info);
            Logger.w(TAG, "Using fallback icon - add ic_pool_assistant_icon.png to drawable/");
        }
        
        iconButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        iconButton.setElevation(4f);
        container.addView(iconButton);
        
        // REMOVED: No green status indicator - cleaner look
        
        return container;
    }
    
    /**
     * Create Full State (320dp dengan toggles + sliders)
     */
    private ViewGroup createFullState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (320 * getResources().getDisplayMetrics().density),
            LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        
        // Use fallback background if custom not found
        try {
            container.setBackgroundResource(R.drawable.overlay_full_background);
        } catch (Exception e) {
            container.setBackgroundColor(Color.parseColor("#2A2A2A")); // Dark fallback
        }
        
        container.setElevation(8f);
        container.setPadding(16, 16, 16, 16);
        
        // Header
        LinearLayout header = createFullHeader();
        container.addView(header);
        
        // Status text
        tvStatus = new TextView(getContext());
        tvStatus.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tvStatus.setText("Pool Assistant Ready");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(14f);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        tvStatus.setPadding(0, 16, 0, 16);
        container.addView(tvStatus);
        
        // Toggle switches
        LinearLayout toggleSection = createToggleSection();
        container.addView(toggleSection);
        
        // Sliders
        LinearLayout sliderSection = createSliderSection();
        container.addView(sliderSection);
        
        return container;
    }
    
    /**
     * Create Full State Header
     */
    private LinearLayout createFullHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Use fallback background if custom not found
        try {
            header.setBackgroundResource(R.drawable.overlay_header_gradient);
        } catch (Exception e) {
            header.setBackgroundColor(Color.parseColor("#1976D2")); // Blue fallback
        }
        
        header.setPadding(12, 12, 12, 12);
        
        // Settings button (⚙️)
        btnSettings = new ImageButton(getContext());
        LayoutParams settingsParams = new LayoutParams(32, 32);
        btnSettings.setLayoutParams(settingsParams);
        
        try {
            btnSettings.setBackgroundResource(R.drawable.overlay_button_background);
        } catch (Exception e) {
            btnSettings.setBackgroundResource(android.R.drawable.btn_default);
        }
        
        try {
            btnSettings.setImageResource(R.drawable.ic_settings);
        } catch (Exception e) {
            btnSettings.setImageResource(android.R.drawable.ic_menu_preferences);
        }
        
        btnSettings.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        header.addView(btnSettings);
        
        // Pool Assistant title
        tvPoolAssistant = new TextView(getContext());
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(12, 0, 12, 0);
        tvPoolAssistant.setLayoutParams(titleParams);
        tvPoolAssistant.setText("🎱 Pool Assistant");
        tvPoolAssistant.setTextColor(Color.WHITE);
        tvPoolAssistant.setTextSize(16f);
        tvPoolAssistant.setGravity(android.view.Gravity.CENTER);
        header.addView(tvPoolAssistant);
        
        // Close button (❌)
        btnClose = new ImageButton(getContext());
        LayoutParams closeParams = new LayoutParams(32, 32);
        btnClose.setLayoutParams(closeParams);
        
        try {
            btnClose.setBackgroundResource(R.drawable.overlay_button_background);
        } catch (Exception e) {
            btnClose.setBackgroundResource(android.R.drawable.btn_default);
        }
        
        try {
            btnClose.setImageResource(R.drawable.ic_close);
        } catch (Exception e) {
            btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        
        btnClose.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        header.addView(btnClose);
        
        return header;
    }
    
    /**
     * Create Toggle Switches Section
     */
    private LinearLayout createToggleSection() {
        LinearLayout section = new LinearLayout(getContext());
        section.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, 16, 0, 16);
        
        // Create toggles
        LinearLayout aimLayout = createToggleSwitch("Fitur Aim", true);
        switchFiturAim = (Switch) aimLayout.getChildAt(1);
        section.addView(aimLayout);
        
        LinearLayout rootLayout = createToggleSwitch("Aim Root Mode", false);
        switchAimRootMode = (Switch) rootLayout.getChildAt(1);
        section.addView(rootLayout);
        
        LinearLayout prediksiLayout = createToggleSwitch("Prediksi Bola", true);
        switchPrediksi = (Switch) prediksiLayout.getChildAt(1);
        section.addView(prediksiLayout);
        
        return section;
    }
    
    /**
     * Create individual toggle switch
     */
    private LinearLayout createToggleSwitch(String label, boolean defaultValue) {
        LinearLayout toggleLayout = new LinearLayout(getContext());
        toggleLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        toggleLayout.setOrientation(LinearLayout.HORIZONTAL);
        toggleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        toggleLayout.setPadding(0, 8, 0, 8);
        
        TextView labelView = new TextView(getContext());
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        toggleLayout.addView(labelView);
        
        Switch switchView = new Switch(getContext());
        switchView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        switchView.setChecked(defaultValue);
        toggleLayout.addView(switchView);
        
        return toggleLayout;
    }
    
    /**
     * Create Sliders Section
     */
    private LinearLayout createSliderSection() {
        LinearLayout section = new LinearLayout(getContext());
        section.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout opacityLayout = createSlider("opacity", 80);
        seekBarOpacity = (SeekBar) opacityLayout.getChildAt(1);
        section.addView(opacityLayout);
        
        LinearLayout ketebalanLayout = createSlider("ketebalan garis", 60);
        seekBarKetebalan = (SeekBar) ketebalanLayout.getChildAt(1);
        section.addView(ketebalanLayout);
        
        return section;
    }
    
    /**
     * Create individual slider
     */
    private LinearLayout createSlider(String label, int defaultValue) {
        LinearLayout sliderLayout = new LinearLayout(getContext());
        sliderLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        sliderLayout.setOrientation(LinearLayout.VERTICAL);
        sliderLayout.setPadding(0, 8, 0, 8);
        
        TextView labelView = new TextView(getContext());
        labelView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 40));
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        labelView.setGravity(android.view.Gravity.CENTER);
        
        try {
            labelView.setBackgroundResource(R.drawable.overlay_slider_label_background);
        } catch (Exception e) {
            labelView.setBackgroundColor(Color.parseColor("#42A5F5"));
        }
        
        sliderLayout.addView(labelView);
        
        SeekBar seekBar = new SeekBar(getContext());
        LayoutParams seekParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        seekParams.setMargins(0, 8, 0, 0);
        seekBar.setLayoutParams(seekParams);
        seekBar.setMax(100);
        seekBar.setProgress(defaultValue);
        sliderLayout.addView(seekBar);
        
        return sliderLayout;
    }
    
    /**
     * Create Settings State (280dp settings menu)
     */
    private ViewGroup createSettingsState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (280 * getResources().getDisplayMetrics().density),
            LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        
        try {
            container.setBackgroundResource(R.drawable.overlay_settings_background);
        } catch (Exception e) {
            container.setBackgroundColor(Color.parseColor("#2A2A2A"));
        }
        
        container.setElevation(8f);
        
        // Settings header
        LinearLayout settingsHeader = createSettingsHeader();
        container.addView(settingsHeader);
        
        // Settings options
        LinearLayout settingsOptions = createSettingsOptions();
        container.addView(settingsOptions);
        
        return container;
    }
    
    /**
     * Create Settings Header
     */
    private LinearLayout createSettingsHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        try {
            header.setBackgroundResource(R.drawable.overlay_settings_header_background);
        } catch (Exception e) {
            header.setBackgroundColor(Color.parseColor("#FF9800"));
        }
        
        header.setPadding(16, 12, 16, 12);
        
        TextView settingsTitle = new TextView(getContext());
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        settingsTitle.setLayoutParams(titleParams);
        settingsTitle.setText("Settings");
        settingsTitle.setTextColor(Color.WHITE);
        settingsTitle.setTextSize(16f);
        settingsTitle.setTypeface(settingsTitle.getTypeface(), Typeface.BOLD);
        header.addView(settingsTitle);
        
        // Plus button (back to full)
        btnPlus = new ImageButton(getContext());
        LayoutParams plusParams = new LayoutParams(32, 32);
        plusParams.setMargins(8, 0, 0, 0);
        btnPlus.setLayoutParams(plusParams);
        
        try {
            btnPlus.setBackgroundResource(R.drawable.overlay_button_background);
        } catch (Exception e) {
            btnPlus.setBackgroundResource(android.R.drawable.btn_default);
        }
        
        try {
            btnPlus.setImageResource(R.drawable.ic_add);
        } catch (Exception e) {
            btnPlus.setImageResource(android.R.drawable.ic_input_add);
        }
        
        btnPlus.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        header.addView(btnPlus);
        
        return header;
    }
    
    /**
     * Create Settings Options
     */
    private LinearLayout createSettingsOptions() {
        LinearLayout options = new LinearLayout(getContext());
        options.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(16, 16, 16, 16);
        
        // Theme toggle
        LinearLayout themeOption = createSettingsOption("🌙 Tema Gelap", false);
        switchTheme = (Switch) ((LinearLayout) themeOption.getChildAt(1)).getChildAt(0);
        options.addView(themeOption);
        
        // Reset position
        btnReset = createSettingsButton("↻ Reset Posisi", false);
        options.addView(btnReset);
        
        // Exit app
        btnExit = createSettingsButton("🚪 Keluar Aplikasi", true);
        options.addView(btnExit);
        
        return options;
    }
    
    /**
     * Create settings option dengan toggle
     */
    private LinearLayout createSettingsOption(String label, boolean defaultValue) {
        LinearLayout optionLayout = new LinearLayout(getContext());
        optionLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        optionLayout.setOrientation(LinearLayout.HORIZONTAL);
        optionLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        optionLayout.setPadding(0, 12, 0, 12);
        
        TextView labelView = new TextView(getContext());
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        optionLayout.addView(labelView);
        
        LinearLayout switchContainer = new LinearLayout(getContext());
        switchContainer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        Switch switchView = new Switch(getContext());
        switchView.setChecked(defaultValue);
        switchContainer.addView(switchView);
        optionLayout.addView(switchContainer);
        
        return optionLayout;
    }
    
    /**
     * Create settings button
     */
    private Button createSettingsButton(String label, boolean isRed) {
        Button button = new Button(getContext());
        LayoutParams buttonParams = new LayoutParams(LayoutParams.MATCH_PARENT, 48);
        buttonParams.setMargins(0, 12, 0, 12);
        button.setLayoutParams(buttonParams);
        button.setText(label);
        button.setTextColor(isRed ? Color.parseColor("#FF5252") : Color.WHITE);
        button.setTextSize(14f);
        
        // FIXED: Use fallback backgrounds instead of missing drawables
        try {
            button.setBackgroundResource(isRed ? 
                R.drawable.overlay_settings_button_red_background : 
                R.drawable.overlay_settings_button_background
            );
        } catch (Exception e) {
            // Fallback to system button background
            button.setBackgroundResource(android.R.drawable.btn_default);
            if (isRed) {
                button.setBackgroundColor(Color.parseColor("#FF5252"));
            }
        }
        
        return button;
    }
    
    /**
     * Setup click listeners untuk semua states
     */
    private void setupClickListeners() {
        // Icon state - Click to expand to Full
        if (iconButton != null) {
            iconButton.setOnClickListener(v -> {
                Logger.d(TAG, "Icon clicked - expanding to Full state");
                setState(OverlayState.FULL);
            });
        }
        
        // Full state - Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Logger.d(TAG, "Settings button clicked - switching to Settings state");
                setState(OverlayState.SETTINGS);
            });
        }
        
        // Full state - Close button
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                Logger.d(TAG, "Close button clicked - minimizing to Icon state");
                setState(OverlayState.ICON);
            });
        }
        
        // Settings state - Plus button (back to Full)
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                Logger.d(TAG, "Plus button clicked - back to Full state");
                setState(OverlayState.FULL);
            });
        }
        
        // Settings state - Reset position
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                Logger.d(TAG, "Reset position clicked");
                resetOverlayPosition();
            });
        }
        
        // Settings state - Exit app
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                Logger.d(TAG, "Exit app clicked");
                if (service != null) {
                    service.hideOverlay();
                    service.stopSelf();
                }
            });
        }
    }
    
    /**
     * Setup touch handling untuk dragging
     */
    private void setupTouchHandling() {
        setOnTouchListener((v, event) -> {
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
                    
                    if (!isDragging && (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD)) {
                        isDragging = true;
                    }
                    
                    if (isDragging && service != null) {
                        int newX = (int) (event.getRawX() - initialTouchX);
                        int newY = (int) (event.getRawY() - initialTouchY);
                        service.updateOverlayPosition(newX, newY);
                    }
                    return true;
                    
                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    return true;
                    
                default:
                    return false;
            }
        });
    }
    
    /**
     * Set overlay state dengan smooth transition
     */
    public void setState(OverlayState newState) {
        if (!isInitialized || newState == currentState) return;
        
        Logger.d(TAG, "Transitioning from " + currentState + " to " + newState);
        
        // Hide all containers first
        iconContainer.setVisibility(GONE);
        fullContainer.setVisibility(GONE);
        settingsContainer.setVisibility(GONE);
        
        // Show target container
        switch (newState) {
            case ICON:
                iconContainer.setVisibility(VISIBLE);
                animateTransition(iconContainer, 0.6f, 1.0f);
                isMinimized = true; // Compatibility
                break;
            case FULL:
                fullContainer.setVisibility(VISIBLE);
                animateTransition(fullContainer, 0.8f, 1.0f);
                isMinimized = false; // Compatibility
                break;
            case SETTINGS:
                settingsContainer.setVisibility(VISIBLE);
                animateTransition(settingsContainer, 0.8f, 1.0f);
                isMinimized = false; // Compatibility
                break;
        }
        
        currentState = newState;
        Logger.d(TAG, "State changed to: " + currentState);
    }
    
    /**
     * Animate state transition
     */
    private void animateTransition(View targetView, float fromScale, float toScale) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(targetView, "scaleX", fromScale, toScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(targetView, "scaleY", fromScale, toScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(targetView, "alpha", 0.7f, 1.0f);
        
        scaleX.setDuration(250);
        scaleY.setDuration(250);
        alpha.setDuration(250);
        
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new DecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
        alpha.start();
    }
    
    /**
     * Reset overlay position
     */
    private void resetOverlayPosition() {
        if (service != null) {
            service.updateOverlayPosition(100, 100);
            Logger.d(TAG, "Overlay position reset to (100, 100)");
        }
    }
    
    /**
     * COMPATIBILITY: Setup backward compatibility dengan old system
     */
    private void setupBackwardCompatibility() {
        // Map new containers to old names untuk compatibility
        mainContainer = fullContainer;
        minimizedContainer = iconContainer;
        btnMinimize = btnClose; // Close button acts as minimize
        btnExpand = iconButton; // Icon button acts as expand
    }
    
    // =============================================================================
    // PUBLIC API METHODS (Required by OverlayManager dan backward compatibility)
    // =============================================================================
    
    /**
     * Get current state
     */
    public OverlayState getCurrentState() {
        return currentState;
    }
    
    /**
     * COMPATIBILITY: Old minimize/expand system
     */
    public void setMinimized(boolean minimized) {
        setState(minimized ? OverlayState.ICON : OverlayState.FULL);
        isMinimized = minimized;
    }
    
    public boolean isMinimized() {
        return currentState == OverlayState.ICON;
    }
    
    /**
     * REQUIRED BY OverlayManager: Feature state methods
     */
    public boolean isBasicAimEnabled() {
        return switchFiturAim != null ? switchFiturAim.isChecked() : false;
    }
    
    public boolean isRootAimEnabled() {
        return switchAimRootMode != null ? switchAimRootMode.isChecked() : false;
    }
    
    public boolean isPredictionEnabled() {
        return switchPrediksi != null ? switchPrediksi.isChecked() : false;
    }
    
    public int getOpacityValue() {
        return seekBarOpacity != null ? seekBarOpacity.getProgress() : 80;
    }
    
    public int getLineThicknessValue() {
        return seekBarKetebalan != null ? seekBarKetebalan.getProgress() : 5;
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
     * Custom drawing (future trajectory lines)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentState != OverlayState.ICON) {
            drawBorder(canvas);
        }
    }
    
    /**
     * Draw border untuk overlay window
     */
    private void drawBorder(Canvas canvas) {
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#4CAF50"));
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