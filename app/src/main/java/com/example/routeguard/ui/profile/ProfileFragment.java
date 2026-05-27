package com.example.routeguard.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.routeguard.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        View mainBottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        if (mainBottomNav != null) mainBottomNav.setVisibility(View.GONE);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            TextView tvName = view.findViewById(R.id.tvProfileName);
            TextView tvInitials = view.findViewById(R.id.tvProfileInitials);
            TextView tvPoints = view.findViewById(R.id.tvSafetyPoints);
            
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = user.getEmail();
            }
            
            tvName.setText(name);
            if (name != null && !name.isEmpty()) {
                tvInitials.setText(name.substring(0, Math.min(2, name.length())).toUpperCase());
            }

            // Fetch extra info from our backend
            com.example.routeguard.network.RetrofitClient.getInstance()
                    .getApiService()
                    .getUserProfile()
                    .enqueue(new retrofit2.Callback<com.example.routeguard.data.model.User>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.example.routeguard.data.model.User> call,
                                               retrofit2.Response<com.example.routeguard.data.model.User> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                tvPoints.setText(String.valueOf(response.body().getReputationPoints()));
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.example.routeguard.data.model.User> call, Throwable t) {
                            // Fallback
                        }
                    });
        }

        return view;
    }
}