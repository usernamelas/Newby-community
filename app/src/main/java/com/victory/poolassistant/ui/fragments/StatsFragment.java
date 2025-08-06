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

    private TextView tvPermissionStatus;
    private TextView tvSystemInfo;
    private TextView tvUsageStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        tvPermissionStatus = view.findViewById(R.id.tv_permission_status);
        tvSystemInfo = view.findViewById(R.id.tv_system_info);
        tvUsageStats = view.findViewById(R.id.tv_usage_stats);

        // Update views
        updatePermissionStatus();
        updateSystemInfo();
        updateUsageStats();

        Logger.d(TAG, "StatsFragment created");
    }

    private void updatePermissionStatus() {
        if (getActivity() != null && tvPermissionStatus != null) {
            String status = PermissionHelper.getPermissionStatusInfo(getActivity());
            tvPermissionStatus.setText("Permission Status:\n" + status);
        }
    }

    private void updateSystemInfo() {
        if (tvSystemInfo != null) {
            tvSystemInfo.setText("System Information:\n" + getSystemInfo());
        }
    }

    private void updateUsageStats() {
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
        updatePermissionStatus();
        updateSystemInfo();
        updateUsageStats();
    }
}