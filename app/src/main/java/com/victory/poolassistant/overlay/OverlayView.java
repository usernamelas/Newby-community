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
import android.widget.TextView;
import android.widget.Switch;
import android.widget.SeekBar;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

/**
 * Fixed 3-State Overlay System untuk Pool Assistant
 * States: ICON → FULL → SETTINGS
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // 3 States
    public enum OverlayState {
        ICON,       // Floating icon (64dp)
        FULL,       // Full overlay (320dp) 
        SETTINGS    // Settings menu (280dp)
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
    private View statusIndicator;
    
    // Full state components
    private ImageButton btnSettings;
    private ImageButton btnClose;
    private TextView tvPoolAssistant;
    private Switch switchFiturAim;
    private Switch switchAimRootMode;
    private Switch switchPrediksi;
    private SeekBar seekBarOpacity;
    private SeekBar seekBarKetebalan;
    
    // Settings state components
    private ImageButton btnCloseSettings;
    private ImageButton btnPlus;
    private Switch switchTheme;
    private ImageButton btnReset;
    private ImageButton btnExit;
    
    // Current state
    private OverlayState currentState = OverlayState.ICON;
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
     * Initialize 3-state overlay view
     */
    private void initView() {
        try {
            // Inflate all 3 states
            LayoutInflater inflater = LayoutInflater.from(getContext());
            
            // Create containers
            iconContainer = createIconState();
            fullContainer = createFullState();
            settingsContainer = createSettingsState();
            
            // Add all containers
            addView(iconContainer);
            addView(fullContainer);
            addView(settingsContainer);
            
            // Setup interactions
            setupClickListeners();
            setupTouchHandling();
            
            // Set initial state
            setState(OverlayState.ICON);
            
            isInitialized = true;
            Logger.d(TAG, "3-State OverlayView initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize OverlayView", e);
        }
    }
    
    /**
     * Create Icon State (64dp floating icon)
     */
    private ViewGroup createIconState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (64 * getResources().getDisplayMetrics().density),
            (int) (64 * getResources().getDisplayMetrics().density)
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.setBackgroundResource(R.drawable.overlay_icon_background);
        container.setElevation(8f);
        
        // Pool assistant icon
        iconButton = new ImageButton(getContext());
        LayoutParams iconParams = new LayoutParams(
            (int) (40 * getResources().getDisplayMetrics().density),
            (int) (40 * getResources().getDisplayMetrics().density)
        );
        iconButton.setLayoutParams(iconParams);
        iconButton.setBackgroundResource(R.drawable.overlay_icon_button_background);
        iconButton.setImageResource(R.drawable.ic_pool_ball); // 8-ball icon
        iconButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        container.addView(iconButton);
        
        // Status indicator (green dot)
        statusIndicator = new View(getContext());
        LayoutParams statusParams = new LayoutParams(
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        statusParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
        statusIndicator.setLayoutParams(statusParams);
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
        container.addView(statusIndicator);
        
        return container;
    }
    
    /**
     * Create Full State (320dp full overlay)
     */
    private ViewGroup createFullState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (320 * getResources().getDisplayMetrics().density),
            LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.overlay_full_background);
        container.setElevation(8f);
        container.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        
        // Header with blue gradient
        LinearLayout header = createFullHeader();
        container.addView(header);
        
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
        header.setBackgroundResource(R.drawable.overlay_header_gradient);
        header.setPadding(
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        
        // Settings button (⚙️)
        btnSettings = new ImageButton(getContext());
        LayoutParams settingsParams = new LayoutParams(
            (int) (32 * getResources().getDisplayMetrics().density),
            (int) (32 * getResources().getDisplayMetrics().density)
        );
        btnSettings.setLayoutParams(settingsParams);
        btnSettings.setBackgroundResource(R.drawable.overlay_button_background);
        btnSettings.setImageResource(R.drawable.ic_settings);
        btnSettings.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        header.addView(btnSettings);
        
        // Pool Assistant title with 8-ball icon
        tvPoolAssistant = new TextView(getContext());
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(
            (int) (12 * getResources().getDisplayMetrics().density), 0, 
            (int) (12 * getResources().getDisplayMetrics().density), 0
        );
        tvPoolAssistant.setLayoutParams(titleParams);
        tvPoolAssistant.setText("🎱 Pool Assistant");
        tvPoolAssistant.setTextColor(Color.WHITE);
        tvPoolAssistant.setTextSize(16f);
        tvPoolAssistant.setGravity(android.view.Gravity.CENTER);
        header.addView(tvPoolAssistant);
        
        // Close button (❌)
        btnClose = new ImageButton(getContext());
        LayoutParams closeParams = new LayoutParams(
            (int) (32 * getResources().getDisplayMetrics().density),
            (int) (32 * getResources().getDisplayMetrics().density)
        );
        btnClose.setLayoutParams(closeParams);
        btnClose.setBackgroundResource(R.drawable.overlay_button_background);
        btnClose.setImageResource(R.drawable.ic_close);
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
        section.setPadding(0, 
            (int) (16 * getResources().getDisplayMetrics().density), 0, 
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        
        // Fitur Aim toggle
        switchFiturAim = createToggleSwitch("Fitur Aim", true);
        section.addView(switchFiturAim);
        
        // Aim Root Mode toggle  
        switchAimRootMode = createToggleSwitch("Aim Root Mode", false);
        section.addView(switchAimRootMode);
        
        // Prediksi Bola toggle
        switchPrediksi = createToggleSwitch("Prediksi Bola", true);
        section.addView(switchPrediksi);
        
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
        toggleLayout.setPadding(0, 
            (int) (8 * getResources().getDisplayMetrics().density), 0, 
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        
        // Label
        TextView labelView = new TextView(getContext());
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        toggleLayout.addView(labelView);
        
        // Switch
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
        
        // Opacity slider
        seekBarOpacity = createSlider("opacity", 80);
        section.addView(seekBarOpacity);
        
        // Ketebalan garis slider
        seekBarKetebalan = createSlider("ketebalan garis", 60);
        section.addView(seekBarKetebalan);
        
        return section;
    }
    
    /**
     * Create individual slider
     */
    private LinearLayout createSlider(String label, int defaultValue) {
        LinearLayout sliderLayout = new LinearLayout(getContext());
        sliderLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        sliderLayout.setOrientation(LinearLayout.VERTICAL);
        sliderLayout.setPadding(0, 
            (int) (8 * getResources().getDisplayMetrics().density), 0, 
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        
        // Label background
        TextView labelView = new TextView(getContext());
        labelView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 
            (int) (40 * getResources().getDisplayMetrics().density)
        ));
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        labelView.setGravity(android.view.Gravity.CENTER);
        labelView.setBackgroundResource(R.drawable.overlay_slider_label_background);
        sliderLayout.addView(labelView);
        
        // SeekBar
        SeekBar seekBar = new SeekBar(getContext());
        LayoutParams seekParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        seekParams.setMargins(0, 
            (int) (8 * getResources().getDisplayMetrics().density), 0, 0
        );
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
        container.setBackgroundResource(R.drawable.overlay_settings_background);
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
        header.setBackgroundResource(R.drawable.overlay_settings_header_background);
        header.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        
        // Settings title
        TextView settingsTitle = new TextView(getContext());
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        settingsTitle.setLayoutParams(titleParams);
        settingsTitle.setText("Settings");
        settingsTitle.setTextColor(Color.WHITE);
        settingsTitle.setTextSize(16f);
        settingsTitle.setTextStyle(1); // Bold
        header.addView(settingsTitle);
        
        // Plus button
        btnPlus = new ImageButton(getContext());
        LayoutParams plusParams = new LayoutParams(
            (int) (32 * getResources().getDisplayMetrics().density),
            (int) (32 * getResources().getDisplayMetrics().density)
        );
        plusParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        btnPlus.setLayoutParams(plusParams);
        btnPlus.setBackgroundResource(R.drawable.overlay_button_background);
        btnPlus.setImageResource(R.drawable.ic_add);
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
        options.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        
        // Theme toggle
        LinearLayout themeOption = createSettingsOption("🌙 Terang", false);
        switchTheme = (Switch) ((LinearLayout) themeOption.getChildAt(1)).getChildAt(0);
        options.addView(themeOption);
        
        // Reset position
        LinearLayout resetOption = createSettingsButton("↻ Reset Posisi");
        btnReset = (ImageButton) resetOption.getChildAt(0);
        options.addView(resetOption);
        
        // Exit app
        LinearLayout exitOption = createSettingsButton("🚪 Keluar Aplikasi", true);
        btnExit = (ImageButton) exitOption.getChildAt(0);
        options.addView(exitOption);
        
        return options;
    }
    
    /**
     * Create settings option with toggle
     */
    private LinearLayout createSettingsOption(String label, boolean defaultValue) {
        LinearLayout optionLayout = new LinearLayout(getContext());
        optionLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        optionLayout.setOrientation(LinearLayout.HORIZONTAL);
        optionLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        optionLayout.setPadding(0, 
            (int) (12 * getResources().getDisplayMetrics().density), 0, 
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        
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
     * Create settings button option
     */
    private LinearLayout createSettingsButton(String label) {
        return createSettingsButton(label, false);
    }
    
    private LinearLayout createSettingsButton(String label, boolean isRed) {
        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        buttonLayout.setPadding(0, 
            (int) (12 * getResources().getDisplayMetrics().density), 0, 
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        
        ImageButton button = new ImageButton(getContext());
        button.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 
            (int) (48 * getResources().getDisplayMetrics().density)
        ));
        button.setText(label);
        button.setTextColor(isRed ? Color.parseColor("#FF5252") : Color.WHITE);
        button.setTextSize(14f);
        button.setBackgroundResource(isRed ? 
            R.drawable.overlay_settings_button_red_background : 
            R.drawable.overlay_settings_button_background
        );
        buttonLayout.addView(button);
        
        return buttonLayout;
    }
    
    /**
     * Setup click listeners for all states
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
     * Setup touch handling for dragging
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
                break;
            case FULL:
                fullContainer.setVisibility(VISIBLE);
                animateTransition(fullContainer, 0.8f, 1.0f);
                break;
            case SETTINGS:
                settingsContainer.setVisibility(VISIBLE);
                animateTransition(settingsContainer, 0.8f, 1.0f);
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
     * Reset overlay position to center
     */
    private void resetOverlayPosition() {
        if (service != null) {
            service.updateOverlayPosition(100, 100);
            Logger.d(TAG, "Overlay position reset to (100, 100)");
        }
    }
    
    /**
     * Get current state
     */
    public OverlayState getCurrentState() {
        return currentState;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayView resources");
        setOnTouchListener(null);
        service = null;
    }
}