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
import android.widget.EditText;
import android.widget.ProgressBar;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_navigate, container, false);

        mapView = view.findViewById(R.id.mapViewNav);
        etDestination = view.findViewById(R.id.etDestination);
        pbSearch = view.findViewById(R.id.pbSearch);
        routeInfoCard = view.findViewById(R.id.routeInfoCard);

        initMap();

        etDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etDestination.getText().toString());
                return true;
            }
            return false;
        });

        return view;
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
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker && !(overlay instanceof MyLocationNewOverlay));
        
        Marker marker = new Marker(mapView);
        marker.setPosition(target);
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        
        mapView.getController().animateTo(target);
        
        calculateMockRoute(target);
    }

    private void calculateMockRoute(GeoPoint target) {
        GeoPoint start = locationOverlay.getMyLocation();
        if (start == null) start = new GeoPoint(11.2543, 124.9999);

        String coords = start.getLongitude() + "," + start.getLatitude() + ";" +
                       target.getLongitude() + "," + target.getLatitude();

        com.example.routeguard.network.RetrofitClient.getInstance()
                .getRoutingService()
                .getRoute(coords, "full", "polyline", false)
                .enqueue(new retrofit2.Callback<com.example.routeguard.network.RoutingService.OsrmResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.routeguard.network.RoutingService.OsrmResponse> call,
                                           retrofit2.Response<com.example.routeguard.network.RoutingService.OsrmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().routes.isEmpty()) {
                            drawRoute(response.body().routes.get(0).geometry);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.routeguard.network.RoutingService.OsrmResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Routing failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawRoute(String encodedPolyline) {
        Polyline polyline = new Polyline();
        polyline.setPoints(decodePolyline(encodedPolyline));
        polyline.setColor(getResources().getColor(R.color.accent_teal, null));
        polyline.setWidth(12f);
        
        mapView.getOverlays().removeIf(o -> o instanceof Polyline);
        mapView.getOverlays().add(polyline);
        mapView.invalidate();
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new GeoPoint(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
}