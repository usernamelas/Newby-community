package com.victory.poolassistant.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.victory.poolassistant.core.Logger;

/**
 * Permission utilities for handling app permissions
 */
public class PermissionUtils {
    
    private static final String TAG = "PermissionUtils";
    
    /**
     * Check if overlay permission is granted
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Always granted on older versions
    }
    
    /**
     * Check if storage permission is granted
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * Check if accessibility service is enabled
     */
    public static boolean hasAccessibilityPermission(Context context) {
        // This would require checking if our accessibility service is enabled
        // For now, return false as placeholder
        return false;
    }
    
    /**
     * Get permission status summary
     */
    public static String getPermissionStatus(Context context) {
        StringBuilder status = new StringBuilder();
        
        status.append("Overlay: ").append(hasOverlayPermission(context) ? "✓" : "✗").append("\n");
        status.append("Storage: ").append(hasStoragePermission(context) ? "✓" : "✗").append("\n");
        status.append("Accessibility: ").append(hasAccessibilityPermission(context) ? "✓" : "✗");
        
        return status.toString();
    }
}