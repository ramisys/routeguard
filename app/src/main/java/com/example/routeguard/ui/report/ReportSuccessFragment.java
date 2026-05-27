package com.example.routeguard.ui.report;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.routeguard.R;
import com.example.routeguard.ui.map.MapFragment;

public class ReportSuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_report_submitted, container, false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            
            if (getActivity() instanceof com.example.routeguard.MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                    getActivity().findViewById(R.id.bottomNavigation);
                if (nav != null) {
                    nav.setVisibility(View.VISIBLE);
                    nav.setSelectedItemId(R.id.nav_map);
                    return;
                }
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new MapFragment())
                    .commit();
        }, 3000);

        return view;
    }
}