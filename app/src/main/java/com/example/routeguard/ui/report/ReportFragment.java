package com.example.routeguard.ui.report;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.routeguard.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class ReportFragment extends Fragment {

    private ReportViewModel viewModel;
    private View selectedCard = null;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_report_step1, container, false);

        // Share ViewModel across all 3 steps using Activity scope
        viewModel = new ViewModelProvider(requireActivity())
                .get(ReportViewModel.class);
        viewModel.reset();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        getCurrentLocation();

        View btnFlood        = view.findViewById(R.id.btnFlood);
        View btnConstruction = view.findViewById(R.id.btnConstruction);
        View btnAccident     = view.findViewById(R.id.btnAccident);
        View btnDebris       = view.findViewById(R.id.btnDebris);
        View btnOthers       = view.findViewById(R.id.btnOthers);
        Button btnContinue   = view.findViewById(R.id.btnContinue);

        View.OnClickListener typeListener = v -> {
            if (selectedCard != null) {
                selectedCard.setBackground(AppCompatResources.getDrawable(
                        requireContext(), R.drawable.bg_hazard_card));
            }
            v.setBackground(AppCompatResources.getDrawable(
                    requireContext(), R.drawable.bg_hazard_card_selected));
            selectedCard = v;

            if (v.getId() == R.id.btnFlood)        viewModel.setType("FLOOD");
            if (v.getId() == R.id.btnConstruction) viewModel.setType("CONSTRUCTION");
            if (v.getId() == R.id.btnAccident)     viewModel.setType("ACCIDENT");
            if (v.getId() == R.id.btnDebris)       viewModel.setType("DEBRIS");
            if (v.getId() == R.id.btnOthers)       viewModel.setType("OTHER");
        };

        btnFlood.setOnClickListener(typeListener);
        btnConstruction.setOnClickListener(typeListener);
        btnAccident.setOnClickListener(typeListener);
        btnDebris.setOnClickListener(typeListener);
        btnOthers.setOnClickListener(typeListener);

        btnContinue.setOnClickListener(v -> {
            if (viewModel.selectedType.getValue() == null) {
                Toast.makeText(requireContext(),
                        "Please select a hazard type",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ReportStep2Fragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        viewModel.setLocation(location.getLatitude(), location.getLongitude());
                    }
                });
    }
}