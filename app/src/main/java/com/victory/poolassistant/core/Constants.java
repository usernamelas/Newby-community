package com.victory.poolassistant.core;

public class Constants {
    
    // App Info
    public static final String APP_NAME = "Pool Assistant Pro";
    public static final String PACKAGE_NAME = "com.victory.poolassistant";
    
    // Permissions
    public static final int REQUEST_OVERLAY_PERMISSION = 1001;
    public static final int REQUEST_ACCESSIBILITY_PERMISSION = 1002;
    public static final int REQUEST_STORAGE_PERMISSION = 1003;
    
    // Preferences Keys
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_OVERLAY_ENABLED = "pref_overlay_enabled";
    public static final String PREF_GAME_DETECTION = "pref_game_detection";
    public static final String PREF_ROOT_ACCESS = "pref_root_access";
    
    // Theme Values
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    
    // Game Detection
    public static final String TARGET_PACKAGE = "com.miniclip.eightballpool";
    public static final String GAME_ACTIVITY = "com.miniclip.eightballpool.GameActivity";
    
    // Overlay Settings
    public static final int DEFAULT_OVERLAY_SIZE = 100;
    public static final float DEFAULT_OVERLAY_ALPHA = 0.8f;
    
    // Service Actions
    public static final String ACTION_START_OVERLAY = "start_overlay";
    public static final String ACTION_STOP_OVERLAY = "stop_overlay";
    public static final String ACTION_START_DETECTION = "start_detection";
    public static final String ACTION_STOP_DETECTION = "stop_detection";
    
    // Notification IDs
    public static final int NOTIFICATION_OVERLAY_ID = 1001;
    public static final int NOTIFICATION_DETECTION_ID = 1002;
    
    // File Paths
    public static final String LOG_FILE_NAME = "pool_assistant.log";
    public static final String CONFIG_FILE_NAME = "config.json";
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}