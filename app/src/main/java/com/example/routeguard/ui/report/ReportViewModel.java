package com.example.routeguard.ui.report;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.data.repository.ObstacleRepository;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.Map;
import java.util.UUID;

public class ReportViewModel extends AndroidViewModel {

    private final ObstacleRepository repository;

    // Holds state across all 3 steps
    public final MutableLiveData<String> selectedType     = new MutableLiveData<>();
    public final MutableLiveData<String> selectedSeverity = new MutableLiveData<>();
    public final MutableLiveData<Uri>    mediaUri         = new MutableLiveData<>();
    public final MutableLiveData<String> description      = new MutableLiveData<>();
    public final MutableLiveData<Double> latitude         = new MutableLiveData<>();
    public final MutableLiveData<Double> longitude        = new MutableLiveData<>();

    // UI state
    public final MutableLiveData<Boolean> isSubmitting  = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> submitSuccess = new MutableLiveData<>(false);
    public final MutableLiveData<String>  errorMessage  = new MutableLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ObstacleRepository(application);
        // Default location — will be replaced by real GPS
        latitude.setValue(11.2543);
        longitude.setValue(124.9999);
    }

    public void setType(String type) {
        selectedType.setValue(type);
    }

    public void setSeverity(String severity) {
        selectedSeverity.setValue(severity);
    }

    public void setMediaUri(Uri uri) {
        mediaUri.setValue(uri);
    }

    public void setDescription(String desc) {
        description.setValue(desc);
    }

    public void setLocation(double lat, double lon) {
        latitude.setValue(lat);
        longitude.setValue(lon);
    }

    public void submitReport() {
        String type = selectedType.getValue();
        if (type == null || type.isEmpty()) {
            errorMessage.setValue("Please select a hazard type");
            return;
        }

        isSubmitting.setValue(true);

        if (mediaUri.getValue() != null) {
            uploadImageAndSubmit(mediaUri.getValue());
        } else {
            finalizeSubmission(null);
        }
    }

    private void uploadImageAndSubmit(Uri uri) {
        // Use Cloudinary for Image Upload
        MediaManager.get().upload(uri)
                .option("folder", "obstacles")
                .option("unsigned", true)
                .option("upload_preset", "mzmd1way") // Update with your actual Unsigned Preset Name
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        finalizeSubmission(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        isSubmitting.setValue(false);
                        errorMessage.setValue("Cloudinary upload failed: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch();
    }

    private void finalizeSubmission(String imageUrl) {
        Obstacle report = new Obstacle();
        report.setId(String.valueOf(System.currentTimeMillis()));
        report.setType(selectedType.getValue());
        report.setSeverity(selectedSeverity.getValue() != null ? selectedSeverity.getValue() : "MODERATE");
        report.setLat(latitude.getValue() != null ? latitude.getValue() : 11.2543);
        report.setLon(longitude.getValue() != null ? longitude.getValue() : 124.9999);
        report.setImageUrl(imageUrl);
        report.setActive(true);
        report.setReportedAt(System.currentTimeMillis());
        report.setExpiresAt(System.currentTimeMillis() + (4 * 60 * 60 * 1000));

        // Save locally
        repository.insert(report);

        // Send to backend
        com.example.routeguard.network.RetrofitClient.getInstance()
                .getApiService()
                .submitReport(report)
                .enqueue(new retrofit2.Callback<Obstacle>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<Obstacle> call, @NonNull retrofit2.Response<Obstacle> response) {
                        isSubmitting.setValue(false);
                        submitSuccess.setValue(true);
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<Obstacle> call, @NonNull Throwable t) {
                        isSubmitting.setValue(false);
                        submitSuccess.setValue(true); // Still success because saved locally
                    }
                });
    }

    public void reset() {
        selectedType.setValue(null);
        selectedSeverity.setValue(null);
        mediaUri.setValue(null);
        description.setValue(null);
        isSubmitting.setValue(false);
        submitSuccess.setValue(false);
        errorMessage.setValue(null);
    }
}