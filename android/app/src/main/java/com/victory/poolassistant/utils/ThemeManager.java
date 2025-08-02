package com.victory.poolassistant.utils;

import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Logger;

/**
 * Theme Manager - Handles app theming and dark/light mode
 */
public class ThemeManager {
    
    private static final String TAG = "ThemeManager";
    private Context context;
    
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
                return "Light";
            case AppConfig.THEME_DARK:
                return "Dark";
            case AppConfig.THEME_SYSTEM:
            default:
                return "System";
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
            "System Default",
            "Light Mode", 
            "Dark Mode"
        };
    }
    
    /**
     * Get primary color based on current theme
     */
    public int getPrimaryColor() {
        return isDarkMode() ? 0xFF1E88E5 : 0xFF2196F3; // Blue variants
    }
    
    /**
     * Get accent color based on current theme
     */
    public int getAccentColor() {
        return isDarkMode() ? 0xFF4CAF50 : 0xFF8BC34A; // Green variants
    }
    
    /**
     * Get surface color based on current theme
     */
    public int getSurfaceColor() {
        return isDarkMode() ? 0xFF1E1E1E : 0xFFFFFFFF;
    }
    
    /**
     * Get background color based on current theme
     */
    public int getBackgroundColor() {
        return isDarkMode() ? 0xFF121212 : 0xFFF5F5F5;
    }
    
    /**
     * Get text color based on current theme
     */
    public int getTextColor() {
        return isDarkMode() ? 0xFFFFFFFF : 0xFF212121;
    }
    
    /**
     * Get secondary text color based on current theme
     */
    public int getSecondaryTextColor() {
        return isDarkMode() ? 0xFFB0B0B0 : 0xFF757575;
    }
}