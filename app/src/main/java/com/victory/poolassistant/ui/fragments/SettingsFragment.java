package com.victory.poolassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.victory.poolassistant.R;
import com.victory.poolassistant.core.Logger;

public class SettingsFragment extends Fragment {
    
    private static final String TAG = "SettingsFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        setupViews(view);
        Logger.d(TAG, "SettingsFragment created");
        
        return view;
    }
    
    private void setupViews(View view) {
        // Setup switches and preferences
        Switch switchOverlay = view.findViewById(R.id.switch_overlay_enabled);
        Switch switchGameDetection = view.findViewById(R.id.switch_game_detection);
        Switch switchRootAccess = view.findViewById(R.id.switch_root_access);
        
        TextView tvThemeSelection = view.findViewById(R.id.tv_theme_selection);
        
        // Set up listeners
        if (switchOverlay != null) {
            switchOverlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Overlay enabled: " + isChecked);
                // TODO: Save preference
            });
        }
        
        if (switchGameDetection != null) {
            switchGameDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Game detection enabled: " + isChecked);
                // TODO: Save preference
            });
        }
        
        if (switchRootAccess != null) {
            switchRootAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Logger.d(TAG, "Root access enabled: " + isChecked);
                // TODO: Save preference
            });
        }
        
        if (tvThemeSelection != null) {
            tvThemeSelection.setOnClickListener(v -> {
                Logger.d(TAG, "Theme selection clicked");
                // TODO: Show theme selection dialog
            });
        }
    }
}