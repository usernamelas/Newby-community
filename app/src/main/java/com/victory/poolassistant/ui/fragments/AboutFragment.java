package com.victory.poolassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.victory.poolassistant.BuildConfig;
import com.victory.poolassistant.R;
import com.victory.poolassistant.core.AppConfig;
import com.victory.poolassistant.core.Constants;
import com.victory.poolassistant.core.Logger;

public class AboutFragment extends Fragment {
    
    private static final String TAG = "AboutFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        setupViews(view);
        Logger.d(TAG, "AboutFragment created");
        
        return view;
    }
    
    private void setupViews(View view) {
        TextView tvAppName = view.findViewById(R.id.tv_app_name);
        TextView tvVersion = view.findViewById(R.id.tv_version);
        TextView tvBuildInfo = view.findViewById(R.id.tv_build_info);
        TextView tvDescription = view.findViewById(R.id.tv_description);
        TextView tvFeatures = view.findViewById(R.id.tv_features);
        TextView tvCopyright = view.findViewById(R.id.tv_copyright);
        
        if (tvAppName != null) {
            tvAppName.setText(Constants.APP_NAME);
        }
        
        if (tvVersion != null) {
            tvVersion.setText("Version " + AppConfig.VERSION_NAME + " (" + AppConfig.VERSION_CODE + ")");
        }
        
        if (tvBuildInfo != null) {
            String buildType = BuildConfig.DEBUG ? "Debug" : "Release";
            tvBuildInfo.setText("Build: " + buildType + " • " + BuildConfig.BUILD_TYPE);
        }
        
        if (tvDescription != null) {
            tvDescription.setText("Advanced trajectory assistant for 8 Ball Pool with professional UI and root access support.");
        }
        
        if (tvFeatures != null) {
            tvFeatures.setText("✓ Real-time game detection\n" +
                    "✓ Advanced trajectory calculation\n" +
                    "✓ Floating overlay window\n" +
                    "✓ Material Design 3 UI\n" +
                    "✓ Root access integration\n" +
                    "✓ Multi-flavor builds\n" +
                    "✓ Professional architecture");
        }
        
        if (tvCopyright != null) {
            tvCopyright.setText("© 2024 Victory Dev. All rights reserved.");
        }
    }
}