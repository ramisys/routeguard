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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
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
    private AutoCompleteTextView etDestination;
    private ProgressBar pbSearch;
    private ImageView btnClearSearch;
    private ImageView btnBack;
    private ImageView ivSearchIcon;
    private View routeInfoCard;
    private View searchActionsBar;
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
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnBack = view.findViewById(R.id.btnBack);
        ivSearchIcon = view.findViewById(R.id.ivSearchIcon);
        routeInfoCard = view.findViewById(R.id.routeInfoCard);
        searchActionsBar = view.findViewById(R.id.searchActionsBar);
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

        btnClearSearch.setOnClickListener(v -> etDestination.setText(""));

        btnBack.setOnClickListener(v -> {
            etDestination.clearFocus();
            hideKeyboard();
        });

        searchActionsBar.setOnClickListener(v -> {
            etDestination.clearFocus();
            hideKeyboard();
        });

        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnBack.setVisibility(View.VISIBLE);
                ivSearchIcon.setVisibility(View.GONE);
                searchActionsBar.setVisibility(View.VISIBLE);
                if (!etDestination.getText().toString().isEmpty()) btnClearSearch.setVisibility(View.VISIBLE);
            } else {
                btnBack.setVisibility(View.GONE);
                ivSearchIcon.setVisibility(View.VISIBLE);
                btnClearSearch.setVisibility(View.GONE);
                searchActionsBar.setVisibility(View.GONE);
            }
        });

        etDestination.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(!s.toString().isEmpty() && etDestination.hasFocus() ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        setupAutocomplete();

        view.findViewById(R.id.btnStartNavigation).setOnClickListener(v -> {
            if (!isNavigating) {
                startNavigation();
            } else {
                stopNavigation();
            }
        });

        view.findViewById(R.id.fabMyLocation).setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(locationOverlay.getMyLocation());
                mapView.getController().setZoom(17.0);
            } else {
                Toast.makeText(getContext(), "Finding location...", Toast.LENGTH_SHORT).show();
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

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etDestination.getWindowToken(), 0);
    }

    private void setupAutocomplete() {
        PlaceAdapter adapter = new PlaceAdapter(requireContext(), R.layout.item_place_suggestion);
        etDestination.setAdapter(adapter);
        
        // Ensure dropdown is styled for dark mode
        etDestination.setDropDownBackgroundResource(R.color.bg_card);
        
        // Make dropdown wider and longer
        etDestination.setDropDownWidth(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        // Set height to a significant portion of the screen (e.g., 60% of height)
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        etDestination.setDropDownHeight((int) (metrics.heightPixels * 0.7));

        etDestination.setOnItemClickListener((parent, view, position, id) -> {
            Address selected = adapter.getItem(position);
            if (selected != null) {
                GeoPoint target = new GeoPoint(selected.getLatitude(), selected.getLongitude());
                String name = selected.getFeatureName();
                if (name == null) name = selected.getAddressLine(0);
                showDestinationOnMap(target, name);
            }
        });
    }

    private static class PlaceAdapter extends ArrayAdapter<Address> {
        private final Geocoder geocoder;
        private List<Address> results = new ArrayList<>();
        private String currentQuery = "";

        public PlaceAdapter(@NonNull android.content.Context context, int resource) {
            super(context, resource);
            geocoder = new Geocoder(context, Locale.getDefault());
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Nullable
        @Override
        public Address getItem(int position) {
            return results.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place_suggestion, parent, false);
            }

            Address address = getItem(position);
            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvSubtitle = convertView.findViewById(R.id.tvSubtitle);

            if (address != null) {
                String fullLine = address.getAddressLine(0);
                String[] parts = fullLine.split(",", 2);
                
                String title = parts[0].trim();
                String subtitle = parts.length > 1 ? parts[1].trim() : "";

                // Highlight matching text in title
                android.text.SpannableString spannableTitle = new android.text.SpannableString(title);
                if (!currentQuery.isEmpty()) {
                    int startIdx = title.toLowerCase().indexOf(currentQuery.toLowerCase());
                    if (startIdx != -1) {
                        int endIdx = startIdx + currentQuery.length();
                        spannableTitle.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 
                                startIdx, Math.min(endIdx, title.length()), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                tvTitle.setText(spannableTitle);

                if (!subtitle.isEmpty()) {
                    tvSubtitle.setText(subtitle);
                    tvSubtitle.setVisibility(View.VISIBLE);
                } else {
                    tvSubtitle.setVisibility(View.GONE);
                }
            }
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null && constraint.length() >= 2) {
                        currentQuery = constraint.toString();
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(currentQuery, 10);
                            if (addresses != null) {
                                filterResults.values = addresses;
                                filterResults.count = addresses.size();
                            }
                        } catch (IOException e) {
                            android.util.Log.e("PlaceAdapter", "Geocoder error", e);
                        }
                    }
                    return filterResults;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults filterResults) {
                    if (filterResults != null && filterResults.count > 0) {
                        results = (List<Address>) filterResults.values;
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    if (resultValue instanceof Address) {
                        return ((Address) resultValue).getAddressLine(0);
                    }
                    return super.convertResultToString(resultValue);
                }
            };
        }
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
            hm.setIcon(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), R.drawable.ic_warning, null));
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