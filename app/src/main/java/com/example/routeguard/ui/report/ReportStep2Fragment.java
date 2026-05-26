package com.example.routeguard.ui.report;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.routeguard.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportStep2Fragment extends Fragment {

    private static final int REQUEST_CAMERA  = 101;
    private static final int REQUEST_GALLERY = 102;

    private ReportViewModel viewModel;
    private ImageView ivPreview;
    private View previewContainer;
    private Uri photoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_report_step2, container, false);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ReportViewModel.class);

        ivPreview        = view.findViewById(R.id.ivMediaPreview);
        previewContainer = view.findViewById(R.id.mediaPreviewContainer);

        // Restore preview if coming back
        if (viewModel.mediaUri.getValue() != null) {
            ivPreview.setImageURI(viewModel.mediaUri.getValue());
            previewContainer.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.btnTakePhoto).setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Toast.makeText(requireContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(requireContext(),
                            "com.routeguard.app.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_CAMERA);
                }
            }
        });

        view.findViewById(R.id.btnUploadGallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY);
        });

        view.findViewById(R.id.btnRemoveMedia).setOnClickListener(v -> {
            viewModel.setMediaUri(null);
            previewContainer.setVisibility(View.GONE);
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnContinue).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ReportStep3Fragment())
                        .addToBackStack(null)
                        .commit());

        return view;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    viewModel.setMediaUri(uri);
                    ivPreview.setImageURI(uri);
                    previewContainer.setVisibility(View.VISIBLE);
                }
            } else if (requestCode == REQUEST_CAMERA) {
                if (photoUri != null) {
                    viewModel.setMediaUri(photoUri);
                    ivPreview.setImageURI(photoUri);
                    previewContainer.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
