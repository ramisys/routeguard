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

import com.example.routeguard.R;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.ui.report.ReportFragment;

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
    private MapViewModel viewModel;

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

        // Update ViewModel when GPS moves
        locationOverlay.runOnFirstFix(() -> {
            if (viewModel != null) {
                GeoPoint loc = locationOverlay.getMyLocation();
                if (loc != null) {
                    requireActivity().runOnUiThread(() ->
                            viewModel.updateUserLocation(
                                    loc.getLatitude(), loc.getLongitude()));
                }
            }
        });

        mapView.getOverlays().add(locationOverlay);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // Observe obstacles — auto-updates map when DB changes
        viewModel.getNearbyObstacles().observe(getViewLifecycleOwner(),
                this::plotObstacles);

        // Load fresh data from network
        viewModel.refreshObstacles();
    }


    private void plotObstacles(List<Obstacle> obstacles) {
        if (obstacles == null) return;
        mapView.getOverlays().clear();
        mapView.getOverlays().add(locationOverlay);
        for (Obstacle o : obstacles) {
            addMarker(o);
        }
        mapView.invalidate();
    }

    private void addMarker(Obstacle obstacle) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(obstacle.getLat(), obstacle.getLon()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(obstacle.getType());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        marker.setSnippet("Reported at " + sdf.format(new Date(obstacle.getReportedAt())));

        marker.setOnMarkerClickListener((m, map) -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, HazardDetailFragment.newInstance(obstacle))
                    .addToBackStack(null)
                    .commit();
            return true;
        });
        mapView.getOverlays().add(marker);
    }

    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
}