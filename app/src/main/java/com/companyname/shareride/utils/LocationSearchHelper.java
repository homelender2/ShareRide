package com.companyname.shareride.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.companyname.shareride.Ride;
import com.companyname.shareride.database.RideDAO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationSearchHelper {

    private static final String TAG = "LocationSearchHelper";
    private static final int MAX_SUGGESTIONS = 5;
    private static final int SEARCH_DELAY_MS = 500; // Delay to reduce API calls

    // Cache for recent searches
    private static Map<String, List<LocationSuggestion>> searchCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    public static class LocationSuggestion {
        public String displayName;
        public String fullAddress;
        public double latitude;
        public double longitude;
        public boolean isFromDatabase; // Flag to identify database vs geocoder results

        public LocationSuggestion(String displayName, String fullAddress, double lat, double lng) {
            this.displayName = displayName;
            this.fullAddress = fullAddress;
            this.latitude = lat;
            this.longitude = lng;
            this.isFromDatabase = false;
        }

        public LocationSuggestion(String displayName, String fullAddress, double lat, double lng, boolean isFromDatabase) {
            this.displayName = displayName;
            this.fullAddress = fullAddress;
            this.latitude = lat;
            this.longitude = lng;
            this.isFromDatabase = isFromDatabase;
        }

        @Override
        public String toString() {
            return displayName;
        }

        // Helper method to check if coordinates are valid
        public boolean hasValidCoordinates() {
            return LocationUtils.isValidCoordinate(latitude, longitude);
        }
    }

    // Listener interface for location selection
    public interface OnLocationSelectedListener {
        void onLocationSelected(LocationSuggestion location, AutoCompleteTextView view);
        void onLocationCleared(AutoCompleteTextView view);
    }

    // Enhanced setup method with listener
    public static void setupLocationAutoComplete(AutoCompleteTextView autoCompleteTextView,
                                                 Context context,
                                                 OnLocationSelectedListener listener) {
        List<LocationSuggestion> suggestions = new ArrayList<>();
        ArrayAdapter<LocationSuggestion> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, suggestions);

        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(2); // Start searching after 2 characters

        // Handler for delayed search to reduce API calls
        Handler searchHandler = new Handler(Looper.getMainLooper());
        Runnable searchRunnable = new Runnable() {
            @Override
            public void run() {
                String query = autoCompleteTextView.getText().toString().trim();
                if (query.length() >= 2) {
                    searchLocations(query, context, suggestions, adapter);
                }
            }
        };

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear previous searches
                searchHandler.removeCallbacks(searchRunnable);

                if (s.length() >= 2) {
                    // Delay search to reduce API calls
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    suggestions.clear();
                    adapter.notifyDataSetChanged();

                    // Clear stored coordinates
                    autoCompleteTextView.setTag(null);
                    if (listener != null) {
                        listener.onLocationCleared(autoCompleteTextView);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < suggestions.size()) {
                LocationSuggestion selected = suggestions.get(position);

                // Validate coordinates before storing
                if (selected.hasValidCoordinates()) {
                    // Store coordinates and full address in tag
                    String coordinatesTag = "coords:" + selected.latitude + "," + selected.longitude +
                            "|address:" + selected.fullAddress +
                            "|display:" + selected.displayName;
                    autoCompleteTextView.setTag(coordinatesTag);
                    autoCompleteTextView.setText(selected.displayName);

                    // Notify listener
                    if (listener != null) {
                        listener.onLocationSelected(selected, autoCompleteTextView);
                    }

                    Log.d(TAG, "Location selected: " + selected.displayName +
                            " (" + selected.latitude + ", " + selected.longitude + ")");
                } else {
                    Log.w(TAG, "Invalid coordinates for location: " + selected.displayName);
                }
            }
        });
    }

    // Overloaded method for backward compatibility
    public static void setupLocationAutoComplete(AutoCompleteTextView autoCompleteTextView, Context context) {
        setupLocationAutoComplete(autoCompleteTextView, context, null);
    }

    // Enhanced search with database integration and caching
    private static void searchLocations(String query, Context context,
                                        List<LocationSuggestion> suggestions,
                                        ArrayAdapter<LocationSuggestion> adapter) {

        // Check cache first
        String cacheKey = query.toLowerCase().trim();
        if (searchCache.containsKey(cacheKey)) {
            suggestions.clear();
            suggestions.addAll(searchCache.get(cacheKey));
            new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged());
            return;
        }

        new Thread(() -> {
            List<LocationSuggestion> combinedResults = new ArrayList<>();

            try {
                // 1. Search in database for previously used locations
                List<LocationSuggestion> databaseResults = searchInDatabase(query, context);
                combinedResults.addAll(databaseResults);

                // 2. Search using Geocoder
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, MAX_SUGGESTIONS);

                if (addresses != null) {
                    for (Address address : addresses) {
                        String displayName = buildDisplayName(address);
                        String fullAddress = buildFullAddress(address);

                        // Avoid duplicates from database results
                        if (!isDuplicate(combinedResults, address.getLatitude(), address.getLongitude())) {
                            combinedResults.add(new LocationSuggestion(
                                    displayName,
                                    fullAddress,
                                    address.getLatitude(),
                                    address.getLongitude(),
                                    false
                            ));
                        }
                    }
                }

                // Cache the results
                if (combinedResults.size() > 0) {
                    cacheSearchResults(cacheKey, combinedResults);
                }

                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    suggestions.clear();
                    suggestions.addAll(combinedResults);
                    adapter.notifyDataSetChanged();
                });

            } catch (IOException e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage(), e);

                // Fall back to database results only
                new Handler(Looper.getMainLooper()).post(() -> {
                    List<LocationSuggestion> databaseResults = searchInDatabase(query, context);
                    suggestions.clear();
                    suggestions.addAll(databaseResults);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    // Search in database for previously used locations
    private static List<LocationSuggestion> searchInDatabase(String query, Context context) {
        List<LocationSuggestion> dbResults = new ArrayList<>();

        try {
            RideDAO rideDAO = new RideDAO(context);
            List<Ride> rides = rideDAO.getAllActiveRides();

            String queryLower = query.toLowerCase();

            for (Ride ride : rides) {
                // Search in from addresses
                if (ride.getFromAddress() != null &&
                        ride.getFromAddress().toLowerCase().contains(queryLower) &&
                        ride.hasValidCoordinates()) {

                    if (!isDuplicate(dbResults, ride.getFromLatitude(), ride.getFromLongitude())) {
                        dbResults.add(new LocationSuggestion(
                                ride.getFromAddress(),
                                ride.getFromAddress(),
                                ride.getFromLatitude(),
                                ride.getFromLongitude(),
                                true
                        ));
                    }
                }

                // Search in to addresses
                if (ride.getToAddress() != null &&
                        ride.getToAddress().toLowerCase().contains(queryLower) &&
                        ride.hasValidCoordinates()) {

                    if (!isDuplicate(dbResults, ride.getToLatitude(), ride.getToLongitude())) {
                        dbResults.add(new LocationSuggestion(
                                ride.getToAddress(),
                                ride.getToAddress(),
                                ride.getToLatitude(),
                                ride.getToLongitude(),
                                true
                        ));
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Database search error: " + e.getMessage(), e);
        }

        return dbResults;
    }

    // Check for duplicate locations
    private static boolean isDuplicate(List<LocationSuggestion> suggestions, double lat, double lng) {
        for (LocationSuggestion suggestion : suggestions) {
            if (LocationUtils.calculateDistance(suggestion.latitude, suggestion.longitude, lat, lng) < 0.1) {
                return true; // Within 100 meters, consider duplicate
            }
        }
        return false;
    }

    // Cache management
    private static void cacheSearchResults(String query, List<LocationSuggestion> results) {
        if (searchCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries (simple FIFO, could be improved with LRU)
            String firstKey = searchCache.keySet().iterator().next();
            searchCache.remove(firstKey);
        }
        searchCache.put(query, new ArrayList<>(results));
    }

    // Clear cache (useful for memory management)
    public static void clearCache() {
        searchCache.clear();
    }

    // Extract coordinates from AutoCompleteTextView tag
    public static LocationSuggestion getSelectedLocation(AutoCompleteTextView autoCompleteTextView) {
        Object tag = autoCompleteTextView.getTag();
        if (tag != null && tag.toString().startsWith("coords:")) {
            try {
                String tagStr = tag.toString();

                // Parse coordinates
                String coordsPart = tagStr.split("\\|")[0].replace("coords:", "");
                String[] coords = coordsPart.split(",");
                double lat = Double.parseDouble(coords[0]);
                double lng = Double.parseDouble(coords[1]);

                // Parse address
                String address = "";
                String displayName = "";
                if (tagStr.contains("|address:")) {
                    address = tagStr.split("\\|address:")[1].split("\\|")[0];
                }
                if (tagStr.contains("|display:")) {
                    displayName = tagStr.split("\\|display:")[1];
                }

                return new LocationSuggestion(displayName, address, lat, lng);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing location tag: " + e.getMessage(), e);
            }
        }
        return null;
    }

    // Validate if AutoCompleteTextView has valid location selected
    public static boolean hasValidLocationSelected(AutoCompleteTextView autoCompleteTextView) {
        LocationSuggestion location = getSelectedLocation(autoCompleteTextView);
        return location != null && location.hasValidCoordinates();
    }

    private static String buildDisplayName(Address address) {
        StringBuilder name = new StringBuilder();

        if (address.getFeatureName() != null &&
                !address.getFeatureName().matches("\\d+")) { // Exclude house numbers
            name.append(address.getFeatureName()).append(", ");
        }
        if (address.getThoroughfare() != null) {
            name.append(address.getThoroughfare()).append(", ");
        }
        if (address.getLocality() != null) {
            name.append(address.getLocality()).append(", ");
        }
        if (address.getAdminArea() != null) {
            name.append(address.getAdminArea());
        }

        String result = name.toString();
        return result.replaceAll(",\\s*$", "").trim();
    }

    private static String buildFullAddress(Address address) {
        StringBuilder fullAddr = new StringBuilder();

        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            if (address.getAddressLine(i) != null) {
                fullAddr.append(address.getAddressLine(i)).append(", ");
            }
        }

        String result = fullAddr.toString();
        return result.replaceAll(",\\s*$", "").trim();
    }

    // Utility method to get distance between two AutoCompleteTextViews with selected locations
    public static String getDistanceBetweenSelections(AutoCompleteTextView from, AutoCompleteTextView to) {
        LocationSuggestion fromLocation = getSelectedLocation(from);
        LocationSuggestion toLocation = getSelectedLocation(to);

        if (fromLocation != null && toLocation != null &&
                fromLocation.hasValidCoordinates() && toLocation.hasValidCoordinates()) {

            double distance = LocationUtils.calculateDistance(
                    fromLocation.latitude, fromLocation.longitude,
                    toLocation.latitude, toLocation.longitude
            );
            return LocationUtils.formatDistance(distance);
        }
        return null;
    }
}
