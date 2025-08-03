package com.victory.poolassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;
import com.victory.poolassistant.utils.PermissionHelper;

public class StatsFragment extends Fragment {
    
    private static final String TAG = "StatsFragment";
    
    private PermissionHelper permissionHelper;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        
        permissionHelper = new PermissionHelper(getActivity());
        setupViews(view);
        
        Logger.d(TAG, "StatsFragment created");
        return view;
    }
    
    private void setupViews(View view) {
        TextView tvPermissionStatus = view.findViewById(R.id.tv_permission_status);
        TextView tvSystemInfo = view.findViewById(R.id.tv_system_info);
        TextView tvUsageStats = view.findViewById(R.id.tv_usage_stats);
        
        if (tvPermissionStatus != null) {
            tvPermissionStatus.setText("Permission Status:\n" + permissionHelper.getPermissionStatus());
        }
        
        if (tvSystemInfo != null) {
            tvSystemInfo.setText("System Information:\n" + getSystemInfo());
        }
        
        if (tvUsageStats != null) {
            tvUsageStats.setText("Usage Statistics:\n" + getUsageStats());
        }
    }
    
    private String getSystemInfo() {
        return "Android: " + android.os.Build.VERSION.RELEASE + "\n" +
               "API Level: " + android.os.Build.VERSION.SDK_INT + "\n" +
               "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n" +
               "Architecture: " + System.getProperty("os.arch");
    }
    
    private String getUsageStats() {
        return "Sessions: 0\n" +
               "Games detected: 0\n" +
               "Overlay activations: 0\n" +
               "Last used: Never";
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats when fragment becomes visible
        if (getView() != null) {
            setupViews(getView());
        }
    }
}