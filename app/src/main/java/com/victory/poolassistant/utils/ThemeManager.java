package com.victory.poolassistant.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;

/**
 * Enhanced ThemeManager dengan overlay theme support
 * Handles app theming dan broadcasts changes ke floating overlay
 */
public class ThemeManager {
    
    private static final String TAG = "ThemeManager";
    private Context context;
    
    // Broadcast actions untuk theme changes
    public static final String ACTION_THEME_CHANGED = "com.victory.poolassistant.THEME_CHANGED";
    public static final String EXTRA_THEME_NAME = "theme_name";
    public static final String EXTRA_IS_DARK_MODE = "is_dark_mode";
    
    public ThemeManager(Context context) {
        this.context = context;
    }
    
    /**
     * Apply theme based on preference
     */
    public void applyTheme(String theme) {
        Logger.d(TAG, "Applying theme: " + theme);
        
        switch (theme) {
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
        
        // Save preference
        AppConfig.setString(AppConfig.PREF_THEME, theme);
        
        // Broadcast theme change untuk overlay
        broadcastThemeChange(theme);
    }
    
    /**
     * Get current theme
     */
    public String getCurrentTheme() {
        return AppConfig.getCurrentTheme();
    }
    
    /**
     * Check if currently in dark mode
     */
    public boolean isDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Toggle between light and dark theme
     */
    public void toggleTheme() {
        String currentTheme = getCurrentTheme();
        String newTheme;
        
        if (AppConfig.THEME_LIGHT.equals(currentTheme)) {
            newTheme = AppConfig.THEME_DARK;
        } else if (AppConfig.THEME_DARK.equals(currentTheme)) {
            newTheme = AppConfig.THEME_LIGHT;
        } else {
            // System theme - toggle to opposite of current mode
            newTheme = isDarkMode() ? AppConfig.THEME_LIGHT : AppConfig.THEME_DARK;
        }
        
        applyTheme(newTheme);
    }
    
    /**
     * Get theme display name
     */
    public String getThemeDisplayName(String theme) {
        switch (theme) {
            case AppConfig.THEME_LIGHT:
                return "Terang";
            case AppConfig.THEME_DARK:
                return "Gelap";
            case AppConfig.THEME_SYSTEM:
            default:
                return "Sistem";
        }
    }
    
    /**
     * Get all available themes
     */
    public String[] getAvailableThemes() {
        return new String[]{
            AppConfig.THEME_SYSTEM,
            AppConfig.THEME_LIGHT,
            AppConfig.THEME_DARK
        };
    }
    
    /**
     * Get theme display names
     */
    public String[] getThemeDisplayNames() {
        return new String[]{
            "Ikuti Sistem",
            "Mode Terang", 
            "Mode Gelap"
        };
    }
    
    /**
     * Broadcast theme change untuk overlay dan components lain
     */
    private void broadcastThemeChange(String themeName) {
        try {
            Intent intent = new Intent(ACTION_THEME_CHANGED);
            intent.putExtra(EXTRA_THEME_NAME, themeName);
            intent.putExtra(EXTRA_IS_DARK_MODE, isDarkMode());
            
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            Logger.d(TAG, "Theme change broadcasted: " + themeName + ", dark: " + isDarkMode());
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to broadcast theme change", e);
        }
    }
    
    /**
     * Get overlay-specific colors based on current theme
     */
    
    /**
     * Get primary color based on current theme
     */
    public int getPrimaryColor() {
        return isDarkMode() ? 0xFF2196F3 : 0xFF1976D2; // Blue variants
    }
    
    /**
     * Get accent color based on current theme
     */
    public int getAccentColor() {
        return isDarkMode() ? 0xFF4CAF50 : 0xFF388E3C; // Green variants
    }
    
    /**
     * Get overlay background color
     */
    public int getOverlayBackgroundColor() {
        return isDarkMode() ? 0xFF121212 : 0xFFFAFAFA;
    }
    
    /**
     * Get overlay surface color
     */
    public int getOverlaySurfaceColor() {
        return isDarkMode() ? 0xFF1E1E1E : 0xFFFFFFFF;
    }
    
    /**
     * Get overlay surface variant color
     */
    public int getOverlaySurfaceVariantColor() {
        return isDarkMode() ? 0xFF2A2A2A : 0xFFF5F5F5;
    }
    
    /**
     * Get overlay primary text color
     */
    public int getOverlayTextPrimaryColor() {
        return isDarkMode() ? 0xFFFFFFFF : 0xFF212121;
    }
    
    /**
     * Get overlay secondary text color
     */
    public int getOverlayTextSecondaryColor() {
        return isDarkMode() ? 0xFFB0B0B0 : 0xFF757575;
    }
    
    /**
     * Get overlay divider color
     */
    public int getOverlayDividerColor() {
        return isDarkMode() ? 0xFF333333 : 0xFFE0E0E0;
    }
    
    /**
     * Get overlay header gradient colors
     */
    public int[] getOverlayHeaderGradientColors() {
        // Header always blue gradient regardless of theme
        return new int[]{
            0xFF2196F3, // start
            0xFF1976D2, // center  
            0xFF1565C0  // end
        };
    }
    
    /**
     * Get overlay switch track color (off state)
     */
    public int getOverlaySwitchTrackOffColor() {
        return isDarkMode() ? 0xFF616161 : 0xFFBDBDBD;
    }
    
    /**
     * Get overlay slider progress background color
     */
    public int getOverlaySliderProgressBgColor() {
        return isDarkMode() ? 0xFF424242 : 0xFFE0E0E0;
    }
    
    /**
     * Get overlay ripple color
     */
    public int getOverlayRippleColor() {
        return isDarkMode() ? 0x33FFFFFF : 0x33000000;
    }
    
    /**
     * Apply theme to specific view programmatically
     */
    public void applyThemeToView(android.view.View view) {
        if (view == null) return;
        
        // This can be used to manually apply theme colors to overlay components
        // when XML theme attributes are not sufficient
        
        boolean isDark = isDarkMode();
        Logger.d(TAG, "Applying theme to view, dark mode: " + isDark);
        
        // Example: Set background color based on theme
        // view.setBackgroundColor(getOverlaySurfaceColor());
    }
    
    /**
     * Get theme resource ID for overlay
     */
    public int getOverlayThemeResId() {
        return isDarkMode() ? 
            com.victory.poolassistant.R.style.OverlayDarkTheme : 
            com.victory.poolassistant.R.style.OverlayLightTheme;
    }
    
    /**
     * Check if system is using dark theme
     */
    public boolean isSystemDarkTheme() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Get effective theme (resolves SYSTEM to actual LIGHT/DARK)
     */
    public String getEffectiveTheme() {
        String currentTheme = getCurrentTheme();
        
        if (AppConfig.THEME_SYSTEM.equals(currentTheme)) {
            return isSystemDarkTheme() ? AppConfig.THEME_DARK : AppConfig.THEME_LIGHT;
        }
        
        return currentTheme;
    }
    
    /**
     * Force refresh theme (useful after system theme changes)
     */
    public void refreshTheme() {
        String currentTheme = getCurrentTheme();
        Logger.d(TAG, "Refreshing theme: " + currentTheme);
        
        // Re-apply current theme to trigger refresh
        applyTheme(currentTheme);
    }
    
    /**
     * Static helper methods
     */
    
    /**
     * Create theme manager instance
     */
    public static ThemeManager getInstance(Context context) {
        return new ThemeManager(context.getApplicationContext());
    }
    
    /**
     * Quick check for dark mode without creating instance
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}