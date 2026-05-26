package com.example.routeguard.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.routeguard.R;
import com.example.routeguard.data.model.Comment;
import com.example.routeguard.data.model.User;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HazardDetailFragment extends Fragment {

    private Obstacle obstacle;
    private User currentUserProfile;
    private CommentsAdapter adapter;
    private TextView tvCommentsLabel;
    private TextView tvNoComments;
    private TextView tvVerificationCount;
    private Button btnStillThere;
    private Button btnCleared;
    
    // Mock persistence for comments during the session
    private static final Map<String, List<Comment>> mockCommentsMap = new HashMap<>();
    private static final Map<String, Integer> mockVerificationCounts = new HashMap<>();
    private static final Map<String, Boolean> userHasInteracted = new HashMap<>();

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
            
            String badgeText = obstacle.getType();
            if (obstacle.getSeverity() != null) {
                badgeText += " • " + obstacle.getSeverity();
            }
            ((TextView) view.findViewById(R.id.tvTypeBadge)).setText(badgeText);

            ((TextView) view.findViewById(R.id.tvLocation)).setText(
                    String.format(java.util.Locale.getDefault(), "Lat: %.4f, Lon: %.4f", obstacle.getLat(), obstacle.getLon()));
            
            String reportedTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    obstacle.getReportedAtMillis(),
                    System.currentTimeMillis(), 
                    android.text.format.DateUtils.MINUTE_IN_MILLIS).toString();
            ((TextView) view.findViewById(R.id.tvReportedTime)).setText("Reported " + reportedTime);

            String desc = obstacle.getDescription();
            if (desc == null || desc.isEmpty()) {
                desc = "No description provided for this " + obstacle.getType().toLowerCase() + ".";
            }
            ((TextView) view.findViewById(R.id.tvDescription)).setText(desc);

            // Update reporter info
            if (obstacle.getReporterName() != null) {
                ((TextView) view.findViewById(R.id.tvReporterName)).setText(obstacle.getReporterName());
                ((TextView) view.findViewById(R.id.tvReporterStats)).setText("Verified Community Member");
            }
            
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

            tvVerificationCount = view.findViewById(R.id.tvVerificationCount);
            btnStillThere = view.findViewById(R.id.btnStillThere);
            btnCleared = view.findViewById(R.id.btnCleared);

            int count = mockVerificationCounts.getOrDefault(obstacle.getId(), obstacle.getConfirmCount());
            updateVerificationUI(count);
            
            if (Boolean.TRUE.equals(userHasInteracted.get(obstacle.getId()))) {
                disableInteraction();
            }
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
                requireActivity().getSupportFragmentManager().popBackStack());

        btnStillThere.setOnClickListener(v -> confirmObstacle());
        btnCleared.setOnClickListener(v -> clearObstacle());

        setupComments(view);
        fetchUserProfile();

        return view;
    }

    private void fetchUserProfile() {
        RetrofitClient.getInstance().getApiService().getUserProfile()
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()) {
                            currentUserProfile = response.body();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        android.util.Log.e("HazardDetail", "Failed to fetch profile: " + t.getMessage());
                    }
                });
    }

    private void updateVerificationUI(int count) {
        if (tvVerificationCount != null) {
            tvVerificationCount.setText(String.format(java.util.Locale.getDefault(), "%d confirmations", count));
        }
    }

    private void disableInteraction() {
        if (btnStillThere != null) btnStillThere.setEnabled(false);
        if (btnCleared != null) btnCleared.setEnabled(false);
        if (btnStillThere != null) btnStillThere.setAlpha(0.5f);
        if (btnCleared != null) btnCleared.setAlpha(0.5f);
    }

    private void setupComments(View view) {
        tvCommentsLabel = view.findViewById(R.id.tvCommentsLabel);
        tvNoComments = view.findViewById(R.id.tvNoComments);
        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        EditText etComment = view.findViewById(R.id.etComment);
        Button btnPost = view.findViewById(R.id.btnPost);

        adapter = new CommentsAdapter();
        rvComments.setAdapter(adapter);

        // Load comments from backend
        if (obstacle != null) {
            loadCommentsFromBackend();
        }

        btnPost.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                String userName = "Anonymous";
                
                if (currentUserProfile != null && currentUserProfile.getUsername() != null && !currentUserProfile.getUsername().isEmpty()) {
                    userName = currentUserProfile.getUsername();
                } else {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        userName = user.getDisplayName();
                        if (userName == null || userName.isEmpty()) {
                            userName = user.getEmail();
                        }
                    }
                }

                Comment newComment = new Comment(
                        UUID.randomUUID().toString(),
                        userName,
                        text,
                        System.currentTimeMillis()
                );
                
                // Post to backend
                if (obstacle != null) {
                    RetrofitClient.getInstance().getApiService()
                            .postComment(obstacle.getId(), newComment)
                            .enqueue(new Callback<Comment>() {
                                @Override
                                public void onResponse(Call<Comment> call, Response<Comment> response) {
                                    if (response.isSuccessful()) {
                                        adapter.addComment(response.body());
                                        updateCommentCount(adapter.getItemCount());
                                        etComment.setText("");
                                        rvComments.scrollToPosition(0);
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to post comment", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Comment> call, Throwable t) {
                                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });
    }

    private void loadCommentsFromBackend() {
        RetrofitClient.getInstance().getApiService()
                .getComments(obstacle.getId())
                .enqueue(new Callback<List<Comment>>() {
                    @Override
                    public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setComments(response.body());
                            updateCommentCount(response.body().size());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Comment>> call, Throwable t) {
                        // Silent fail or show indicator
                    }
                });
    }

    private void updateCommentCount(int count) {
        if (tvCommentsLabel != null) {
            tvCommentsLabel.setText(String.format(java.util.Locale.getDefault(), "COMMENTS (%d)", count));
        }
        if (tvNoComments != null) {
            tvNoComments.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void confirmObstacle() {
        if (obstacle == null) return;
        
        // Mock UI update immediately
        int currentCount = mockVerificationCounts.getOrDefault(obstacle.getId(), obstacle.getConfirmCount());
        int newCount = currentCount + 1;
        mockVerificationCounts.put(obstacle.getId(), newCount);
        userHasInteracted.put(obstacle.getId(), true);
        updateVerificationUI(newCount);
        disableInteraction();
        
        RetrofitClient.getInstance().getApiService().confirmObstacle(obstacle.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Toast.makeText(requireContext(), "Report confirmed!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        // In a real app we might revert UI, but for mock we keep it
                        android.util.Log.e("HazardDetail", "Confirm failed: " + t.getMessage());
                    }
                });
    }

    private void clearObstacle() {
        if (obstacle == null) return;

        // Mock UI update
        userHasInteracted.put(obstacle.getId(), true);
        disableInteraction();
        Toast.makeText(requireContext(), "Reported as cleared!", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getApiService().clearObstacle(obstacle.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        // Backend logic
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        android.util.Log.e("HazardDetail", "Clear failed: " + t.getMessage());
                    }
                });
    }
}