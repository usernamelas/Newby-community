package com.victory.poolassistant.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import com.victory.poolassistant.core.Logger;

/**
 * Helper class untuk permission management
 * Handles overlay permission, storage, dan permissions lainnya
 */
public class PermissionHelper {
    
    private static final String TAG = "PermissionHelper";
    
    // Request codes
    public static final int REQUEST_OVERLAY_PERMISSION = 1001;
    public static final int REQUEST_STORAGE_PERMISSION = 1002;
    public static final int REQUEST_ACCESSIBILITY_PERMISSION = 1003;
    
    // Required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    /**
     * Check if overlay permission is granted
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermission = Settings.canDrawOverlays(context);
            Logger.d(TAG, "Overlay permission status: " + hasPermission);
            return hasPermission;
        }
        return true; // Pre-M devices don't need this permission
    }
    
    /**
     * Request overlay permission
     */
    public static void requestOverlayPermission(Context context) {
        Logger.d(TAG, "Requesting overlay permission...");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                try {
                    context.startActivity(intent);
                    Logger.i(TAG, "Overlay permission request sent");
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to request overlay permission", e);
                }
            } else {
                Logger.d(TAG, "Overlay permission already granted");
            }
        }
    }
    
    /**
     * Request overlay permission from Activity (dengan result callback)
     */
    public static void requestOverlayPermission(Activity activity) {
        Logger.d(TAG, "Requesting overlay permission from activity...");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                
                try {
                    activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                    Logger.i(TAG, "Overlay permission request sent with result callback");
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to request overlay permission", e);
                }
            } else {
                Logger.d(TAG, "Overlay permission already granted");
            }
        }
    }
    
    /**
     * Check storage permissions
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permission for app-specific data
            return true;
        }
        
        boolean hasRead = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasWrite = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        
        Logger.d(TAG, "Storage permission status - Read: " + hasRead + ", Write: " + hasWrite);
        return hasRead && hasWrite;
    }
    
    /**
     * Request storage permissions
     */
    public static void requestStoragePermission(Activity activity) {
        Logger.d(TAG, "Requesting storage permissions...");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Logger.d(TAG, "Storage permission not needed on Android 13+");
            return;
        }
        
        if (!hasStoragePermission(activity)) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 
                REQUEST_STORAGE_PERMISSION);
        }
    }
    
    /**
     * Check all required permissions
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        boolean hasOverlay = hasOverlayPermission(context);
        boolean hasStorage = hasStoragePermission(context);
        
        Logger.d(TAG, "All permissions status - Overlay: " + hasOverlay + ", Storage: " + hasStorage);
        return hasOverlay && hasStorage;
    }
    
    /**
     * Request all missing permissions
     */
    public static void requestAllPermissions(Activity activity) {
        Logger.d(TAG, "Requesting all required permissions...");
        
        // Request storage first
        requestStoragePermission(activity);
        
        // Then request overlay (will show system dialog)
        new android.os.Handler().postDelayed(() -> {
            requestOverlayPermission(activity);
        }, 1000); // Delay to avoid overlapping dialogs
    }
    
    /**
     * Check accessibility service permission
     */
    public static boolean hasAccessibilityPermission(Context context) {
        // TODO: Implement accessibility service check if needed
        // For now, we're not using accessibility service
        return true;
    }
    
    /**
     * Open app settings
     */
    public static void openAppSettings(Context context) {
        Logger.d(TAG, "Opening app settings...");
        
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.e(TAG, "Failed to open app settings", e);
        }
    }
    
    /**
     * Handle permission result untuk overlay
     */
    public static boolean handleOverlayPermissionResult(Context context) {
        boolean hasPermission = hasOverlayPermission(context);
        Logger.d(TAG, "Overlay permission result: " + hasPermission);
        return hasPermission;
    }
    
    /**
     * Handle permission result untuk storage
     */
    public static boolean handleStoragePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            Logger.d(TAG, "Storage permission result: " + allGranted);
            return allGranted;
        }
        return false;
    }
    
    /**
     * Get permission status info
     */
    public static String getPermissionStatusInfo(Context context) {
        return String.format(
            "Overlay: %s, Storage: %s, All Required: %s",
            hasOverlayPermission(context),
            hasStoragePermission(context),
            hasAllRequiredPermissions(context)
        );
    }
    
    /**
     * Check if we should show rationale untuk storage permission
     */
    public static boolean shouldShowStorageRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    
    /**
     * Show permission rationale dialog
     */
    public static void showPermissionRationale(Activity activity, String title, String message, Runnable onPositive) {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission", (dialog, which) -> {
                if (onPositive != null) {
                    onPositive.run();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}