package com.victory.poolassistant;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatDelegate;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.ThemeManager;
import com.victory.poolassistant.utils.RootManager;

/**
 * Pool Assistant Application Class
 * Initializes app-wide configurations and managers
 */
public class PoolAssistantApplication extends Application {
    
    private static final String TAG = "PoolAssistantApp";
    private static PoolAssistantApplication instance;
    private static Handler mainHandler;
    
    // App managers
    private ThemeManager themeManager;
    private RootManager rootManager;
    private SharedPreferences preferences;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set instance
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize core components
        initializeCore();
        
        // Initialize managers
        initializeManagers();
        
        // Setup theme
        setupTheme();
        
        // Initialize app config
        AppConfig.initialize(this);
        
        Logger.i(TAG, "Pool Assistant Application initialized");
        Logger.i(TAG, "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        Logger.i(TAG, "Build Type: " + BuildConfig.BUILD_TYPE_NAME);
        Logger.i(TAG, "Root Features: " + BuildConfig.ROOT_FEATURES);
        Logger.i(TAG, "Obfuscated: " + BuildConfig.OBFUSCATED);
    }
    
    /**
     * Initialize core components
     */
    private void initializeCore() {
        // Initialize preferences
        preferences = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize logger
        Logger.initialize(BuildConfig.DEBUG_MODE);
        
        Logger.d(TAG, "Core components initialized");
    }
    
    /**
     * Initialize app managers
     */
    private void initializeManagers() {
        // Initialize theme manager
        themeManager = new ThemeManager(this);
        
        // Initialize root manager (only for pro version)
        if (BuildConfig.ROOT_FEATURES) {
            rootManager = new RootManager();
            Logger.d(TAG, "Root manager initialized");
        }
        
        Logger.d(TAG, "App managers initialized");
    }
    
    /**
     * Setup app theme based on user preferences
     */
    private void setupTheme() {
        // Get theme preference
        String themePref = preferences.getString(AppConfig.PREF_THEME, AppConfig.THEME_SYSTEM);
        
        // Apply theme
        switch (themePref) {
            case AppConfig.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case AppConfig.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case AppConfig.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        
        Logger.d(TAG, "Theme applied: " + themePref);
    }
    
    /**
     * Get application instance
     */
    public static PoolAssistantApplication getInstance() {
        return instance;
    }
    
    /**
     * Get main thread handler
     */
    public static Handler getMainHandler() {
        return mainHandler;
    }
    
    /**
     * Run on main thread
     */
    public static void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Get theme manager
     */
    public ThemeManager getThemeManager() {
        return themeManager;
    }
    
    /**
     * Get root manager (pro version only)
     */
    public RootManager getRootManager() {
        return rootManager;
    }
    
    /**
     * Get app preferences
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }
    
    /**
     * Check if app is in debug mode
     */
    public boolean isDebugMode() {
        return BuildConfig.DEBUG_MODE;
    }
    
    /**
     * Check if app has root features
     */
    public boolean hasRootFeatures() {
        return BuildConfig.ROOT_FEATURES;
    }
    
    /**
     * Get app version info
     */
    public String getVersionInfo() {
        return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    }
    
    /**
     * Get build info
     */
    public String getBuildInfo() {
        return "Build: " + BuildConfig.BUILD_TYPE_NAME + 
               (BuildConfig.OBFUSCATED ? " (Obfuscated)" : " (Open)") +
               "\nTime: " + BuildConfig.BUILD_TIME +
               "\nCommit: " + BuildConfig.GIT_COMMIT;
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Logger.w(TAG, "Low memory warning received");
        // Cleanup non-essential resources
        System.gc();
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Logger.d(TAG, "Memory trim requested, level: " + level);
        
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
                // App is running but system is low on memory
                // Release non-critical resources
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // System is in critical memory state
                // Release all non-essential resources
                System.gc();
                break;
        }
    }
}