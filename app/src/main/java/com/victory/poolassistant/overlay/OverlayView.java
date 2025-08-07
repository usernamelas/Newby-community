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
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.ThemeManager;

/**
 * Enhanced OverlayView dengan 3-state system:
 * - FULL: Full overlay dengan controls (overlay_full_layout.xml)
 * - ICON: Minimized floating icon (overlay_icon_layout.xml)  
 * - SETTINGS: Settings menu (overlay_settings_layout.xml)
 */
public class OverlayView extends LinearLayout {
    
    private static final String TAG = "OverlayView";
    
    // Overlay States
    public enum OverlayState {
        FULL,       // Full overlay dengan semua controls
        ICON,       // Minimized floating icon
        SETTINGS    // Settings menu
    }
    
    // Touch handling
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private static final int CLICK_THRESHOLD = 10; // pixels
    
    // State management
    private OverlayState currentState = OverlayState.FULL;
    private boolean isInitialized = false;
    
    // Layout containers
    private ViewGroup fullContainer;
    private ViewGroup iconContainer; 
    private ViewGroup settingsContainer;
    
    // Full overlay components
    private ImageButton btnHeaderSettings;
    private ImageButton btnHeaderClose;
    private ImageView ivPoolBall;
    private TextView tvAppName;
    
    // Feature toggles
    private SwitchCompat switchBasicAim;
    private SwitchCompat switchRootAim; 
    private SwitchCompat switchPrediction;
    
    // Sliders
    private SeekBar sliderOpacity;
    private SeekBar sliderLineThickness;
    
    // Icon mode components
    private ImageView ivFloatingIcon;
    private View statusIndicator;
    
    // Settings components
    private ViewGroup settingsHideOverlay;
    private ViewGroup settingsThemeToggle;
    private ViewGroup settingsResetPosition;
    private ViewGroup settingsExitApp;
    private TextView tvCurrentTheme;
    private ImageButton btnSettingsBack;
    
    // Service reference & theme
    private FloatingOverlayService service;
    private ThemeManager themeManager;
    
    // Feature states
    private boolean basicAimEnabled = true;
    private boolean rootAimEnabled = false;
    private boolean predictionEnabled = true;
    private int opacityValue = 80;
    private int lineThicknessValue = 5;
    
    public OverlayView(Context context) {
        super(context);
        this.service = (FloatingOverlayService) context;
        this.themeManager = new ThemeManager(context);
        initView();
    }
    
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.themeManager = new ThemeManager(context);
        initView();
    }
    
    /**
     * Initialize all overlay views and components
     */
    private void initView() {
        try {
            setupContainers();
            setState(OverlayState.FULL);
            isInitialized = true;
            Logger.d(TAG, "OverlayView initialized with 3-state system");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize OverlayView", e);
        }
    }
    
    /**
     * Setup all layout containers
     */
    private void setupContainers() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        // Inflate all layouts
        fullContainer = (ViewGroup) inflater.inflate(R.layout.overlay_full_layout, null);
        iconContainer = (ViewGroup) inflater.inflate(R.layout.overlay_icon_layout, null);
        settingsContainer = (ViewGroup) inflater.inflate(R.layout.overlay_settings_layout, null);
        
        // Add all containers
        addView(fullContainer);
        addView(iconContainer);
        addView(settingsContainer);
        
        // Find views and setup
        findFullOverlayViews();
        findIconViews();
        findSettingsViews();
        
        setupClickListeners();
        setupTouchHandling();
        applyTheme();
    }
    
    /**
     * Find all components in full overlay
     */
    private void findFullOverlayViews() {
        // Header components
        btnHeaderSettings = fullContainer.findViewById(R.id.btn_header_settings);
        btnHeaderClose = fullContainer.findViewById(R.id.btn_header_close);
        ivPoolBall = fullContainer.findViewById(R.id.iv_pool_ball);
        tvAppName = fullContainer.findViewById(R.id.tv_app_name);
        
        // Toggle switches
        switchBasicAim = fullContainer.findViewById(R.id.switch_basic_aim);
        switchRootAim = fullContainer.findViewById(R.id.switch_root_aim);
        switchPrediction = fullContainer.findViewById(R.id.switch_prediction);
        
        // Sliders
        sliderOpacity = fullContainer.findViewById(R.id.slider_opacity);
        sliderLineThickness = fullContainer.findViewById(R.id.slider_line_thickness);
    }
    
    /**
     * Find components in icon mode
     */
    private void findIconViews() {
        ivFloatingIcon = iconContainer.findViewById(R.id.iv_floating_icon);
        statusIndicator = iconContainer.findViewById(R.id.status_indicator);
    }
    
    /**
     * Find components in settings menu
     */
    private void findSettingsViews() {
        btnSettingsBack = settingsContainer.findViewById(R.id.btn_settings_back);
        settingsHideOverlay = settingsContainer.findViewById(R.id.settings_hide_overlay);
        settingsThemeToggle = settingsContainer.findViewById(R.id.settings_theme_toggle);
        settingsResetPosition = settingsContainer.findViewById(R.id.settings_reset_position);
        settingsExitApp = settingsContainer.findViewById(R.id.settings_exit_app);
        tvCurrentTheme = settingsContainer.findViewById(R.id.tv_current_theme);
    }
    
    /**
     * Setup click listeners untuk semua components
     */
    private void setupClickListeners() {
        // Full overlay buttons
        if (btnHeaderSettings != null) {
            btnHeaderSettings.setOnClickListener(v -> {
                Logger.d(TAG, "Settings button clicked");
                setState(OverlayState.SETTINGS);
            });
        }
        
        if (btnHeaderClose != null) {
            btnHeaderClose.setOnClickListener(v -> {
                Logger.d(TAG, "Close button clicked - switching to icon mode");
                setState(OverlayState.ICON);
            });
        }
        
        // Icon mode - tap to expand
        if (iconContainer != null) {
            iconContainer.setOnClickListener(v -> {
                Logger.d(TAG, "Icon clicked - expanding to full overlay");
                setState(OverlayState.FULL);
            });
        }
        
        // Settings back button
        if (btnSettingsBack != null) {
            btnSettingsBack.setOnClickListener(v -> {
                Logger.d(TAG, "Settings back button clicked");
                setState(OverlayState.FULL);
            });
        }
        
        // Feature toggles
        setupToggleListeners();
        
        // Sliders
        setupSliderListeners();
        
        // Settings menu items
        setupSettingsListeners();
    }
    
    /**
     * Setup toggle switch listeners
     */
    private void setupToggleListeners() {
        if (switchBasicAim != null) {
            switchBasicAim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                basicAimEnabled = isChecked;
                Logger.d(TAG, "Basic aim toggled: " + isChecked);
                updateStatusIndicator();
            });
            switchBasicAim.setChecked(basicAimEnabled);
        }
        
        if (switchRootAim != null) {
            switchRootAim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                rootAimEnabled = isChecked;
                Logger.d(TAG, "Root aim toggled: " + isChecked);
                updateStatusIndicator();
            });
            switchRootAim.setChecked(rootAimEnabled);
        }
        
        if (switchPrediction != null) {
            switchPrediction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                predictionEnabled = isChecked;
                Logger.d(TAG, "Prediction toggled: " + isChecked);
                updateStatusIndicator();
            });
            switchPrediction.setChecked(predictionEnabled);
        }
    }
    
    /**
     * Setup slider listeners
     */
    private void setupSliderListeners() {
        if (sliderOpacity != null) {
            sliderOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        opacityValue = progress;
                        Logger.d(TAG, "Opacity changed: " + progress + "%");
                        applyOpacity();
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            sliderOpacity.setProgress(opacityValue);
        }
        
        if (sliderLineThickness != null) {
            sliderLineThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        lineThicknessValue = progress;
                        Logger.d(TAG, "Line thickness changed: " + progress);
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            sliderLineThickness.setProgress(lineThicknessValue);
        }
    }
    
    /**
     * Setup settings menu listeners
     */
    private void setupSettingsListeners() {
        if (settingsHideOverlay != null) {
            settingsHideOverlay.setOnClickListener(v -> {
                Logger.d(TAG, "Hide overlay clicked");
                if (service != null) {
                    service.hideOverlay();
                }
            });
        }
        
        if (settingsThemeToggle != null) {
            settingsThemeToggle.setOnClickListener(v -> {
                Logger.d(TAG, "Theme toggle clicked");
                themeManager.toggleTheme();
                applyTheme();
                updateThemeDisplay();
            });
        }
        
        if (settingsResetPosition != null) {
            settingsResetPosition.setOnClickListener(v -> {
                Logger.d(TAG, "Reset position clicked");
                if (service != null) {
                    service.resetOverlayPosition();
                }
            });
        }
        
        if (settingsExitApp != null) {
            settingsExitApp.setOnClickListener(v -> {
                Logger.d(TAG, "Exit app clicked");
                if (service != null) {
                    service.exitApplication();
                }
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
     * Handle touch events
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
                if (!isDragging) {
                    performClick();
                }
                isDragging = false;
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Set overlay state dengan animasi
     */
    public void setState(OverlayState newState) {
        if (!isInitialized || currentState == newState) return;
        
        Logger.d(TAG, "Changing state from " + currentState + " to " + newState);
        
        // Hide current container
        hideContainer(getCurrentContainer());
        
        // Update state
        currentState = newState;
        
        // Show new container
        showContainer(getCurrentContainer());
        
        // Update service window size if needed
        if (service != null) {
            service.updateOverlaySize(newState);
        }
    }
    
    /**
     * Get current container based on state
     */
    private ViewGroup getCurrentContainer() {
        switch (currentState) {
            case FULL:
                return fullContainer;
            case ICON:
                return iconContainer;
            case SETTINGS:
                return settingsContainer;
            default:
                return fullContainer;
        }
    }
    
    /**
     * Hide container dengan animasi
     */
    private void hideContainer(ViewGroup container) {
        if (container != null) {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f);
            fadeOut.setDuration(150);
            fadeOut.start();
            container.setVisibility(GONE);
        }
    }
    
    /**
     * Show container dengan animasi
     */
    private void showContainer(ViewGroup container) {
        if (container != null) {
            container.setVisibility(VISIBLE);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f);
            fadeIn.setDuration(200);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            fadeIn.start();
        }
    }
    
    /**
     * Apply theme to overlay components
     */
    private void applyTheme() {
        if (!isInitialized || themeManager == null) return;
        
        boolean isDark = themeManager.isDarkMode();
        Logger.d(TAG, "Applying theme, dark mode: " + isDark);
        
        // Update theme-specific colors
        updateThemeColors(isDark);
        updateThemeDisplay();
    }
    
    /**
     * Update colors based on theme
     */
    private void updateThemeColors(boolean isDark) {
        // This will be handled by XML theme attributes
        // But we can override specific elements here if needed
    }
    
    /**
     * Update theme display text
     */
    private void updateThemeDisplay() {
        if (tvCurrentTheme != null) {
            String currentTheme = themeManager.getCurrentTheme();
            tvCurrentTheme.setText(themeManager.getThemeDisplayName(currentTheme));
        }
    }
    
    /**
     * Update status indicator based on active features
     */
    private void updateStatusIndicator() {
        if (statusIndicator == null) return;
        
        // Change indicator color based on active features
        if (basicAimEnabled || rootAimEnabled || predictionEnabled) {
            statusIndicator.setBackgroundResource(R.drawable.overlay_status_indicator_active);
        } else {
            statusIndicator.setBackgroundResource(R.drawable.overlay_status_indicator_inactive);
        }
    }
    
    /**
     * Apply opacity to overlay
     */
    private void applyOpacity() {
        float alpha = opacityValue / 100f;
        setAlpha(alpha);
    }
    
    /**
     * Get current overlay state
     */
    public OverlayState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get feature states
     */
    public boolean isBasicAimEnabled() { return basicAimEnabled; }
    public boolean isRootAimEnabled() { return rootAimEnabled; }
    public boolean isPredictionEnabled() { return predictionEnabled; }
    public int getOpacityValue() { return opacityValue; }
    public int getLineThicknessValue() { return lineThicknessValue; }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.d(TAG, "Cleaning up OverlayView resources");
        
        setOnTouchListener(null);
        service = null;
        themeManager = null;
    }
}