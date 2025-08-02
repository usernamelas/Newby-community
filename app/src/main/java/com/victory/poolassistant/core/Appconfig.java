package com.victory.poolassistant.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * App Configuration Constants and Settings
 */
public class AppConfig {
    
    // App Info
    public static final String APP_NAME = "Pool Assistant Pro";
    public static final String PACKAGE_NAME = "com.victory.poolassistant";
    
    // Preferences
    public static final String PREF_NAME = "pool_assistant_prefs";
    
    // Theme Constants
    public static final String PREF_THEME = "theme_preference";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    
    // App State
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_LAST_VERSION = "last_version";
    public static final String PREF_OVERLAY_ENABLED = "overlay_enabled";
    public static final String PREF_ROOT_MODE = "root_mode";
    
    // Overlay Settings
    public static final String PREF_OVERLAY_OPACITY = "overlay_opacity";
    public static final String PREF_TRAJECTORY_COLOR = "trajectory_color";
    public static final String PREF_LINE_THICKNESS = "line_thickness";
    public static final String PREF_ANIMATION_SPEED = "animation_speed";
    public static final String PREF_AUTO_HIDE = "auto_hide";
    
    // Detection Settings
    public static final String PREF_DETECTION_METHOD = "detection_method";
    public static final String PREF_DETECTION_SENSITIVITY = "detection_sensitivity";
    public static final String PREF_AUTO_START = "auto_start";
    
    // Performance Settings
    public static final String PREF_FRAME_RATE = "frame_rate";
    public static final String PREF_BATTERY_OPTIMIZATION = "battery_optimization";
    public static final String PREF_HARDWARE_ACCELERATION = "hardware_acceleration";
    
    // Floating Icon Settings
    public static final String PREF_FLOATING_ICON_ENABLED = "floating_icon_enabled";
    public static final String PREF_ICON_POSITION_X = "icon_position_x";
    public static final String PREF_ICON_POSITION_Y = "icon_position_y";
    public static final String PREF_ICON_SIZE = "icon_size";
    public static final String PREF_ICON_TRANSPARENCY = "icon_transparency";
    
    // Game Detection
    public static final String[] SUPPORTED_GAMES = {
        "com.miniclip.eightballpool",
        "com.pool.billiards.ball",
        "com.gameindy.nineballpool",
        "com.zingmagic.poolrebel"
    };
    
    // Default Values
    public static final int DEFAULT_OVERLAY_OPACITY = 80;
    public static final int DEFAULT_TRAJECTORY_COLOR = 0xFF00FF00; // Green
    public static final int DEFAULT_LINE_THICKNESS = 3;
    public static final int DEFAULT_ANIMATION_SPEED = 50;
    public static final int DEFAULT_FRAME_RATE = 60;
    public static final int DEFAULT_DETECTION_SENSITIVITY = 75;
    public static final int DEFAULT_ICON_SIZE = 64;
    public static final int DEFAULT_ICON_TRANSPARENCY = 90;
    
    // Animation Durations (milliseconds)
    public static final int ANIMATION_DURATION_SHORT = 200;
    public static final int ANIMATION_DURATION_MEDIUM = 300;
    public static final int ANIMATION_DURATION_LONG = 500;
    
    // Detection Methods
    public static final String DETECTION_PACKAGE = "package";
    public static final String DETECTION_SCREEN = "screen";
    public static final String DETECTION_ROOT = "root";
    public static final String DETECTION_HYBRID = "hybrid";
    
    // Trajectory Colors
    public static final int COLOR_GREEN = 0xFF00FF00;
    public static final int COLOR_BLUE = 0xFF0080FF;
    public static final int COLOR_RED = 0xFFFF0040;
    public static final int COLOR_YELLOW = 0xFFFFD700;
    public static final int COLOR_PURPLE = 0xFF8A2BE2;
    public static final int COLOR_CYAN = 0xFF00FFFF;
    
    // App State
    private static Context context;
    private static SharedPreferences preferences;
    private static boolean initialized = false;
    
    /**
     * Initialize app config
     */
    public static void initialize(Context appContext) {
        if (initialized) return;
        
        context = appContext.getApplicationContext();
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initialized = true;
        
        // Check if this is first launch
        if (isFirstLaunch()) {
            setupDefaultPreferences();
        }
        
        // Update last version
        updateLastVersion();
    }
    
    /**
     * Check if this is the first app launch
     */
    public static boolean isFirstLaunch() {
        return preferences.getBoolean(PREF_FIRST_LAUNCH, true);
    }
    
    /**
     * Setup default preferences on first launch
     */
    private static void setupDefaultPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        
        // Theme
        editor.putString(PREF_THEME, THEME_SYSTEM);
        
        // Overlay settings
        editor.putInt(PREF_OVERLAY_OPACITY, DEFAULT_OVERLAY_OPACITY);
        editor.putInt(PREF_TRAJECTORY_COLOR, DEFAULT_TRAJECTORY_COLOR);
        editor.putInt(PREF_LINE_THICKNESS, DEFAULT_LINE_THICKNESS);
        editor.putInt(PREF_ANIMATION_SPEED, DEFAULT_ANIMATION_SPEED);
        editor.putBoolean(PREF_AUTO_HIDE, true);
        
        // Detection settings
        editor.putString(PREF_DETECTION_METHOD, DETECTION_HYBRID);
        editor.putInt(PREF_DETECTION_SENSITIVITY, DEFAULT_DETECTION_SENSITIVITY);
        editor.putBoolean(PREF_AUTO_START, false);
        
        // Performance settings
        editor.putInt(PREF_FRAME_RATE, DEFAULT_FRAME_RATE);
        editor.putBoolean(PREF_BATTERY_OPTIMIZATION, true);
        editor.putBoolean(PREF_HARDWARE_ACCELERATION, true);
        
        // Floating icon settings
        editor.putBoolean(PREF_FLOATING_ICON_ENABLED, true);
        editor.putInt(PREF_ICON_SIZE, DEFAULT_ICON_SIZE);
        editor.putInt(PREF_ICON_TRANSPARENCY, DEFAULT_ICON_TRANSPARENCY);
        
        // App state
        editor.putBoolean(PREF_FIRST_LAUNCH, false);
        editor.putBoolean(PREF_OVERLAY_ENABLED, false);
        editor.putBoolean(PREF_ROOT_MODE, false);
        
        editor.apply();
    }
    
    /**
     * Update last version preference
     */
    private static void updateLastVersion() {
        try {
            String currentVersion = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
            
            preferences.edit()
                .putString(PREF_LAST_VERSION, currentVersion)
                .apply();
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Get string preference
     */
    public static String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }
    
    /**
     * Get int preference
     */
    public static int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }
    
    /**
     * Get boolean preference
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    /**
     * Set string preference
     */
    public static void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }
    
    /**
     * Set int preference
     */
    public static void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }
    
    /**
     * Set boolean preference
     */
    public static void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }
    
    /**
     * Get current theme
     */
    public static String getCurrentTheme() {
        return getString(PREF_THEME, THEME_SYSTEM);
    }
    
    /**
     * Check if overlay is enabled
     */
    public static boolean isOverlayEnabled() {
        return getBoolean(PREF_OVERLAY_ENABLED, false);
    }
    
    /**
     * Check if root mode is enabled
     */
    public static boolean isRootModeEnabled() {
        return getBoolean(PREF_ROOT_MODE, false);
    }
    
    /**
     * Get overlay opacity (0-100)
     */
    public static int getOverlayOpacity() {
        return getInt(PREF_OVERLAY_OPACITY, DEFAULT_OVERLAY_OPACITY);
    }
    
    /**
     * Get trajectory color
     */
    public static int getTrajectoryColor() {
        return getInt(PREF_TRAJECTORY_COLOR, DEFAULT_TRAJECTORY_COLOR);
    }
    
    /**
     * Get detection method
     */
    public static String getDetectionMethod() {
        return getString(PREF_DETECTION_METHOD, DETECTION_HYBRID);
    }
    
    /**
     * Check if floating icon is enabled
     */
    public static boolean isFloatingIconEnabled() {
        return getBoolean(PREF_FLOATING_ICON_ENABLED, true);
    }
    
    /**
     * Reset all preferences to defaults
     */
    public static void resetToDefaults() {
        preferences.edit().clear().apply();
        setupDefaultPreferences();
    }
    
    /**
     * Export preferences (for backup)
     */
    public static String exportPreferences() {
        // Implementation would serialize all preferences to JSON
        // For now, return empty string
        return "{}";
    }
    
    /**
     * Import preferences (from backup)
     */
    public static boolean importPreferences(String data) {
        // Implementation would deserialize JSON and restore preferences
        // For now, return false
        return false;
    }
}