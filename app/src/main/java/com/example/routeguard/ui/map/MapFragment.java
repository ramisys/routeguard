package com.example.routeguard.ui.map;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.widget.PopupMenu;

import com.example.routeguard.R;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.ui.report.ReportFragment;
import com.example.routeguard.ui.profile.ProfileFragment;
import com.example.routeguard.ui.settings.SettingsFragment;
// HazardDetailFragment is in the same package, no import needed

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private org.osmdroid.views.overlay.FolderOverlay hazardOverlay;
    private MapViewModel viewModel;
    
    // Debouncing for map refresh
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(
                requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        
        // Initialize overlays
        hazardOverlay = new org.osmdroid.views.overlay.FolderOverlay();
        
        View mainBottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        if (mainBottomNav != null) mainBottomNav.setVisibility(View.VISIBLE);
        
        view.findViewById(R.id.ivProfile).setOnClickListener(this::showProfileMenu);
        
        view.findViewById(R.id.fabMyLocation).setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(locationOverlay.getMyLocation());
                mapView.getController().setZoom(17.0);
            } else {
                android.widget.Toast.makeText(getContext(), "Finding location...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        initMap();
        initViewModel();

        return view;
    }

    private void initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(
                new GeoPoint(11.2543, 124.9999));

        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        mapView.getOverlays().add(hazardOverlay);
        mapView.getOverlays().add(locationOverlay);

        // Fetch initial data for the current center
        updateViewModelLocation();

        // Dynamic refresh when map is moved
        mapView.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                updateViewModelLocation();
                return false;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                updateViewModelLocation();
                return false;
            }
        });
    }

    private void updateViewModelLocation() {
        if (viewModel != null && mapView != null) {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            
            // Debounce: Cancel pending refresh and schedule a new one
            if (refreshRunnable != null) {
                refreshHandler.removeCallbacks(refreshRunnable);
            }
            
            refreshRunnable = () -> {
                android.util.Log.d("MapFragment", "Debounced refresh. New center: " + center.getLatitude() + ", " + center.getLongitude());
                viewModel.updateUserLocation(center.getLatitude(), center.getLongitude());
            };
            
            refreshHandler.postDelayed(refreshRunnable, 800); // Wait 800ms after last movement
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // Observe obstacles — auto-updates map when DB changes
        viewModel.getNearbyObstacles().observe(getViewLifecycleOwner(),
                this::plotObstacles);

        // Load fresh data from network
        viewModel.refreshObstacles();
    }

    private void showProfileMenu(View v) {
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_profile_dropdown, null);
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupView.findViewById(R.id.menu_profile).setOnClickListener(view -> {
            popupWindow.dismiss();
            navigateTo(new ProfileFragment());
        });

        popupView.findViewById(R.id.menu_settings).setOnClickListener(view -> {
            popupWindow.dismiss();
            navigateTo(new SettingsFragment());
        });

        popupWindow.setElevation(10);
        popupWindow.showAsDropDown(v, 0, 10);
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }


    private void plotObstacles(List<Obstacle> obstacles) {
        if (obstacles == null || hazardOverlay == null) return;
        
        android.util.Log.d("RouteGuard", "Map showing " + obstacles.size() + " total hazards from DB");
        
        hazardOverlay.getItems().clear();
        
        for (Obstacle o : obstacles) {
            if (o.getLat() != 0 && o.getLon() != 0) {
                addMarker(o);
            }
        }
        mapView.invalidate();
    }

    private void addMarker(Obstacle obstacle) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(obstacle.getLat(), obstacle.getLon()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        // BETTER VISUALS: Create a colored circle icon based on severity
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        
        int color = android.graphics.Color.YELLOW; // Default
        if ("HIGH".equalsIgnoreCase(obstacle.getSeverity())) {
            color = android.graphics.Color.RED;
        } else if ("MODERATE".equalsIgnoreCase(obstacle.getSeverity())) {
            color = android.graphics.Color.rgb(255, 165, 0); // Orange
        }
        
        shape.setColor(color);
        shape.setStroke(4, android.graphics.Color.WHITE);
        shape.setSize(60, 60);
        
        marker.setIcon(shape);
        marker.setTitle(obstacle.getType());
        marker.setSubDescription("Severity: " + obstacle.getSeverity());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        marker.setSnippet(obstacle.getDescription() != null ? obstacle.getDescription() : "Hazard reported at " + sdf.format(new Date(obstacle.getReportedAtMillis())));

        marker.setOnMarkerClickListener((m, map) -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, HazardDetailFragment.newInstance(obstacle))
                    .addToBackStack(null)
                    .commit();
            return true;
        });
        
        hazardOverlay.add(marker);
    }

    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
}