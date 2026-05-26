package com.example.routeguard.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.routeguard.R;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HazardDetailFragment extends Fragment {

    private Obstacle obstacle;

    public static HazardDetailFragment newInstance(Obstacle obstacle) {
        HazardDetailFragment fragment = new HazardDetailFragment();
        fragment.obstacle = obstacle;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hazard_detail, container, false);

        if (obstacle != null) {
            ((TextView) view.findViewById(R.id.tvHazardTitle)).setText(obstacle.getType());
            ((TextView) view.findViewById(R.id.tvDescription)).setText("Reported at " + obstacle.getLat() + ", " + obstacle.getLon());
            
            ImageView ivPhoto = view.findViewById(R.id.ivHazardPhoto);
            if (obstacle.getImageUrl() != null && !obstacle.getImageUrl().isEmpty()) {
                if (obstacle.getImageUrl().startsWith("data:image")) {
                    // It's a Base64 string
                    String base64Data = obstacle.getImageUrl().split(",")[1];
                    byte[] decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivPhoto.setImageBitmap(decodedByte);
                } else {
                    // It's a normal URL
                    Glide.with(this).load(obstacle.getImageUrl()).into(ivPhoto);
                }
            }
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btnStillThere).setOnClickListener(v -> confirmObstacle());
        view.findViewById(R.id.btnCleared).setOnClickListener(v -> clearObstacle());

        return view;
    }

    private void confirmObstacle() {
        RetrofitClient.getInstance().getApiService().confirmObstacle(obstacle.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Toast.makeText(requireContext(), "Report confirmed!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(requireContext(), "Failed to confirm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearObstacle() {
        RetrofitClient.getInstance().getApiService().clearObstacle(obstacle.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Toast.makeText(requireContext(), "Reported as cleared!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(requireContext(), "Failed to report cleared", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}