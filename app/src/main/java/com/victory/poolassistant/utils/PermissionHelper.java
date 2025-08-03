package com.victory.poolassistant.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.victory.poolassistant.core.Constants;
import com.victory.poolassistant.core.Logger;

public class PermissionHelper {
    
    private static final String TAG = "PermissionHelper";
    private final Activity activity;
    private final Context context;
    
    public PermissionHelper(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }
    
    public PermissionHelper(Context context) {
        this.activity = null;
        this.context = context;
    }
    
    // Check overlay permission
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    // Request overlay permission
    public void requestOverlayPermission() {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        
        if (!hasOverlayPermission()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            activity.startActivityForResult(intent, Constants.REQUEST_OVERLAY_PERMISSION);
            Logger.d(TAG, "Overlay permission requested");
        }
    }
    
    // Check storage permission
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses different permissions
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    // Request storage permission
    public void requestStoragePermission() {
        if (activity == null) return;
        
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        Constants.REQUEST_STORAGE_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        Constants.REQUEST_STORAGE_PERMISSION);
            }
            Logger.d(TAG, "Storage permission requested");
        }
    }
    
    // Check accessibility permission
    public boolean hasAccessibilityPermission() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                
                if (settingValue != null) {
                    return settingValue.contains(context.getPackageName());
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            Logger.e(TAG, "Accessibility setting not found", e);
        }
        
        return false;
    }
    
    // Request accessibility permission
    public void requestAccessibilityPermission() {
        if (activity == null) return;
        
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        activity.startActivityForResult(intent, Constants.REQUEST_ACCESSIBILITY_PERMISSION);
        Logger.d(TAG, "Accessibility permission requested");
    }
    
    // Check all required permissions
    public void checkAllPermissions() {
        Logger.d(TAG, "Checking all permissions:");
        Logger.d(TAG, "Overlay: " + hasOverlayPermission());
        Logger.d(TAG, "Storage: " + hasStoragePermission());
        Logger.d(TAG, "Accessibility: " + hasAccessibilityPermission());
    }
    
    // Get permission status summary
    public String getPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Overlay: ").append(hasOverlayPermission() ? "✓" : "✗").append("\n");
        status.append("Storage: ").append(hasStoragePermission() ? "✓" : "✗").append("\n");
        status.append("Accessibility: ").append(hasAccessibilityPermission() ? "✓" : "✗");
        return status.toString();
    }
}