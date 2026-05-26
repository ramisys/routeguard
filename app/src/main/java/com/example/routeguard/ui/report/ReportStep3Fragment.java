package com.example.routeguard.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.routeguard.R;

public class ReportStep3Fragment extends Fragment {

    private ReportViewModel viewModel;

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
        EditText etDescription  = view.findViewById(R.id.etDescription);

        // Populate summary from ViewModel
        tvSummaryType.setText(viewModel.selectedType.getValue() != null
                ? viewModel.selectedType.getValue() : "Not selected");
        tvSummaryMedia.setText(viewModel.mediaUri.getValue() != null
                ? "Photo attached" : "No media");

        // Observe submission result
        viewModel.submitSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer,
                                new ReportSuccessFragment())
                        .commit();
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
}