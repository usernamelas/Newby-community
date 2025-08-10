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
 * Fixed 3-State Overlay System untuk Pool Assistant
 * States: ICON → FULL → SETTINGS
 * FIXED: Icon size & appearance improved + DRAGGABLE FUNCTIONALITY + SNAP TO EDGE
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // 3 States
    public enum OverlayState {
        ICON,       // Floating icon (48dp - FIXED size)
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
    private Button btnReset;
    private Button btnExit;
    
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
            Logger.d(TAG, "🚀 Starting initView()...");
            
            // Create containers
            iconContainer = createIconState();
            fullContainer = createFullState();
            settingsContainer = createSettingsState();
            
            Logger.d(TAG, "📦 Containers created");
            
            // Add all containers
            addView(iconContainer);
            addView(fullContainer);
            addView(settingsContainer);
            
            Logger.d(TAG, "📦 Containers added to view");
            
            // Setup interactions
            setupClickListeners();
            Logger.d(TAG, "🖱️ Click listeners setup");
            
            setupTouchHandling();
            Logger.d(TAG, "👆 Touch handling setup");
            
            // Set initial state
            setState(OverlayState.ICON);
            Logger.d(TAG, "🎯 Initial state set to ICON");
            
            isInitialized = true;
            Logger.d(TAG, "✅ OverlayView initialized successfully");
            
        } catch (Exception e) {
            Logger.e(TAG, "❌ Failed to initialize OverlayView", e);
        }
    }
    
    /**
     * Create Icon State (48dp floating icon - FIXED appearance)
     */
    private ViewGroup createIconState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            (int) (72 * getResources().getDisplayMetrics().density), 
            (int) (72 * getResources().getDisplayMetrics().density)
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.setElevation(8f);
        
        // Pool assistant icon - using clean app icon
        iconButton = new ImageButton(getContext());
        LayoutParams iconParams = new LayoutParams(
            (int) (64 * getResources().getDisplayMetrics().density), 
            (int) (64 * getResources().getDisplayMetrics().density)
        );
        iconButton.setLayoutParams(iconParams);
        
        // FIXED: Clean appearance - no blue background
        iconButton.setBackground(null); // Transparent background
        iconButton.setImageResource(R.drawable.ic_pool_assistant_icon);
        iconButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        
        // Add subtle shadow/border for better visibility
        iconButton.setElevation(4f);
        
        container.addView(iconButton);
        
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
        
        // Fitur Aim toggle - Get switch from container
        LinearLayout aimLayout = createToggleSwitch("Fitur Aim", true);
        switchFiturAim = (Switch) aimLayout.getChildAt(1);
        section.addView(aimLayout);
        
        // Aim Root Mode toggle - Get switch from container
        LinearLayout rootLayout = createToggleSwitch("Aim Root Mode", false);
        switchAimRootMode = (Switch) rootLayout.getChildAt(1);
        section.addView(rootLayout);
        
        // Prediksi Bola toggle - Get switch from container
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
        
        // Opacity slider - Get SeekBar from container
        LinearLayout opacityLayout = createSlider("opacity", 80);
        seekBarOpacity = (SeekBar) opacityLayout.getChildAt(1);
        section.addView(opacityLayout);
        
        // Ketebalan garis slider - Get SeekBar from container
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
        settingsTitle.setTypeface(settingsTitle.getTypeface(), Typeface.BOLD);
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
        btnReset = (Button) resetOption.getChildAt(0);
        options.addView(resetOption);
        
        // Exit app
        LinearLayout exitOption = createSettingsButton("🚪 Keluar Aplikasi", true);
        btnExit = (Button) exitOption.getChildAt(0);
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
    
    /**
     * Create settings button option - COMPLETED METHOD
     */
    private LinearLayout createSettingsButton(String label, boolean isDanger) {
        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 
            (int) (8 * getResources().getDisplayMetrics().density), 0, 
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        
        Button button = new Button(getContext());
        button.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 
            (int) (40 * getResources().getDisplayMetrics().density)
        ));
        button.setText(label);
        button.setTextColor(isDanger ? Color.RED : Color.WHITE);
        button.setTextSize(14f);
        button.setBackgroundResource(isDanger ? R.drawable.overlay_danger_button : R.drawable.overlay_settings_button);
        buttonLayout.addView(button);
        
        return buttonLayout;
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        Logger.d(TAG, "🖱️ Setting up click listeners...");
        
        // Icon state - click to expand
        if (iconButton != null) {
            iconButton.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Icon clicked - isDragging: " + isDragging);
                if (!isDragging) {
                    setState(OverlayState.FULL);
                    animateStateTransition();
                }
            });
            Logger.d(TAG, "✅ Icon click listener set");
        }
        
        // Full state buttons
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Settings button clicked");
                setState(OverlayState.SETTINGS);
                animateStateTransition();
            });
        }
        
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Close button clicked");
                setState(OverlayState.ICON);
                animateStateTransition();
            });
        }
        
        // Settings state buttons
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Plus button clicked");
                setState(OverlayState.ICON);
                animateStateTransition();
            });
        }
        
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Reset button clicked");
                resetOverlayPosition();
            });
        }
        
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                Logger.d(TAG, "🖱️ Exit button clicked");
                exitApplication();
            });
        }
        
        // Setup SeekBar listeners for real-time feedback
        if (seekBarOpacity != null) {
            seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Logger.d(TAG, "Opacity changed to: " + progress + "%");
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        if (seekBarKetebalan != null) {
            seekBarKetebalan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Logger.d(TAG, "Line thickness changed to: " + progress + "%");
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        // Setup Switch listeners
        if (switchFiturAim != null) {
            switchFiturAim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Fitur Aim " + (isChecked ? "enabled" : "disabled"));
            });
        }
        
        if (switchAimRootMode != null) {
            switchAimRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Aim Root Mode " + (isChecked ? "enabled" : "disabled"));
            });
        }
        
        if (switchPrediksi != null) {
            switchPrediksi.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Prediksi Bola " + (isChecked ? "enabled" : "disabled"));
            });
        }
        
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Theme changed to: " + (isChecked ? "Light" : "Dark"));
            });
        }
    }

    /**
     * Setup touch handling for drag functionality - FIXED IMPLEMENTATION
     */
    private void setupTouchHandling() {
        Logger.d(TAG, "=== SETTING UP TOUCH HANDLING ===");
        Logger.d(TAG, "Current state: " + currentState);
        Logger.d(TAG, "Service: " + (service != null ? "Connected" : "NULL"));
        
        // Set touch listener on the entire view
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Logger.d(TAG, "🔥 TOUCH EVENT RECEIVED! Action: " + event.getAction() + ", State: " + currentState);
                
                // Only handle touch on ICON state for dragging
                if (currentState != OverlayState.ICON) {
                    Logger.d(TAG, "❌ Touch ignored - Current state is: " + currentState + " (not ICON)");
                    return false;
                }
                
                Logger.d(TAG, "✅ Processing touch in ICON state");
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Logger.d(TAG, "👆 ACTION_DOWN detected");
                        return handleTouchDown(event);
                        
                    case MotionEvent.ACTION_MOVE:
                        return handleTouchMove(event);
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        Logger.d(TAG, "👆 ACTION_UP/CANCEL detected");
                        return handleTouchUp(event);
                }
                
                Logger.d(TAG, "❓ Unhandled touch action: " + event.getAction());
                return false;
            }
        });
        
        // TAMBAHAN: Set touch listener juga pada icon button untuk backup
        if (iconButton != null) {
            iconButton.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Logger.d(TAG, "🔥 ICON BUTTON TOUCH! Action: " + event.getAction());
                    
                    // Forward touch events to parent handling
                    return OverlayView.this.onTouchEvent(event);
                }
            });
            Logger.d(TAG, "✅ Icon button touch listener set");
        }
        
        Logger.d(TAG, "✅ Touch handling setup completed");
    }

    /**
     * Handle touch down - record initial positions
     */
    private boolean handleTouchDown(MotionEvent event) {
        Logger.d(TAG, "🟢 handleTouchDown called");
        
        // Record initial touch position
        initialTouchX = event.getRawX();
        initialTouchY = event.getRawY();
        
        // Record current overlay position from service
        if (service != null) {
            initialX = service.getCurrentX();
            initialY = service.getCurrentY();
            Logger.d(TAG, "✅ Got position from service: " + initialX + ", " + initialY);
        } else {
            initialX = 0;
            initialY = 0;
            Logger.w(TAG, "⚠️ Service is null, using default position");
        }
        
        isDragging = false;
        
        // Visual feedback - scale down icon slightly
        animatePress(true);
        
        Logger.d(TAG, "Touch down - Start: " + initialTouchX + ", " + initialTouchY + 
                 " | Current pos: " + initialX + ", " + initialY);
        return true;
    }

    /**
     * Handle touch move - update overlay position
     */
    private boolean handleTouchMove(MotionEvent event) {
        if (service == null) {
            Logger.w(TAG, "⚠️ Service is null in handleTouchMove");
            return false;
        }
        
        float deltaX = event.getRawX() - initialTouchX;
        float deltaY = event.getRawY() - initialTouchY;
        
        // Check if movement exceeds click threshold
        if (!isDragging && (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD)) {
            isDragging = true;
            Logger.d(TAG, "🚀 Started dragging - Delta: " + deltaX + ", " + deltaY);
            
            // Visual feedback - scale up slightly during drag
            animatePress(false);
            iconButton.setAlpha(0.8f);
        }
        
        if (isDragging) {
            // Calculate new position
            int newX = Math.round(initialX + deltaX);
            int newY = Math.round(initialY + deltaY);
            
            // Update overlay position through service
            service.updateOverlayPosition(newX, newY);
            
            return true;
        }
        
        return false;
    }

    /**
     * Handle touch up - finalize drag or handle click
     */
    private boolean handleTouchUp(MotionEvent event) {
        Logger.d(TAG, "🔴 handleTouchUp called - isDragging: " + isDragging);
        
        // Reset visual feedback
        animatePress(false);
        iconButton.setAlpha(1.0f);
        
        if (isDragging) {
            Logger.d(TAG, "Drag ended - Final position: " + 
                     (service != null ? service.getCurrentX() : "unknown") + ", " + 
                     (service != null ? service.getCurrentY() : "unknown"));
            
            // SNAP TO EDGE setelah drag selesai
            if (service != null) {
                Logger.d(TAG, "🎯 Triggering snap to edge...");
                service.snapToEdge();
            } else {
                Logger.w(TAG, "⚠️ Cannot snap - service is null");
            }
            
            isDragging = false;
            return true;
        }
        
        Logger.d(TAG, "Not dragging - allowing click events");
        return false;
    }

    /**
     * Animate press feedback
     */
    private void animatePress(boolean pressed) {
        if (iconButton != null) {
            float scale = pressed ? 0.95f : 1.0f;
            iconButton.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(100)
                .start();
        }
    }

    /**
     * Set overlay state and update visibility - FIXED TO PUBLIC
     */
    public void setState(OverlayState newState) {
        if (currentState == newState) return;
        
        OverlayState previousState = currentState;
        currentState = newState;
        
        Logger.d(TAG, "🔄 State changing from " + previousState + " to " + currentState);
        
        // Hide all containers
        iconContainer.setVisibility(View.GONE);
        fullContainer.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.GONE);
        
        // Show current state container
        switch (currentState) {
            case ICON:
                iconContainer.setVisibility(View.VISIBLE);
                Logger.d(TAG, "📱 Icon container visible");
                break;
            case FULL:
                fullContainer.setVisibility(View.VISIBLE);
                Logger.d(TAG, "📋 Full container visible");
                break;
            case SETTINGS:
                settingsContainer.setVisibility(View.VISIBLE);
                Logger.d(TAG, "⚙️ Settings container visible");
                break;
        }
        
        Logger.d(TAG, "State changed from " + previousState + " to " + currentState);
        
        // Update service notification if needed
        if (service != null) {
            String stateText = "";
            switch (currentState) {
                case ICON:
                    stateText = "Pool Assistant overlay minimized";
                    break;
                case FULL:
                    stateText = "Pool Assistant overlay expanded";
                    break;
                case SETTINGS:
                    stateText = "Pool Assistant settings opened";
                    break;
            }
        }
    }

    /**
     * Animate state transition
     */
    private void animateStateTransition() {
        // Simple fade animation
        setAlpha(0.7f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0.7f, 1.0f);
        fadeIn.setDuration(200);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.start();
        
        // Scale animation for smooth transition
        setScaleX(0.95f);
        setScaleY(0.95f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0.95f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0.95f, 1.0f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    /**
     * Reset overlay to center position
     */
    private void resetOverlayPosition() {
        if (service != null) {
            // Move to center of screen (approximate)
            int centerX = 100;
            int centerY = 300;
            service.updateOverlayPosition(centerX, centerY);
            Logger.d(TAG, "Overlay position reset to center: " + centerX + ", " + centerY);
            
            // Visual feedback
            animateStateTransition();
        } else {
            Logger.w(TAG, "Cannot reset position - service is null");
        }
    }

    /**
     * Exit application
     */
    private void exitApplication() {
        if (service != null) {
            Logger.d(TAG, "Application exit requested");
            
            // Hide overlay first
            service.hideOverlay();
            
            // Stop service
            service.stopSelf();
        } else {
            Logger.w(TAG, "Cannot exit - service is null");
        }
    }

    /**
     * Get current overlay state
     */
    public OverlayState getCurrentState() {
        return currentState;
    }

    /**
     * Update initial position (called by service when position changes)
     */
    public void updateInitialPosition(int x, int y) {
        initialX = x;
        initialY = y;
        Logger.d(TAG, "Initial position updated to: " + x + ", " + y);
    }

    /**
     * Check if overlay is currently being dragged
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Force state change (for external control)
     */
    public void forceState(OverlayState state) {
        setState(state);
        animateStateTransition();
    }

    /**
     * Get switch states (for settings persistence)
     */
    public boolean isFiturAimEnabled() {
        return switchFiturAim != null && switchFiturAim.isChecked();
    }

    public boolean isAimRootModeEnabled() {
        return switchAimRootMode != null && switchAimRootMode.isChecked();
    }

    public boolean isPrediksiEnabled() {
        return switchPrediksi != null && switchPrediksi.isChecked();
    }

    public boolean isLightThemeEnabled() {
        return switchTheme != null && switchTheme.isChecked();
    }

    /**
     * Get slider values (for settings persistence)
     */
    public int getOpacityValue() {
        return seekBarOpacity != null ? seekBarOpacity.getProgress() : 80;
    }

    public int getThicknessValue() {
        return seekBarKetebalan != null ? seekBarKetebalan.getProgress() : 60;
    }

    /**
     * Set switch states (for settings restoration)
     */
    public void setFiturAim(boolean enabled) {
        if (switchFiturAim != null) {
            switchFiturAim.setChecked(enabled);
        }
    }

    public void setAimRootMode(boolean enabled) {
        if (switchAimRootMode != null) {
            switchAimRootMode.setChecked(enabled);
        }
    }

    public void setPrediksi(boolean enabled) {
        if (switchPrediksi != null) {
            switchPrediksi.setChecked(enabled);
        }
    }

    public void setLightTheme(boolean enabled) {
        if (switchTheme != null) {
            switchTheme.setChecked(enabled);
        }
    }

    /**
     * Set slider values (for settings restoration)
     */
    public void setOpacityValue(int value) {
        if (seekBarOpacity != null) {
            seekBarOpacity.setProgress(Math.max(0, Math.min(100, value)));
        }
    }

    public void setThicknessValue(int value) {
        if (seekBarKetebalan != null) {
            seekBarKetebalan.setProgress(Math.max(0, Math.min(100, value)));
        }
    }

    /**
     * Check if view is properly initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayView resources");
        
        // Clear click listeners
        if (iconButton != null) iconButton.setOnClickListener(null);
        if (btnSettings != null) btnSettings.setOnClickListener(null);
        if (btnClose != null) btnClose.setOnClickListener(null);
        if (btnPlus != null) btnPlus.setOnClickListener(null);
        if (btnReset != null) btnReset.setOnClickListener(null);
        if (btnExit != null) btnExit.setOnClickListener(null);
        
        // Clear seekbar listeners
        if (seekBarOpacity != null) seekBarOpacity.setOnSeekBarChangeListener(null);
        if (seekBarKetebalan != null) seekBarKetebalan.setOnSeekBarChangeListener(null);
        
        // Clear switch listeners
        if (switchFiturAim != null) switchFiturAim.setOnCheckedChangeListener(null);
        if (switchAimRootMode != null) switchAimRootMode.setOnCheckedChangeListener(null);
        if (switchPrediksi != null) switchPrediksi.setOnCheckedChangeListener(null);
        if (switchTheme != null) switchTheme.setOnCheckedChangeListener(null);
        
        // Clear touch listener
        setOnTouchListener(null);
        if (iconButton != null) iconButton.setOnTouchListener(null);
        
        // Clear service reference
        service = null;
        
        isInitialized = false;
        Logger.d(TAG, "OverlayView cleanup completed");
    }

    /**
     * Debug method - log current state
     */
    public void logCurrentState() {
        Logger.d(TAG, "=== OverlayView State Debug ===");
        Logger.d(TAG, "Current State: " + currentState);
        Logger.d(TAG, "Is Dragging: " + isDragging);
        Logger.d(TAG, "Is Initialized: " + isInitialized);
        Logger.d(TAG, "Service: " + (service != null ? "Connected" : "Null"));
        Logger.d(TAG, "Position: " + initialX + ", " + initialY);
        
        if (isInitialized) {
            Logger.d(TAG, "Fitur Aim: " + isFiturAimEnabled());
            Logger.d(TAG, "Root Mode: " + isAimRootModeEnabled());
            Logger.d(TAG, "Prediksi: " + isPrediksiEnabled());
            Logger.d(TAG, "Light Theme: " + isLightThemeEnabled());
            Logger.d(TAG, "Opacity: " + getOpacityValue() + "%");
            Logger.d(TAG, "Thickness: " + getThicknessValue() + "%");
        }
        
        Logger.d(TAG, "================================");
    }

    /**
     * Method untuk kompatibilitas dengan OverlayManager
     */
    public boolean isBasicAimEnabled() {
        return isFiturAimEnabled();
    }

    public boolean isRootAimEnabled() {
        return isAimRootModeEnabled();
    }

    public boolean isPredictionEnabled() {
        return isPrediksiEnabled();
    }

    public int getLineThicknessValue() {
        return getThicknessValue();
    }

    /**
     * Force refresh touch handling (untuk troubleshooting)
     */
    public void refreshTouchHandling() {
        Logger.d(TAG, "🔄 Refreshing touch handling...");
        setupTouchHandling();
    }

    /**
     * Test method untuk cek apakah touch berfungsi
     */
    public void testTouch() {
        Logger.d(TAG, "🧪 TOUCH TEST:");
        Logger.d(TAG, "- Is Clickable: " + isClickable());
        Logger.d(TAG, "- Is Enabled: " + isEnabled());
        Logger.d(TAG, "- Current State: " + currentState);
        Logger.d(TAG, "- Service Connected: " + (service != null));
        Logger.d(TAG, "- Is Initialized: " + isInitialized);
        Logger.d(TAG, "- Is Dragging: " + isDragging);
        
        if (iconButton != null) {
            Logger.d(TAG, "- Icon Button Clickable: " + iconButton.isClickable());
            Logger.d(TAG, "- Icon Button Enabled: " + iconButton.isEnabled());
            Logger.d(TAG, "- Icon Button Visibility: " + (iconButton.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
        }
        
        if (iconContainer != null) {
            Logger.d(TAG, "- Icon Container Visibility: " + (iconContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
        }
    }
}