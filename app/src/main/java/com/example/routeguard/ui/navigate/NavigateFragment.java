package com.example.routeguard.ui.navigate;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.routeguard.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NavigateFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private EditText etDestination;
    private ProgressBar pbSearch;
    private View routeInfoCard;
    private TextView tvRouteSummary;
    private TextView tvObstacleInfo;
    private ImageView btnGo;
    private boolean isNavigating = false;
    private final Handler rerouteHandler = new Handler(Looper.getMainLooper());
    private GeoPoint lastDestination;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_navigate, container, false);

        View mainBottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        if (mainBottomNav != null) mainBottomNav.setVisibility(View.VISIBLE);

        mapView = view.findViewById(R.id.mapViewNav);
        etDestination = view.findViewById(R.id.etDestination);
        pbSearch = view.findViewById(R.id.pbSearch);
        routeInfoCard = view.findViewById(R.id.routeInfoCard);
        tvRouteSummary = view.findViewById(R.id.tvRouteSummary);
        tvObstacleInfo = view.findViewById(R.id.tvObstacleInfo);
        btnGo = view.findViewById(R.id.btnGo);

        initMap();

        etDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etDestination.getText().toString());
                return true;
            }
            return false;
        });

        btnGo.setOnClickListener(v -> performSearch(etDestination.getText().toString()));

        view.findViewById(R.id.btnStartNavigation).setOnClickListener(v -> {
            if (!isNavigating) {
                startNavigation();
            } else {
                stopNavigation();
            }
        });

        return view;
    }

    private void startNavigation() {
        isNavigating = true;
        Toast.makeText(requireContext(), "Navigation started", Toast.LENGTH_SHORT).show();
        startRerouteCheck();
    }

    private void stopNavigation() {
        isNavigating = false;
        rerouteHandler.removeCallbacksAndMessages(null);
        Toast.makeText(requireContext(), "Navigation stopped", Toast.LENGTH_SHORT).show();
    }

    private void startRerouteCheck() {
        rerouteHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNavigating && lastDestination != null) {
                    calculateSafeRoute(lastDestination);
                    rerouteHandler.postDelayed(this, 30000); // Check every 30 seconds
                }
            }
        }, 30000);
    }

    private void initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(11.2543, 124.9999));

        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        mapView.getOverlays().add(locationOverlay);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;

        pbSearch.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                new Handler(Looper.getMainLooper()).post(() -> {
                    pbSearch.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        GeoPoint target = new GeoPoint(address.getLatitude(), address.getLongitude());
                        showDestinationOnMap(target, query);
                    } else {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    pbSearch.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showDestinationOnMap(GeoPoint target, String name) {
        lastDestination = target;
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker && !(overlay instanceof MyLocationNewOverlay));
        
        Marker marker = new Marker(mapView);
        marker.setPosition(target);
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        
        mapView.getController().animateTo(target);
        
        calculateSafeRoute(target);
    }

    private void calculateSafeRoute(GeoPoint target) {
        GeoPoint start = locationOverlay.getMyLocation();
        if (start == null) {
            // Fallback to a default location if GPS not ready
            start = new GeoPoint(11.2543, 124.9999);
        }

        String startStr = start.getLongitude() + "," + start.getLatitude();
        String endStr = target.getLongitude() + "," + target.getLatitude();

        pbSearch.setVisibility(View.VISIBLE);

        com.example.routeguard.network.RetrofitClient.getInstance()
                .getApiService()
                .getSafeRoutes(startStr, endStr)
                .enqueue(new retrofit2.Callback<com.example.routeguard.network.NavigationResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.routeguard.network.NavigationResponse> call,
                                           retrofit2.Response<com.example.routeguard.network.NavigationResponse> response) {
                        pbSearch.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && !response.body().routes.isEmpty()) {
                            // Find the recommended route
                            com.example.routeguard.network.NavigationResponse.SafeRoute recommended = null;
                            for (com.example.routeguard.network.NavigationResponse.SafeRoute r : response.body().routes) {
                                if (r.isRecommended) {
                                    recommended = r;
                                    break;
                                }
                            }
                            if (recommended == null) recommended = response.body().routes.get(0);
                            
                            drawSafeRoute(recommended);
                            showSafeRouteInfo(recommended);
                        } else {
                            Toast.makeText(requireContext(), "No safe routes found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.routeguard.network.NavigationResponse> call, Throwable t) {
                        pbSearch.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Navigation API error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSafeRouteInfo(com.example.routeguard.network.NavigationResponse.SafeRoute route) {
        if (routeInfoCard != null) {
            routeInfoCard.setVisibility(View.VISIBLE);
            
            int minutes = (int) (route.duration / 60);
            double distanceKm = route.distance / 1000.0;
            
            String summary = String.format(Locale.getDefault(), 
                    "Safe Route: %d mins (%.1f km)\nSafety Score: %d%%", 
                    minutes, distanceKm, route.safetyScore);
            tvRouteSummary.setText(summary);
            
            if (route.hazards.isEmpty()) {
                tvObstacleInfo.setText("No hazards detected on this route.");
                tvObstacleInfo.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            } else {
                tvObstacleInfo.setText(String.format(Locale.getDefault(), 
                        "Warning: %d hazards nearby. Extra caution advised.", route.hazards.size()));
                tvObstacleInfo.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, null));
            }
        }
    }

    private void drawSafeRoute(com.example.routeguard.network.NavigationResponse.SafeRoute route) {
        Polyline polyline = new Polyline();
        List<GeoPoint> points = new ArrayList<>();
        
        for (List<Double> coord : route.geometry.coordinates) {
            // GeoJSON is [lng, lat]
            points.add(new GeoPoint(coord.get(1), coord.get(0)));
        }
        
        polyline.setPoints(points);
        
        // Color based on safety score
        int color;
        if (route.safetyScore > 80) {
            color = getResources().getColor(R.color.accent_teal, null); // Safe
        } else if (route.safetyScore > 50) {
            color = getResources().getColor(android.R.color.holo_orange_light, null); // Caution
        } else {
            color = getResources().getColor(android.R.color.holo_red_light, null); // Risky
        }
        
        polyline.setColor(color);
        polyline.setWidth(14f);
        
        mapView.getOverlays().removeIf(o -> o instanceof Polyline);
        mapView.getOverlays().add(polyline);
        
        // Add hazard markers
        for (com.example.routeguard.network.NavigationResponse.Hazard h : route.hazards) {
            Marker hm = new Marker(mapView);
            hm.setPosition(new GeoPoint(h.coordinates.get(1), h.coordinates.get(0)));
            hm.setTitle(h.type + " (" + h.severity + ")");
            hm.setSubDescription("Hazard on or near your route");
            hm.setIcon(getResources().getDrawable(R.drawable.ic_warning, null));
            mapView.getOverlays().add(hm);
        }

        mapView.invalidate();
    }

    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    public void onPause() { 
        super.onPause(); 
        mapView.onPause(); 
        rerouteHandler.removeCallbacksAndMessages(null);
    }
}