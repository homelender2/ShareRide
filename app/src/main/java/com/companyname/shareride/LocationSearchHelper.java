package com.companyname.shareride;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationSearchHelper {

    public static class LocationSuggestion {
        public String displayName;
        public String fullAddress;
        public double latitude;
        public double longitude;

        public LocationSuggestion(String displayName, String fullAddress, double lat, double lng) {
            this.displayName = displayName;
            this.fullAddress = fullAddress;
            this.latitude = lat;
            this.longitude = lng;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static void setupLocationAutoComplete(AutoCompleteTextView autoCompleteTextView, Context context) {
        List<LocationSuggestion> suggestions = new ArrayList<>();
        ArrayAdapter<LocationSuggestion> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, suggestions);

        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(3); // Start searching after 3 characters

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    searchLocations(s.toString(), context, suggestions, adapter);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            LocationSuggestion selected = suggestions.get(position);
            // Store coordinates in tag
            autoCompleteTextView.setTag("coords:" + selected.latitude + "," + selected.longitude);
            autoCompleteTextView.setText(selected.displayName);
        });
    }

    private static void searchLocations(String query, Context context,
                                        List<LocationSuggestion> suggestions,
                                        ArrayAdapter<LocationSuggestion> adapter) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 5);

                suggestions.clear();
                if (addresses != null) {
                    for (Address address : addresses) {
                        String displayName = buildDisplayName(address);
                        String fullAddress = buildFullAddress(address);

                        suggestions.add(new LocationSuggestion(
                                displayName,
                                fullAddress,
                                address.getLatitude(),
                                address.getLongitude()
                        ));
                    }
                }

                // Update UI on main thread
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String buildDisplayName(Address address) {
        StringBuilder name = new StringBuilder();

        if (address.getFeatureName() != null) {
            name.append(address.getFeatureName()).append(", ");
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
}