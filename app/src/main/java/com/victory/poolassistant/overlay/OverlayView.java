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
 * Enhanced OverlayView dengan 3-State System + Draggable Icon
 * FIXED: Icon 72dp yang bisa drag + overlay always visible
 * States: ICON (draggable) → FULL → SETTINGS
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // 3-State System Enhanced
    public enum OverlayState {
        ICON,       // 72dp draggable icon - clean tanpa biru/hijau
        FULL,       // 320dp full overlay dengan toggles + sliders
        SETTINGS    // 280dp settings menu
    }
    
    // Enhanced touch handling
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isClickPending = false;
    private static final int CLICK_THRESHOLD = 15; // Increased for better detection
    private static final long CLICK_TIMEOUT = 200; // ms
    
    // State containers
    private ViewGroup iconContainer;
    private ViewGroup fullContainer;
    private ViewGroup settingsContainer;
    
    // Icon state components (draggable)
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
    
    // COMPATIBILITY: Backward compatibility properties
    private boolean isMinimized = false;
    private ViewGroup mainContainer;
    private ViewGroup minimizedContainer;
    private ImageButton btnMinimize;
    private ImageButton btnExpand;
    private ImageButton btnSettings_compat;
    
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
     * ENHANCED: Initialize 3-state overlay dengan draggable icon
     */
    private void initView() {
        try {
            // Create all 3 state containers
            iconContainer = createDraggableIconState(); // ENHANCED: Draggable
            fullContainer = createFullState();
            settingsContainer = createSettingsState();
            
            // Add containers
            addView(iconContainer);
            addView(fullContainer);
            addView(settingsContainer);
            setBackgroundColor(Color.TRANSPARENT); // FIX #2: Ensure root container is transparent
            
            // Setup enhanced interactions
            setupEnhancedClickListeners();
            setupEnhancedTouchHandling(); // ENHANCED: Better touch handling
            
            // Set initial state
            setState(OverlayState.ICON);
            
            // Setup backward compatibility
            setupBackwardCompatibility();
            
            isInitialized = true;
            Logger.d(TAG, "Enhanced 3-State OverlayView initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize enhanced OverlayView", e);
        }
    }
    
    /**
     * ENHANCED: Create draggable 72dp icon state
     */
    private ViewGroup createDraggableIconState() {
        LinearLayout container = new LinearLayout(getContext());
        
        // ENHANCED: 72dp size as requested
        iint iconSize = (int) (72 * getResources().getDisplayMetrics().density); // Keep size but ensure proper layout
        LayoutParams iconContainerParams = new LayoutParams(iconSize, iconSize);
        iconContainerParams.gravity = android.view.Gravity.CENTER; // FIX #3: Proper centering
        container.setLayoutParams(iconContainerParams);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        
        // ENHANCED: Clean appearance - no blue background
        container.setBackground(null); // FIX #2: Remove white background
        container.setElevation(12f); // Higher elevation untuk always-on-top
        
        // ENHANCED: App icon button - draggable
        iconButton = new ImageButton(getContext());
        int buttonSize = (int) (64 * getResources().getDisplayMetrics().density);
        LayoutParams iconParams = new LayoutParams(buttonSize, buttonSize);
        iconButton.setLayoutParams(iconParams);
        
        // ENHANCED: Clean transparent background
        iconButton.setBackground(null); // Already correct, keep this
        iconButton.setElevation(4f);
        
        // Use app icon with fallback
        try {
            iconButton.setImageResource(R.drawable.ic_pool_assistant_icon);
        } catch (Exception e) {
            // Fallback to launcher icon if available
            try {
                iconButton.setImageResource(R.mipmap.ic_launcher);
            } catch (Exception e2) {
                // Ultimate fallback to system icon
                iconButton.setImageResource(android.R.drawable.ic_dialog_info);
                Logger.w(TAG, "Using fallback icon - add ic_pool_assistant_icon.png to drawable/");
            }
        }
        
        iconButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        container.addView(iconButton);
        
        // ENHANCED: Make container itself draggable
        container.setClickable(true);
        container.setFocusable(true);
        
        return container;
    }
    
    /**
     * Create Full State (320dp)
     */
    private ViewGroup createFullState() {
        LinearLayout container = new LinearLayout(getContext());
        float density = getResources().getDisplayMetrics().density;
        int fullWidth = (int) (320 * density);
        int fullHeight = (int) (200 * density); // FIX #3: Set proper height instead of WRAP_CONTENT
        LayoutParams fullParams = new LayoutParams(fullWidth, fullHeight);
        container.setLayoutParams(fullParams);
        container.setOrientation(LinearLayout.VERTICAL);
        
        // Background with fallback
        try {
            container.setBackgroundResource(R.drawable.overlay_full_background);
        } catch (Exception e) {
            container.setBackgroundColor(Color.parseColor("#2A2A2A"));
        }
        
        container.setElevation(8f);
        container.setPadding(16, 16, 16, 16);
        
        // Header
        LinearLayout header = createFullHeader();
        container.addView(header);
        
        // Status
        tvStatus = new TextView(getContext());
        tvStatus.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tvStatus.setText("Pool Assistant Ready");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(14f);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        tvStatus.setPadding(0, 16, 0, 16);
        container.addView(tvStatus);
        
        // Toggles section
        LinearLayout toggleSection = createToggleSection();
        container.addView(toggleSection);
        
        // Sliders section
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
        
        // Header background
        try {
            header.setBackgroundResource(R.drawable.overlay_header_gradient);
        } catch (Exception e) {
            header.setBackgroundColor(Color.parseColor("#1976D2"));
        }
        header.setPadding(12, 12, 12, 12);
        
        // Settings button
        btnSettings = new ImageButton(getContext());
        LayoutParams settingsParams = new LayoutParams(32, 32);
        btnSettings.setLayoutParams(settingsParams);
        
        try {
            btnSettings.setBackgroundResource(R.drawable.overlay_button_background);
            btnSettings.setImageResource(R.drawable.ic_settings);
        } catch (Exception e) {
            btnSettings.setBackgroundResource(android.R.drawable.btn_default);
            btnSettings.setImageResource(android.R.drawable.ic_menu_preferences);
        }
        btnSettings.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        header.addView(btnSettings);
        
        // Title
        tvPoolAssistant = new TextView(getContext());
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(12, 0, 12, 0);
        tvPoolAssistant.setLayoutParams(titleParams);
        tvPoolAssistant.setText("🎱 Pool Assistant");
        tvPoolAssistant.setTextColor(Color.WHITE);
        tvPoolAssistant.setTextSize(16f);
        tvPoolAssistant.setGravity(android.view.Gravity.CENTER);
        header.addView(tvPoolAssistant);
        
        // Close button
        btnClose = new ImageButton(getContext());
        LayoutParams closeParams = new LayoutParams(32, 32);
        btnClose.setLayoutParams(closeParams);
        
        try {
            btnClose.setBackgroundResource(R.drawable.overlay_button_background);
            btnClose.setImageResource(R.drawable.ic_close);
        } catch (Exception e) {
            btnClose.setBackgroundResource(android.R.drawable.btn_default);
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
        
        // Create toggles and extract switches
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
        LinearLayout layout = new LinearLayout(getContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(0, 8, 0, 8);
        
        TextView labelView = new TextView(getContext());
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        layout.addView(labelView);
        
        Switch switchView = new Switch(getContext());
        switchView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        switchView.setChecked(defaultValue);
        layout.addView(switchView);
        
        return layout;
    }
    
    /**
     * Create Sliders Section
     */
    private LinearLayout createSliderSection() {
        LinearLayout section = new LinearLayout(getContext());
        section.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.setOrientation(LinearLayout.VERTICAL);
        
        // Opacity slider
        LinearLayout opacityLayout = createSlider("opacity", 80);
        seekBarOpacity = (SeekBar) opacityLayout.getChildAt(1);
        section.addView(opacityLayout);
        
        // Thickness slider
        LinearLayout ketebalanLayout = createSlider("ketebalan garis", 60);
        seekBarKetebalan = (SeekBar) ketebalanLayout.getChildAt(1);
        section.addView(ketebalanLayout);
        
        return section;
    }
    
    /**
     * Create individual slider
     */
    private LinearLayout createSlider(String label, int defaultValue) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 8, 0, 8);
        
        // Label
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
        layout.addView(labelView);
        
        // SeekBar
        SeekBar seekBar = new SeekBar(getContext());
        LayoutParams seekParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        seekParams.setMargins(0, 8, 0, 0);
        seekBar.setLayoutParams(seekParams);
        seekBar.setMax(100);
        seekBar.setProgress(defaultValue);
        layout.addView(seekBar);
        
        return layout;
    }
    
    /**
     * Create Settings State (280dp)
     */
    private ViewGroup createSettingsState() {
        LinearLayout container = new LinearLayout(getContext());
        float density = getResources().getDisplayMetrics().density;
        int settingsWidth = (int) (280 * density);
        int settingsHeight = (int) (180 * density); // FIX #3: Set proper height
        LayoutParams settingsParams = new LayoutParams(settingsWidth, settingsHeight);
        container.setLayoutParams(settingsParams);
        
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
        
        // Title
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
            btnPlus.setImageResource(R.drawable.ic_add);
        } catch (Exception e) {
            btnPlus.setBackgroundResource(android.R.drawable.btn_default);
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
        LinearLayout layout = new LinearLayout(getContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(0, 12, 0, 12);
        
        TextView labelView = new TextView(getContext());
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(14f);
        layout.addView(labelView);
        
        LinearLayout switchContainer = new LinearLayout(getContext());
        switchContainer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        Switch switchView = new Switch(getContext());
        switchView.setChecked(defaultValue);
        switchContainer.addView(switchView);
        layout.addView(switchContainer);
        
        return layout;
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
        
        try {
            button.setBackgroundResource(isRed ? 
                R.drawable.overlay_settings_button_red_background : 
                R.drawable.overlay_settings_button_background
            );
        } catch (Exception e) {
            button.setBackgroundResource(android.R.drawable.btn_default);
            if (isRed) {
                button.setBackgroundColor(Color.parseColor("#FF5252"));
            }
        }
        
        return button;
    }
    
    /**
     * ENHANCED: Setup click listeners dengan proper state transitions
     */
    private void setupEnhancedClickListeners() {
        // Icon state - Click to expand to Full (only when not dragging)
        if (iconButton != null) {
            iconButton.setOnClickListener(v -> {
                if (!isDragging) {
                    Logger.d(TAG, "Icon clicked - expanding to Full state");
                    setState(OverlayState.FULL);
                }
            });
        }
        
        // Also make container clickable for larger touch area
        if (iconContainer != null) {
            iconContainer.setOnClickListener(v -> {
                if (!isDragging) {
                    Logger.d(TAG, "Icon container clicked - expanding to Full state");
                    setState(OverlayState.FULL);
                }
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
     * ENHANCED: Touch handling dengan better drag detection untuk icon
     */
    private void setupEnhancedTouchHandling() {
        setOnTouchListener((v, event) -> {
            // Only handle touch in ICON state for dragging
            if (currentState == OverlayState.ICON) {
                return handleIconDragTouch(event);
            } else {
                // For FULL/SETTINGS states, allow limited dragging via header
                return handleContainerTouch(event);
            }
        });
    }
    
    /**
     * ENHANCED: Handle touch events untuk icon drag
     */
    private boolean handleIconDragTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = event.getRawX();
                initialY = event.getRawY();
                initialTouchX = event.getX();
                initialTouchY = event.getY();
                isDragging = false;
                isClickPending = true;
                
                // Start click timeout
                postDelayed(() -> isClickPending = false, CLICK_TIMEOUT);
                
                Logger.v(TAG, "Icon touch down at (" + initialX + "," + initialY + ")");
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - initialX;
                float deltaY = event.getRawY() - initialY;
                float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                
                // Start dragging if moved beyond threshold
                if (!isDragging && distance > CLICK_THRESHOLD) {
                    isDragging = true;
                    isClickPending = false;
                    Logger.d(TAG, "Icon drag started, distance: " + distance);
                }
                
                if (isDragging && service != null) {
                    // Calculate new position
                    int newX = (int) (event.getRawX() - initialTouchX);
                    int newY = (int) (event.getRawY() - initialTouchY);
                    
                    // Update overlay position
                    service.updateOverlayPosition(newX, newY);
                    Logger.v(TAG, "Icon dragging to (" + newX + "," + newY + ")");
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                Logger.d(TAG, "Icon touch up - isDragging: " + isDragging + ", isClickPending: " + isClickPending);
                
                if (!isDragging && isClickPending) {
                    // This was a click, not a drag
                    performClick();
                    Logger.d(TAG, "Icon click detected");
                } else if (isDragging) {
                    Logger.d(TAG, "Icon drag completed");
                }
                
                // Reset states
                isDragging = false;
                isClickPending = false;
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Handle touch for container drag (FULL/SETTINGS states)
     */
    private boolean handleContainerTouch(MotionEvent event) {
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
    }
    
    /**
     * ENHANCED: Set overlay state dengan smooth transition
     */
    public void setState(OverlayState newState) {
        if (!isInitialized || newState == currentState) return;
        
        Logger.d(TAG, "State transition: " + currentState + " → " + newState);
        
        // Hide all containers
        iconContainer.setVisibility(GONE);
        fullContainer.setVisibility(GONE);
        settingsContainer.setVisibility(GONE);
        
        // Show target container with animation
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
     * Setup backward compatibility dengan old system
     */
    private void setupBackwardCompatibility() {
        mainContainer = fullContainer;
        minimizedContainer = iconContainer;
        btnMinimize = btnClose; 
        btnExpand = iconButton;
        btnSettings_compat = btnSettings;
    }
    
    // =============================================================================
    // PUBLIC API METHODS (Required by OverlayManager + backward compatibility)
    // =============================================================================
    
    public OverlayState getCurrentState() {
        return currentState;
    }
    
    // COMPATIBILITY: Old minimize/expand system
    public void setMinimized(boolean minimized) {
        setState(minimized ? OverlayState.ICON : OverlayState.FULL);
        isMinimized = minimized;
    }
    
    public boolean isMinimized() {
        return currentState == OverlayState.ICON;
    }
    
    // REQUIRED BY OverlayManager: Feature state methods
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
     * ENHANCED: Custom drawing dengan state-aware borders
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Only draw border for FULL/SETTINGS states
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
     * ENHANCED: Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up Enhanced OverlayView resources");
        
        // Remove touch listeners
        setOnTouchListener(null);
        if (iconContainer != null) {
            iconContainer.setOnTouchListener(null);
        }
        
        // Clear references
        service = null;
    }
    
    /**
     * ENHANCED: Get enhanced view info untuk debugging
     */
    public String getViewInfo() {
        return String.format(
            "Enhanced OverlayView - State: %s, Dragging: %s, Initialized: %s", 
            currentState, isDragging, isInitialized
        );
    }
}