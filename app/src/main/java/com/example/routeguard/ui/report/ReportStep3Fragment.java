package com.example.routeguard.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.routeguard.R;

public class ReportStep3Fragment extends Fragment {

    private ReportViewModel viewModel;
    private View loadingOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_report_step3, container, false);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ReportViewModel.class);

        TextView tvSummaryType  = view.findViewById(R.id.tvSummaryType);
        TextView tvSummaryMedia = view.findViewById(R.id.tvSummaryMedia);
        TextView tvLocation     = view.findViewById(R.id.tvLocation);
        EditText etDescription  = view.findViewById(R.id.etDescription);
        loadingOverlay          = view.findViewById(R.id.loadingOverlay);

        // Populate summary from ViewModel
        tvSummaryType.setText(viewModel.selectedType.getValue() != null
                ? viewModel.selectedType.getValue() : "Not selected");
        tvSummaryMedia.setText(viewModel.mediaUri.getValue() != null
                ? "Photo attached" : "No media");

        // Display exact location address
        if (viewModel.latitude.getValue() != null && viewModel.longitude.getValue() != null) {
            updateLocationText(tvLocation, viewModel.latitude.getValue(), viewModel.longitude.getValue());
        }

        // Observe loading state
        viewModel.isSubmitting.observe(getViewLifecycleOwner(), isSubmitting -> {
            if (isSubmitting != null) {
                loadingOverlay.setVisibility(isSubmitting ? View.VISIBLE : View.GONE);
            }
        });

        // Observe submission result
        viewModel.submitSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                showSuccessDialog();
            }
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error,
                        Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnSubmitReport).setOnClickListener(v -> {
            String desc = etDescription.getText().toString().trim();
            if (desc.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please add a description",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.setDescription(desc);
            viewModel.submitReport();
        });

        return view;
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Report Submitted")
                .setMessage("Your hazard report has been successfully uploaded and is now visible to other users on the map.")
                .setPositiveButton("Finish", (dialog, which) -> {
                    // Navigate to Success Fragment or clear stack
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, new ReportSuccessFragment())
                            .commit();
                })
                .setCancelable(false)
                .show();
    }

    private void updateLocationText(TextView textView, double lat, double lon) {
        Geocoder geocoder = new Geocoder(requireContext(), java.util.Locale.getDefault());
        try {
            java.util.List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressStr = address.getAddressLine(0);
                textView.setText(addressStr);
            } else {
                textView.setText(String.format(java.util.Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
            }
        } catch (java.io.IOException e) {
            textView.setText(String.format(java.util.Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
        }
    }
}
